package com.edurite.career.controller; // declares the package path for this Java file

import com.edurite.career.entity.Career; // imports a class so it can be used in this file
import com.edurite.career.repository.CareerRepository; // imports a class so it can be used in this file
import org.springframework.data.domain.Page; // imports a class so it can be used in this file
import org.springframework.data.domain.PageRequest; // imports a class so it can be used in this file
import java.util.UUID; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.GetMapping; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.PathVariable; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.RequestMapping; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.RequestParam; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.RestController; // imports a class so it can be used in this file

// @RestController tells Spring this class exposes REST API endpoints.
@RestController // marks this class as a REST controller that handles HTTP requests
// @RequestMapping defines the base URL path for endpoints in this controller.
@RequestMapping("/api/v1/careers") // sets the base URL path for endpoints in this controller
/**
 * This class named CareerController is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class CareerController { // defines a class type

    private final CareerRepository careerRepository; // reads or writes data through the database layer

    public CareerController(CareerRepository careerRepository) { // reads or writes data through the database layer
        this.careerRepository = careerRepository; // reads or writes data through the database layer
    } // ends the current code block

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping // maps this method to handle HTTP GET requests
    public Page<Career> list( // supports the surrounding application logic
            @RequestParam(defaultValue = "") String q, // binds a query parameter value to this method parameter
            @RequestParam(defaultValue = "") String field, // binds a query parameter value to this method parameter
            @RequestParam(defaultValue = "") String industry, // binds a query parameter value to this method parameter
            @RequestParam(defaultValue = "") String qualificationLevel, // binds a query parameter value to this method parameter
            @RequestParam(defaultValue = "") String location, // binds a query parameter value to this method parameter
            @RequestParam(defaultValue = "") String demand, // binds a query parameter value to this method parameter
            @RequestParam(defaultValue = "") String salaryRange, // binds a query parameter value to this method parameter
            @RequestParam(defaultValue = "0") int page, // binds a query parameter value to this method parameter
            @RequestParam(defaultValue = "10") int size // binds a query parameter value to this method parameter
    ) { // supports the surrounding application logic
        String industryFilter = !industry.isBlank() ? industry : field; // executes this statement as part of the application logic
        return careerRepository.search( // returns a value from this method to the caller
                q, // supports the surrounding application logic
                industryFilter, // supports the surrounding application logic
                qualificationLevel, // supports the surrounding application logic
                location, // supports the surrounding application logic
                demand, // supports the surrounding application logic
                salaryRange, // supports the surrounding application logic
                PageRequest.of(page, size)); // executes this statement as part of the application logic
    } // ends the current code block

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping("/{id}") // maps this method to handle HTTP GET requests
    public Career get(@PathVariable UUID id) { return careerRepository.findById(id).orElseThrow(); } // reads or writes data through the database layer
} // ends the current code block
