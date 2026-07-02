package com.tripio.social.dto;

public record EtfScrapResponse(
        Long etfId,
        boolean scrapped,
        int scrapCount
) {
}
