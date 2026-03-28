package com.guideconnect.repository;

import com.guideconnect.model.Booking;
import com.guideconnect.model.Review;
import com.guideconnect.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Review} entities.
 * Provides query methods for review lookup, duplicate prevention,
 * and aggregate rating calculations.
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * Finds all reviews received by a specific user (reviewee), with pagination.
     * Used for displaying a guide's or tourist's review history.
     *
     * @param reviewee the user who was reviewed
     * @param pageable pagination and sorting parameters
     * @return a page of reviews for the reviewee
     */
    Page<Review> findByReviewee(User reviewee, Pageable pageable);

    /**
     * Finds a review for a specific booking written by a specific reviewer.
     * Useful for checking whether a user has already reviewed a completed booking.
     *
     * @param booking  the booking being reviewed
     * @param reviewer the user who wrote the review
     * @return an Optional containing the review if found
     */
    Optional<Review> findByBookingAndReviewer(Booking booking, User reviewer);

    /**
     * Calculates the average star rating received by a specific user.
     * Returns {@code null} if the user has no reviews.
     *
     * @param reviewee the user whose average rating to calculate
     * @return the average star rating, or null if no reviews exist
     */
    @Query("SELECT AVG(r.starRating) FROM Review r WHERE r.reviewee = :reviewee")
    Double avgRatingByReviewee(@Param("reviewee") User reviewee);

    /**
     * Counts the total number of reviews received by a specific user.
     *
     * @param reviewee the user whose reviews to count
     * @return the review count for the reviewee
     */
    long countByReviewee(User reviewee);
}
