package com.edurite.career.dto;

import java.util.UUID;

public record OpportunityDto(
        String id,
        UUID careerId,
        String title,
        String type,
        String industry,
        String location,
        String qualification,
        String demand,
        String description,
        boolean recommended,
        boolean saved
) {
}
