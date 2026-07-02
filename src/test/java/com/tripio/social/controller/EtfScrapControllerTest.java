package com.tripio.social.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tripio.etf.dto.EtfCardResponse;
import com.tripio.etf.dto.EtfListResponse;
import com.tripio.global.config.SecurityConfig;
import com.tripio.social.dto.EtfScrapResponse;
import com.tripio.social.dto.ScrappedEtfListRequest;
import com.tripio.social.service.EtfScrapService;
import com.tripio.social.service.ScrappedEtfService;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EtfScrapController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityConfig.class)
class EtfScrapControllerTest {

    private static final long USER_ID = 10L;
    private static final long ETF_ID = 20L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EtfScrapService etfScrapService;

    @MockitoBean
    private ScrappedEtfService scrappedEtfService;

    @Test
    void addScrapReturnsCurrentScrapState() throws Exception {
        given(etfScrapService.addScrap(USER_ID, ETF_ID))
                .willReturn(new EtfScrapResponse(ETF_ID, true, 3));

        mockMvc.perform(post("/api/etfs/{etfId}/scraps", ETF_ID).principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess", is(true)))
                .andExpect(jsonPath("$.code", is("COMMON200")))
                .andExpect(jsonPath("$.result.etfId", is((int) ETF_ID)))
                .andExpect(jsonPath("$.result.scrapped", is(true)))
                .andExpect(jsonPath("$.result.scrapCount", is(3)));
    }

    @Test
    void removeScrapReturnsCurrentScrapState() throws Exception {
        given(etfScrapService.removeScrap(USER_ID, ETF_ID))
                .willReturn(new EtfScrapResponse(ETF_ID, false, 2));

        mockMvc.perform(delete("/api/etfs/{etfId}/scraps", ETF_ID).principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.scrapped", is(false)))
                .andExpect(jsonPath("$.result.scrapCount", is(2)));
    }

    @Test
    void getScrappedEtfsReturnsEtfCardPage() throws Exception {
        EtfCardResponse card = new EtfCardResponse(
                ETF_ID,
                "공주 저장 ETF",
                30L,
                "공주",
                2,
                200000,
                List.of("힐링"),
                80,
                75,
                5,
                3,
                2,
                1,
                new BigDecimal("4.50"),
                "https://example.com/thumbnail.jpg"
        );
        given(scrappedEtfService.getScrappedEtfs(
                org.mockito.ArgumentMatchers.eq(USER_ID),
                org.mockito.ArgumentMatchers.any(ScrappedEtfListRequest.class)
        )).willReturn(new EtfListResponse(List.of(card), 1, 5, 6, 2, false));

        mockMvc.perform(get("/api/users/me/scrapped-etfs")
                        .principal(principal())
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess", is(true)))
                .andExpect(jsonPath("$.result.content", hasSize(1)))
                .andExpect(jsonPath("$.result.content[0].etfId", is((int) ETF_ID)))
                .andExpect(jsonPath("$.result.content[0].regionName", is("공주")))
                .andExpect(jsonPath("$.result.page", is(1)))
                .andExpect(jsonPath("$.result.size", is(5)));

        ArgumentCaptor<ScrappedEtfListRequest> requestCaptor =
                ArgumentCaptor.forClass(ScrappedEtfListRequest.class);
        org.mockito.Mockito.verify(scrappedEtfService).getScrappedEtfs(
                org.mockito.ArgumentMatchers.eq(USER_ID),
                requestCaptor.capture()
        );
        org.assertj.core.api.Assertions.assertThat(requestCaptor.getValue().getPage()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(requestCaptor.getValue().getSize()).isEqualTo(5);
    }

    @Test
    void scrapRequestWithoutPrincipalReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/etfs/{etfId}/scraps", ETF_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("COMMON401")));

        verifyNoInteractions(etfScrapService, scrappedEtfService);
    }

    @Test
    void invalidListPageSizeReturnsValidationError() throws Exception {
        mockMvc.perform(get("/api/users/me/scrapped-etfs")
                        .principal(principal())
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("COMMON400_1")));

        verifyNoInteractions(scrappedEtfService);
    }

    private Principal principal() {
        return new UsernamePasswordAuthenticationToken(String.valueOf(USER_ID), null, List.of());
    }
}
