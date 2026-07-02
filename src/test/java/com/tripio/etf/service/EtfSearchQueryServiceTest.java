package com.tripio.etf.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.tripio.etf.dto.EtfListResponse;
import com.tripio.etf.dto.EtfSearchRequest;
import com.tripio.etf.entity.TravelEtf;
import com.tripio.etf.repository.EtfSearchCriteria;
import com.tripio.etf.repository.EtfStyleTagRow;
import com.tripio.etf.repository.TravelEtfRepository;
import com.tripio.etf.repository.TravelEtfStyleTagRepository;
import com.tripio.etf.type.EtfSearchSort;
import com.tripio.global.apiPayload.code.GeneralErrorCode;
import com.tripio.global.apiPayload.exception.GeneralException;
import com.tripio.region.entity.Region;
import com.tripio.region.repository.RegionRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class EtfSearchQueryServiceTest {

    @Mock
    private TravelEtfRepository travelEtfRepository;

    @Mock
    private RegionRepository regionRepository;

    @Mock
    private TravelEtfStyleTagRepository styleTagRepository;

    @InjectMocks
    private EtfSearchQueryService etfSearchQueryService;

    @Test
    void searchEtfsComposesCardPageWithBulkRegionAndTagQueries() {
        EtfSearchRequest request = new EtfSearchRequest();
        request.setKeyword("  공주  ");
        request.setRegionId(20L);
        request.setStyleTagIds(List.of(1L, 1L, 2L));
        request.setMinBudget(100000);
        request.setMaxBudget(300000);
        request.setMinDurationDays(1);
        request.setMaxDurationDays(3);
        request.setSort("latest");
        request.setSize(10);

        TravelEtf etf = createTravelEtf();
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<TravelEtf> etfPage = new PageImpl<>(List.of(etf), pageRequest, 1);
        given(travelEtfRepository.search(org.mockito.ArgumentMatchers.any(EtfSearchCriteria.class),
                org.mockito.ArgumentMatchers.eq(pageRequest))).willReturn(etfPage);
        given(regionRepository.findAllById(List.of(20L)))
                .willReturn(List.of(new Region(20L, null, "공주", "CITY")));
        given(styleTagRepository.findTagRowsByTravelEtfIds(List.of(1L)))
                .willReturn(List.of(
                        new EtfStyleTagRow(1L, "로컬푸드"),
                        new EtfStyleTagRow(1L, "역사")
                ));

        EtfListResponse response = etfSearchQueryService.searchEtfs(request);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).etfId()).isEqualTo(1L);
        assertThat(response.content().get(0).regionName()).isEqualTo("공주");
        assertThat(response.content().get(0).styleTags()).containsExactly("로컬푸드", "역사");
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.hasNext()).isFalse();

        ArgumentCaptor<EtfSearchCriteria> criteriaCaptor = ArgumentCaptor.forClass(EtfSearchCriteria.class);
        verify(travelEtfRepository).search(criteriaCaptor.capture(), org.mockito.ArgumentMatchers.eq(pageRequest));
        EtfSearchCriteria criteria = criteriaCaptor.getValue();
        assertThat(criteria.keyword()).isEqualTo("공주");
        assertThat(criteria.styleTagIds()).containsExactly(1L, 2L);
        assertThat(criteria.sort()).isEqualTo(EtfSearchSort.LATEST);
    }

    @Test
    void searchEtfsReturnsEmptyContentWithoutAdditionalQueries() {
        EtfSearchRequest request = new EtfSearchRequest();
        PageRequest pageRequest = PageRequest.of(0, 20);
        Page<TravelEtf> emptyPage = new PageImpl<>(List.of(), pageRequest, 0);
        given(travelEtfRepository.search(org.mockito.ArgumentMatchers.any(EtfSearchCriteria.class),
                org.mockito.ArgumentMatchers.eq(pageRequest))).willReturn(emptyPage);

        EtfListResponse response = etfSearchQueryService.searchEtfs(request);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
        assertThat(response.totalPages()).isZero();
        verifyNoInteractions(regionRepository, styleTagRepository);
    }

    @Test
    void searchEtfsKeepsPaginationMetadataForEmptyPageBeyondLastPage() {
        EtfSearchRequest request = new EtfSearchRequest();
        request.setPage(2);
        request.setSize(2);
        PageRequest pageRequest = PageRequest.of(2, 2);
        Page<TravelEtf> emptyPage = new PageImpl<>(List.of(), pageRequest, 3);
        given(travelEtfRepository.search(org.mockito.ArgumentMatchers.any(EtfSearchCriteria.class),
                org.mockito.ArgumentMatchers.eq(pageRequest))).willReturn(emptyPage);

        EtfListResponse response = etfSearchQueryService.searchEtfs(request);

        assertThat(response.content()).isEmpty();
        assertThat(response.page()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(3);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.hasNext()).isFalse();
        verifyNoInteractions(regionRepository, styleTagRepository);
    }

    @Test
    void searchEtfsRejectsUnsupportedSortBeforeRepositoryQuery() {
        EtfSearchRequest request = new EtfSearchRequest();
        request.setSort("rising");

        assertThatThrownBy(() -> etfSearchQueryService.searchEtfs(request))
                .isInstanceOfSatisfying(GeneralException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(GeneralErrorCode.BAD_REQUEST));

        verifyNoInteractions(travelEtfRepository, regionRepository, styleTagRepository);
    }

    private TravelEtf createTravelEtf() {
        return new TravelEtf(
                1L,
                10L,
                20L,
                "공주 로컬푸드 ETF",
                "공주 시장과 한옥마을 여행",
                "PUBLIC",
                180000,
                2,
                82,
                76,
                1200,
                "https://example.com/thumbnail.jpg",
                3,
                4,
                5,
                6,
                new BigDecimal("4.50"),
                LocalDateTime.of(2026, 6, 15, 12, 0),
                LocalDateTime.of(2026, 6, 16, 12, 0)
        );
    }
}
