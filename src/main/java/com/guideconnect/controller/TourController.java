package com.guideconnect.controller;

import com.guideconnect.model.TourListing;
import com.guideconnect.service.ReviewService;
import com.guideconnect.service.TourService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

/**
 * Controller for public tour browsing and detail pages.
 *
 * <p>Provides a paginated search endpoint with filtering by city, language,
 * and price range, as well as a detail page that includes guide information
 * and reviews.</p>
 */
@Controller
@RequestMapping("/tours")
public class TourController {

    private final TourService tourService;
    private final ReviewService reviewService;

    /**
     * Constructs a {@code TourController} with the required service dependencies.
     *
     * @param tourService   the service for tour queries
     * @param reviewService the service for review queries
     */
    public TourController(TourService tourService, ReviewService reviewService) {
        this.tourService = tourService;
        this.reviewService = reviewService;
    }

    /**
     * Searches for tours with optional filters and pagination.
     *
     * <p>Supports filtering by city, language, minimum price, maximum price,
     * and sorting. Results are returned as a paginated list.</p>
     *
     * @param city     optional city filter
     * @param language optional language filter
     * @param minPrice optional minimum price filter
     * @param maxPrice optional maximum price filter
     * @param sortBy   optional sort field (e.g. "price_asc", "price_desc", "rating"); defaults to "relevance"
     * @param page     the zero-based page index; defaults to 0
     * @param size     the page size; defaults to 10
     * @param model    the Thymeleaf model
     * @return the tour search results view name
     */
    @GetMapping("/search")
    public String searchTours(@RequestParam(required = false) String city,
                              @RequestParam(required = false) String language,
                              @RequestParam(required = false) BigDecimal minPrice,
                              @RequestParam(required = false) BigDecimal maxPrice,
                              @RequestParam(defaultValue = "relevance") String sortBy,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size,
                              Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<TourListing> results = tourService.searchTours(city, language, minPrice, maxPrice, sortBy, pageable);

        model.addAttribute("tours", results);
        model.addAttribute("city", city);
        model.addAttribute("language", language);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("sortBy", sortBy);
        return "tour/search";
    }

    /**
     * Displays the detail page for a specific tour.
     *
     * <p>Populates the model with the tour information, the associated guide,
     * and all reviews for the guide.</p>
     *
     * @param id    the ID of the tour to display
     * @param model the Thymeleaf model
     * @return the tour detail view name
     */
    @GetMapping("/{id}")
    public String showTourDetail(@PathVariable Long id, Model model) {
        TourListing tour = tourService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tour not found with id: " + id));
        model.addAttribute("tour", tour);
        model.addAttribute("guide", tour.getGuide());
        model.addAttribute("reviews", reviewService.getReviewsForUser(tour.getGuide().getId(), PageRequest.of(0, 20)));
        return "tour/detail";
    }
}
