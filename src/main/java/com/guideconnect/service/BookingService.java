package com.guideconnect.service;

import com.guideconnect.model.Booking;
import com.guideconnect.model.BookingStatus;
import com.guideconnect.model.TourListing;
import com.guideconnect.model.User;
import com.guideconnect.repository.BookingRepository;
import com.guideconnect.repository.TourListingRepository;
import com.guideconnect.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.List;

/**
 * Service layer for booking lifecycle management.
 * Implements the State Pattern for booking status transitions, ensuring
 * that only valid transitions are allowed at each stage of the lifecycle.
 *
 * <p>Lifecycle: REQUESTED -> NEGOTIATING -> CONFIRMED -> COMPLETED
 * <br>Cancellation is allowed from: REQUESTED, NEGOTIATING, CONFIRMED
 * <br>Rejection is allowed from: REQUESTED, NEGOTIATING</p>
 */
@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final TourListingRepository tourListingRepository;

    /**
     * Constructs a BookingService with required dependencies.
     *
     * @param bookingRepository     the booking repository for data access
     * @param userRepository        the user repository for user lookup
     * @param tourListingRepository the tour listing repository for tour lookup
     */
    public BookingService(BookingRepository bookingRepository,
                          UserRepository userRepository,
                          TourListingRepository tourListingRepository) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.tourListingRepository = tourListingRepository;
    }

    /**
     * Creates a new booking for a tourist on a specific tour. The total price
     * is calculated as pricePerPerson multiplied by the group size.
     *
     * @param touristId the ID of the tourist making the booking
     * @param tourId    the ID of the tour being booked
     * @param date      the requested tour date
     * @param time      the requested tour time
     * @param groupSize the number of people in the group
     * @param message   an optional message from the tourist to the guide
     * @return the persisted Booking entity
     * @throws IllegalArgumentException if tourist or tour is not found
     */
    @Transactional
    public Booking createBooking(Long touristId, Long tourId, LocalDate date,
                                 LocalTime time, Integer groupSize, String message) {
        User tourist = userRepository.findById(touristId)
                .orElseThrow(() -> new IllegalArgumentException("Tourist not found with id: " + touristId));

        TourListing tour = tourListingRepository.findById(tourId)
                .orElseThrow(() -> new IllegalArgumentException("Tour not found with id: " + tourId));

        BigDecimal totalPrice = tour.getPricePerPerson().multiply(BigDecimal.valueOf(groupSize));

        Booking booking = new Booking();
        booking.setTourist(tourist);
        booking.setTour(tour);
        booking.setGuide(tour.getGuide());
        booking.setRequestedDate(date);
        booking.setRequestedTime(time);
        booking.setGroupSize(groupSize);
        booking.setMessage(message);
        booking.setTotalPrice(totalPrice);
        booking.setStatus(BookingStatus.REQUESTED);

        return bookingRepository.save(booking);
    }

    /**
     * Accepts a booking request, transitioning it to CONFIRMED status.
     * Only the assigned guide may accept the booking.
     *
     * @param bookingId the ID of the booking to accept
     * @param guideId   the ID of the guide accepting the booking
     * @return the updated Booking entity
     * @throws IllegalArgumentException if the booking is not found
     * @throws SecurityException        if the guide is not assigned to the booking
     * @throws IllegalStateException    if the transition is not valid
     */
    @Transactional
    public Booking acceptBooking(Long bookingId, Long guideId) {
        Booking booking = getBookingForGuide(bookingId, guideId);
        transitionStatus(booking, BookingStatus.CONFIRMED);
        return bookingRepository.save(booking);
    }

    /**
     * Rejects a booking request, transitioning it to REJECTED status.
     * Only the assigned guide may reject the booking.
     *
     * @param bookingId the ID of the booking to reject
     * @param guideId   the ID of the guide rejecting the booking
     * @return the updated Booking entity
     * @throws IllegalArgumentException if the booking is not found
     * @throws SecurityException        if the guide is not assigned to the booking
     * @throws IllegalStateException    if the transition is not valid
     */
    @Transactional
    public Booking rejectBooking(Long bookingId, Long guideId) {
        Booking booking = getBookingForGuide(bookingId, guideId);
        transitionStatus(booking, BookingStatus.REJECTED);
        return bookingRepository.save(booking);
    }

    /**
     * Starts a negotiation phase for a booking, transitioning it to NEGOTIATING status.
     *
     * @param bookingId the ID of the booking to negotiate
     * @return the updated Booking entity
     * @throws IllegalArgumentException if the booking is not found
     * @throws IllegalStateException    if the transition is not valid
     */
    @Transactional
    public Booking startNegotiation(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));
        transitionStatus(booking, BookingStatus.NEGOTIATING);
        return bookingRepository.save(booking);
    }

    /**
     * Completes a booking after the tour date has passed, transitioning it
     * to COMPLETED status. The booking can only be completed if the requested
     * date is in the past.
     *
     * @param bookingId the ID of the booking to complete
     * @param userId    the ID of the user marking the booking complete
     * @return the updated Booking entity
     * @throws IllegalArgumentException if the booking is not found
     * @throws SecurityException        if the user is neither the tourist nor the guide
     * @throws IllegalStateException    if the tour date has not yet passed or the transition is not valid
     */
    @Transactional
    public Booking completeBooking(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));

        if (!booking.getTourist().getId().equals(userId) && !booking.getGuide().getId().equals(userId)) {
            throw new SecurityException("Only the tourist or guide can complete this booking");
        }

        if (!booking.getRequestedDate().isBefore(LocalDate.now())) {
            throw new IllegalStateException("Cannot complete booking before the tour date has passed");
        }

        transitionStatus(booking, BookingStatus.COMPLETED);
        return bookingRepository.save(booking);
    }

    /**
     * Cancels a booking, transitioning it to CANCELLED status.
     * Either the tourist or the guide may cancel the booking.
     *
     * @param bookingId the ID of the booking to cancel
     * @param userId    the ID of the user requesting cancellation
     * @return the updated Booking entity
     * @throws IllegalArgumentException if the booking is not found
     * @throws SecurityException        if the user is neither the tourist nor the guide
     * @throws IllegalStateException    if the transition is not valid
     */
    @Transactional
    public Booking cancelBooking(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));

        if (!booking.getTourist().getId().equals(userId) && !booking.getGuide().getId().equals(userId)) {
            throw new SecurityException("Only the tourist or guide can cancel this booking");
        }

        transitionStatus(booking, BookingStatus.CANCELLED);
        return bookingRepository.save(booking);
    }

    /**
     * Finds a booking by its unique identifier.
     *
     * @param id the booking ID
     * @return an Optional containing the booking if found
     */
    public Optional<Booking> findById(Long id) {
        return bookingRepository.findById(id);
    }

    /**
     * Finds all bookings placed by a specific tourist, with pagination.
     *
     * @param touristId the ID of the tourist
     * @param pageable  pagination and sorting parameters
     * @return a page of bookings for the tourist
     * @throws IllegalArgumentException if the tourist is not found
     */
    public Page<Booking> findByTourist(Long touristId, Pageable pageable) {
        User tourist = userRepository.findById(touristId)
                .orElseThrow(() -> new IllegalArgumentException("Tourist not found with id: " + touristId));
        return bookingRepository.findByTourist(tourist, pageable);
    }

    /**
     * Finds all bookings assigned to a specific guide, with pagination.
     *
     * @param guideId  the ID of the guide
     * @param pageable pagination and sorting parameters
     * @return a page of bookings for the guide
     * @throws IllegalArgumentException if the guide is not found
     */
    public Page<Booking> findByGuide(Long guideId, Pageable pageable) {
        User guide = userRepository.findById(guideId)
                .orElseThrow(() -> new IllegalArgumentException("Guide not found with id: " + guideId));
        return bookingRepository.findByGuide(guide, pageable);
    }

    /**
     * Counts the number of bookings with the specified status.
     *
     * @param status the booking status to count
     * @return the count of bookings with that status
     */
    public long countByStatus(BookingStatus status) {
        return bookingRepository.countByStatus(status);
    }

    /**
     * Retrieves a booking and verifies that the specified guide is assigned to it.
     *
     * @param bookingId the booking ID
     * @param guideId   the guide ID to verify
     * @return the Booking entity
     * @throws IllegalArgumentException if the booking is not found
     * @throws SecurityException        if the guide is not assigned to the booking
     */
    private Booking getBookingForGuide(Long bookingId, Long guideId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));

        if (!booking.getGuide().getId().equals(guideId)) {
            throw new SecurityException("Guide is not assigned to this booking");
        }

        return booking;
    }

    /**
     * Validates and performs a booking status transition using the State Pattern.
     * Delegates validation to {@link Booking#canTransitionTo(BookingStatus)}.
     *
     * @param booking   the booking to transition
     * @param newStatus the target status
     * @throws IllegalStateException if the transition is not allowed
     */
    private void transitionStatus(Booking booking, BookingStatus newStatus) {
        if (!booking.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    "Cannot transition booking from " + booking.getStatus() + " to " + newStatus);
        }
        booking.setStatus(newStatus);
    }
    public Page<Booking> findByGuideAndStatusIn(Long guideId, List<BookingStatus> statuses, Pageable pageable) {
        User guide = userRepository.findById(guideId)
                .orElseThrow(() -> new IllegalArgumentException("Guide not found"));
        return bookingRepository.findByGuideAndStatusIn(guide, statuses, pageable);
    }

}
