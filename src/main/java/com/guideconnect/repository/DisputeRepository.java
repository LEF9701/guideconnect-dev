package com.guideconnect.repository;

import com.guideconnect.model.Booking;
import com.guideconnect.model.Dispute;
import com.guideconnect.model.DisputeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link Dispute} entities.
 * Provides query methods for dispute management and admin resolution workflows.
 */
@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long> {

    /**
     * Finds all disputes with the specified status, with pagination.
     * Commonly used by admins to view open or escalated disputes.
     *
     * @param status   the dispute status to filter by
     * @param pageable pagination and sorting parameters
     * @return a page of disputes with the given status
     */
    Page<Dispute> findByStatus(DisputeStatus status, Pageable pageable);

    /**
     * Finds all disputes whose status is one of the provided values.
     *
     * @param statuses the statuses to include
     * @param pageable pagination and sorting parameters
     * @return a page of disputes matching any of the provided statuses
     */
    Page<Dispute> findByStatusIn(List<DisputeStatus> statuses, Pageable pageable);

    /**
     * Finds all disputes associated with a specific booking.
     *
     * @param booking the booking to look up disputes for
     * @return list of disputes linked to the booking
     */
    List<Dispute> findByBooking(Booking booking);

    /**
     * Counts the number of disputes with the specified status.
     *
     * @param status the dispute status to count
     * @return the count of disputes with that status
     */
    long countByStatus(DisputeStatus status);
}
