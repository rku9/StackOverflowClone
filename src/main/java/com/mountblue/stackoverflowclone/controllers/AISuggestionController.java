package com.mountblue.stackoverflowclone.controllers;

import com.mountblue.stackoverflowclone.services.AISuggestionService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AISuggestionController {
    private final AISuggestionService aiSuggestionService;

    public AISuggestionController(AISuggestionService aiSuggestionService) {
        this.aiSuggestionService = aiSuggestionService;
    }
    @GetMapping("/suggestion/{questionId}")
    public Map<String, String> getSuggestion(@PathVariable Long questionId) {
        String aiResponse = aiSuggestionService.getResponse(questionId);
        return Map.of("aiResponse", aiResponse);
    }
}

