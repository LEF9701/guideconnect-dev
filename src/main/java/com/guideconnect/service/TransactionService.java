package com.guideconnect.service;

import com.guideconnect.model.Booking;
import com.guideconnect.model.Transaction;
import com.guideconnect.repository.BookingRepository;
import com.guideconnect.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

/**
 * Service layer for payment processing and transaction management.
 * Handles simulated payment creation with commission calculation
 * and provides aggregate financial reporting methods.
 */
@Service
public class TransactionService {

    /** Platform commission rate applied to each transaction (10%). */
    private static final BigDecimal COMMISSION_RATE = new BigDecimal("0.10");

    private final TransactionRepository transactionRepository;
    private final BookingRepository bookingRepository;

    /**
     * Constructs a TransactionService with required dependencies.
     *
     * @param transactionRepository the transaction repository for data access
     * @param bookingRepository     the booking repository for booking lookup
     */
    public TransactionService(TransactionRepository transactionRepository,
                              BookingRepository bookingRepository) {
        this.transactionRepository = transactionRepository;
        this.bookingRepository = bookingRepository;
    }

    /**
     * Processes a simulated payment for the given booking. Calculates the
     * platform commission at 10% of the booking total, generates a unique
     * transaction reference using UUID, and persists the transaction record.
     *
     * @param bookingId the ID of the booking to process payment for
     * @return the persisted Transaction entity
     * @throws IllegalArgumentException if the booking is not found
     * @throws IllegalStateException    if a transaction already exists for the booking
     */
    @Transactional
    public Transaction processPayment(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));

        if (transactionRepository.findByBooking(booking).isPresent()) {
            throw new IllegalStateException("Transaction already exists for booking: " + bookingId);
        }

        BigDecimal totalAmount = booking.getTotalPrice();
        BigDecimal commissionAmount = totalAmount.multiply(COMMISSION_RATE).setScale(2, RoundingMode.HALF_UP);
        String transactionRef = UUID.randomUUID().toString();

        Transaction transaction = new Transaction(booking, totalAmount, commissionAmount, transactionRef);
        return transactionRepository.save(transaction);
    }

    /**
     * Finds the transaction associated with a specific booking.
     *
     * @param bookingId the ID of the booking
     * @return an Optional containing the transaction if found
     * @throws IllegalArgumentException if the booking is not found
     */
    public Optional<Transaction> findByBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));
        return transactionRepository.findByBooking(booking);
    }

    /**
     * Calculates the total revenue across all transactions.
     * Returns zero if no transactions exist.
     *
     * @return the sum of all transaction total amounts
     */
    public BigDecimal getTotalRevenue() {
        BigDecimal total = transactionRepository.sumTotalAmount();
        return total != null ? total : BigDecimal.ZERO;
    }

    /**
     * Calculates the total commission collected across all transactions.
     * Returns zero if no transactions exist.
     *
     * @return the sum of all commission amounts
     */
    public BigDecimal getTotalCommission() {
        BigDecimal total = transactionRepository.sumCommissionAmount();
        return total != null ? total : BigDecimal.ZERO;
    }
}
