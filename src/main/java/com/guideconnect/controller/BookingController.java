package com.guideconnect.controller;

import com.guideconnect.model.Booking;
import com.guideconnect.model.TourListing;
import com.guideconnect.model.User;
import com.guideconnect.service.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Controller for booking lifecycle management.
 *
 * <p>Handles the full booking workflow including creation, acceptance,
 * rejection, negotiation, completion, cancellation, and payment processing.
 * Actions are scoped based on the authenticated user's role (tourist or guide)
 * and the current booking status.</p>
 */
@Controller
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final TourService tourService;
    private final UserService userService;
    private final TransactionService transactionService;
    private final ReviewService reviewService;
    private final MessageService messageService;
    private final AdminService adminService;


    /**
     * Constructs a {@code BookingController} with the required service dependencies.
     *
     * @param bookingService     the service for booking CRUD and status transitions
     * @param tourService        the service for tour queries
     * @param userService        the service for user lookup
     * @param transactionService the service for payment processing
     */
    public BookingController(BookingService bookingService,
                             TourService tourService,
                             UserService userService,
                             TransactionService transactionService,ReviewService reviewService, MessageService messageService,
                             AdminService adminService) {
        this.bookingService = bookingService;
        this.tourService = tourService;
        this.userService = userService;
        this.transactionService = transactionService;
        this.reviewService = reviewService;
        this.messageService = messageService;
        this.adminService = adminService;

    }

    /**
     * Displays the booking request form pre-filled with tour information.
     *
     * @param tourId    the ID of the tour to book
     * @param principal the currently authenticated user's security principal
     * @param model     the Thymeleaf model
     * @return the booking request form view name
     */
    @GetMapping("/new")
    public String showBookingForm(@RequestParam Long tourId,
                                  @AuthenticationPrincipal UserDetails principal,
                                  Model model) {
        User user = userService.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        TourListing tour = tourService.findById(tourId)
                .orElseThrow(() -> new IllegalArgumentException("Tour not found with id: " + tourId));
        model.addAttribute("user", user);
        model.addAttribute("tour", tour);
        model.addAttribute("booking", new Booking());
        return "booking/request";
    }

    /**
     * Creates a new booking for the authenticated tourist.
     *
     * @param principal     the currently authenticated user's security principal
     * @param tourId        the ID of the tour being booked
     * @param requestedDate the requested tour date
     * @param requestedTime the requested tour time
     * @param groupSize     the number of people in the group
     * @param message       an optional message from the tourist
     * @return a redirect to the newly created booking's detail page
     */
    @PostMapping
    public String createBooking(@AuthenticationPrincipal UserDetails principal,
                                @RequestParam Long tourId,
                                @RequestParam LocalDate requestedDate,
                                @RequestParam(required = false) LocalTime requestedTime,
                                @RequestParam Integer groupSize,
                                @RequestParam(required = false) String message) {
        User user = userService.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Booking created = bookingService.createBooking(user.getId(), tourId, requestedDate, requestedTime, groupSize, message);
        return "redirect:/bookings/" + created.getId();
    }

    /**
     * Displays the detail page for a specific booking.
     *
     * <p>Includes booking information, current status, and available actions
     * based on the authenticated user's role and the booking status.</p>
     *
     * @param id        the booking ID
     * @param principal the currently authenticated user's security principal
     * @param model     the Thymeleaf model
     * @return the booking detail view name
     */
    @GetMapping("/{id}")
    public String showBookingDetail(@PathVariable Long id,
                                    @AuthenticationPrincipal UserDetails principal,
                                    @RequestParam(value = "reported", required = false) String reported,
                                    Model model) {
        User user = userService.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Booking booking = bookingService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + id));

        boolean canComplete = booking.getRequestedDate().isBefore(LocalDate.now());
        boolean hasReviewed = reviewService.hasReviewed(id, user.getId());

        model.addAttribute("user", user);
        model.addAttribute("booking", booking);
        model.addAttribute("canComplete", canComplete);
        model.addAttribute("hasReviewed", hasReviewed);
        model.addAttribute("messageCount", messageService.getMessagesForBooking(id).size());
        model.addAttribute("transaction", transactionService.findByBooking(id).orElse(null));
        model.addAttribute("reported", reported != null);

        return "booking/detail";
    }

    /**
     * Allows a guide to accept a pending booking request.
     *
     * @param id        the booking ID
     * @param principal the currently authenticated user's security principal
     * @return a redirect to the booking detail page
     */
    @PostMapping("/{id}/accept")
    public String acceptBooking(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails principal) {
        User user = userService.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        bookingService.acceptBooking(id, user.getId());
        return "redirect:/bookings/" + id;
    }

    /**
     * Allows a guide to reject a pending booking request.
     *
     * @param id        the booking ID
     * @param principal the currently authenticated user's security principal
     * @return a redirect to the booking detail page
     */
    @PostMapping("/{id}/reject")
    public String rejectBooking(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails principal) {
        User user = userService.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        bookingService.rejectBooking(id, user.getId());
        return "redirect:/bookings/" + id;
    }

    /**
     * Initiates a negotiation on a booking.
     *
     * @param id        the booking ID
     * @param principal the currently authenticated user's security principal
     * @return a redirect to the booking detail page
     */
    @PostMapping("/{id}/negotiate")
    public String negotiateBooking(@PathVariable Long id,
                                   @AuthenticationPrincipal UserDetails principal) {
        bookingService.startNegotiation(id);
        return "redirect:/bookings/" + id;
    }

    /**
     * Marks a booking as completed.
     *
     * @param id        the booking ID
     * @param principal the currently authenticated user's security principal
     * @return a redirect to the booking detail page
     */
    @PostMapping("/{id}/complete")
    public String completeBooking(@PathVariable Long id,
                                  @AuthenticationPrincipal UserDetails principal) {
        User user = userService.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        bookingService.completeBooking(id, user.getId());
        return "redirect:/bookings/" + id;
    }

    /**
     * Cancels a booking.
     *
     * @param id        the booking ID
     * @param principal the currently authenticated user's security principal
     * @return a redirect to the booking detail page
     */
    @PostMapping("/{id}/cancel")
    public String cancelBooking(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails principal) {
        User user = userService.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        bookingService.cancelBooking(id, user.getId());
        return "redirect:/bookings/" + id;
    }

    /**
     * Processes payment for a booking.
     *
     * <p>Delegates to {@link TransactionService} and redirects to the payment
     * confirmation page on success.</p>
     *
     * @param id        the booking ID
     * @param principal the currently authenticated user's security principal
     * @return a redirect to the payment confirmation page
     */
    @PostMapping("/{id}/pay")
    public String processPayment(@PathVariable Long id,
                                 @AuthenticationPrincipal UserDetails principal) {
        transactionService.processPayment(id);
        return "redirect:/bookings/" + id + "/payment";
    }

    /**
     * Displays the payment confirmation page with transaction details.
     *
     * @param id        the booking ID
     * @param principal the currently authenticated user's security principal
     * @param model     the Thymeleaf model
     * @return the payment confirmation view name
     */
    @GetMapping("/{id}/payment")
    public String showPaymentConfirmation(@PathVariable Long id,
                                          @AuthenticationPrincipal UserDetails principal,
                                          Model model) {
        User user = userService.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Booking booking = bookingService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + id));
        model.addAttribute("user", user);
        model.addAttribute("booking", booking);
        model.addAttribute("transaction", transactionService.findByBooking(id).orElse(null));
        return "booking/payment-confirmation";
    }

    @PostMapping("/{id}/report")
    public String reportUser(@PathVariable Long id,
                             @AuthenticationPrincipal UserDetails principal,
                             @RequestParam String description) {
        User user = userService.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Booking booking = bookingService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + id));

        Long reportedUserId = user.getId().equals(booking.getTourist().getId())
                ? booking.getGuide().getId()
                : booking.getTourist().getId();

        adminService.createDirectDispute(user.getId(), reportedUserId, id, description);
        return "redirect:/bookings/" + id + "?reported=true";
    }
}
