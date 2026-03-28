package com.guideconnect.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Booking entity implementing the booking lifecycle (FR-BK-01, FR-BK-02).
 * State transitions: REQUESTED -> NEGOTIATING -> CONFIRMED -> COMPLETED -> CANCELLED/REJECTED
 */
@Entity
@Table(name = "bookings", indexes = {
    @Index(name = "idx_booking_tourist", columnList = "tourist_id"),
    @Index(name = "idx_booking_guide", columnList = "guide_id"),
    @Index(name = "idx_booking_status", columnList = "status")
})
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tourist_id", nullable = false)
    private User tourist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id", nullable = false)
    private TourListing tour;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_id", nullable = false)
    private User guide;

    @Column(nullable = false)
    private LocalDate requestedDate;

    private LocalTime requestedTime;

    @Min(1)
    @Column(nullable = false)
    private Integer groupSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.REQUESTED;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

    public Booking() {}

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Validates whether a status transition is allowed (State Pattern).
     */
    public boolean canTransitionTo(BookingStatus newStatus) {
        return switch (this.status) {
            case REQUESTED -> newStatus == BookingStatus.NEGOTIATING
                    || newStatus == BookingStatus.CONFIRMED
                    || newStatus == BookingStatus.REJECTED
                    || newStatus == BookingStatus.CANCELLED;
            case NEGOTIATING -> newStatus == BookingStatus.CONFIRMED
                    || newStatus == BookingStatus.REJECTED
                    || newStatus == BookingStatus.CANCELLED;
            case CONFIRMED -> newStatus == BookingStatus.COMPLETED
                    || newStatus == BookingStatus.CANCELLED;
            default -> false;
        };
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getTourist() { return tourist; }
    public void setTourist(User tourist) { this.tourist = tourist; }

    public TourListing getTour() { return tour; }
    public void setTour(TourListing tour) { this.tour = tour; }

    public User getGuide() { return guide; }
    public void setGuide(User guide) { this.guide = guide; }

    public LocalDate getRequestedDate() { return requestedDate; }
    public void setRequestedDate(LocalDate requestedDate) { this.requestedDate = requestedDate; }

    public LocalTime getRequestedTime() { return requestedTime; }
    public void setRequestedTime(LocalTime requestedTime) { this.requestedTime = requestedTime; }

    public Integer getGroupSize() { return groupSize; }
    public void setGroupSize(Integer groupSize) { this.groupSize = groupSize; }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
