package com.tripio.region.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tripio.global.apiPayload.code.GeneralErrorCode;
import com.tripio.global.apiPayload.exception.GeneralException;
import com.tripio.global.config.SecurityConfig;
import com.tripio.region.dto.MapPlaceListResponse;
import com.tripio.region.dto.MapPlaceResponse;
import com.tripio.region.dto.MapRegionListResponse;
import com.tripio.region.dto.MapRegionResponse;
import com.tripio.region.service.RegionMapService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RegionMapController.class)
@Import(SecurityConfig.class)
class RegionMapControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegionMapService regionMapService;

    @Test
    void getMapRegionsReturnsTopLevelRegionsWhenParentRegionIdIsMissing() throws Exception {
        given(regionMapService.getMapRegions(null)).willReturn(new MapRegionListResponse(List.of(
                new MapRegionResponse(
                        10L,
                        "충청권",
                        "PROVINCE",
                        new BigDecimal("36.6424000"),
                        new BigDecimal("127.4890000"),
                        44,
                        70,
                        true
                )
        )));

        mockMvc.perform(get("/api/map/regions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess", is(true)))
                .andExpect(jsonPath("$.code", is("COMMON200")))
                .andExpect(jsonPath("$.message", is("성공입니다.")))
                .andExpect(jsonPath("$.result.regions", hasSize(1)))
                .andExpect(jsonPath("$.result.regions[0].regionId", is(10)))
                .andExpect(jsonPath("$.result.regions[0].name", is("충청권")))
                .andExpect(jsonPath("$.result.regions[0].regionType", is("PROVINCE")))
                .andExpect(jsonPath("$.result.regions[0].latitude", is(36.6424000)))
                .andExpect(jsonPath("$.result.regions[0].longitude", is(127.4890000)))
                .andExpect(jsonPath("$.result.regions[0].perScore", is(44)))
                .andExpect(jsonPath("$.result.regions[0].localContributionBaseScore", is(70)))
                .andExpect(jsonPath("$.result.regions[0].hasChildren", is(true)));

        verify(regionMapService).getMapRegions(null);
    }

    @Test
    void getMapRegionsReturnsChildRegionsWhenParentRegionIdExists() throws Exception {
        given(regionMapService.getMapRegions(10L)).willReturn(new MapRegionListResponse(List.of(
                new MapRegionResponse(
                        20L,
                        "공주시",
                        "CITY",
                        new BigDecimal("36.4466000"),
                        new BigDecimal("127.1190000"),
                        52,
                        80,
                        false
                )
        )));

        mockMvc.perform(get("/api/map/regions").param("parentRegionId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.regions", hasSize(1)))
                .andExpect(jsonPath("$.result.regions[0].regionId", is(20)))
                .andExpect(jsonPath("$.result.regions[0].name", is("공주시")))
                .andExpect(jsonPath("$.result.regions[0].hasChildren", is(false)));

        verify(regionMapService).getMapRegions(10L);
    }

    @Test
    void getMapRegionsReturnsNotFoundWhenParentRegionDoesNotExist() throws Exception {
        given(regionMapService.getMapRegions(999L)).willThrow(new GeneralException(GeneralErrorCode.NOT_FOUND));

        mockMvc.perform(get("/api/map/regions").param("parentRegionId", "999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess", is(false)))
                .andExpect(jsonPath("$.code", is("COMMON404")));
    }

    @Test
    void getMapRegionsReturnsValidationErrorWhenParentRegionIdIsNotPositive() throws Exception {
        mockMvc.perform(get("/api/map/regions").param("parentRegionId", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess", is(false)))
                .andExpect(jsonPath("$.code", is("COMMON400_1")));

        verifyNoInteractions(regionMapService);
    }

    @Test
    void getMapRegionPlacesReturnsMapPinsForRegion() throws Exception {
        given(regionMapService.getMapRegionPlaces(20L)).willReturn(new MapPlaceListResponse(List.of(
                new MapPlaceResponse(
                        100L,
                        "공주산성시장",
                        "충남 공주시 산성시장",
                        new BigDecimal("36.4550000"),
                        new BigDecimal("127.1230000"),
                        "MARKET",
                        true,
                        true,
                        "https://example.com/place.jpg",
                        15000
                )
        )));

        mockMvc.perform(get("/api/map/regions/{regionId}/places", 20))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess", is(true)))
                .andExpect(jsonPath("$.code", is("COMMON200")))
                .andExpect(jsonPath("$.result.places", hasSize(1)))
                .andExpect(jsonPath("$.result.places[0].placeId", is(100)))
                .andExpect(jsonPath("$.result.places[0].name", is("공주산성시장")))
                .andExpect(jsonPath("$.result.places[0].address", is("충남 공주시 산성시장")))
                .andExpect(jsonPath("$.result.places[0].latitude", is(36.4550000)))
                .andExpect(jsonPath("$.result.places[0].longitude", is(127.1230000)))
                .andExpect(jsonPath("$.result.places[0].category", is("MARKET")))
                .andExpect(jsonPath("$.result.places[0].isLocal", is(true)))
                .andExpect(jsonPath("$.result.places[0].isCoreSpot", is(true)))
                .andExpect(jsonPath("$.result.places[0].imageUrl", is("https://example.com/place.jpg")))
                .andExpect(jsonPath("$.result.places[0].estimatedCost", is(15000)));

        verify(regionMapService).getMapRegionPlaces(20L);
    }

    @Test
    void getMapRegionPlacesReturnsNotFoundWhenRegionDoesNotExist() throws Exception {
        given(regionMapService.getMapRegionPlaces(999L)).willThrow(new GeneralException(GeneralErrorCode.NOT_FOUND));

        mockMvc.perform(get("/api/map/regions/{regionId}/places", 999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess", is(false)))
                .andExpect(jsonPath("$.code", is("COMMON404")));
    }

    @Test
    void getMapRegionPlacesReturnsValidationErrorWhenRegionIdIsNotPositive() throws Exception {
        mockMvc.perform(get("/api/map/regions/{regionId}/places", 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess", is(false)))
                .andExpect(jsonPath("$.code", is("COMMON400_1")));
    }
}
