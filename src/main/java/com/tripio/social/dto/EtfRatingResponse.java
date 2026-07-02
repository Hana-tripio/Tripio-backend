package com.tripio.social.dto;

import java.math.BigDecimal;

public record EtfRatingResponse(
        Long etfId,
        boolean rated,
        Integer score,
        BigDecimal ratingAverage
) {
}
