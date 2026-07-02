package com.tripio.etf.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tripio.etf.dto.EtfDetailResponse;
import com.tripio.etf.dto.EtfCardResponse;
import com.tripio.etf.dto.EtfListResponse;
import com.tripio.etf.dto.EtfSearchRequest;
import com.tripio.etf.service.EtfSearchService;
import com.tripio.etf.service.EtfService;
import com.tripio.etf.type.EtfErrorCode;
import com.tripio.global.apiPayload.code.GeneralErrorCode;
import com.tripio.global.apiPayload.exception.GeneralException;
import com.tripio.global.config.SecurityConfig;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EtfController.class)
@Import(SecurityConfig.class)
class EtfControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EtfService etfService;

    @MockitoBean
    private EtfSearchService etfSearchService;

    @Test
    void searchEtfsReturnsCardPageResponse() throws Exception {
        EtfCardResponse card = new EtfCardResponse(
                1L,
                "공주 로컬푸드 ETF",
                20L,
                "공주",
                2,
                180000,
                List.of("로컬푸드", "역사"),
                76,
                82,
                3,
                4,
                5,
                6,
                new BigDecimal("4.50"),
                "https://example.com/thumbnail.jpg"
        );
        EtfListResponse response = new EtfListResponse(List.of(card), 0, 10, 1, 1, false);
        given(etfSearchService.searchEtfs(org.mockito.ArgumentMatchers.any(EtfSearchRequest.class)))
                .willReturn(response);

        mockMvc.perform(get("/api/etfs")
                        .param("keyword", "공주")
                        .param("regionId", "20")
                        .param("styleTagIds", "1", "2")
                        .param("minBudget", "100000")
                        .param("maxBudget", "300000")
                        .param("minDurationDays", "1")
                        .param("maxDurationDays", "3")
                        .param("sort", "latest")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess", is(true)))
                .andExpect(jsonPath("$.code", is("COMMON200")))
                .andExpect(jsonPath("$.result.content", hasSize(1)))
                .andExpect(jsonPath("$.result.content[0].etfId", is(1)))
                .andExpect(jsonPath("$.result.content[0].regionName", is("공주")))
                .andExpect(jsonPath("$.result.content[0].styleTags", hasSize(2)))
                .andExpect(jsonPath("$.result.page", is(0)))
                .andExpect(jsonPath("$.result.size", is(10)))
                .andExpect(jsonPath("$.result.hasNext", is(false)));

        ArgumentCaptor<EtfSearchRequest> requestCaptor = ArgumentCaptor.forClass(EtfSearchRequest.class);
        verify(etfSearchService).searchEtfs(requestCaptor.capture());
        EtfSearchRequest capturedRequest = requestCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(capturedRequest.getKeyword()).isEqualTo("공주");
        org.assertj.core.api.Assertions.assertThat(capturedRequest.getStyleTagIds()).containsExactly(1L, 2L);
        org.assertj.core.api.Assertions.assertThat(capturedRequest.getSort()).isEqualTo("latest");
        org.assertj.core.api.Assertions.assertThat(capturedRequest.getSize()).isEqualTo(10);
    }

    @Test
    void searchEtfsReturnsValidationErrorWhenBudgetRangeIsInvalid() throws Exception {
        mockMvc.perform(get("/api/etfs")
                        .param("minBudget", "300000")
                        .param("maxBudget", "100000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess", is(false)))
                .andExpect(jsonPath("$.code", is("COMMON400_1")));

        verifyNoInteractions(etfSearchService);
    }

    @Test
    void searchEtfsReturnsValidationErrorWhenPageSizeExceedsMaximum() throws Exception {
        mockMvc.perform(get("/api/etfs").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess", is(false)))
                .andExpect(jsonPath("$.code", is("COMMON400_1")));

        verifyNoInteractions(etfSearchService);
    }

    @Test
    void searchEtfsReturnsValidationErrorWhenOffsetExceedsSupportedRange() throws Exception {
        mockMvc.perform(get("/api/etfs")
                        .param("page", String.valueOf(Integer.MAX_VALUE))
                        .param("size", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess", is(false)))
                .andExpect(jsonPath("$.code", is("COMMON400_1")));

        verifyNoInteractions(etfSearchService);
    }

    @Test
    void searchEtfsReturnsBadRequestWhenSortIsUnsupported() throws Exception {
        given(etfSearchService.searchEtfs(org.mockito.ArgumentMatchers.any(EtfSearchRequest.class)))
                .willThrow(new GeneralException(GeneralErrorCode.BAD_REQUEST));

        mockMvc.perform(get("/api/etfs").param("sort", "rising"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess", is(false)))
                .andExpect(jsonPath("$.code", is("COMMON400")));
    }

    @Test
    void getEtfDetailReturnsApiResponse() throws Exception {
        EtfDetailResponse response = new EtfDetailResponse(
                1L,
                "공주 로컬푸드 2일 ETF",
                "공주 시장과 한옥마을을 잇는 2일 여행",
                "PUBLIC",
                new EtfDetailResponse.OwnerResponse(10L, "하나여행자"),
                new EtfDetailResponse.RegionResponse(20L, "공주", "CITY"),
                180000,
                2,
                new EtfDetailResponse.ScoreResponse(82, 76, 1200),
                "https://example.com/thumbnail.jpg",
                new EtfDetailResponse.ReactionCountResponse(3, 4, 5, 6, new BigDecimal("4.50")),
                List.of("로컬푸드", "역사"),
                List.of(new EtfDetailResponse.ItineraryDayResponse(
                        1L,
                        1,
                        List.of(new EtfDetailResponse.ItineraryItemResponse(
                                100L,
                                200L,
                                "공주산성시장",
                                "MARKET",
                                1,
                                LocalTime.of(10, 0),
                                LocalTime.of(11, 30),
                                25000,
                                true,
                                "아침 시장 투어"
                        ))
                )),
                new EtfDetailResponse.PortfolioRatioResponse(
                        new BigDecimal("20.00"),
                        new BigDecimal("35.00"),
                        new BigDecimal("10.00"),
                        new BigDecimal("15.00"),
                        new BigDecimal("5.00"),
                        new BigDecimal("10.00"),
                        new BigDecimal("5.00")
                ),
                LocalDateTime.of(2026, 6, 15, 12, 0),
                LocalDateTime.of(2026, 6, 16, 12, 0)
        );

        given(etfService.getEtfDetail(1L)).willReturn(response);

        mockMvc.perform(get("/api/etfs/{etfId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess", is(true)))
                .andExpect(jsonPath("$.code", is("COMMON200")))
                .andExpect(jsonPath("$.message", is("성공입니다.")))
                .andExpect(jsonPath("$.result.id", is(1)))
                .andExpect(jsonPath("$.result.title", is("공주 로컬푸드 2일 ETF")))
                .andExpect(jsonPath("$.result.owner.nickname", is("하나여행자")))
                .andExpect(jsonPath("$.result.region.name", is("공주")))
                .andExpect(jsonPath("$.result.reactionCounts.likeCount", is(3)))
                .andExpect(jsonPath("$.result.styleTags", hasSize(2)))
                .andExpect(jsonPath("$.result.itineraryDays[0].items[0].placeName", is("공주산성시장")))
                .andExpect(jsonPath("$.result.portfolioRatios.foodRatio", is(35.00)));
    }

    @Test
    void getEtfDetailReturnsDomainErrorWhenEtfNotFound() throws Exception {
        given(etfService.getEtfDetail(999L)).willThrow(new GeneralException(EtfErrorCode.ETF_NOT_FOUND));

        mockMvc.perform(get("/api/etfs/{etfId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess", is(false)))
                .andExpect(jsonPath("$.code", is("ETF404")))
                .andExpect(jsonPath("$.message", is("ETF를 찾을 수 없습니다.")));
    }

    @Test
    void getEtfDetailReturnsBadRequestWhenEtfIdIsNotNumber() throws Exception {
        mockMvc.perform(get("/api/etfs/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess", is(false)))
                .andExpect(jsonPath("$.code", is("COMMON400")))
                .andExpect(jsonPath("$.message", is("잘못된 요청입니다.")));

        verifyNoInteractions(etfService);
    }

    @Test
    void getEtfDetailReturnsValidationErrorWhenEtfIdIsZero() throws Exception {
        mockMvc.perform(get("/api/etfs/{etfId}", 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess", is(false)))
                .andExpect(jsonPath("$.code", is("COMMON400_1")))
                .andExpect(jsonPath("$.message", is("입력값 검증에 실패했습니다.")));

        verifyNoInteractions(etfService);
    }

    @Test
    void getEtfDetailReturnsValidationErrorWhenEtfIdIsNegative() throws Exception {
        mockMvc.perform(get("/api/etfs/{etfId}", -1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess", is(false)))
                .andExpect(jsonPath("$.code", is("COMMON400_1")))
                .andExpect(jsonPath("$.message", is("입력값 검증에 실패했습니다.")));

        verifyNoInteractions(etfService);
    }
}
