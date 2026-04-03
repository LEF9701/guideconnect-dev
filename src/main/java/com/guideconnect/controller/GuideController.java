package com.guideconnect.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.guideconnect.model.BookingStatus;
import com.guideconnect.model.TourListing;
import com.guideconnect.model.User;
import com.guideconnect.service.BookingService;
import com.guideconnect.service.ReviewService;
import com.guideconnect.service.TourService;
import com.guideconnect.service.TransactionService;
import com.guideconnect.service.UserService;

/**
 * Controller for guide-facing pages.
 *
 * <p>Provides the guide dashboard, profile management, tour CRUD operations,
 * and incoming booking request views. All endpoints require an authenticated
 * user with the guide role.</p>
 */
@Controller
@RequestMapping("/guide")
public class GuideController {

    private final UserService userService;
    private final TourService tourService;
    private final BookingService bookingService;
    private final ReviewService reviewService;
    private final TransactionService transactionService;

    /**
     * Constructs a {@code GuideController} with the required service dependencies.
     *
     * @param userService    the service for user lookup and profile updates
     * @param tourService    the service for tour CRUD operations
     * @param bookingService the service for booking queries
     */
    public GuideController(UserService userService,
                           TourService tourService,
                           BookingService bookingService,
                           ReviewService reviewService,
                           TransactionService transactionService) {
        this.userService = userService;
        this.tourService = tourService;
        this.bookingService = bookingService;
        this.reviewService = reviewService;
        this.transactionService = transactionService;
    }

