package com.guideconnect.service;

import com.guideconnect.model.Booking;
import com.guideconnect.model.Dispute;
import com.guideconnect.model.DisputeStatus;
import com.guideconnect.model.Message;
import com.guideconnect.model.User;
import com.guideconnect.repository.BookingRepository;
import com.guideconnect.repository.DisputeRepository;
import com.guideconnect.repository.MessageRepository;
import com.guideconnect.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service layer for in-platform messaging between tourists and guides.
 * Messages are always associated with a booking context. Supports
 * message flagging which automatically creates a dispute for admin review.
 */
@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final DisputeRepository disputeRepository;

    /**
     * Constructs a MessageService with required dependencies.
     *
     * @param messageRepository the message repository for data access
     * @param userRepository    the user repository for user lookup
     * @param bookingRepository the booking repository for booking validation
     * @param disputeRepository the dispute repository for dispute creation on flag
     */
    public MessageService(MessageRepository messageRepository,
                          UserRepository userRepository,
                          BookingRepository bookingRepository,
                          DisputeRepository disputeRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.disputeRepository = disputeRepository;
    }

    /**
     * Sends a message from one user to another within the context of a booking.
     * Validates that the sender, receiver, and booking all exist before sending.
     *
     * @param senderId   the ID of the message sender
     * @param receiverId the ID of the message receiver
     * @param bookingId  the ID of the associated booking
     * @param content    the message text content
     * @return the persisted Message entity
     * @throws IllegalArgumentException if sender, receiver, or booking is not found
     */
    @Transactional
    public Message sendMessage(Long senderId, Long receiverId, Long bookingId, String content) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("Sender not found with id: " + senderId));

        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new IllegalArgumentException("Receiver not found with id: " + receiverId));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));

        Message message = new Message(sender, receiver, booking, content);
        return messageRepository.save(message);
    }

    /**
     * Retrieves all messages for a booking in chronological order.
     *
     * @param bookingId the ID of the booking
     * @return list of messages sorted by timestamp ascending
     * @throws IllegalArgumentException if the booking is not found
     */
    public List<Message> getMessagesForBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));
        return messageRepository.findByBookingAndFlaggedFalseOrderByTimestampAsc(booking);
    }

    public List<Booking> getBookingsWithMessagesForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        return messageRepository.findDistinctBookingsByUser(user);
    }

    public long getUnreadCountForBooking(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return messageRepository.countByBookingAndReceiverAndReadFalse(booking, user);
    }

    @Transactional
    public void markMessagesAsRead(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        List<Message> unread = messageRepository
                .findByBookingAndReceiverAndReadFalse(booking, user);
        unread.forEach(m -> m.setRead(true));
        messageRepository.saveAll(unread);
    }
    /**
     * Flags a message for admin review and automatically creates an OPEN dispute.
     * The dispute is linked to both the flagged message and its associated booking.
     *
     * @param messageId the ID of the message to flag
     * @param userId    the ID of the user reporting the message
     * @return the flagged Message entity
     * @throws IllegalArgumentException if the message or reporting user is not found
     */
    @Transactional
    public Message flagMessage(Long messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found with id: " + messageId));

        User reporter = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        if (message.isFlagged()) {
            return message;
        }

        message.setFlagged(true);
        messageRepository.save(message);

        Dispute dispute = new Dispute();
        dispute.setFlaggedMessage(message);
        dispute.setBooking(message.getBooking());
        dispute.setReporter(reporter);
        dispute.setDescription("Flagged message in booking #" + message.getBooking().getId());
        dispute.setStatus(DisputeStatus.OPEN);
        disputeRepository.save(dispute);

        return message;
    }

    /**
     * Retrieves all flagged messages with pagination support.
     * Intended for admin review of reported messages.
     *
     * @param pageable pagination and sorting parameters
     * @return a page of flagged messages
     */
    public Page<Message> getFlaggedMessages(Pageable pageable) {
        return messageRepository.findByFlaggedTrue(pageable);
    }
}
