package com.campussafety.controller;

import com.campussafety.dto.AiRecommendationResponse;
import com.campussafety.dto.AiRequest;
import com.campussafety.entity.User;
import com.campussafety.service.AiService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/classify-incident")
    @PreAuthorize("hasAnyRole('STUDENT','SECURITY_OFFICER','ADMIN')")
    public ResponseEntity<AiRecommendationResponse> classify(@AuthenticationPrincipal User user,
                                                            @Valid @RequestBody AiRequest request) {
        return ResponseEntity.ok(aiService.classifyIncident(request.description()));
    }

    @PostMapping("/recommend-severity")
    @PreAuthorize("hasAnyRole('STUDENT','SECURITY_OFFICER','ADMIN')")
    public ResponseEntity<AiRecommendationResponse> recommend(@AuthenticationPrincipal User user,
                                                             @Valid @RequestBody AiRequest request) {
        return ResponseEntity.ok(aiService.recommendSeverity(request.description()));
    }

    @GetMapping("/safety-insights")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> insights() {
        return ResponseEntity.ok(aiService.getSafetyInsights());
    }

    @PostMapping("/summarize-incident")
    @PreAuthorize("hasAnyRole('SECURITY_OFFICER','ADMIN')")
    public ResponseEntity<Map<String, String>> summarize(@AuthenticationPrincipal User user,
                                                        @Valid @RequestBody AiRequest request) {
        return ResponseEntity.ok(Map.of(
                "summary", aiService.generateSummary(request.description()),
                "label", "AI suggestion"
        ));
    }
}
