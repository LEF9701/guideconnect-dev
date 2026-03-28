package com.guideconnect.repository;

import com.guideconnect.model.Booking;
import com.guideconnect.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.guideconnect.model.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;

/**
 * Spring Data JPA repository for {@link Message} entities.
 * Provides query methods for retrieving conversation threads, flagged messages,
 * and message counts per booking.
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Retrieves all messages for a booking, ordered by timestamp ascending
     * to display a chronological conversation thread.
     *
     * @param booking the booking whose messages to retrieve
     * @return list of messages sorted by timestamp ascending
     */
    List<Message> findByBookingOrderByTimestampAsc(Booking booking);
    long countByBookingAndReceiverAndReadFalse(Booking booking, User receiver);

    /**
     * Finds all messages that have been flagged for admin review, with pagination.
     *
     * @param pageable pagination and sorting parameters
     * @return a page of flagged messages
     */
    Page<Message> findByFlaggedTrue(Pageable pageable);
    List<Message> findByBookingAndReceiverAndReadFalse(Booking booking, User receiver);

    /**
     * Counts the number of messages associated with a specific booking.
     *
     * @param booking the booking to count messages for
     * @return the message count for the booking
     */
    long countByBooking(Booking booking);
    @Query("SELECT DISTINCT m.booking FROM Message m WHERE m.sender = :user OR m.receiver = :user")
    List<Booking> findDistinctBookingsByUser(@Param("user") User user);
}
