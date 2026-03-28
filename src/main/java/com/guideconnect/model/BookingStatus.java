package com.guideconnect.model;

/**
 * Booking lifecycle states implementing the State Pattern (FR-BK-02).
 * Transitions: REQUESTED -> NEGOTIATING -> CONFIRMED -> COMPLETED
 * Cancellation from: REQUESTED, NEGOTIATING, CONFIRMED
 */
public enum BookingStatus {
    REQUESTED,
    NEGOTIATING,
    CONFIRMED,
    COMPLETED,
    CANCELLED,
    REJECTED
}
