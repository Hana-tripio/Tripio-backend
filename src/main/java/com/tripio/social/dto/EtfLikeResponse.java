package com.tripio.social.dto;

public record EtfLikeResponse(
        Long etfId,
        boolean liked,
        int likeCount
) {
}
