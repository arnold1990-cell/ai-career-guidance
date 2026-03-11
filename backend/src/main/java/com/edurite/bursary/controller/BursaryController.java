package com.edurite.bursary.controller; // declares the package path for this Java file

import com.edurite.bursary.entity.Bursary; // imports a class so it can be used in this file
import com.edurite.bursary.repository.BursaryRepository; // imports a class so it can be used in this file
import java.util.UUID; // imports a class so it can be used in this file
import org.springframework.data.domain.Page; // imports a class so it can be used in this file
import org.springframework.data.domain.PageRequest; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.GetMapping; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.PathVariable; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.RequestMapping; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.RequestParam; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.RestController; // imports a class so it can be used in this file

// @RestController tells Spring this class exposes REST API endpoints.
@RestController // marks this class as a REST controller that handles HTTP requests
// @RequestMapping defines the base URL path for endpoints in this controller.
@RequestMapping("/api/v1/bursaries") // sets the base URL path for endpoints in this controller
/**
 * This class named BursaryController is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class BursaryController { // defines a class type

    private final BursaryRepository bursaryRepository; // reads or writes data through the database layer

    public BursaryController(BursaryRepository bursaryRepository) { // reads or writes data through the database layer
        this.bursaryRepository = bursaryRepository; // reads or writes data through the database layer
    } // ends the current code block

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping // maps this method to handle HTTP GET requests
    public Page<Bursary> list( // supports the surrounding application logic
            @RequestParam(defaultValue = "") String q, // binds a query parameter value to this method parameter
            @RequestParam(defaultValue = "") String qualificationLevel, // binds a query parameter value to this method parameter
            @RequestParam(defaultValue = "") String location, // binds a query parameter value to this method parameter
            @RequestParam(defaultValue = "") String eligibility, // binds a query parameter value to this method parameter
            @RequestParam(defaultValue = "0") int page, // binds a query parameter value to this method parameter
            @RequestParam(defaultValue = "10") int size // binds a query parameter value to this method parameter
    ) { // supports the surrounding application logic
        return bursaryRepository.findByTitleContainingIgnoreCaseAndQualificationLevelContainingIgnoreCaseAndLocationContainingIgnoreCaseAndEligibilityContainingIgnoreCase( // returns a value from this method to the caller
                q, qualificationLevel, location, eligibility, PageRequest.of(page, size)); // executes this statement as part of the application logic
    } // ends the current code block

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping("/{id}") // maps this method to handle HTTP GET requests
    public Bursary get(@PathVariable UUID id) { return bursaryRepository.findById(id).orElseThrow(); } // reads or writes data through the database layer
} // ends the current code block
