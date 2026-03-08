package com.edurite.career.controller;

import com.edurite.career.entity.Career;
import com.edurite.career.repository.CareerRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/careers")
public class CareerController {

    private final CareerRepository careerRepository;

    public CareerController(CareerRepository careerRepository) {
        this.careerRepository = careerRepository;
    }

    @GetMapping
    public List<Career> list() { return careerRepository.findAll(); }

    @GetMapping("/{id}")
    public Career get(@PathVariable UUID id) { return careerRepository.findById(id).orElseThrow(); }
}
