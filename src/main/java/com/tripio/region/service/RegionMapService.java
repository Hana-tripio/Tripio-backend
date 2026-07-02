package com.tripio.region.service;

import com.tripio.region.dto.MapRegionListResponse;

public interface RegionMapService {

    MapRegionListResponse getMapRegions(Long parentRegionId);
}
