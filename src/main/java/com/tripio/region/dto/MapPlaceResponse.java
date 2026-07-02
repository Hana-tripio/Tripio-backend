package com.tripio.region.dto;

import java.math.BigDecimal;

public record MapPlaceResponse(
        Long placeId,
        String name,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String category,
        boolean isLocal,
        boolean isCoreSpot,
        String imageUrl,
        Integer estimatedCost
) {
}
