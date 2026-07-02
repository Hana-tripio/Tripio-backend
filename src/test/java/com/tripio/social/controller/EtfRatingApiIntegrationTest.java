package com.tripio.social.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tripio.support.IntegrationTestSupport;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc(addFilters = false)
class EtfRatingApiIntegrationTest extends IntegrationTestSupport {

    private static final long USER_ID = 550001L;
    private static final long OTHER_USER_ID = 550002L;
    private static final long REGION_ID = 550101L;
    private static final long PUBLIC_ETF_ID = 550201L;
    private static final long PRIVATE_ETF_ID = 550202L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpFixtures() {
        jdbcTemplate.update(
                "DELETE FROM etf_ratings WHERE user_id IN (?, ?) OR travel_etf_id IN (?, ?)",
                USER_ID,
                OTHER_USER_ID,
                PUBLIC_ETF_ID,
                PRIVATE_ETF_ID
        );
        jdbcTemplate.update("DELETE FROM travel_etfs WHERE id IN (?, ?)", PUBLIC_ETF_ID, PRIVATE_ETF_ID);
        jdbcTemplate.update("DELETE FROM regions WHERE id = ?", REGION_ID);
        jdbcTemplate.update("DELETE FROM users WHERE id IN (?, ?)", USER_ID, OTHER_USER_ID);

        insertUser(USER_ID, "rating-user@example.com", "평점 사용자");
        insertUser(OTHER_USER_ID, "other-rating-user@example.com", "다른 사용자");
        jdbcTemplate.update("""
                INSERT INTO regions (id, name, region_type, latitude, longitude)
                VALUES (?, ?, ?, ?, ?)
                """, REGION_ID, "공주", "CITY", 36.4467, 127.1190);
        insertEtf(PUBLIC_ETF_ID, "공개 평점 ETF", "PUBLIC");
        insertEtf(PRIVATE_ETF_ID, "비공개 평점 ETF", "PRIVATE");
    }

    @Test
    void createRatingRejectsDuplicateAndKeepsOneRating() throws Exception {
        performPost(USER_ID, PUBLIC_ETF_ID, 4)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess", is(true)))
                .andExpect(jsonPath("$.result.rated", is(true)))
                .andExpect(jsonPath("$.result.score", is(4)))
                .andExpect(jsonPath("$.result.ratingAverage", is(4.0)));

        performPost(USER_ID, PUBLIC_ETF_ID, 5)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.isSuccess", is(false)))
                .andExpect(jsonPath("$.code", is("RATING409")));

        assertThat(ratingRows()).isEqualTo(1);
        assertThat(cachedAverage()).isEqualByComparingTo("4.00");
    }

    @Test
    void updateRatingRecalculatesAverageFromActualRatings() throws Exception {
        insertRating(USER_ID, PUBLIC_ETF_ID, 4);
        insertRating(OTHER_USER_ID, PUBLIC_ETF_ID, 5);
        jdbcTemplate.update("UPDATE travel_etfs SET rating_average = 1.00 WHERE id = ?", PUBLIC_ETF_ID);

        performPut(USER_ID, PUBLIC_ETF_ID, 3)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.score", is(3)))
                .andExpect(jsonPath("$.result.ratingAverage", is(4.0)));

        assertThat(scoreOf(USER_ID)).isEqualTo(3);
        assertThat(cachedAverage()).isEqualByComparingTo("4.00");
    }

