package com.tripio.social.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import com.tripio.etf.dto.EtfListResponse;
import com.tripio.etf.entity.TravelEtf;
import com.tripio.etf.repository.EtfStyleTagRow;
import com.tripio.etf.repository.TravelEtfStyleTagRepository;
import com.tripio.region.entity.Region;
import com.tripio.region.repository.RegionRepository;
import com.tripio.social.dto.ScrappedEtfListRequest;
import com.tripio.social.repository.EtfScrapRepository;
import com.tripio.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ScrappedEtfQueryServiceTest {

    private static final long USER_ID = 10L;

    @Mock
    private EtfScrapRepository etfScrapRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RegionRepository regionRepository;

    @Mock
    private TravelEtfStyleTagRepository styleTagRepository;

    @InjectMocks
    private ScrappedEtfQueryService scrappedEtfQueryService;

    @Test
    void getScrappedEtfsComposesExistingEtfCardPageWithBulkQueries() {
        ScrappedEtfListRequest request = new ScrappedEtfListRequest();
        request.setPage(0);
        request.setSize(10);
        PageRequest pageRequest = PageRequest.of(0, 10);
        TravelEtf etf = createEtf();
        Page<TravelEtf> page = new PageImpl<>(List.of(etf), pageRequest, 1);

        given(userRepository.existsById(USER_ID)).willReturn(true);
        given(etfScrapRepository.findScrappedEtfsByUserIdAndStatus(USER_ID, "PUBLIC", pageRequest))
                .willReturn(page);
        given(regionRepository.findAllById(List.of(30L)))
                .willReturn(List.of(new Region(30L, null, "공주", "CITY")));
        given(styleTagRepository.findTagRowsByTravelEtfIds(List.of(20L)))
                .willReturn(List.of(new EtfStyleTagRow(20L, "힐링")));

        EtfListResponse response = scrappedEtfQueryService.getScrappedEtfs(USER_ID, request);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).etfId()).isEqualTo(20L);
        assertThat(response.content().get(0).regionName()).isEqualTo("공주");
        assertThat(response.content().get(0).styleTags()).containsExactly("힐링");
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    void emptyPagePreservesMetadataWithoutRegionOrTagQueries() {
        ScrappedEtfListRequest request = new ScrappedEtfListRequest();
        request.setPage(2);
        request.setSize(2);
        PageRequest pageRequest = PageRequest.of(2, 2);
        Page<TravelEtf> page = new PageImpl<>(List.of(), pageRequest, 3);

        given(userRepository.existsById(USER_ID)).willReturn(true);
        given(etfScrapRepository.findScrappedEtfsByUserIdAndStatus(USER_ID, "PUBLIC", pageRequest))
                .willReturn(page);

        EtfListResponse response = scrappedEtfQueryService.getScrappedEtfs(USER_ID, request);

        assertThat(response.content()).isEmpty();
        assertThat(response.page()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(3);
        assertThat(response.totalPages()).isEqualTo(2);
        verifyNoInteractions(regionRepository, styleTagRepository);
    }

    private TravelEtf createEtf() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 2, 12, 0);
        return new TravelEtf(
                20L,
                USER_ID,
                30L,
                "공주 저장 ETF",
                "저장 목록 테스트",
                "PUBLIC",
                200000,
                2,
                75,
                80,
                0,
                "https://example.com/thumbnail.jpg",
                5,
                3,
                2,
                1,
                new BigDecimal("4.50"),
                now,
                now
        );
    }
}
