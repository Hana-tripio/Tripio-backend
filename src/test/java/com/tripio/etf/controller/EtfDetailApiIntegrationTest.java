package com.tripio.etf.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tripio.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
class EtfDetailApiIntegrationTest extends IntegrationTestSupport {

    private static final long OWNER_ID = 91001L;
    private static final long REGION_ID = 92001L;
    private static final long PUBLIC_ETF_ID = 93001L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpBaseFixture() {
        jdbcTemplate.update("""
                INSERT INTO users (id, email, password_hash, nickname, status)
                VALUES (?, ?, ?, ?, ?)
                """, OWNER_ID, "etf-owner@example.com", "encoded-password", "ETF 소유자", "ACTIVE");

        jdbcTemplate.update("""
                INSERT INTO regions (id, name, region_type, latitude, longitude)
                VALUES (?, ?, ?, ?, ?)
                """, REGION_ID, "공주", "CITY", 36.4467, 127.1190);
    }

    @Test
    void getPublicEtfDetailReturnsDataFromPostgreSql() throws Exception {
        long styleTagId = 94001L;
        long placeId = 95001L;
        long itineraryDayId = 96001L;
        long itineraryItemId = 97001L;
        long portfolioRatioId = 98001L;

        insertEtf(PUBLIC_ETF_ID, "PUBLIC");
        jdbcTemplate.update("INSERT INTO style_tags (id, name) VALUES (?, ?)", styleTagId, "로컬푸드");
        jdbcTemplate.update("""
                INSERT INTO travel_etf_style_tags (travel_etf_id, style_tag_id)
                VALUES (?, ?)
                """, PUBLIC_ETF_ID, styleTagId);
        jdbcTemplate.update("""
                INSERT INTO places (id, region_id, name, address, latitude, longitude, category)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, placeId, REGION_ID, "공주산성시장", "충남 공주시 용당길 22", 36.4550, 127.1240, "MARKET");
        jdbcTemplate.update("""
                INSERT INTO etf_itinerary_days (id, travel_etf_id, day_number)
                VALUES (?, ?, ?)
                """, itineraryDayId, PUBLIC_ETF_ID, 1);
        jdbcTemplate.update("""
                INSERT INTO etf_itinerary_items (
                    id, itinerary_day_id, place_id, sequence, start_time, end_time,
                    estimated_cost, is_core, memo
                )
                VALUES (?, ?, ?, ?, CAST(? AS TIME), CAST(? AS TIME), ?, ?, ?)
                """, itineraryItemId, itineraryDayId, placeId, 1, "10:00", "11:30", 25000, true, "시장 투어");
        jdbcTemplate.update("""
                INSERT INTO etf_portfolio_ratios (
                    id, travel_etf_id, lodging_ratio, food_ratio, cafe_ratio,
                    activity_ratio, festival_ratio, local_store_ratio, transport_ratio
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, portfolioRatioId, PUBLIC_ETF_ID, 20.00, 35.00, 10.00, 15.00, 5.00, 10.00, 5.00);

        mockMvc.perform(get("/api/etfs/{etfId}", PUBLIC_ETF_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess", is(true)))
                .andExpect(jsonPath("$.code", is("COMMON200")))
                .andExpect(jsonPath("$.result.id", is((int) PUBLIC_ETF_ID)))
                .andExpect(jsonPath("$.result.status", is("PUBLIC")))
                .andExpect(jsonPath("$.result.owner.nickname", is("ETF 소유자")))
                .andExpect(jsonPath("$.result.region.name", is("공주")))
                .andExpect(jsonPath("$.result.styleTags", hasSize(1)))
                .andExpect(jsonPath("$.result.styleTags[0]", is("로컬푸드")))
                .andExpect(jsonPath("$.result.itineraryDays", hasSize(1)))
                .andExpect(jsonPath("$.result.itineraryDays[0].dayNumber", is(1)))
                .andExpect(jsonPath("$.result.itineraryDays[0].items[0].placeName", is("공주산성시장")))
                .andExpect(jsonPath("$.result.portfolioRatios.foodRatio", is(35.00)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"DRAFT", "PRIVATE", "ARCHIVED"})
    void getNonPublicEtfDetailReturnsSameNotFoundResponse(String statusValue) throws Exception {
        insertEtf(PUBLIC_ETF_ID, statusValue);

        mockMvc.perform(get("/api/etfs/{etfId}", PUBLIC_ETF_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess", is(false)))
                .andExpect(jsonPath("$.code", is("ETF404")))
                .andExpect(jsonPath("$.message", is("ETF를 찾을 수 없습니다.")));
    }

    @Test
    void getMissingEtfDetailReturnsEtfNotFound() throws Exception {
        mockMvc.perform(get("/api/etfs/{etfId}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess", is(false)))
                .andExpect(jsonPath("$.code", is("ETF404")))
                .andExpect(jsonPath("$.message", is("ETF를 찾을 수 없습니다.")));
    }

    @Test
    void getEtfDetailWithNonNumericIdReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/etfs/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess", is(false)))
                .andExpect(jsonPath("$.code", is("COMMON400")))
                .andExpect(jsonPath("$.message", is("잘못된 요청입니다.")));
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L})
    void getEtfDetailWithNonPositiveIdReturnsValidationError(long etfId) throws Exception {
        mockMvc.perform(get("/api/etfs/{etfId}", etfId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess", is(false)))
                .andExpect(jsonPath("$.code", is("COMMON400_1")))
                .andExpect(jsonPath("$.message", is("입력값 검증에 실패했습니다.")));
    }

    private void insertEtf(long etfId, String statusValue) {
        jdbcTemplate.update("""
                INSERT INTO travel_etfs (
                    id, owner_id, region_id, title, summary, status, total_budget, duration_days
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, etfId, OWNER_ID, REGION_ID, "공주 로컬푸드 ETF", "공개 상태 검증용 ETF", statusValue, 180000, 1);
    }
}
