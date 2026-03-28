package com.guideconnect.model;

<<<<<<< HEAD
import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

=======
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

>>>>>>> 3229964df188615c94251e3acd655976dbec09b2
/**
 * Tour listing entity for guide-created tours (FR-TM-01, FR-TM-03).
 */
@Entity
@Table(name = "tour_listings", indexes = {
    @Index(name = "idx_tour_city", columnList = "city"),
    @Index(name = "idx_tour_price", columnList = "pricePerPerson"),
    @Index(name = "idx_tour_guide", columnList = "guide_id")
})
public class TourListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String title;

    @NotBlank
    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @NotBlank
    @Column(nullable = false)
    private String city;

    @NotBlank
    @Column(nullable = false)
    private String meetingLocation;

    @Min(1)
    @Column(nullable = false)
    private Integer durationHours;

    @NotNull
    @DecimalMin("0.01")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerPerson;

    @NotBlank
    private String languages;

    @Min(1)
    @Column(nullable = false)
    private Integer maxGroupSize;

    private String category;

    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_id", nullable = false)
    private User guide;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

<<<<<<< HEAD
    private String imgPath="/images/testing.png";

=======
>>>>>>> 3229964df188615c94251e3acd655976dbec09b2
    public TourListing() {}

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getMeetingLocation() { return meetingLocation; }
    public void setMeetingLocation(String meetingLocation) { this.meetingLocation = meetingLocation; }

    public Integer getDurationHours() { return durationHours; }
    public void setDurationHours(Integer durationHours) { this.durationHours = durationHours; }

    public BigDecimal getPricePerPerson() { return pricePerPerson; }
    public void setPricePerPerson(BigDecimal pricePerPerson) { this.pricePerPerson = pricePerPerson; }

    public String getLanguages() { return languages; }
    public void setLanguages(String languages) { this.languages = languages; }

    public Integer getMaxGroupSize() { return maxGroupSize; }
    public void setMaxGroupSize(Integer maxGroupSize) { this.maxGroupSize = maxGroupSize; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public User getGuide() { return guide; }
    public void setGuide(User guide) { this.guide = guide; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
<<<<<<< HEAD

    public String getImgPath() { 
        return imgPath; 
    }

    public void setImgPath(String imgPath) { 
        this.imgPath = imgPath; 
    }
=======
>>>>>>> 3229964df188615c94251e3acd655976dbec09b2
}
