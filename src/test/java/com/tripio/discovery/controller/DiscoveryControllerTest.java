package com.tripio.discovery.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tripio.discovery.dto.HomeDiscoveryResponse;
import com.tripio.discovery.service.HomeDiscoveryService;
import com.tripio.etf.dto.EtfCardResponse;
import com.tripio.global.config.SecurityConfig;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DiscoveryController.class)
@Import(SecurityConfig.class)
class DiscoveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HomeDiscoveryService homeDiscoveryService;

    @Test
    void getHomeDiscoveryReturnsThreeCurationSections() throws Exception {
        EtfCardResponse card = createCard(1L, "공주 힐링 ETF");
        given(homeDiscoveryService.getHomeDiscovery()).willReturn(new HomeDiscoveryResponse(
                List.of(card),
                List.of(card),
                List.of(card)
        ));

        mockMvc.perform(get("/api/discovery/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess", is(true)))
                .andExpect(jsonPath("$.code", is("COMMON200")))
                .andExpect(jsonPath("$.message", is("성공입니다.")))
                .andExpect(jsonPath("$.result.popularEtfs", hasSize(1)))
                .andExpect(jsonPath("$.result.risingEtfs", hasSize(1)))
                .andExpect(jsonPath("$.result.undervaluedEtfs", hasSize(1)))
                .andExpect(jsonPath("$.result.popularEtfs[0].etfId", is(1)));
    }

    @Test
    void getHomeDiscoveryReturnsEmptyArraysWhenNoEtfsExist() throws Exception {
        given(homeDiscoveryService.getHomeDiscovery()).willReturn(new HomeDiscoveryResponse(
                List.of(),
                List.of(),
                List.of()
        ));

        mockMvc.perform(get("/api/discovery/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.popularEtfs", hasSize(0)))
                .andExpect(jsonPath("$.result.risingEtfs", hasSize(0)))
                .andExpect(jsonPath("$.result.undervaluedEtfs", hasSize(0)));
    }

    private EtfCardResponse createCard(long id, String title) {
        return new EtfCardResponse(
                id,
                title,
                20L,
                "공주",
                2,
                200000,
                List.of("힐링"),
                80,
                75,
                10,
                5,
                3,
                2,
                new BigDecimal("4.50"),
                "https://example.com/thumbnail.jpg"
        );
    }
}
