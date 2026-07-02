package com.tripio.region.dto;

import java.math.BigDecimal;

public record MapRegionResponse(
        Long regionId,
        String name,
        String regionType,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer perScore,
        Integer localContributionBaseScore,
        boolean hasChildren
) {
}
