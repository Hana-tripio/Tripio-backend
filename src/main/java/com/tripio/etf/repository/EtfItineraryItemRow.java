package com.tripio.etf.repository;

import java.time.LocalTime;

public record EtfItineraryItemRow(
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
