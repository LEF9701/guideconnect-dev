package com.guideconnect.controller;

import com.guideconnect.model.Role;
import com.guideconnect.model.User;
import com.guideconnect.service.BookingService;
import com.guideconnect.service.ReviewService;
import com.guideconnect.service.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Public controller for viewing tourist profiles.
 */
@Controller
@RequestMapping("/tourists")
public class TouristProfileController {

    private final UserService userService;
    private final BookingService bookingService;
    private final ReviewService reviewService;

    public TouristProfileController(UserService userService,
                                    BookingService bookingService,
                                    ReviewService reviewService) {
        this.userService = userService;
        this.bookingService = bookingService;
        this.reviewService = reviewService;
    }

    @GetMapping("/{id}")
    public String showTouristProfile(@PathVariable Long id, Model model) {
        User tourist = userService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tourist not found with id: " + id));

        if (tourist.getRole() != Role.TOURIST) {
            throw new IllegalArgumentException("User is not a tourist");
        }

        model.addAttribute("tourist", tourist);
        model.addAttribute("bookingHistory", bookingService.findByTourist(
                id,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "requestedDate")
                        .and(Sort.by(Sort.Direction.DESC, "createdAt")))
        ));
        model.addAttribute("reviews", reviewService.getReviewsForUser(
                id,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        ));
        return "tourist/public-profile";
    }
}
