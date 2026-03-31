package com.guideconnect.controller;

import com.guideconnect.model.Role;
import com.guideconnect.model.User;
import com.guideconnect.service.ReviewService;
import com.guideconnect.service.TourService;
import com.guideconnect.service.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Public controller for viewing guide profiles.
 */
@Controller
@RequestMapping("/guides")
public class GuideProfileController {

    private final UserService userService;
    private final TourService tourService;
    private final ReviewService reviewService;

    public GuideProfileController(UserService userService,
                                  TourService tourService,
                                  ReviewService reviewService) {
        this.userService = userService;
        this.tourService = tourService;
        this.reviewService = reviewService;
    }

    @GetMapping("/{id}")
    public String showGuideProfile(@PathVariable Long id, Model model) {
        User guide = userService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Guide not found with id: " + id));

        if (guide.getRole() != Role.GUIDE) {
            throw new IllegalArgumentException("User is not a guide");
        }

        model.addAttribute("guide", guide);
        model.addAttribute("tours", tourService.findByGuide(id).stream()
                .filter(tour -> Boolean.TRUE.equals(tour.isActive()))
                .toList());
        model.addAttribute("reviews", reviewService.getReviewsForUser(id, PageRequest.of(0, 10)));
        return "guide/public-profile";
    }
}
