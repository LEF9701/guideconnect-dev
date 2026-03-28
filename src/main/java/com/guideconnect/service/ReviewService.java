package com.guideconnect.service;

import com.guideconnect.model.Booking;
import com.guideconnect.model.BookingStatus;
import com.guideconnect.model.Review;
import com.guideconnect.model.User;
import com.guideconnect.repository.BookingRepository;
import com.guideconnect.repository.ReviewRepository;
import com.guideconnect.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for review management with Observer pattern integration.
 * When a review is submitted, the reviewee's average rating and review count
 * are automatically recalculated, ensuring that user ratings stay consistent
 * with the underlying review data.
 */
@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    /**
     * Constructs a ReviewService with required dependencies.
     *
     * @param reviewRepository  the review repository for data access
     * @param userRepository    the user repository for user lookup and rating updates
     * @param bookingRepository the booking repository for booking validation
     */
    public ReviewService(ReviewRepository reviewRepository,
                         UserRepository userRepository,
                         BookingRepository bookingRepository) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
    }

    /**
     * Submits a review for a completed booking. Validates that the booking
     * is in COMPLETED status and that the reviewer has not already submitted
     * a review for this booking. After saving the review, the reviewee's
     * average rating is recalculated (Observer pattern).
     *
     * @param reviewerId the ID of the user writing the review
     * @param revieweeId the ID of the user being reviewed
     * @param bookingId  the ID of the completed booking
     * @param starRating the star rating (1-5)
     * @param comment    an optional text comment
     * @return the persisted Review entity
     * @throws IllegalArgumentException if reviewer, reviewee, or booking is not found
     * @throws IllegalStateException    if the booking is not completed or a duplicate review exists
     */
    @Transactional
    public Review submitReview(Long reviewerId, Long revieweeId, Long bookingId,
                               Integer starRating, String comment) {
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new IllegalArgumentException("Reviewer not found with id: " + reviewerId));

        User reviewee = userRepository.findById(revieweeId)
                .orElseThrow(() -> new IllegalArgumentException("Reviewee not found with id: " + revieweeId));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new IllegalStateException("Reviews can only be submitted for completed bookings");
        }

        if (reviewRepository.findByBookingAndReviewer(booking, reviewer).isPresent()) {
            throw new IllegalStateException("Reviewer has already submitted a review for this booking");
        }

        Review review = new Review(reviewer, reviewee, booking, starRating, comment);
        reviewRepository.save(review);

        recalculateRating(revieweeId);

        return review;
    }

    /**
     * Retrieves all reviews received by a specific user, with pagination.
     *
     * @param userId   the ID of the user whose reviews to retrieve
     * @param pageable pagination and sorting parameters
     * @return a page of reviews for the user
     * @throws IllegalArgumentException if the user is not found
     */
    public Page<Review> getReviewsForUser(Long userId, Pageable pageable) {
        User reviewee = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        return reviewRepository.findByReviewee(reviewee, pageable);
    }

    /**
     * Checks whether a specific reviewer has already submitted a review
     * for a given booking.
     *
     * @param bookingId  the ID of the booking
     * @param reviewerId the ID of the reviewer
     * @return true if a review already exists, false otherwise
     * @throws IllegalArgumentException if booking or reviewer is not found
     */
    public boolean hasReviewed(Long bookingId, Long reviewerId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));

        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new IllegalArgumentException("Reviewer not found with id: " + reviewerId));

        return reviewRepository.findByBookingAndReviewer(booking, reviewer).isPresent();
    }

    /**
     * Recalculates and updates the average rating and review count for a user.
     * Queries the review repository for the aggregate average and count,
     * then persists the updated values on the User entity.
     *
     * @param userId the ID of the user whose rating to recalculate
     * @throws IllegalArgumentException if the user is not found
     */
    @Transactional
    public void recalculateRating(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        Double avgRating = reviewRepository.avgRatingByReviewee(user);
        long reviewCount = reviewRepository.countByReviewee(user);

        user.setAvgRating(avgRating != null ? avgRating : 0.0);
        user.setReviewCount((int) reviewCount);
        userRepository.save(user);
    }
}
