package com.edurite.ai.controller;

import com.edurite.ai.dto.AiGuidanceRequest;
import com.edurite.ai.dto.AiGuidanceResponse;
import com.edurite.ai.service.AiGuidanceService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
/**
 * Exposes AI guidance endpoints while keeping request handling thin.
 */
public class AiGuidanceController {

    private final AiGuidanceService aiGuidanceService;

    public AiGuidanceController(AiGuidanceService aiGuidanceService) {
        this.aiGuidanceService = aiGuidanceService;
    }

    @PostMapping("/guidance")
    public ResponseEntity<AiGuidanceResponse> generateGuidance(
            @Valid @RequestBody AiGuidanceRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(aiGuidanceService.generateGuidance(request, principal));
    }

    @PostMapping("/guidance/me")
    public ResponseEntity<AiGuidanceResponse> generateGuidanceFromStoredProfile(Principal principal) {
        return ResponseEntity.ok(aiGuidanceService.generateGuidanceFromProfile(principal));
    }
}
