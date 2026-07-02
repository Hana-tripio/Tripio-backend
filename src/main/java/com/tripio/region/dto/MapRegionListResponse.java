package com.tripio.region.dto;

import java.util.List;

public record MapRegionListResponse(
        List<MapRegionResponse> regions
) {
}
