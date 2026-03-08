package com.edurite.bursary.controller;

import com.edurite.bursary.entity.Bursary;
import com.edurite.bursary.repository.BursaryRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bursaries")
public class BursaryController {

    private final BursaryRepository bursaryRepository;

    public BursaryController(BursaryRepository bursaryRepository) {
        this.bursaryRepository = bursaryRepository;
    }

    @GetMapping
    public List<Bursary> list() { return bursaryRepository.findAll(); }

    @GetMapping("/{id}")
    public Bursary get(@PathVariable UUID id) { return bursaryRepository.findById(id).orElseThrow(); }
}
