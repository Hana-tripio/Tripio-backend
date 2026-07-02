package com.tripio.region.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.tripio.global.apiPayload.code.GeneralErrorCode;
import com.tripio.global.apiPayload.exception.GeneralException;
import com.tripio.place.entity.Place;
import com.tripio.place.repository.PlaceRepository;
import com.tripio.region.dto.MapPlaceListResponse;
import com.tripio.region.dto.MapRegionListResponse;
import com.tripio.region.entity.Region;
import com.tripio.region.repository.RegionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegionMapQueryServiceTest {

    @Mock
    private RegionRepository regionRepository;

    @Mock
    private PlaceRepository placeRepository;

    @InjectMocks
    private RegionMapQueryService regionMapQueryService;

    @Test
    void getMapRegionsReturnsTopLevelRegionsWithChildFlags() {
        Region chungcheong = createRegion(10L, null, "충청권", "PROVINCE", 36.6424, 127.4890, 44, 70);
        Region jeju = createRegion(11L, null, "제주도", "PROVINCE", 33.3800, 126.5300, 55, 60);
        given(regionRepository.findByParentRegionIdIsNullOrderByNameAsc()).willReturn(List.of(jeju, chungcheong));
        given(regionRepository.findByParentRegionIdIn(List.of(11L, 10L))).willReturn(List.of(
                createRegion(20L, 10L, "공주시", "CITY", 36.4466, 127.1190, 52, 80)
        ));

        MapRegionListResponse response = regionMapQueryService.getMapRegions(null);

        assertThat(response.regions()).extracting(region -> region.regionId()).containsExactly(11L, 10L);
        assertThat(response.regions()).extracting(region -> region.hasChildren()).containsExactly(false, true);
        assertThat(response.regions().get(1).name()).isEqualTo("충청권");
        assertThat(response.regions().get(1).latitude()).isEqualByComparingTo("36.6424000");
        assertThat(response.regions().get(1).longitude()).isEqualByComparingTo("127.4890000");
        assertThat(response.regions().get(1).perScore()).isEqualTo(44);
        assertThat(response.regions().get(1).localContributionBaseScore()).isEqualTo(70);
    }

    @Test
    void getMapRegionsReturnsChildrenWhenParentRegionExists() {
        Region parent = createRegion(10L, null, "충청권", "PROVINCE", 36.6424, 127.4890, 44, 70);
        Region gongju = createRegion(20L, 10L, "공주시", "CITY", 36.4466, 127.1190, 52, 80);
        given(regionRepository.findById(10L)).willReturn(Optional.of(parent));
        given(regionRepository.findByParentRegionIdOrderByNameAsc(10L)).willReturn(List.of(gongju));
        given(regionRepository.findByParentRegionIdIn(List.of(20L))).willReturn(List.of());

        MapRegionListResponse response = regionMapQueryService.getMapRegions(10L);

        assertThat(response.regions()).hasSize(1);
        assertThat(response.regions().get(0).regionId()).isEqualTo(20L);
        assertThat(response.regions().get(0).hasChildren()).isFalse();
    }

    @Test
    void getMapRegionsThrowsNotFoundWhenParentRegionDoesNotExist() {
        given(regionRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> regionMapQueryService.getMapRegions(999L))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(GeneralErrorCode.NOT_FOUND);

        verify(regionRepository).findById(999L);
        verifyNoMoreInteractions(regionRepository);
    }

    @Test
    void getMapRegionPlacesReturnsPlacesForExistingRegion() {
        Region region = createRegion(20L, 10L, "공주시", "CITY", 36.4466, 127.1190, 52, 80);
        Place market = createPlace(100L, 20L, "공주산성시장", "충남 공주시 산성시장", 36.4550, 127.1230,
                "MARKET", true, true, "https://example.com/market.jpg", 15000);
        Place cafe = createPlace(101L, 20L, "공주 한옥카페", "충남 공주시 한옥길", 36.4520, 127.1200,
                "CAFE", false, false, "https://example.com/cafe.jpg", 9000);
        given(regionRepository.findById(20L)).willReturn(Optional.of(region));
        given(placeRepository.findByRegionIdOrderByCoreSpotDescNameAsc(20L)).willReturn(List.of(market, cafe));

        MapPlaceListResponse response = regionMapQueryService.getMapRegionPlaces(20L);

        assertThat(response.places()).extracting(place -> place.placeId()).containsExactly(100L, 101L);
        assertThat(response.places().get(0).name()).isEqualTo("공주산성시장");
        assertThat(response.places().get(0).address()).isEqualTo("충남 공주시 산성시장");
        assertThat(response.places().get(0).latitude()).isEqualByComparingTo("36.4550000");
        assertThat(response.places().get(0).longitude()).isEqualByComparingTo("127.1230000");
        assertThat(response.places().get(0).category()).isEqualTo("MARKET");
        assertThat(response.places().get(0).isLocal()).isTrue();
        assertThat(response.places().get(0).isCoreSpot()).isTrue();
        assertThat(response.places().get(0).imageUrl()).isEqualTo("https://example.com/market.jpg");
        assertThat(response.places().get(0).estimatedCost()).isEqualTo(15000);
    }

    @Test
    void getMapRegionPlacesReturnsEmptyListWhenRegionHasNoPlaces() {
        Region region = createRegion(20L, 10L, "공주시", "CITY", 36.4466, 127.1190, 52, 80);
        given(regionRepository.findById(20L)).willReturn(Optional.of(region));
        given(placeRepository.findByRegionIdOrderByCoreSpotDescNameAsc(20L)).willReturn(List.of());

        MapPlaceListResponse response = regionMapQueryService.getMapRegionPlaces(20L);

        assertThat(response.places()).isEmpty();
    }

    @Test
    void getMapRegionPlacesThrowsNotFoundWhenRegionDoesNotExist() {
        given(regionRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> regionMapQueryService.getMapRegionPlaces(999L))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(GeneralErrorCode.NOT_FOUND);

        verify(regionRepository).findById(999L);
        verifyNoMoreInteractions(regionRepository, placeRepository);
    }

    private Region createRegion(
            Long id,
            Long parentRegionId,
            String name,
            String regionType,
            double latitude,
            double longitude,
            int perScore,
            int localContributionBaseScore
    ) {
        return new Region(
                id,
                parentRegionId,
                name,
                regionType,
                BigDecimal.valueOf(latitude).setScale(7),
                BigDecimal.valueOf(longitude).setScale(7),
                perScore,
                localContributionBaseScore
        );
    }

    private Place createPlace(
            Long id,
            Long regionId,
            String name,
            String address,
            double latitude,
            double longitude,
            String category,
            boolean isLocal,
            boolean isCoreSpot,
            String imageUrl,
            int estimatedCost
    ) {
        return new Place(
                id,
                regionId,
                name,
                address,
                BigDecimal.valueOf(latitude).setScale(7),
                BigDecimal.valueOf(longitude).setScale(7),
                category,
                isLocal,
                isCoreSpot,
                imageUrl,
                estimatedCost
        );
    }
}
