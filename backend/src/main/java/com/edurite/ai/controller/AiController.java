package com.edurite.ai.controller;

import com.edurite.ai.dto.CareerAdviceRequest;
import com.edurite.ai.dto.CareerAdviceResponse;
import com.edurite.ai.service.AiServiceException;
import com.edurite.ai.service.GeminiService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final GeminiService geminiService;

    public AiController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping("/career-advice")
    public ResponseEntity<?> getCareerAdvice(
            @Valid @RequestBody CareerAdviceRequest request,
            HttpServletRequest httpRequest
    ) {
        try {
            CareerAdviceResponse response = geminiService.getCareerAdvice(request);
            return ResponseEntity.ok(response);
        } catch (AiServiceException e) {
            return ResponseEntity.status(e.getStatus()).body(errorBody(e.getStatus().value(), e.getMessage(), httpRequest));
        }
    }

    private Map<String, Object> errorBody(int status, String message, HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status);
        body.put("error", message);
        body.put("path", request.getRequestURI());
        return body;
    }
}