    /**
     * Displays the guide dashboard.
     *
     * <p>Populates the model with the guide's tours and booking information.</p>
     *
     * @param principal the currently authenticated user's security principal
     * @param model     the Thymeleaf model
     * @return the guide dashboard view name
     */
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = userService.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        model.addAttribute("user", user);
        model.addAttribute("tours", tourService.findByGuide(user.getId()));
        model.addAttribute("bookings", bookingService.findByGuideAndStatusIn(
                user.getId(),
                List.of(BookingStatus.REQUESTED, BookingStatus.NEGOTIATING),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))));
        model.addAttribute("upcomingTours", bookingService.findByGuideAndStatusIn(
                user.getId(),
                List.of(BookingStatus.CONFIRMED),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "requestedDate").and(Sort.by(Sort.Direction.ASC, "requestedTime")))));
        model.addAttribute("reviews", reviewService.getReviewsForUser(
                user.getId(),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))));
        model.addAttribute("netEarnings", transactionService.getGuideNetEarnings(user));
        return "guide/dashboard";
    }

    @GetMapping("/earnings")
    public String showEarnings(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = userService.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        model.addAttribute("user", user);
        model.addAttribute("grossRevenue", transactionService.getGuideGrossRevenue(user));
        model.addAttribute("platformCommission", transactionService.getGuideCommission(user));
        model.addAttribute("netEarnings", transactionService.getGuideNetEarnings(user));
        model.addAttribute("transactions", transactionService.getGuideTransactions(user, 20));
        return "guide/earnings";
    }

    /**
     * Displays the guide profile page.
     *
     * @param principal the currently authenticated user's security principal
     * @param model     the Thymeleaf model
     * @return the guide profile view name
     */
    @GetMapping("/profile")
    public String showProfile(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = userService.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        model.addAttribute("user", user);
        return "guide/profile";
    }

    /**
     * Updates the guide's profile information.
     *
     * @param principal   the currently authenticated user's security principal
     * @param updatedUser the updated profile data from the form
     * @return a redirect to the guide profile page
     */
    @PostMapping("/profile")
    public String updateProfile(@AuthenticationPrincipal UserDetails principal,
                                User updatedUser,
                                @RequestParam(value = "profilePhotoFile", required = false) MultipartFile profilePhotoFile) throws IOException {
        User user = userService.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        updatedUser.setProfilePhoto(user.getProfilePhoto());
        saveProfilePhotoIfPresent(updatedUser, profilePhotoFile);
        userService.updateGuideProfile(user.getId(), updatedUser.getDisplayName(),
                updatedUser.getBiography(), updatedUser.getLanguagesSpoken(),
                updatedUser.getProfilePhoto(), updatedUser.getGuidePricing());
        return "redirect:/guide/profile";
    }

    /**
     * Displays the tour creation form.
     *
     * <p>Adds an empty {@link TourListing} object to the model so the Thymeleaf form
     * can bind to it.</p>
     *
     * @param model the Thymeleaf model
     * @return the tour form view name
     */
    @GetMapping("/tours/new")
    public String showNewTourForm(Model model) {
        model.addAttribute("tour", new TourListing());
        return "guide/tour-form";
    }

    /**
     * Creates a new tour for the authenticated guide.
     *
     * @param principal the currently authenticated user's security principal
     * @param tour      the tour data from the form
     * @return a redirect to the guide dashboard
     */
    @PostMapping("/tours")
    public String createTour(@AuthenticationPrincipal UserDetails principal, 
                                TourListing tour, 
                                @RequestParam("imageFile") MultipartFile imageFile) throws IOException {
        User user = userService.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Save image and set the imgPath in the tour object
        saveImageIfPresent(tour, imageFile);
        
        tourService.createTour(tour, user.getId());
        return "redirect:/guide/dashboard";
    }

    /**
     * Displays the tour edit form pre-populated with existing tour data.
     *
     * @param id    the ID of the tour to edit
     * @param model the Thymeleaf model
     * @return the tour form view name
     */
    @GetMapping("/tours/{id}/edit")
    public String showEditTourForm(@PathVariable Long id, Model model) {
        TourListing tour = tourService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tour not found with id: " + id));
        model.addAttribute("tour", tour);
        return "guide/tour-form";
    }

    /**
     * Updates an existing tour.
     *
     * @param id          the ID of the tour to update
     * @param principal   the currently authenticated user's security principal
     * @param updatedTour the updated tour data from the form
     * @return a redirect to the guide dashboard
     */
    @PostMapping("/tours/{id}")
    public String updateTour(@PathVariable Long id,
                         @AuthenticationPrincipal UserDetails principal,
                         TourListing updatedTour,
                         @RequestParam("imageFile") MultipartFile imageFile) throws IOException {
        User user = userService.findByEmail(principal.getUsername()).orElseThrow();

        // Handle the image (if no new file is uploaded, it keeps the old imgPath)
        saveImageIfPresent(updatedTour, imageFile);
    
        tourService.updateTour(id, updatedTour, user.getId());
        return "redirect:/guide/dashboard";
}

    /**
     * Deletes a tour by its ID.
     *
     * @param id        the ID of the tour to delete
     * @param principal the currently authenticated user's security principal
     * @return a redirect to the guide dashboard
     */
    @PostMapping("/tours/{id}/delete")
    public String deleteTour(@PathVariable Long id,
                             @AuthenticationPrincipal UserDetails principal) {
        User user = userService.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        tourService.deleteTour(id, user.getId());
        return "redirect:/guide/dashboard";
    }

    /**
     * Displays incoming booking requests for the guide's tours.
     *
     * @param principal the currently authenticated user's security principal
     * @param model     the Thymeleaf model
     * @return the guide requests view name
     */
    @GetMapping("/requests")
    public String showRequests(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = userService.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        model.addAttribute("user", user);
        model.addAttribute("requests", bookingService.findByGuideAndStatusIn(
                user.getId(),
                List.of(BookingStatus.REQUESTED, BookingStatus.NEGOTIATING),
                PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "createdAt"))));
        return "guide/requests";
    }
    private void saveImageIfPresent(TourListing tour, MultipartFile file) throws IOException {
        if (file != null && !file.isEmpty()) {
                tour.setImgPath(storeStaticImage(file, "tours"));
        }
        }

    private void saveProfilePhotoIfPresent(User user, MultipartFile file) throws IOException {
        if (file != null && !file.isEmpty()) {
            user.setProfilePhoto(storeStaticImage(file, "guides"));
        }
    }

    private String storeStaticImage(MultipartFile file, String folder) throws IOException {
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload";
        String extension = "";
        int extensionIndex = originalName.lastIndexOf('.');
        if (extensionIndex >= 0) {
            extension = originalName.substring(extensionIndex).toLowerCase(Locale.ROOT);
        }

        String baseName = extensionIndex >= 0 ? originalName.substring(0, extensionIndex) : originalName;
        String sanitizedBaseName = baseName.replaceAll("[^a-zA-Z0-9-_]+", "_");
        if (sanitizedBaseName.isBlank()) {
            sanitizedBaseName = "image";
        }

        String fileName = System.currentTimeMillis() + "_" + sanitizedBaseName + extension;
        byte[] bytes = file.getBytes();

        Path sourceUploadPath = Paths.get("src/main/resources/static/images", folder);
        Path compiledUploadPath = Paths.get("target/classes/static/images", folder);

        Files.createDirectories(sourceUploadPath);
        Files.createDirectories(compiledUploadPath);

        Files.write(sourceUploadPath.resolve(fileName), bytes);
        Files.write(compiledUploadPath.resolve(fileName), bytes);

        return "/images/" + folder + "/" + fileName;
    }
}
