package com.guideconnect.service;

import com.guideconnect.model.AccountStatus;
import com.guideconnect.model.BookingStatus;
import com.guideconnect.model.Dispute;
import com.guideconnect.model.DisputeStatus;
import com.guideconnect.model.User;
import com.guideconnect.repository.BookingRepository;
import com.guideconnect.repository.DisputeRepository;
import com.guideconnect.repository.TourListingRepository;
import com.guideconnect.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service layer for administrative operations and dashboard aggregation.
 * Provides consolidated metrics for the admin dashboard, dispute resolution
 * workflows, and user management capabilities.
 */
@Service
public class AdminService {

    private final UserRepository userRepository;
    private final TourListingRepository tourListingRepository;
    private final BookingRepository bookingRepository;
    private final DisputeRepository disputeRepository;
    private final TransactionService transactionService;

    /**
     * Constructs an AdminService with required dependencies.
     *
     * @param userRepository        the user repository for user metrics
     * @param tourListingRepository the tour listing repository for tour metrics
     * @param bookingRepository     the booking repository for booking metrics
     * @param disputeRepository     the dispute repository for dispute management
     * @param transactionService    the transaction service for financial metrics
     */
    public AdminService(UserRepository userRepository,
                        TourListingRepository tourListingRepository,
                        BookingRepository bookingRepository,
                        DisputeRepository disputeRepository,
                        TransactionService transactionService) {
        this.userRepository = userRepository;
        this.tourListingRepository = tourListingRepository;
        this.bookingRepository = bookingRepository;
        this.disputeRepository = disputeRepository;
        this.transactionService = transactionService;
    }

    /**
     * Aggregates key platform metrics for the admin dashboard.
     * Returns a map containing total users, total tours, total bookings,
     * bookings broken down by status, total revenue, and total commission.
     *
     * @return a map of dashboard metric names to their values
     */
    public Map<String, Object> getDashboardMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        metrics.put("totalUsers", userRepository.count());
        metrics.put("totalTours", tourListingRepository.countByActiveTrue());
        metrics.put("totalBookings", bookingRepository.count());

        Map<String, Long> bookingsByStatus = new HashMap<>();
        for (BookingStatus status : BookingStatus.values()) {
            bookingsByStatus.put(status.name(), bookingRepository.countByStatus(status));
        }
        metrics.put("bookingsByStatus", bookingsByStatus);

        metrics.put("totalRevenue", transactionService.getTotalRevenue());
        metrics.put("totalCommission", transactionService.getTotalCommission());

        return metrics;
    }

    /**
     * Retrieves all disputes with pagination support, ordered for admin review.
     *
     * @param pageable pagination and sorting parameters
     * @return a page of all disputes
     */
    public Page<Dispute> getAllDisputes(Pageable pageable) {
        return disputeRepository.findAll(pageable);
    }

    /**
     * Retrieves disputes that still require admin attention.
     * Includes OPEN, UNDER_REVIEW, and ESCALATED statuses.
     *
     * @param limit the maximum number of active disputes to return
     * @return list of active disputes ordered by creation date descending
     */
    public List<Dispute> getActiveDisputes(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return disputeRepository.findByStatusIn(
                List.of(DisputeStatus.OPEN, DisputeStatus.UNDER_REVIEW, DisputeStatus.ESCALATED),
                pageable
        ).getContent();
    }

    /**
     * Resolves a dispute by recording the resolution text and optionally
     * taking action against a user (suspend or ban). The dispute status
     * is set to RESOLVED and the resolution timestamp is recorded.
     *
     * @param disputeId  the ID of the dispute to resolve
     * @param resolution the resolution description text
     * @param action     optional action to take: "BAN", "SUSPEND", or null for no action
     * @return the resolved Dispute entity
     * @throws IllegalArgumentException if the dispute is not found
     */
    @Transactional
    public Dispute resolveDispute(Long disputeId, String resolution, String action) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found with id: " + disputeId));

        dispute.setStatus(DisputeStatus.RESOLVED);
        dispute.setResolution(resolution);
        dispute.setResolvedAt(LocalDateTime.now());

        if (action != null && dispute.getFlaggedMessage() != null) {
            User offender = dispute.getFlaggedMessage().getSender();
            if ("BAN".equalsIgnoreCase(action)) {
                offender.setStatus(AccountStatus.BANNED);
                userRepository.save(offender);
            } else if ("SUSPEND".equalsIgnoreCase(action)) {
                offender.setStatus(AccountStatus.SUSPENDED);
                userRepository.save(offender);
            }
        }

        return disputeRepository.save(dispute);
    }

    /**
     * Retrieves the most recently registered users, ordered by creation date
     * descending. Useful for monitoring new user registrations on the dashboard.
     *
     * @param limit the maximum number of recent registrations to return
     * @return list of recently registered users
     */
    public List<User> getRecentRegistrations(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return userRepository.findAll(pageable).getContent();
    }
}
