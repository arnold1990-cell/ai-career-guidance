package com.edurite.ai.controller;

import com.edurite.ai.dto.BookwormChatRequest;
import com.edurite.ai.dto.BookwormChatResponse;
import com.edurite.ai.service.BookwormService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookworm")
public class BookwormController {

    private final BookwormService bookwormService;

    public BookwormController(BookwormService bookwormService) {
        this.bookwormService = bookwormService;
    }

    @PostMapping("/chat")
    public ResponseEntity<BookwormChatResponse> chat(Principal principal, @Valid @RequestBody BookwormChatRequest request) {
        return ResponseEntity.ok(bookwormService.chat(principal, request));
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<String>> suggestions() {
        return ResponseEntity.ok(bookwormService.suggestions());
    }
}
