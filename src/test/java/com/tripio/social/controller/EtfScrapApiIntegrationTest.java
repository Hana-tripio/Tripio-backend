package com.tripio.social.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc(addFilters = false)
class EtfScrapApiIntegrationTest extends IntegrationTestSupport {

    private static final long USER_ID = 440001L;
    private static final long OTHER_USER_ID = 440002L;
    private static final long REGION_ID = 440101L;
    private static final long STYLE_TAG_ID = 440201L;
    private static final long PUBLIC_ETF_ID = 440301L;
    private static final long SECOND_PUBLIC_ETF_ID = 440302L;
    private static final long PRIVATE_ETF_ID = 440303L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpFixtures() {
        jdbcTemplate.update(
                "DELETE FROM etf_scraps WHERE user_id IN (?, ?) OR travel_etf_id IN (?, ?, ?)",
                USER_ID,
                OTHER_USER_ID,
                PUBLIC_ETF_ID,
                SECOND_PUBLIC_ETF_ID,
                PRIVATE_ETF_ID
        );
        jdbcTemplate.update(
                "DELETE FROM travel_etfs WHERE id IN (?, ?, ?)",
                PUBLIC_ETF_ID,
                SECOND_PUBLIC_ETF_ID,
                PRIVATE_ETF_ID
        );
        jdbcTemplate.update("DELETE FROM style_tags WHERE id = ?", STYLE_TAG_ID);
        jdbcTemplate.update("DELETE FROM regions WHERE id = ?", REGION_ID);
        jdbcTemplate.update("DELETE FROM users WHERE id IN (?, ?)", USER_ID, OTHER_USER_ID);

        insertUser(USER_ID, "scrap-user@example.com", "저장 사용자");
        insertUser(OTHER_USER_ID, "other-scrap-user@example.com", "다른 사용자");
        jdbcTemplate.update("""
                INSERT INTO regions (id, name, region_type, latitude, longitude)
                VALUES (?, ?, ?, ?, ?)
                """, REGION_ID, "공주", "CITY", 36.4467, 127.1190);
        jdbcTemplate.update("INSERT INTO style_tags (id, name) VALUES (?, ?)", STYLE_TAG_ID, "힐링");
        insertEtf(PUBLIC_ETF_ID, "먼저 저장한 ETF", "PUBLIC");
        insertEtf(SECOND_PUBLIC_ETF_ID, "최근 저장한 ETF", "PUBLIC");
        insertEtf(PRIVATE_ETF_ID, "비공개 ETF", "PRIVATE");
        insertStyleTag(PUBLIC_ETF_ID);
        insertStyleTag(SECOND_PUBLIC_ETF_ID);
    }

