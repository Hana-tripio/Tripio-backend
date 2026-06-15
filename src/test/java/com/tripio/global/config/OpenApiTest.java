package com.tripio.global.config;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
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
class OpenApiTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDocsEndpointIsAvailable() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi", notNullValue()))
                .andExpect(jsonPath("$.info.title", is("Tripio API 명세서")))
                .andExpect(jsonPath("$.info.description", is("Tripio 프로젝트 백엔드 API 명세서입니다.")))
                .andExpect(jsonPath("$.info.version", is("0.0.1")))
                .andExpect(jsonPath("$.servers[0].url", is("/")))
                .andExpect(jsonPath("$.security", hasSize(1)))
                .andExpect(jsonPath("$.security[0]", hasKey("JWT TOKEN")))
                .andExpect(jsonPath("$.components.securitySchemes['JWT TOKEN'].type", is("http")))
                .andExpect(jsonPath("$.components.securitySchemes['JWT TOKEN'].scheme", is("bearer")))
                .andExpect(jsonPath("$.components.securitySchemes['JWT TOKEN'].bearerFormat", is("JWT")));
    }
}
