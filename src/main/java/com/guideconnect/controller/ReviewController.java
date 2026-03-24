package com.guideconnect.controller;

import com.guideconnect.model.Booking;
import com.guideconnect.model.Review;
import com.guideconnect.model.User;
import com.guideconnect.service.BookingService;
import com.guideconnect.service.ReviewService;
import com.guideconnect.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller for review submission.
 *
 * <p>Allows tourists to leave reviews for completed bookings. The review
 * form is pre-populated with booking context and the submitted review is
 * linked to the corresponding booking.</p>
 */
@Controller
@RequestMapping("/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final BookingService bookingService;
    private final UserService userService;

    /**
     * Constructs a {@code ReviewController} with the required service dependencies.
     *
     * @param reviewService  the service for review operations
     * @param bookingService the service for booking lookup
     * @param userService    the service for user lookup
     */
    public ReviewController(ReviewService reviewService,
                            BookingService bookingService,
                            UserService userService) {
        this.reviewService = reviewService;
        this.bookingService = bookingService;
        this.userService = userService;
    }

    /**
     * Displays the review submission form for a completed booking.
     *
     * @param bookingId the ID of the booking to review
     * @param principal the currently authenticated user's security principal
     * @param model     the Thymeleaf model
     * @return the review form view name
     */
    @GetMapping("/new")
    public String showReviewForm(@RequestParam Long bookingId,
                                 @AuthenticationPrincipal UserDetails principal,
                                 Model model) {
        User user = userService.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Booking booking = bookingService.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));
        boolean guideReviewingTourist = user.getId().equals(booking.getGuide().getId());
        User reviewee = guideReviewingTourist ? booking.getTourist() : booking.getGuide();
        model.addAttribute("user", user);
        model.addAttribute("booking", booking);
        model.addAttribute("review", new Review());
        model.addAttribute("revieweeName", reviewee.getDisplayName());
        model.addAttribute("guideReviewingTourist", guideReviewingTourist);
        return "review/form";
    }

    /**
     * Submits a new review for a completed booking.
     *
     * <p>Links the review to the specified booking and the authenticated user,
     * then redirects to the booking detail page.</p>
     *
     * @param bookingId  the ID of the booking being reviewed
     * @param starRating the star rating (1-5)
     * @param comment    optional review comment
     * @param principal  the currently authenticated user's security principal
     * @return a redirect to the booking detail page
     */
    @PostMapping
    public String submitReview(@RequestParam Long bookingId,
                               @RequestParam Integer starRating,
                               @RequestParam(required = false) String comment,
                               @AuthenticationPrincipal UserDetails principal) {
        User user = userService.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Booking booking = bookingService.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));
        boolean guideReviewingTourist = user.getId().equals(booking.getGuide().getId());
        Long revieweeId = guideReviewingTourist ? booking.getTourist().getId() : booking.getGuide().getId();
        String submittedComment = guideReviewingTourist ? null : comment;
        reviewService.submitReview(user.getId(), revieweeId, bookingId, starRating, submittedComment);
        return "redirect:/bookings/" + bookingId;
    }
}
