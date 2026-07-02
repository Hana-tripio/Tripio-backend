package com.tripio.etf.dto;

import java.math.BigDecimal;
import java.util.List;

public record EtfCardResponse(
        Long etfId,
        String title,
        Long regionId,
        String regionName,
        Integer durationDays,
        Integer totalBudget,
        List<String> styleTags,
        Integer regionValueScore,
        Integer localContributionScore,
        Integer likeCount,
        Integer scrapCount,
        Integer followCount,
        Integer verificationCount,
        BigDecimal ratingAverage,
        String thumbnailUrl
) {
}
