package com.tripio.etf.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.tripio.etf.dto.EtfDetailResponse;
import com.tripio.etf.entity.EtfItineraryDay;
import com.tripio.etf.entity.EtfPortfolioRatio;
import com.tripio.etf.entity.TravelEtf;
import com.tripio.etf.repository.EtfItineraryDayRepository;
import com.tripio.etf.repository.EtfItineraryItemRepository;
import com.tripio.etf.repository.EtfItineraryItemRow;
import com.tripio.etf.repository.EtfPortfolioRatioRepository;
import com.tripio.etf.repository.TravelEtfRepository;
import com.tripio.etf.repository.TravelEtfStyleTagRepository;
import com.tripio.etf.type.EtfErrorCode;
import com.tripio.global.apiPayload.exception.GeneralException;
import com.tripio.region.entity.Region;
import com.tripio.region.repository.RegionRepository;
import com.tripio.user.entity.User;
import com.tripio.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EtfDetailQueryServiceTest {

    @Mock
    private TravelEtfRepository travelEtfRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RegionRepository regionRepository;

    @Mock
    private TravelEtfStyleTagRepository styleTagRepository;

    @Mock
    private EtfItineraryDayRepository itineraryDayRepository;

    @Mock
    private EtfItineraryItemRepository itineraryItemRepository;

    @Mock
    private EtfPortfolioRatioRepository portfolioRatioRepository;

    @InjectMocks
    private EtfDetailQueryService etfDetailQueryService;

    @Test
    void getEtfDetailComposesFullDetailResponse() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 15, 12, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 6, 16, 12, 0);
        TravelEtf travelEtf = new TravelEtf(
                1L,
                10L,
                20L,
                "공주 로컬푸드 2일 ETF",
                "공주 시장과 한옥마을을 잇는 2일 여행",
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
                createdAt,
                updatedAt
        );
        User owner = new User(10L, "owner@example.com", "password", "하나여행자", "ACTIVE");
        Region region = new Region(20L, null, "공주", "CITY");
        EtfItineraryDay day = new EtfItineraryDay(100L, 1L, 1);
        EtfItineraryItemRow item = new EtfItineraryItemRow(
                200L,
                300L,
                "공주산성시장",
                "MARKET",
                1,
                LocalTime.of(10, 0),
                LocalTime.of(11, 30),
                25000,
                true,
                "아침 시장 투어"
        );
        EtfPortfolioRatio portfolioRatio = new EtfPortfolioRatio(
                400L,
                1L,
                new BigDecimal("20.00"),
                new BigDecimal("35.00"),
                new BigDecimal("10.00"),
                new BigDecimal("15.00"),
                new BigDecimal("5.00"),
                new BigDecimal("10.00"),
                new BigDecimal("5.00")
        );

        given(travelEtfRepository.findById(1L)).willReturn(Optional.of(travelEtf));
        given(userRepository.findById(10L)).willReturn(Optional.of(owner));
        given(regionRepository.findById(20L)).willReturn(Optional.of(region));
        given(styleTagRepository.findTagNamesByTravelEtfId(1L)).willReturn(List.of("로컬푸드", "역사"));
        given(itineraryDayRepository.findByTravelEtfIdOrderByDayNumber(1L)).willReturn(List.of(day));
        given(itineraryItemRepository.findRowsByItineraryDayId(100L)).willReturn(List.of(item));
        given(portfolioRatioRepository.findByTravelEtfId(1L)).willReturn(Optional.of(portfolioRatio));

        EtfDetailResponse response = etfDetailQueryService.getEtfDetail(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("공주 로컬푸드 2일 ETF");
        assertThat(response.owner().nickname()).isEqualTo("하나여행자");
        assertThat(response.region().name()).isEqualTo("공주");
        assertThat(response.scores().localContributionScore()).isEqualTo(82);
        assertThat(response.reactionCounts().ratingAverage()).isEqualByComparingTo("4.50");
        assertThat(response.styleTags()).containsExactly("로컬푸드", "역사");
        assertThat(response.itineraryDays()).hasSize(1);
        assertThat(response.itineraryDays().get(0).items().get(0).placeName()).isEqualTo("공주산성시장");
        assertThat(response.portfolioRatios().foodRatio()).isEqualByComparingTo("35.00");
        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(response.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void getEtfDetailThrowsDomainExceptionWhenEtfNotFound() {
        given(travelEtfRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> etfDetailQueryService.getEtfDetail(999L))
                .isInstanceOfSatisfying(GeneralException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(EtfErrorCode.ETF_NOT_FOUND));
    }
}
