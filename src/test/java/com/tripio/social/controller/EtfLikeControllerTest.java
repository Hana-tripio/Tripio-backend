package com.tripio.social.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tripio.global.config.SecurityConfig;
import com.tripio.social.dto.EtfLikeResponse;
import com.tripio.social.service.EtfLikeService;
import java.security.Principal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EtfLikeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityConfig.class)
class EtfLikeControllerTest {

    private static final long USER_ID = 10L;
    private static final long ETF_ID = 20L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EtfLikeService etfLikeService;

    @Test
    void addLikeReturnsCurrentLikeState() throws Exception {
        given(etfLikeService.addLike(USER_ID, ETF_ID))
                .willReturn(new EtfLikeResponse(ETF_ID, true, 3));

        mockMvc.perform(post("/api/etfs/{etfId}/likes", ETF_ID).principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess", is(true)))
                .andExpect(jsonPath("$.code", is("COMMON200")))
                .andExpect(jsonPath("$.result.etfId", is((int) ETF_ID)))
                .andExpect(jsonPath("$.result.liked", is(true)))
                .andExpect(jsonPath("$.result.likeCount", is(3)));
    }

    @Test
    void removeLikeReturnsCurrentLikeState() throws Exception {
        given(etfLikeService.removeLike(USER_ID, ETF_ID))
                .willReturn(new EtfLikeResponse(ETF_ID, false, 2));

        mockMvc.perform(delete("/api/etfs/{etfId}/likes", ETF_ID).principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess", is(true)))
                .andExpect(jsonPath("$.result.liked", is(false)))
                .andExpect(jsonPath("$.result.likeCount", is(2)));
    }

    @Test
    void likeRequestWithoutPrincipalReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/etfs/{etfId}/likes", ETF_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess", is(false)))
                .andExpect(jsonPath("$.code", is("COMMON401")));

        verifyNoInteractions(etfLikeService);
    }

    @Test
    void likeRequestWithNonNumericPrincipalReturnsUnauthorized() throws Exception {
        Principal invalidPrincipal = new UsernamePasswordAuthenticationToken("user@example.com", null, List.of());

        mockMvc.perform(post("/api/etfs/{etfId}/likes", ETF_ID).principal(invalidPrincipal))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("COMMON401")));

        verifyNoInteractions(etfLikeService);
    }

    @Test
    void likeRequestWithNonPositiveEtfIdReturnsValidationError() throws Exception {
        mockMvc.perform(post("/api/etfs/{etfId}/likes", 0).principal(principal()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("COMMON400_1")));

        verifyNoInteractions(etfLikeService);
    }

    private Principal principal() {
        return new UsernamePasswordAuthenticationToken(String.valueOf(USER_ID), null, List.of());
    }
}