    @Test
    void updateMissingRatingReturnsRatingNotFound() throws Exception {
        performPut(USER_ID, PUBLIC_ETF_ID, 3)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("RATING404")));
    }

    @Test
    void deleteRatingIsIdempotentAndLastDeleteResetsAverageToZero() throws Exception {
        insertRating(USER_ID, PUBLIC_ETF_ID, 4);
        insertRating(OTHER_USER_ID, PUBLIC_ETF_ID, 2);
        jdbcTemplate.update("UPDATE travel_etfs SET rating_average = 3.00 WHERE id = ?", PUBLIC_ETF_ID);

        performDelete(USER_ID, PUBLIC_ETF_ID)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.rated", is(false)))
                .andExpect(jsonPath("$.result.ratingAverage", is(2.0)));
        performDelete(USER_ID, PUBLIC_ETF_ID)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.ratingAverage", is(2.0)));
        performDelete(OTHER_USER_ID, PUBLIC_ETF_ID)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.ratingAverage", is(0.0)));

        assertThat(ratingRows()).isZero();
        assertThat(cachedAverage()).isEqualByComparingTo("0.00");
    }

    @Test
    void nonPublicAndMissingEtfsReturnSameNotFoundResponse() throws Exception {
        for (long etfId : List.of(PRIVATE_ETF_ID, 999999L)) {
            performPost(USER_ID, etfId, 4)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code", is("ETF404")));
            performPut(USER_ID, etfId, 4)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code", is("ETF404")));
            performDelete(USER_ID, etfId)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code", is("ETF404")));
        }
    }

    @Test
    void databaseConstraintsRejectDuplicateAndOutOfRangeRatings() {
        insertRating(USER_ID, PUBLIC_ETF_ID, 4);

        assertThatThrownBy(() -> insertRating(USER_ID, PUBLIC_ETF_ID, 5))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertRating(OTHER_USER_ID, PUBLIC_ETF_ID, 0))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertRating(OTHER_USER_ID, PUBLIC_ETF_ID, 6))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(ratingRows()).isEqualTo(1);
    }

    @Test
    void concurrentDuplicateCreatesReturnOneSuccessAndOneConflict() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<MvcResult> first = executor.submit(() -> performConcurrentPost(start, USER_ID, 4));
            Future<MvcResult> second = executor.submit(() -> performConcurrentPost(start, USER_ID, 5));
            start.countDown();

            List<Integer> statuses = List.of(
                    first.get(10, TimeUnit.SECONDS).getResponse().getStatus(),
                    second.get(10, TimeUnit.SECONDS).getResponse().getStatus()
            );
            assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        } finally {
            executor.shutdownNow();
        }

        assertThat(ratingRows()).isEqualTo(1);
        assertThat(cachedAverage()).isEqualByComparingTo(BigDecimal.valueOf(scoreOf(USER_ID)));
    }

    @Test
    void concurrentDifferentUsersCreateExactAverage() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<MvcResult> first = executor.submit(() -> performConcurrentPost(start, USER_ID, 1));
            Future<MvcResult> second = executor.submit(() -> performConcurrentPost(start, OTHER_USER_ID, 5));
            start.countDown();

            assertThat(first.get(10, TimeUnit.SECONDS).getResponse().getStatus()).isEqualTo(200);
            assertThat(second.get(10, TimeUnit.SECONDS).getResponse().getStatus()).isEqualTo(200);
        } finally {
            executor.shutdownNow();
        }

        assertThat(ratingRows()).isEqualTo(2);
        assertThat(cachedAverage()).isEqualByComparingTo("3.00");
    }

    @Test
    void ratingAverageIsVisibleInDetailSearchAndHomeApis() throws Exception {
        performPost(USER_ID, PUBLIC_ETF_ID, 4).andExpect(status().isOk());
        performPost(OTHER_USER_ID, PUBLIC_ETF_ID, 2).andExpect(status().isOk());

        mockMvc.perform(get("/api/etfs/{etfId}", PUBLIC_ETF_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.reactionCounts.ratingAverage", is(3.0)));
        mockMvc.perform(get("/api/etfs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[0].etfId", is((int) PUBLIC_ETF_ID)))
                .andExpect(jsonPath("$.result.content[0].ratingAverage", is(3.0)));
        mockMvc.perform(get("/api/discovery/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.popularEtfs[0].etfId", is((int) PUBLIC_ETF_ID)))
                .andExpect(jsonPath("$.result.popularEtfs[0].ratingAverage", is(3.0)));
    }

    private org.springframework.test.web.servlet.ResultActions performPost(long userId, long etfId, int score)
            throws Exception {
        return mockMvc.perform(post("/api/etfs/{etfId}/ratings", etfId)
                .principal(principal(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"score\":" + score + "}"));
    }

    private org.springframework.test.web.servlet.ResultActions performPut(long userId, long etfId, int score)
            throws Exception {
        return mockMvc.perform(put("/api/etfs/{etfId}/ratings", etfId)
                .principal(principal(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"score\":" + score + "}"));
    }

    private org.springframework.test.web.servlet.ResultActions performDelete(long userId, long etfId)
            throws Exception {
        return mockMvc.perform(delete("/api/etfs/{etfId}/ratings", etfId).principal(principal(userId)));
    }

    private MvcResult performConcurrentPost(CountDownLatch start, long userId, int score) throws Exception {
        start.await();
        return performPost(userId, PUBLIC_ETF_ID, score).andReturn();
    }

    private Principal principal(long userId) {
        return new UsernamePasswordAuthenticationToken(String.valueOf(userId), null, List.of());
    }

    private int ratingRows() {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM etf_ratings WHERE travel_etf_id = ?",
                Integer.class,
                PUBLIC_ETF_ID
        );
    }

    private int scoreOf(long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT score FROM etf_ratings WHERE user_id = ? AND travel_etf_id = ?",
                Integer.class,
                userId,
                PUBLIC_ETF_ID
        );
    }

    private BigDecimal cachedAverage() {
        return jdbcTemplate.queryForObject(
                "SELECT rating_average FROM travel_etfs WHERE id = ?",
                BigDecimal.class,
                PUBLIC_ETF_ID
        );
    }

    private void insertUser(long userId, String email, String nickname) {
        jdbcTemplate.update("""
                INSERT INTO users (id, email, password_hash, nickname, status)
                VALUES (?, ?, ?, ?, ?)
                """, userId, email, "encoded-password", nickname, "ACTIVE");
    }

    private void insertEtf(long etfId, String title, String status) {
        jdbcTemplate.update("""
                INSERT INTO travel_etfs (
                    id, owner_id, region_id, title, summary, status, total_budget, duration_days
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, etfId, USER_ID, REGION_ID, title, "평점 통합 테스트", status, 100000, 1);
    }

    private void insertRating(long userId, long etfId, int score) {
        jdbcTemplate.update("""
                INSERT INTO etf_ratings (user_id, travel_etf_id, score)
                VALUES (?, ?, ?)
                """, userId, etfId, score);
    }
}
