package com.tripio.social.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tripio.global.apiPayload.exception.GeneralException;
import com.tripio.global.config.SecurityConfig;
import com.tripio.social.dto.EtfRatingResponse;
import com.tripio.social.service.EtfRatingService;
import com.tripio.social.type.RatingErrorCode;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EtfRatingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityConfig.class)
class EtfRatingControllerTest {

    private static final long USER_ID = 10L;
    private static final long ETF_ID = 20L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EtfRatingService etfRatingService;

    @Test
    void createRatingReturnsCurrentRatingAndAverage() throws Exception {
        given(etfRatingService.createRating(USER_ID, ETF_ID, 4))
                .willReturn(new EtfRatingResponse(ETF_ID, true, 4, new BigDecimal("4.00")));

        mockMvc.perform(post("/api/etfs/{etfId}/ratings", ETF_ID)
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess", is(true)))
                .andExpect(jsonPath("$.code", is("COMMON200")))
                .andExpect(jsonPath("$.result.etfId", is((int) ETF_ID)))
                .andExpect(jsonPath("$.result.rated", is(true)))
                .andExpect(jsonPath("$.result.score", is(4)))
                .andExpect(jsonPath("$.result.ratingAverage", is(4.0)));
    }

    @Test
    void updateRatingReturnsUpdatedRatingAndAverage() throws Exception {
        given(etfRatingService.updateRating(USER_ID, ETF_ID, 2))
                .willReturn(new EtfRatingResponse(ETF_ID, true, 2, new BigDecimal("3.50")));

        mockMvc.perform(put("/api/etfs/{etfId}/ratings", ETF_ID)
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.score", is(2)))
                .andExpect(jsonPath("$.result.ratingAverage", is(3.5)));
    }

    @Test
    void deleteRatingReturnsUnratedStateAndAverage() throws Exception {
        given(etfRatingService.deleteRating(USER_ID, ETF_ID))
                .willReturn(new EtfRatingResponse(ETF_ID, false, null, new BigDecimal("0.00")));

        mockMvc.perform(delete("/api/etfs/{etfId}/ratings", ETF_ID).principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.rated", is(false)))
                .andExpect(jsonPath("$.result.score").doesNotExist())
                .andExpect(jsonPath("$.result.ratingAverage", is(0.0)));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 6})
    void ratingOutsideRangeReturnsValidationError(int score) throws Exception {
        mockMvc.perform(post("/api/etfs/{etfId}/ratings", ETF_ID)
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":" + score + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("COMMON400_1")));

        verifyNoInteractions(etfRatingService);
    }

    @Test
    void missingScoreReturnsValidationError() throws Exception {
        mockMvc.perform(post("/api/etfs/{etfId}/ratings", ETF_ID)
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("COMMON400_1")));

        verifyNoInteractions(etfRatingService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"4.5", "\"4\""})
    void nonIntegerScoreReturnsBadRequest(String score) throws Exception {
        mockMvc.perform(post("/api/etfs/{etfId}/ratings", ETF_ID)
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":" + score + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("COMMON400")));

        verifyNoInteractions(etfRatingService);
    }

    @Test
    void duplicateCreateReturnsConflict() throws Exception {
        given(etfRatingService.createRating(USER_ID, ETF_ID, 4))
                .willThrow(new GeneralException(RatingErrorCode.RATING_ALREADY_EXISTS));

        mockMvc.perform(post("/api/etfs/{etfId}/ratings", ETF_ID)
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":4}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("RATING409")));
    }

    @Test
    void ratingRequestWithoutPrincipalReturnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/etfs/{etfId}/ratings", ETF_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("COMMON401")));

        verifyNoInteractions(etfRatingService);
    }

    private Principal principal() {
        return new UsernamePasswordAuthenticationToken(String.valueOf(USER_ID), null, List.of());
    }
}
