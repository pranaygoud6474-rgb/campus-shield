package com.campussafety.service.impl;

import com.campussafety.dto.AiRecommendationResponse;
import com.campussafety.entity.Incident;
import com.campussafety.repository.IncidentRepository;
import com.campussafety.service.AiService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AiServiceImpl implements AiService {

    private final IncidentRepository incidentRepository;

    public AiServiceImpl(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    @Override
    public AiRecommendationResponse classifyIncident(String description) {
        String normalized = description == null ? "" : description.toLowerCase();
        String category = "OTHER_SAFETY_CONCERN";
        if (normalized.contains("fire") || normalized.contains("smoke")) {
            category = "FIRE_OR_SMOKE";
        } else if (normalized.contains("medical") || normalized.contains("injury") || normalized.contains("unconscious") || normalized.contains("heart") || normalized.contains("bleeding")) {
            category = "MEDICAL_EMERGENCY";
        } else if (normalized.contains("fight") || normalized.contains("assault") || normalized.contains("harassment") || normalized.contains("threat")) {
            category = "PHYSICAL_ASSAULT_OR_HARASSMENT";
        } else if (normalized.contains("theft") || normalized.contains("stolen") || normalized.contains("vandal")) {
            category = "THEFT_OR_PROPERTY_DAMAGE";
        } else if (normalized.contains("power") || normalized.contains("hazard") || normalized.contains("leak") || normalized.contains("broken")) {
            category = "INFRASTRUCTURE_HAZARD";
        }

        List<String> reasons = new ArrayList<>();
        reasons.add("Keyword-based local risk analysis");
        reasons.add("AI suggestion only — verify with human review");

        String severity = severityFor(normalized);
        return new AiRecommendationResponse(
                category,
                severity,
                scoreFor(severity),
                List.of("Keyword-based local risk analysis", "AI suggestion only — verify with human review"),
                "Local fallback classification suggests: " + category,
                "AI suggestion"
        );
    }

    @Override
    public AiRecommendationResponse recommendSeverity(String description) {
        String normalized = description == null ? "" : description.toLowerCase();
        String severity = "MEDIUM";
        List<String> reasons = new java.util.ArrayList<>();
        if (normalized.contains("gun") || normalized.contains("fire") || normalized.contains("unconscious") || normalized.contains("assault")) {
            severity = "CRITICAL";
            reasons.add("Serious threat or immediate harm risk detected");
        } else if (normalized.contains("medical") || normalized.contains("injury") || normalized.contains("smoke") || normalized.contains("theft")) {
            severity = "HIGH";
            reasons.add("Potential injury, exposure, or disruption was mentioned");
        } else {
            reasons.add("Routine campus safety concern without immediate life-threatening indicators");
        }

        return new AiRecommendationResponse(
                "OTHER_SAFETY_CONCERN",
                severity,
                severity.equals("CRITICAL") ? 95 : severity.equals("HIGH") ? 80 : 55,
                reasons,
                "AI suggestion: severity " + severity + " based on provided details.",
                "AI suggestion"
        );
    }

    @Override
    public Map<String, Object> getSafetyInsights() {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        List<Incident> incidents = incidentRepository.findAll();
        Map<String, Long> categoryCounts = incidents.stream()
                .collect(Collectors.groupingBy(incident -> incident.getCategory().name(), Collectors.counting()));
        map.put("generatedAt", java.time.LocalDateTime.now());
        map.put("source", "local-fallback");
        map.put("totalIncidentsAnalyzed", incidents.size());
        map.put("categoryCounts", categoryCounts);
        map.put("insights", incidents.isEmpty()
                ? List.of("No incident records are available to analyze yet.")
                : List.of("Most frequently reported category in stored records: " + categoryCounts.entrySet().stream()
                        .max(Map.Entry.comparingByValue()).orElseThrow().getKey() + "."));
        map.put("label", "Local analysis of stored incident records; recommendations require human review");
        return map;
    }

    @Override
    public String generateSummary(String description) {
        if (description == null || description.isBlank()) {
            return "AI suggestion: no description provided for summary generation.";
        }
        return "AI suggestion: The report describes a safety concern requiring review by campus staff and, if needed, security response.";
    }

    private String severityFor(String description) {
        if (description.contains("gun") || description.contains("weapon") || description.contains("unconscious")
                || description.contains("fire") || description.contains("assault") || description.contains("bleeding")) {
            return "CRITICAL";
        }
        if (description.contains("medical") || description.contains("injury") || description.contains("smoke")
                || description.contains("threat") || description.contains("fight")) {
            return "HIGH";
        }
        return "MEDIUM";
    }

    private int scoreFor(String severity) {
        return switch (severity) {
            case "CRITICAL" -> 95;
            case "HIGH" -> 80;
            default -> 55;
        };
    }
}
