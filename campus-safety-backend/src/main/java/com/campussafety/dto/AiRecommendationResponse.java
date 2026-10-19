package com.campussafety.dto;

import java.util.List;

public record AiRecommendationResponse(
        String predictedCategory,
        String severity,
        int priorityScore,
        List<String> reasons,
        String summary,
        String suggestionLabel
) {
}
