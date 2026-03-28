package com.guideconnect.repository;

import com.guideconnect.model.TourListing;
import com.guideconnect.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Spring Data JPA repository for {@link TourListing} entities.
 * Provides query methods for tour discovery, filtering, and guide-specific lookups.
 */
@Repository
public interface TourListingRepository extends JpaRepository<TourListing, Long> {

    /**
     * Finds all tour listings created by a specific guide.
     *
     * @param guide the guide user who owns the listings
     * @return list of tour listings for the given guide
     */
    List<TourListing> findByGuide(User guide);

    /**
     * Finds all currently active tour listings.
     *
     * @return list of active tour listings
     */
    List<TourListing> findByActiveTrue();

    /**
     * Searches active tour listings with optional filters for city, language, and price range.
     * All filter parameters are optional; pass {@code null} to skip a filter.
     * Results are paginated.
     *
     * @param city     partial city name match (case-insensitive), or null to skip
     * @param language partial language match (case-insensitive), or null to skip
     * @param minPrice minimum price per person, or null for no lower bound
     * @param maxPrice maximum price per person, or null for no upper bound
     * @param pageable pagination and sorting parameters
     * @return a page of matching active tour listings
     */
    @Query("SELECT t FROM TourListing t WHERE t.active = true "
         + "AND (:city IS NULL OR LOWER(t.city) LIKE LOWER(CONCAT('%', CAST(:city AS string), '%'))) "
         + "AND (:language IS NULL OR LOWER(t.languages) LIKE LOWER(CONCAT('%', CAST(:language AS string), '%'))) "
         + "AND (:minPrice IS NULL OR t.pricePerPerson >= :minPrice) "
         + "AND (:maxPrice IS NULL OR t.pricePerPerson <= :maxPrice)")
    Page<TourListing> search(@Param("city") String city,
                             @Param("language") String language,
                             @Param("minPrice") BigDecimal minPrice,
                             @Param("maxPrice") BigDecimal maxPrice,
                             Pageable pageable);

    /**
     * Finds tour listings whose city contains the given string (case-insensitive).
     *
     * @param city the city substring to search for
     * @return list of matching tour listings
     */
    List<TourListing> findByCityContainingIgnoreCase(String city);

    /**
     * Counts the total number of active tour listings.
     *
     * @return the count of active tour listings
     */
    long countByActiveTrue();
<<<<<<< HEAD

    //new 3 tour as feature
    List<TourListing> findTop3ByOrderByIdDesc();
=======
>>>>>>> 3229964df188615c94251e3acd655976dbec09b2
}
