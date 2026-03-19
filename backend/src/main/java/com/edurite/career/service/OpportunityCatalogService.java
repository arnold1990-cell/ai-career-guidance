package com.edurite.career.service;

import com.edurite.career.dto.OpportunityDto;
import com.edurite.career.entity.Career;
import com.edurite.career.repository.CareerRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class OpportunityCatalogService {

    private final CareerRepository careerRepository;

    public OpportunityCatalogService(CareerRepository careerRepository) {
        this.careerRepository = careerRepository;
    }

    public List<OpportunityDto> search(
            String q,
            String field,
            String industry,
            String qualification,
            String location,
            String demand,
            String opportunityType
    ) {
        String normalizedType = normalizeType(opportunityType);
        String industryFilter = firstNonBlank(industry, field);

        List<OpportunityDto> careers = careerRepository.findAll().stream()
                .map(this::toCareerOpportunity)
                .toList();

        List<OpportunityDto> seeded = List.of(
                new OpportunityDto(
                        "job-junior-web-developer",
                        null,
                        "Junior Web Developer",
                        "JOB",
                        "Technology",
                        "Johannesburg",
                        "Diploma",
                        "High",
                        "Launch production-ready web experiences and support agile delivery teams.",
                        false,
                        false
                ),
                new OpportunityDto(
                        "job-support-analyst",
                        null,
                        "IT Support Analyst",
                        "JOB",
                        "Technology",
                        "Cape Town",
                        "Certificate",
                        "Medium",
                        "Help teams stay productive by solving user issues and improving service quality.",
                        false,
                        false
                ),
                new OpportunityDto(
                        "internship-data-analyst-intern",
                        null,
                        "Data Analyst Intern",
                        "INTERNSHIP",
                        "Technology",
                        "Remote",
                        "Undergraduate",
                        "High",
                        "Build dashboards, clean datasets, and learn how analytics supports business decisions.",
                        true,
                        false
                ),
                new OpportunityDto(
                        "internship-marketing-intern",
                        null,
                        "Marketing Intern",
                        "INTERNSHIP",
                        "Marketing",
                        "Durban",
                        "Undergraduate",
                        "Medium",
                        "Gain hands-on campaign experience while building your professional portfolio.",
                        false,
                        false
                )
        );

        return List.copyOf(java.util.stream.Stream.concat(careers.stream(), seeded.stream())
                .filter(item -> matchesType(item, normalizedType))
                .filter(item -> contains(item.title(), q) || contains(item.description(), q))
                .filter(item -> contains(item.industry(), industryFilter))
                .filter(item -> contains(item.qualification(), qualification))
                .filter(item -> contains(item.location(), location))
                .filter(item -> contains(item.demand(), demand))
                .sorted(java.util.Comparator
                        .comparing(OpportunityDto::recommended).reversed()
                        .thenComparing(OpportunityDto::title))
                .toList());
    }

    private OpportunityDto toCareerOpportunity(Career career) {
        return new OpportunityDto(
                career.getId().toString(),
                career.getId(),
                career.getTitle(),
                "CAREER",
                career.getIndustry(),
                career.getLocation(),
                career.getQualificationLevel(),
                career.getDemandLevel(),
                career.getDescription(),
                false,
                false
        );
    }

    private boolean matchesType(OpportunityDto item, String type) {
        return type.isBlank() || "ALL".equals(type) || item.type().equals(type);
    }

    private boolean contains(String value, String filter) {
        if (filter == null || filter.isBlank()) {
            return true;
        }
        return value != null && value.toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT));
    }

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return "ALL";
        }
        String normalized = type.trim().toUpperCase(Locale.ROOT);
        if (normalized.equals("ALL OPPORTUNITIES")) {
            return "ALL";
        }
        if (normalized.endsWith("S")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String firstNonBlank(String primary, String fallback) {
        return primary != null && !primary.isBlank() ? primary : fallback == null ? "" : fallback;
    }
}
