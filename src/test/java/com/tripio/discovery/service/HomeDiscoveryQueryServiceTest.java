package com.tripio.discovery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.tripio.discovery.dto.HomeDiscoveryResponse;
import com.tripio.discovery.repository.HomeDiscoveryRepository;
import com.tripio.discovery.type.HomeDiscoverySection;
import com.tripio.etf.entity.TravelEtf;
import com.tripio.etf.repository.EtfStyleTagRow;
import com.tripio.etf.repository.TravelEtfStyleTagRepository;
import com.tripio.region.entity.Region;
import com.tripio.region.repository.RegionRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HomeDiscoveryQueryServiceTest {

    @Mock
    private HomeDiscoveryRepository homeDiscoveryRepository;

    @Mock
    private RegionRepository regionRepository;

    @Mock
    private TravelEtfStyleTagRepository styleTagRepository;

    @InjectMocks
    private HomeDiscoveryQueryService homeDiscoveryQueryService;

    @Test
    void getHomeDiscoveryKeepsSectionOrderAndLoadsCardDataOnce() {
        TravelEtf first = createEtf(1L, 20L, "첫 번째 ETF");
        TravelEtf second = createEtf(2L, 20L, "두 번째 ETF");
        TravelEtf third = createEtf(3L, 21L, "세 번째 ETF");
        given(homeDiscoveryRepository.findTop(HomeDiscoverySection.POPULAR, 10))
                .willReturn(List.of(first, second));
        given(homeDiscoveryRepository.findTop(HomeDiscoverySection.RISING, 10))
                .willReturn(List.of(second, third));
        given(homeDiscoveryRepository.findTop(HomeDiscoverySection.UNDERVALUED, 10))
                .willReturn(List.of(third, first));
        given(regionRepository.findAllById(List.of(20L, 21L))).willReturn(List.of(
                new Region(20L, null, "공주", "CITY"),
                new Region(21L, null, "제주", "CITY")
        ));
        given(styleTagRepository.findTagRowsByTravelEtfIds(List.of(1L, 2L, 3L))).willReturn(List.of(
                new EtfStyleTagRow(1L, "힐링"),
                new EtfStyleTagRow(2L, "역사"),
                new EtfStyleTagRow(3L, "카페")
        ));

        HomeDiscoveryResponse response = homeDiscoveryQueryService.getHomeDiscovery();

        assertThat(response.popularEtfs()).extracting(card -> card.etfId()).containsExactly(1L, 2L);
        assertThat(response.risingEtfs()).extracting(card -> card.etfId()).containsExactly(2L, 3L);
        assertThat(response.undervaluedEtfs()).extracting(card -> card.etfId()).containsExactly(3L, 1L);
        assertThat(response.popularEtfs().get(0).regionName()).isEqualTo("공주");
        assertThat(response.risingEtfs().get(1).styleTags()).containsExactly("카페");
        verify(regionRepository).findAllById(List.of(20L, 21L));
        verify(styleTagRepository).findTagRowsByTravelEtfIds(List.of(1L, 2L, 3L));
    }

    @Test
    void getHomeDiscoveryReturnsEmptySectionsWithoutAdditionalQueries() {
        given(homeDiscoveryRepository.findTop(HomeDiscoverySection.POPULAR, 10)).willReturn(List.of());
        given(homeDiscoveryRepository.findTop(HomeDiscoverySection.RISING, 10)).willReturn(List.of());
        given(homeDiscoveryRepository.findTop(HomeDiscoverySection.UNDERVALUED, 10)).willReturn(List.of());

        HomeDiscoveryResponse response = homeDiscoveryQueryService.getHomeDiscovery();

        assertThat(response.popularEtfs()).isEmpty();
        assertThat(response.risingEtfs()).isEmpty();
        assertThat(response.undervaluedEtfs()).isEmpty();
        verifyNoInteractions(regionRepository, styleTagRepository);
    }

    private TravelEtf createEtf(long id, long regionId, String title) {
        return new TravelEtf(
                id,
                10L,
                regionId,
                title,
                "홈 Discovery 테스트",
                "PUBLIC",
                200000,
                2,
                75,
                80,
                1000,
                "https://example.com/thumbnail.jpg",
                10,
                5,
                3,
                2,
                new BigDecimal("4.50"),
                LocalDateTime.of(2026, 7, 1, 10, 0),
                LocalDateTime.of(2026, 7, 1, 10, 0)
        );
    }
}
