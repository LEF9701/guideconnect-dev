package com.guideconnect.repository;

import com.guideconnect.model.Booking;
import com.guideconnect.model.BookingStatus;
import com.guideconnect.model.TourListing;
import com.guideconnect.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Spring Data JPA repository for {@link Booking} entities.
 * Provides query methods for booking lookup by tourist, guide, status,
 * and duplicate-prevention checks.
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * Finds all bookings placed by a specific tourist, with pagination.
     *
     * @param tourist  the tourist user
     * @param pageable pagination and sorting parameters
     * @return a page of bookings for the tourist
     */
    Page<Booking> findByTourist(User tourist, Pageable pageable);

    /**
     * Finds all bookings assigned to a specific guide, with pagination.
     *
     * @param guide    the guide user
     * @param pageable pagination and sorting parameters
     * @return a page of bookings for the guide
     */
    Page<Booking> findByGuide(User guide, Pageable pageable);

    /**
     * Finds all bookings with the specified status.
     *
     * @param status the booking status to filter by
     * @return list of bookings with the given status
     */
    List<Booking> findByStatus(BookingStatus status);

    /**
     * Finds bookings for a specific tour with the specified status.
     *
     * @param tour   the tour listing
     * @param status the booking status to filter by
     * @return list of matching bookings
     */
    List<Booking> findByTourAndStatus(TourListing tour, BookingStatus status);
    Page<Booking> findByGuideAndStatusIn(User guide, List<BookingStatus> statuses, Pageable pageable);

    /**
     * Counts the number of bookings with the specified status.
     *
     * @param status the booking status to count
     * @return the count of bookings with that status
     */
    long countByStatus(BookingStatus status);

    /**
     * Checks whether a booking already exists for a given tourist, tour, and
     * any of the specified statuses. Useful for preventing duplicate active bookings.
     *
     * @param tourist  the tourist user
     * @param tour     the tour listing
     * @param statuses collection of statuses to check against
     * @return true if a matching booking exists
     */
    boolean existsByTouristAndTourAndStatusIn(User tourist, TourListing tour, Collection<BookingStatus> statuses);
}