    @Test
    void addScrapIsIdempotentAndSynchronizesCachedCount() throws Exception {
        performAddScrap()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess", is(true)))
                .andExpect(jsonPath("$.code", is("COMMON200")))
                .andExpect(jsonPath("$.result.etfId", is((int) PUBLIC_ETF_ID)))
                .andExpect(jsonPath("$.result.scrapped", is(true)))
                .andExpect(jsonPath("$.result.scrapCount", is(1)));

        jdbcTemplate.update("UPDATE travel_etfs SET scrap_count = 7 WHERE id = ?", PUBLIC_ETF_ID);

        performAddScrap()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.scrapped", is(true)))
                .andExpect(jsonPath("$.result.scrapCount", is(1)));

        assertThat(scrapRows()).isEqualTo(1);
        assertThat(cachedScrapCount()).isEqualTo(1);
        mockMvc.perform(get("/api/etfs/{etfId}", PUBLIC_ETF_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.reactionCounts.scrapCount", is(1)));
        mockMvc.perform(get("/api/etfs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[0].etfId", is((int) PUBLIC_ETF_ID)))
                .andExpect(jsonPath("$.result.content[0].scrapCount", is(1)));
        mockMvc.perform(get("/api/discovery/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.popularEtfs[0].etfId", is((int) PUBLIC_ETF_ID)))
                .andExpect(jsonPath("$.result.popularEtfs[0].scrapCount", is(1)));
    }

    @Test
    void removeScrapIsIdempotentAndNeverMakesCountNegative() throws Exception {
        performAddScrap().andExpect(status().isOk());
        jdbcTemplate.update("UPDATE travel_etfs SET scrap_count = 7 WHERE id = ?", PUBLIC_ETF_ID);

        mockMvc.perform(delete("/api/etfs/{etfId}/scraps", PUBLIC_ETF_ID).principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.scrapped", is(false)))
                .andExpect(jsonPath("$.result.scrapCount", is(0)));
        mockMvc.perform(delete("/api/etfs/{etfId}/scraps", PUBLIC_ETF_ID).principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.scrapped", is(false)))
                .andExpect(jsonPath("$.result.scrapCount", is(0)));

        assertThat(scrapRows()).isZero();
        assertThat(cachedScrapCount()).isZero();
        mockMvc.perform(get("/api/etfs/{etfId}", PUBLIC_ETF_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.reactionCounts.scrapCount", is(0)));
    }

    @Test
    void nonPublicAndMissingEtfsReturnSameNotFoundResponse() throws Exception {
        for (long etfId : List.of(PRIVATE_ETF_ID, 999999L)) {
            mockMvc.perform(post("/api/etfs/{etfId}/scraps", etfId).principal(principal()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.isSuccess", is(false)))
                    .andExpect(jsonPath("$.code", is("ETF404")));
            mockMvc.perform(delete("/api/etfs/{etfId}/scraps", etfId).principal(principal()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.isSuccess", is(false)))
                    .andExpect(jsonPath("$.code", is("ETF404")));
        }
    }

    @Test
    void getScrappedEtfsReturnsOnlyCurrentUsersPublicEtfsInLatestOrder() throws Exception {
        insertScrap(440401L, USER_ID, PUBLIC_ETF_ID, "2026-07-01T10:00:00+09:00");
        insertScrap(440402L, USER_ID, SECOND_PUBLIC_ETF_ID, "2026-07-02T10:00:00+09:00");
        insertScrap(440403L, USER_ID, PRIVATE_ETF_ID, "2026-07-03T10:00:00+09:00");
        insertScrap(440404L, OTHER_USER_ID, PUBLIC_ETF_ID, "2026-07-04T10:00:00+09:00");

        mockMvc.perform(get("/api/users/me/scrapped-etfs")
                        .principal(principal())
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content", hasSize(1)))
                .andExpect(jsonPath("$.result.content[0].etfId", is((int) SECOND_PUBLIC_ETF_ID)))
                .andExpect(jsonPath("$.result.content[0].regionName", is("공주")))
                .andExpect(jsonPath("$.result.content[0].styleTags[0]", is("힐링")))
                .andExpect(jsonPath("$.result.page", is(0)))
                .andExpect(jsonPath("$.result.size", is(1)))
                .andExpect(jsonPath("$.result.totalElements", is(2)))
                .andExpect(jsonPath("$.result.totalPages", is(2)))
                .andExpect(jsonPath("$.result.hasNext", is(true)));

        mockMvc.perform(get("/api/users/me/scrapped-etfs")
                        .principal(principal())
                        .param("page", "1")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[0].etfId", is((int) PUBLIC_ETF_ID)))
                .andExpect(jsonPath("$.result.totalElements", is(2)))
                .andExpect(jsonPath("$.result.hasNext", is(false)));
    }

    @Test
    void archivedScrappedEtfIsExcludedFromContentAndCount() throws Exception {
        jdbcTemplate.update("UPDATE travel_etfs SET status = 'ARCHIVED' WHERE id = ?", PRIVATE_ETF_ID);
        insertScrap(440405L, USER_ID, PRIVATE_ETF_ID, "2026-07-03T10:00:00+09:00");

        mockMvc.perform(get("/api/users/me/scrapped-etfs").principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content", hasSize(0)))
                .andExpect(jsonPath("$.result.totalElements", is(0)))
                .andExpect(jsonPath("$.result.totalPages", is(0)));
    }

    @Test
    void scrappedEtfsUseScrapIdDescendingAsFinalTieBreaker() throws Exception {
        String sameCreatedAt = "2026-07-02T10:00:00+09:00";
        insertScrap(440406L, USER_ID, PUBLIC_ETF_ID, sameCreatedAt);
        insertScrap(440407L, USER_ID, SECOND_PUBLIC_ETF_ID, sameCreatedAt);

        mockMvc.perform(get("/api/users/me/scrapped-etfs")
                        .principal(principal())
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[0].etfId", is((int) SECOND_PUBLIC_ETF_ID)))
                .andExpect(jsonPath("$.result.content[1].etfId", is((int) PUBLIC_ETF_ID)))
                .andExpect(jsonPath("$.result.totalElements", is(2)));
    }

    @Test
    void databaseUniqueConstraintRejectsDuplicateScrapRows() {
        jdbcTemplate.update("""
                INSERT INTO etf_scraps (user_id, travel_etf_id)
                VALUES (?, ?)
                """, USER_ID, PUBLIC_ETF_ID);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO etf_scraps (user_id, travel_etf_id)
                VALUES (?, ?)
                """, USER_ID, PUBLIC_ETF_ID))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(scrapRows()).isEqualTo(1);
    }

    @Test
    void concurrentDuplicateScrapsCreateOneRowAndOneCachedCount() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<MvcResult> first = executor.submit(() -> performConcurrentAddScrap(start));
            Future<MvcResult> second = executor.submit(() -> performConcurrentAddScrap(start));
            start.countDown();

            assertThat(first.get(10, TimeUnit.SECONDS).getResponse().getStatus()).isEqualTo(200);
            assertThat(second.get(10, TimeUnit.SECONDS).getResponse().getStatus()).isEqualTo(200);
        } finally {
            executor.shutdownNow();
        }

        assertThat(scrapRows()).isEqualTo(1);
        assertThat(cachedScrapCount()).isEqualTo(1);
    }

    private org.springframework.test.web.servlet.ResultActions performAddScrap() throws Exception {
        return mockMvc.perform(post("/api/etfs/{etfId}/scraps", PUBLIC_ETF_ID).principal(principal()));
    }

    private MvcResult performConcurrentAddScrap(CountDownLatch start) throws Exception {
        start.await();
        return mockMvc.perform(post("/api/etfs/{etfId}/scraps", PUBLIC_ETF_ID).principal(principal()))
                .andReturn();
    }

    private Principal principal() {
        return new UsernamePasswordAuthenticationToken(String.valueOf(USER_ID), null, List.of());
    }

    private int scrapRows() {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM etf_scraps WHERE user_id = ? AND travel_etf_id = ?",
                Integer.class,
                USER_ID,
                PUBLIC_ETF_ID
        );
    }

    private int cachedScrapCount() {
        return jdbcTemplate.queryForObject(
                "SELECT scrap_count FROM travel_etfs WHERE id = ?",
                Integer.class,
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
                """, etfId, USER_ID, REGION_ID, title, "스크랩 통합 테스트", status, 100000, 1);
    }

    private void insertStyleTag(long etfId) {
        jdbcTemplate.update("""
                INSERT INTO travel_etf_style_tags (travel_etf_id, style_tag_id)
                VALUES (?, ?)
                """, etfId, STYLE_TAG_ID);
    }

    private void insertScrap(long scrapId, long userId, long etfId, String createdAt) {
        jdbcTemplate.update("""
                INSERT INTO etf_scraps (id, user_id, travel_etf_id, created_at)
                VALUES (?, ?, ?, CAST(? AS TIMESTAMPTZ))
                """, scrapId, userId, etfId, createdAt);
    }
}
