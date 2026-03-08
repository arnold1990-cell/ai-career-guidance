package com.edurite.application.controller;

import com.edurite.application.entity.ApplicationRecord;
import com.edurite.application.service.ApplicationService;
import java.security.Principal;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bursaries")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping("/{id}/applications")
    public ApplicationRecord apply(@PathVariable UUID id, Principal principal) {
        return applicationService.submit(id, UUID.nameUUIDFromBytes(principal.getName().getBytes()));
    }
}
