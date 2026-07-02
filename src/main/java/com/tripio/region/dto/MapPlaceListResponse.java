package com.tripio.region.dto;

import java.util.List;

public record MapPlaceListResponse(
        List<MapPlaceResponse> places
) {
}
