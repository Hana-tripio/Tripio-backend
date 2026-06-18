package com.tripio.etf.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record EtfDetailResponse(
        Long id,
        String title,
        String summary,
        String status,
        OwnerResponse owner,
        RegionResponse region,
        Integer totalBudget,
        Integer durationDays,
        ScoreResponse scores,
        String thumbnailUrl,
        ReactionCountResponse reactionCounts,
        List<String> styleTags,
        List<ItineraryDayResponse> itineraryDays,
        PortfolioRatioResponse portfolioRatios,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public record OwnerResponse(
            Long id,
            String nickname
    ) {
    }

    public record RegionResponse(
            Long id,
            String name,
            String regionType
    ) {
    }

    public record ScoreResponse(
            Integer localContributionScore,
            Integer regionValueScore,
            Integer expectedReward
    ) {
    }

    public record ReactionCountResponse(
            Integer likeCount,
            Integer scrapCount,
            Integer followCount,
            Integer verificationCount,
            BigDecimal ratingAverage
    ) {
    }

    public record ItineraryDayResponse(
            Long id,
            Integer dayNumber,
            List<ItineraryItemResponse> items
    ) {
    }

    public record ItineraryItemResponse(
            Long id,
            Long placeId,
            String placeName,
            String placeCategory,
            Integer sequence,
            LocalTime startTime,
            LocalTime endTime,
            Integer estimatedCost,
            Boolean core,
            String memo
    ) {
    }

    public record PortfolioRatioResponse(
            BigDecimal lodgingRatio,
            BigDecimal foodRatio,
            BigDecimal cafeRatio,
            BigDecimal activityRatio,
            BigDecimal festivalRatio,
            BigDecimal localStoreRatio,
            BigDecimal transportRatio
    ) {
    }
}
