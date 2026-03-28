package com.guideconnect.repository;

import com.guideconnect.model.Booking;
import com.guideconnect.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Transaction} entities.
 * Provides query methods for transaction lookup and aggregate financial calculations.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Finds the transaction associated with a specific booking.
     * Each booking has at most one transaction (OneToOne relationship).
     *
     * @param booking the booking to look up
     * @return an Optional containing the transaction if found
     */
    Optional<Transaction> findByBooking(Booking booking);

    /**
     * Calculates the sum of all transaction total amounts.
     * Returns {@code null} if no transactions exist.
     *
     * @return the sum of all total amounts, or null if none
     */
    @Query("SELECT SUM(t.totalAmount) FROM Transaction t")
    BigDecimal sumTotalAmount();

    /**
     * Calculates the sum of all commission amounts collected.
     * Returns {@code null} if no transactions exist.
     *
     * @return the sum of all commission amounts, or null if none
     */
    @Query("SELECT SUM(t.commissionAmount) FROM Transaction t")
    BigDecimal sumCommissionAmount();
}
