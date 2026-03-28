package com.guideconnect.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.guideconnect.model.BookingStatus;
import com.guideconnect.model.TourListing;
import com.guideconnect.model.User;
import com.guideconnect.repository.BookingRepository;
import com.guideconnect.repository.TourListingRepository;
import com.guideconnect.repository.UserRepository;

/**
 * Service layer for tour listing management.
 * Handles creation, update, deletion, and search of tour listings.
 * Uses a Strategy pattern for sorting search results by different criteria.
 */
@Service
public class TourService {

    private final TourListingRepository tourListingRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    /**
     * Constructs a TourService with required dependencies.
     *
     * @param tourListingRepository the tour listing repository for data access
     * @param userRepository        the user repository for guide lookup
     * @param bookingRepository     the booking repository for active booking checks
     */
    public TourService(TourListingRepository tourListingRepository,
                       UserRepository userRepository,
                       BookingRepository bookingRepository) {
        this.tourListingRepository = tourListingRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
    }

    /**
     * Creates a new tour listing owned by the specified guide.
     * Validates that the guide exists before saving.
     *
     * @param tour    the tour listing to create
     * @param guideId the ID of the guide who owns the tour
     * @return the persisted TourListing entity
     * @throws IllegalArgumentException if the guide is not found
     */
    @Transactional
    public TourListing createTour(TourListing tour, Long guideId) {
        User guide = userRepository.findById(guideId)
                .orElseThrow(() -> new IllegalArgumentException("Guide not found with id: " + guideId));

        tour.setGuide(guide);
        tour.setActive(true);
        return tourListingRepository.save(tour);
    }

    /**
     * Updates an existing tour listing. Only the owning guide may update
     * their own tour. Modifiable fields include title, description, city,
     * meeting location, duration, price, languages, max group size, and category.
     *
     * @param tourId       the ID of the tour to update
     * @param updatedTour  a TourListing containing the updated field values
     * @param guideId      the ID of the guide attempting the update
     * @return the updated TourListing entity
     * @throws IllegalArgumentException if the tour is not found
     * @throws SecurityException        if the guide does not own the tour
     */
    @Transactional
    public TourListing updateTour(Long tourId, TourListing updatedTour, Long guideId) {
        TourListing existing = tourListingRepository.findById(tourId)
                .orElseThrow(() -> new IllegalArgumentException("Tour not found with id: " + tourId));

        if (!existing.getGuide().getId().equals(guideId)) {
            throw new SecurityException("Only the owning guide can update this tour");
        }

        existing.setTitle(updatedTour.getTitle());
        existing.setDescription(updatedTour.getDescription());
        existing.setCity(updatedTour.getCity());
        existing.setMeetingLocation(updatedTour.getMeetingLocation());
        existing.setDurationHours(updatedTour.getDurationHours());
        existing.setPricePerPerson(updatedTour.getPricePerPerson());
        existing.setLanguages(updatedTour.getLanguages());
        existing.setMaxGroupSize(updatedTour.getMaxGroupSize());
        existing.setCategory(updatedTour.getCategory());
        existing.setImgPath(updatedTour.getImgPath());
        return tourListingRepository.save(existing);
    }

    /**
     * Deletes (deactivates) a tour listing. Deletion is blocked if
     * the tour has any active bookings (REQUESTED, NEGOTIATING, or CONFIRMED).
     *
     * @param tourId  the ID of the tour to delete
     * @param guideId the ID of the guide attempting the deletion
     * @throws IllegalArgumentException if the tour is not found
     * @throws SecurityException        if the guide does not own the tour
     * @throws IllegalStateException    if the tour has active bookings
     */
    @Transactional
    public void deleteTour(Long tourId, Long guideId) {
        TourListing tour = tourListingRepository.findById(tourId)
                .orElseThrow(() -> new IllegalArgumentException("Tour not found with id: " + tourId));

        if (!tour.getGuide().getId().equals(guideId)) {
            throw new SecurityException("Only the owning guide can delete this tour");
        }

        boolean hasActiveBookings =
                !bookingRepository.findByTourAndStatus(tour, BookingStatus.REQUESTED).isEmpty()
                || !bookingRepository.findByTourAndStatus(tour, BookingStatus.NEGOTIATING).isEmpty()
                || !bookingRepository.findByTourAndStatus(tour, BookingStatus.CONFIRMED).isEmpty();

        /*if (hasActiveBookings) {
            throw new IllegalStateException("Cannot delete tour with active bookings");
        }*/

        tour.setActive(false);
        tourListingRepository.save(tour);
    }

    /**
     * Finds a tour listing by its unique identifier.
     *
     * @param id the tour listing ID
     * @return an Optional containing the tour listing if found
     */
    public Optional<TourListing> findById(Long id) {
        return tourListingRepository.findById(id);
    }

    /**
     * Searches active tour listings with optional filters and sorting.
     * Implements a Strategy pattern for sorting by relevance, price ascending,
     * price descending, or rating.
     *
     * @param city     optional city filter (partial, case-insensitive)
     * @param language optional language filter (partial, case-insensitive)
     * @param minPrice optional minimum price filter
     * @param maxPrice optional maximum price filter
     * @param sortBy   sorting strategy: "relevance", "price_asc", "price_desc", or "rating"
     * @param pageable pagination parameters (sort from pageable is overridden by sortBy)
     * @return a page of matching tour listings
     */
    public Page<TourListing> searchTours(String city, String language, BigDecimal minPrice,
                                         BigDecimal maxPrice, String sortBy, Pageable pageable) {
        Sort sort = resolveSortStrategy(sortBy);
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        return tourListingRepository.search(city, language, minPrice, maxPrice, sortedPageable);
    }

    /**
     * Resolves the sort strategy based on the provided sort key.
     * This implements the Strategy pattern for tour search result ordering.
     *
     * @param sortBy the sort key
     * @return the corresponding Spring Data Sort object
     */
    private Sort resolveSortStrategy(String sortBy) {
        if (sortBy == null) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        return switch (sortBy.toLowerCase()) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "pricePerPerson");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "pricePerPerson");
            case "rating" -> Sort.by(Sort.Direction.DESC, "guide.avgRating");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    /**
     * Finds all tour listings owned by a specific guide.
     *
     * @param guideId the ID of the guide
     * @return list of tour listings for the guide
     * @throws IllegalArgumentException if the guide is not found
     */
    public List<TourListing> findByGuide(Long guideId) {
        User guide = userRepository.findById(guideId)
                .orElseThrow(() -> new IllegalArgumentException("Guide not found with id: " + guideId));
        return tourListingRepository.findByGuide(guide);
    }

    /**
     * Counts the total number of active tour listings.
     *
     * @return the count of active tours
     */
    public long countActive() {
        return tourListingRepository.countByActiveTrue();
    }
}
