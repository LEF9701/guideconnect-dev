package com.guideconnect.controller;

import com.guideconnect.model.Booking;
import com.guideconnect.model.User;
import com.guideconnect.service.BookingService;
import com.guideconnect.service.MessageService;
import com.guideconnect.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.LocalDateTime;
import com.guideconnect.model.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for in-app messaging tied to bookings.
 *
 * <p>Provides a chat view for each booking where tourists and guides can
 * exchange messages, as well as the ability to flag inappropriate messages.</p>
 */
@Controller
@RequestMapping("/messages")
public class MessageController {

    private final MessageService messageService;
    private final UserService userService;
    private final BookingService bookingService;

    /**
     * Constructs a {@code MessageController} with the required service dependencies.
     *
     * @param messageService the service for message operations
     * @param userService    the service for user lookup
     * @param bookingService the service for booking lookup
     */
    public MessageController(MessageService messageService, UserService userService, BookingService bookingService) {
        this.messageService = messageService;
        this.userService = userService;
        this.bookingService = bookingService;
    }

    /**
     * Displays the chat view with all messages for a specific booking.
     *
     * @param bookingId the ID of the booking whose messages are displayed
     * @param principal the currently authenticated user's security principal
     * @param model     the Thymeleaf model
     * @return the booking messages view name
     */
    @GetMapping("/booking/{bookingId}")
    public String showMessages(@PathVariable Long bookingId,
                               @AuthenticationPrincipal UserDetails principal,
                               @RequestParam(value = "reported", required = false) String reported,
                               Model model) {
        User user = userService.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Booking booking = bookingService.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));
        model.addAttribute("user", user);
        model.addAttribute("booking", booking);
        model.addAttribute("bookingId", bookingId);
        model.addAttribute("messages", messageService.getMessagesForBooking(bookingId));
        model.addAttribute("reported", reported != null);
        messageService.markMessagesAsRead(bookingId, user.getId());
        return "booking/messages";
    }

    /**
     * Sends a new message in the context of a booking conversation.
     *
     * @param bookingId the ID of the booking this message belongs to
     * @param content   the message text
     * @param principal the currently authenticated user's security principal
     * @return a redirect back to the chat view
     */
    @PostMapping("/booking/{bookingId}")
    public String sendMessage(@PathVariable Long bookingId,
                              @RequestParam String content,
                              @AuthenticationPrincipal UserDetails principal) {
        User user = userService.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Booking booking = bookingService.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));
        // Determine the receiver: if the sender is the tourist, receiver is the guide, and vice versa
        User receiver;
        if (user.getId().equals(booking.getTourist().getId())) {
            receiver = booking.getGuide();
        } else {
            receiver = booking.getTourist();
        }
        messageService.sendMessage(user.getId(), receiver.getId(), bookingId, content);
        return "redirect:/messages/booking/" + bookingId;
    }

    /**
     * Flags a message as inappropriate for admin review.
     *
     * @param messageId the ID of the message to flag
     * @param bookingId the booking ID used to redirect back to the chat
     * @param principal the currently authenticated user's security principal
     * @return a redirect back to the chat view
     */
    @PostMapping("/{messageId}/flag")
    public String flagMessage(@PathVariable Long messageId,
                              @RequestParam Long bookingId,
                              @AuthenticationPrincipal UserDetails principal) {
        User user = userService.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        messageService.flagMessage(messageId, user.getId());
        return "redirect:/messages/booking/" + bookingId + "?reported=true";
    }

    @GetMapping
    public String showInbox(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = userService.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        List<Booking> bookingsWithMessages = new ArrayList<>(
                messageService.getBookingsWithMessagesForUser(user.getId()));

        // build unread count map
        Map<Long, Long> unreadCounts = new HashMap<>();
        for (Booking booking : bookingsWithMessages) {
            long count = messageService.getUnreadCountForBooking(booking.getId(), user.getId());
            unreadCounts.put(booking.getId(), count);
        }

        // sort — bookings with unread messages first
        bookingsWithMessages.sort((a, b) -> {
            List<Message> messagesA = messageService.getMessagesForBooking(a.getId());
            List<Message> messagesB = messageService.getMessagesForBooking(b.getId());

            LocalDateTime latestA = messagesA.isEmpty() ? LocalDateTime.MIN :
                    messagesA.get(messagesA.size() - 1).getTimestamp();
            LocalDateTime latestB = messagesB.isEmpty() ? LocalDateTime.MIN :
                    messagesB.get(messagesB.size() - 1).getTimestamp();

            return latestB.compareTo(latestA); // newest first
        });
        model.addAttribute("user", user);
        model.addAttribute("bookingsWithMessages", bookingsWithMessages);
        model.addAttribute("unreadCounts", unreadCounts);
        return "messages/inbox";
    }
}
