package com.tripio.social.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tripio.support.IntegrationTestSupport;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc(addFilters = false)
class EtfLikeApiIntegrationTest extends IntegrationTestSupport {

    private static final long USER_ID = 330001L;
    private static final long REGION_ID = 330101L;
    private static final long PUBLIC_ETF_ID = 330201L;
    private static final long PRIVATE_ETF_ID = 330202L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpFixtures() {
        jdbcTemplate.update(
                "DELETE FROM etf_likes WHERE user_id = ? OR travel_etf_id IN (?, ?)",
                USER_ID,
                PUBLIC_ETF_ID,
                PRIVATE_ETF_ID
        );
        jdbcTemplate.update("DELETE FROM travel_etfs WHERE id IN (?, ?)", PUBLIC_ETF_ID, PRIVATE_ETF_ID);
        jdbcTemplate.update("DELETE FROM regions WHERE id = ?", REGION_ID);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", USER_ID);

        jdbcTemplate.update("""
                INSERT INTO users (id, email, password_hash, nickname, status)
                VALUES (?, ?, ?, ?, ?)
                """, USER_ID, "like-user@example.com", "encoded-password", "좋아요 사용자", "ACTIVE");
        jdbcTemplate.update("""
                INSERT INTO regions (id, name, region_type, latitude, longitude)
                VALUES (?, ?, ?, ?, ?)
                """, REGION_ID, "공주", "CITY", 36.4467, 127.1190);
        insertEtf(PUBLIC_ETF_ID, "PUBLIC");
        insertEtf(PRIVATE_ETF_ID, "PRIVATE");
    }

    @Test
    void addLikeIsIdempotentAndSynchronizesCachedCount() throws Exception {
        performAddLike()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess", is(true)))
                .andExpect(jsonPath("$.code", is("COMMON200")))
                .andExpect(jsonPath("$.result.etfId", is((int) PUBLIC_ETF_ID)))
                .andExpect(jsonPath("$.result.liked", is(true)))
                .andExpect(jsonPath("$.result.likeCount", is(1)));

        jdbcTemplate.update("UPDATE travel_etfs SET like_count = 7 WHERE id = ?", PUBLIC_ETF_ID);

        performAddLike()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.liked", is(true)))
                .andExpect(jsonPath("$.result.likeCount", is(1)));

        assertThat(likeRows()).isEqualTo(1);
        assertThat(cachedLikeCount()).isEqualTo(1);
        mockMvc.perform(get("/api/etfs/{etfId}", PUBLIC_ETF_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.reactionCounts.likeCount", is(1)));
    }

    @Test
    void removeLikeIsIdempotentAndNeverMakesCountNegative() throws Exception {
        performAddLike().andExpect(status().isOk());
        jdbcTemplate.update("UPDATE travel_etfs SET like_count = 7 WHERE id = ?", PUBLIC_ETF_ID);

        mockMvc.perform(delete("/api/etfs/{etfId}/likes", PUBLIC_ETF_ID).principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.liked", is(false)))
                .andExpect(jsonPath("$.result.likeCount", is(0)));
        mockMvc.perform(delete("/api/etfs/{etfId}/likes", PUBLIC_ETF_ID).principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.liked", is(false)))
                .andExpect(jsonPath("$.result.likeCount", is(0)));

        assertThat(likeRows()).isZero();
        assertThat(cachedLikeCount()).isZero();
        mockMvc.perform(get("/api/etfs/{etfId}", PUBLIC_ETF_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.reactionCounts.likeCount", is(0)));
    }

    @Test
    void databaseUniqueConstraintRejectsDuplicateLikeRows() {
        jdbcTemplate.update("""
                INSERT INTO etf_likes (user_id, travel_etf_id)
                VALUES (?, ?)
                """, USER_ID, PUBLIC_ETF_ID);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO etf_likes (user_id, travel_etf_id)
                VALUES (?, ?)
                """, USER_ID, PUBLIC_ETF_ID))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);

        assertThat(likeRows()).isEqualTo(1);
    }

    @Test
    void nonPublicAndMissingEtfsReturnSameNotFoundResponse() throws Exception {
        for (long etfId : List.of(PRIVATE_ETF_ID, 999999L)) {
            mockMvc.perform(post("/api/etfs/{etfId}/likes", etfId).principal(principal()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.isSuccess", is(false)))
                    .andExpect(jsonPath("$.code", is("ETF404")))
                    .andExpect(jsonPath("$.message", is("ETF를 찾을 수 없습니다.")));
            mockMvc.perform(delete("/api/etfs/{etfId}/likes", etfId).principal(principal()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.isSuccess", is(false)))
                    .andExpect(jsonPath("$.code", is("ETF404")))
                    .andExpect(jsonPath("$.message", is("ETF를 찾을 수 없습니다.")));
        }

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM etf_likes WHERE travel_etf_id = ?",
                Integer.class,
                PRIVATE_ETF_ID
        )).isZero();
    }

    @Test
    void invalidEtfIdsReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/etfs/not-a-number/likes").principal(principal()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("COMMON400")));
        mockMvc.perform(post("/api/etfs/0/likes").principal(principal()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("COMMON400_1")));
    }

    @Test
    void concurrentDuplicateLikesCreateOneRowAndOneCachedCount() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<MvcResult> first = executor.submit(() -> performConcurrentAddLike(start));
            Future<MvcResult> second = executor.submit(() -> performConcurrentAddLike(start));
            start.countDown();

            assertThat(first.get(10, TimeUnit.SECONDS).getResponse().getStatus()).isEqualTo(200);
            assertThat(second.get(10, TimeUnit.SECONDS).getResponse().getStatus()).isEqualTo(200);
        } finally {
            executor.shutdownNow();
        }

        assertThat(likeRows()).isEqualTo(1);
        assertThat(cachedLikeCount()).isEqualTo(1);
    }

    private org.springframework.test.web.servlet.ResultActions performAddLike() throws Exception {
        return mockMvc.perform(post("/api/etfs/{etfId}/likes", PUBLIC_ETF_ID).principal(principal()));
    }

    private MvcResult performConcurrentAddLike(CountDownLatch start) throws Exception {
        start.await();
        return mockMvc.perform(post("/api/etfs/{etfId}/likes", PUBLIC_ETF_ID).principal(principal()))
                .andReturn();
    }

    private Principal principal() {
        return new UsernamePasswordAuthenticationToken(String.valueOf(USER_ID), null, List.of());
    }

    private int likeRows() {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM etf_likes WHERE user_id = ? AND travel_etf_id = ?",
                Integer.class,
                USER_ID,
                PUBLIC_ETF_ID
        );
    }

    private int cachedLikeCount() {
        return jdbcTemplate.queryForObject(
                "SELECT like_count FROM travel_etfs WHERE id = ?",
                Integer.class,
                PUBLIC_ETF_ID
        );
    }

    private void insertEtf(long etfId, String status) {
        jdbcTemplate.update("""
                INSERT INTO travel_etfs (
                    id, owner_id, region_id, title, summary, status, total_budget, duration_days
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, etfId, USER_ID, REGION_ID, "공주 좋아요 ETF", "좋아요 통합 테스트", status, 100000, 1);
    }
}
