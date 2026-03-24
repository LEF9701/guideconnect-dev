package com.guideconnect.controller;

import com.guideconnect.model.User;
import com.guideconnect.service.BookingService;
import com.guideconnect.service.ReviewService;
import com.guideconnect.service.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for tourist-facing pages.
 *
 * <p>Provides the tourist dashboard (with booking statistics) and profile
 * management endpoints. All endpoints require an authenticated user with
 * the tourist role.</p>
 */
@Controller
@RequestMapping("/tourist")
public class TouristController {

    private final UserService userService;
    private final BookingService bookingService;
    private final ReviewService reviewService;

    /**
     * Constructs a {@code TouristController} with the required service dependencies.
     *
     * @param userService    the service for user lookup and profile updates
     * @param bookingService the service for booking queries
     * @param reviewService  the service for review queries
     */
    public TouristController(UserService userService,
                             BookingService bookingService,
                             ReviewService reviewService) {
        this.userService = userService;
        this.bookingService = bookingService;
        this.reviewService = reviewService;
    }

    /**
     * Displays the tourist dashboard.
     *
     * <p>Populates the model with the tourist's bookings and review information.</p>
     *
     * @param principal the currently authenticated user's security principal
     * @param model     the Thymeleaf model
     * @return the tourist dashboard view name
     */
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = userService.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        model.addAttribute("user", user);
        model.addAttribute("bookings", bookingService.findByTourist(user.getId(), PageRequest.of(0, 10)));
        model.addAttribute("reviews", reviewService.getReviewsForUser(user.getId(), PageRequest.of(0, 10)));
        return "tourist/dashboard";
    }

    /**
     * Displays the tourist profile page.
     *
     * @param principal the currently authenticated user's security principal
     * @param model     the Thymeleaf model
     * @return the tourist profile view name
     */
    @GetMapping("/profile")
    public String showProfile(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = userService.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        model.addAttribute("user", user);
        return "tourist/profile";
    }

    /**
     * Displays the tourist's full booking history.
     *
     * @param principal the currently authenticated user's security principal
     * @param model     the Thymeleaf model
     * @return the tourist bookings view name
     */
    @GetMapping("/bookings")
    public String showBookings(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = userService.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        model.addAttribute("user", user);
        model.addAttribute("bookingHistory", bookingService.findByTourist(
                user.getId(),
                PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "requestedDate")
                        .and(Sort.by(Sort.Direction.DESC, "createdAt")))
        ));
        return "tourist/bookings";
    }

    /**
     * Updates the tourist's profile information.
     *
     * <p>Accepts the updated user data from the profile form and persists it
     * via {@link UserService}. Redirects back to the profile page on completion.</p>
     *
     * @param principal   the currently authenticated user's security principal
     * @param updatedUser the updated profile data from the form
     * @return a redirect to the tourist profile page
     */
    @PostMapping("/profile")
    public String updateProfile(@AuthenticationPrincipal UserDetails principal, User updatedUser) {
        User user = userService.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        userService.updateTouristProfile(user.getId(), updatedUser.getDisplayName(),
                updatedUser.getContactInfo(), updatedUser.getLanguagePreferences());
        return "redirect:/tourist/profile";
    }
}
