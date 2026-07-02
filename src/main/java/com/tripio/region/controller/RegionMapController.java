package com.tripio.region.controller;

import com.tripio.global.apiPayload.ApiResponse;
import com.tripio.global.apiPayload.code.GeneralSuccessCode;
import com.tripio.region.dto.MapPlaceListResponse;
import com.tripio.region.dto.MapRegionListResponse;
import com.tripio.region.service.RegionMapService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/map")
public class RegionMapController {

    private final RegionMapService regionMapService;

    @GetMapping("/regions")
    public ResponseEntity<ApiResponse<MapRegionListResponse>> getMapRegions(
            @RequestParam(required = false) @Positive(message = "parentRegionId는 1 이상이어야 합니다.")
            Long parentRegionId
    ) {
        MapRegionListResponse response = regionMapService.getMapRegions(parentRegionId);

        return ResponseEntity
                .status(GeneralSuccessCode.OK.getStatus())
                .body(ApiResponse.onSuccess(GeneralSuccessCode.OK, response));
    }

    @GetMapping("/regions/{regionId}/places")
    public ResponseEntity<ApiResponse<MapPlaceListResponse>> getMapRegionPlaces(
            @PathVariable @Positive(message = "regionId는 1 이상이어야 합니다.") Long regionId
    ) {
        MapPlaceListResponse response = regionMapService.getMapRegionPlaces(regionId);

        return ResponseEntity
                .status(GeneralSuccessCode.OK.getStatus())
                .body(ApiResponse.onSuccess(GeneralSuccessCode.OK, response));
    }
}
