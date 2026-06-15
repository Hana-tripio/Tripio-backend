package com.tripio.domain.test.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tripio.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class TestControllerTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void successReturnsApiResponseWithoutAuthorization() throws Exception {
        mockMvc.perform(get("/api/test/success"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess", is(true)))
                .andExpect(jsonPath("$.code", is("COMMON200")))
                .andExpect(jsonPath("$.message", is("성공입니다.")))
                .andExpect(jsonPath("$.result", is("test success")));
    }

    @Test
    void failureReturnsApiResponseWithoutAuthorization() throws Exception {
        mockMvc.perform(get("/api/test/failure"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess", is(false)))
                .andExpect(jsonPath("$.code", is("COMMON400")))
                .andExpect(jsonPath("$.message", is("잘못된 요청입니다.")));
    }

    @Test
    void healthReturnsUpWithoutAuthorization() throws Exception {
        mockMvc.perform(get("/api/test/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }
}
