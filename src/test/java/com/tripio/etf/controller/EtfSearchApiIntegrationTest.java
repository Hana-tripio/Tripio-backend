package com.tripio.etf.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tripio.support.IntegrationTestSupport;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
class EtfSearchApiIntegrationTest extends IntegrationTestSupport {

    private static final long OWNER_ID = 120001L;
    private static final long REGION_ID = 120101L;
    private static final long HEALING_TAG_ID = 120201L;
    private static final long HISTORY_TAG_ID = 120202L;
    private static final long POPULAR_ETF_ID = 120301L;
    private static final long LATEST_ETF_ID = 120302L;
    private static final long PRIVATE_ETF_ID = 120303L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpFixtures() {
        jdbcTemplate.update("""
                INSERT INTO users (id, email, password_hash, nickname, status)
                VALUES (?, ?, ?, ?, ?)
                """, OWNER_ID, "api-search-owner@example.com", "encoded-password", "탐색 API 소유자", "ACTIVE");
        jdbcTemplate.update("""
                INSERT INTO regions (id, name, region_type, latitude, longitude)
                VALUES (?, ?, ?, ?, ?)
                """, REGION_ID, "공주", "CITY", 36.4467, 127.1190);
        jdbcTemplate.update("INSERT INTO style_tags (id, name) VALUES (?, ?)", HEALING_TAG_ID, "힐링");
        jdbcTemplate.update("INSERT INTO style_tags (id, name) VALUES (?, ?)", HISTORY_TAG_ID, "역사");

        insertEtf(POPULAR_ETF_ID, "공주 힐링 ETF", "시장과 한옥마을", "PUBLIC", 200000, 2,
                30, OffsetDateTime.parse("2026-06-01T10:00:00+09:00"));
        insertEtf(LATEST_ETF_ID, "공주 역사 ETF", "공주 역사 여행", "PUBLIC", 300000, 3,
                10, OffsetDateTime.parse("2026-07-01T10:00:00+09:00"));
        insertEtf(PRIVATE_ETF_ID, "공주 비공개 ETF", "노출 금지", "PRIVATE", 100000, 1,
                100, OffsetDateTime.parse("2026-07-02T10:00:00+09:00"));

        insertTagMapping(POPULAR_ETF_ID, HEALING_TAG_ID);
        insertTagMapping(POPULAR_ETF_ID, HISTORY_TAG_ID);
        insertTagMapping(LATEST_ETF_ID, HISTORY_TAG_ID);
        insertTagMapping(PRIVATE_ETF_ID, HEALING_TAG_ID);
    }

    @Test
    void searchEtfsReturnsOnlyPublicCardsWithRegionAndStyleTags() throws Exception {
        mockMvc.perform(get("/api/etfs")
                        .param("regionId", String.valueOf(REGION_ID))
                        .param("styleTagIds", String.valueOf(HEALING_TAG_ID), String.valueOf(HISTORY_TAG_ID))
                        .param("minBudget", "100000")
                        .param("maxBudget", "300000")
                        .param("minDurationDays", "1")
                        .param("maxDurationDays", "3")
                        .param("sort", "popular")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess", is(true)))
                .andExpect(jsonPath("$.code", is("COMMON200")))
                .andExpect(jsonPath("$.result.content", hasSize(2)))
                .andExpect(jsonPath("$.result.content[0].etfId", is((int) POPULAR_ETF_ID)))
                .andExpect(jsonPath("$.result.content[0].regionName", is("공주")))
                .andExpect(jsonPath("$.result.content[0].styleTags", hasSize(2)))
                .andExpect(jsonPath("$.result.content[1].etfId", is((int) LATEST_ETF_ID)))
                .andExpect(jsonPath("$.result.totalElements", is(2)))
                .andExpect(jsonPath("$.result.hasNext", is(false)));
    }

    @Test
    void searchEtfsReturnsEmptyContentWhenNoEtfMatches() throws Exception {
        mockMvc.perform(get("/api/etfs").param("keyword", "없는 검색어"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess", is(true)))
                .andExpect(jsonPath("$.result.content", hasSize(0)))
                .andExpect(jsonPath("$.result.totalElements", is(0)))
                .andExpect(jsonPath("$.result.totalPages", is(0)))
                .andExpect(jsonPath("$.result.hasNext", is(false)));
    }

    @Test
    void searchEtfsReturnsBadRequestForUnsupportedSort() throws Exception {
        mockMvc.perform(get("/api/etfs").param("sort", "rising"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess", is(false)))
                .andExpect(jsonPath("$.code", is("COMMON400")));
    }

    @Test
    void searchEtfsReturnsValidationErrorForInvalidRange() throws Exception {
        mockMvc.perform(get("/api/etfs")
                        .param("minDurationDays", "3")
                        .param("maxDurationDays", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess", is(false)))
                .andExpect(jsonPath("$.code", is("COMMON400_1")));
    }

    @Test
    void searchEtfsReturnsBadRequestForMalformedNumber() throws Exception {
        mockMvc.perform(get("/api/etfs").param("regionId", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess", is(false)));
    }

    private void insertEtf(
            long id,
            String title,
            String summary,
            String status,
            int budget,
            int durationDays,
            int likeCount,
            OffsetDateTime createdAt
    ) {
        jdbcTemplate.update("""
                INSERT INTO travel_etfs (
                    id, owner_id, region_id, title, summary, status, total_budget, duration_days,
                    local_contribution_score, region_value_score, like_count, scrap_count,
                    follow_count, verification_count, rating_average, thumbnail_url, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, OWNER_ID, REGION_ID, title, summary, status, budget, durationDays,
                82, 76, likeCount, 5, 3, 2, new BigDecimal("4.50"),
                "https://example.com/thumbnail.jpg", createdAt, createdAt);
    }

    private void insertTagMapping(long etfId, long tagId) {
        jdbcTemplate.update("""
                INSERT INTO travel_etf_style_tags (travel_etf_id, style_tag_id)
                VALUES (?, ?)
                """, etfId, tagId);
    }
}
