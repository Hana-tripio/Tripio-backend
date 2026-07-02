package com.tripio.discovery.controller;

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
class HomeDiscoveryApiIntegrationTest extends IntegrationTestSupport {

    private static final long OWNER_ID = 220001L;
    private static final long REGION_ID = 220101L;
    private static final long STYLE_TAG_ID = 220201L;
    private static final long POPULAR_ETF_ID = 220301L;
    private static final long RISING_ETF_ID = 220302L;
    private static final long UNDERVALUED_ETF_ID = 220303L;
    private static final long PRIVATE_ETF_ID = 220304L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpBaseFixtures() {
        jdbcTemplate.update("""
                INSERT INTO users (id, email, password_hash, nickname, status)
                VALUES (?, ?, ?, ?, ?)
                """, OWNER_ID, "home-api@example.com", "encoded-password", "홈 API", "ACTIVE");
        jdbcTemplate.update("""
                INSERT INTO regions (id, name, region_type, latitude, longitude)
                VALUES (?, ?, ?, ?, ?)
                """, REGION_ID, "공주", "CITY", 36.4467, 127.1190);
        jdbcTemplate.update("INSERT INTO style_tags (id, name) VALUES (?, ?)", STYLE_TAG_ID, "힐링");
    }

    @Test
    void getHomeDiscoveryReturnsThreeOrderedPublicSections() throws Exception {
        insertEtf(POPULAR_ETF_ID, "인기 ETF", "PUBLIC", 50, 30, 20, 10, 70,
                new BigDecimal("4.50"), OffsetDateTime.parse("2026-06-01T10:00:00+09:00"));
        insertEtf(RISING_ETF_ID, "신규 ETF", "PUBLIC", 2, 1, 0, 0, 80,
                new BigDecimal("3.50"), OffsetDateTime.parse("2026-07-01T10:00:00+09:00"));
        insertEtf(UNDERVALUED_ETF_ID, "저평가 ETF", "PUBLIC", 0, 0, 0, 0, 95,
                new BigDecimal("4.80"), OffsetDateTime.parse("2026-05-01T10:00:00+09:00"));
        insertEtf(PRIVATE_ETF_ID, "비공개 ETF", "PRIVATE", 100, 100, 100, 100, 100,
                new BigDecimal("5.00"), OffsetDateTime.parse("2026-07-02T10:00:00+09:00"));
        insertStyleTag(POPULAR_ETF_ID);
        insertStyleTag(RISING_ETF_ID);

        mockMvc.perform(get("/api/discovery/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess", is(true)))
                .andExpect(jsonPath("$.code", is("COMMON200")))
                .andExpect(jsonPath("$.result.popularEtfs", hasSize(3)))
                .andExpect(jsonPath("$.result.risingEtfs", hasSize(3)))
                .andExpect(jsonPath("$.result.undervaluedEtfs", hasSize(3)))
                .andExpect(jsonPath("$.result.popularEtfs[0].etfId", is((int) POPULAR_ETF_ID)))
                .andExpect(jsonPath("$.result.risingEtfs[0].etfId", is((int) RISING_ETF_ID)))
                .andExpect(jsonPath("$.result.undervaluedEtfs[0].etfId", is((int) UNDERVALUED_ETF_ID)))
                .andExpect(jsonPath("$.result.popularEtfs[0].regionName", is("공주")))
                .andExpect(jsonPath("$.result.popularEtfs[0].styleTags", hasSize(1)))
                .andExpect(jsonPath("$.result.undervaluedEtfs[0].styleTags", hasSize(0)));
    }

    @Test
    void getHomeDiscoveryReturnsEmptyArraysWhenNoPublicEtfsExist() throws Exception {
        mockMvc.perform(get("/api/discovery/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess", is(true)))
                .andExpect(jsonPath("$.result.popularEtfs", hasSize(0)))
                .andExpect(jsonPath("$.result.risingEtfs", hasSize(0)))
                .andExpect(jsonPath("$.result.undervaluedEtfs", hasSize(0)));
    }

    private void insertEtf(
            long id,
            String title,
            String status,
            int likeCount,
            int scrapCount,
            int followCount,
            int verificationCount,
            int regionValueScore,
            BigDecimal ratingAverage,
            OffsetDateTime createdAt
    ) {
        jdbcTemplate.update("""
                INSERT INTO travel_etfs (
                    id, owner_id, region_id, title, summary, status, total_budget, duration_days,
                    local_contribution_score, region_value_score, like_count, scrap_count,
                    follow_count, verification_count, rating_average, thumbnail_url, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, OWNER_ID, REGION_ID, title, "홈 API 통합 테스트", status, 200000, 2,
                75, regionValueScore, likeCount, scrapCount, followCount, verificationCount, ratingAverage,
                "https://example.com/thumbnail.jpg", createdAt, createdAt);
    }

    private void insertStyleTag(long etfId) {
        jdbcTemplate.update("""
                INSERT INTO travel_etf_style_tags (travel_etf_id, style_tag_id)
                VALUES (?, ?)
                """, etfId, STYLE_TAG_ID);
    }
}
