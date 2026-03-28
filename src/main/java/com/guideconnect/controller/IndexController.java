package com.guideconnect.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.guideconnect.model.TourListing;
import com.guideconnect.repository.TourListingRepository;

@Controller
public class IndexController {

    @Autowired
    private TourListingRepository tourListingRepository;

    @GetMapping("/")
    public String index(Model model) {
        // Fetch the newest tours from the database
        List<TourListing> featuredTours = tourListingRepository.findTop3ByOrderByIdDesc();
        
        // Add them to the model so index.html can see them
        model.addAttribute("featuredTours", featuredTours);
        
        // Returns the name of the template (src/main/resources/templates/index.html)
        return "index";
    }
}

