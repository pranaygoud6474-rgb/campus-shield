package com.campussafety.service;

import com.campussafety.dto.AiRecommendationResponse;

import java.util.Map;

public interface AiService {
    AiRecommendationResponse classifyIncident(String description);
    AiRecommendationResponse recommendSeverity(String description);
    Map<String, Object> getSafetyInsights();
    String generateSummary(String description);
}
