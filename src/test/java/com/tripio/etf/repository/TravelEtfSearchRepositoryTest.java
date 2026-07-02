package com.tripio.etf.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tripio.etf.entity.TravelEtf;
import com.tripio.etf.type.EtfSearchSort;
import com.tripio.global.apiPayload.code.GeneralErrorCode;
import com.tripio.global.apiPayload.exception.GeneralException;
import com.tripio.support.IntegrationTestSupport;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class TravelEtfSearchRepositoryTest extends IntegrationTestSupport {

    private static final long OWNER_ID = 110001L;
    private static final long GONGJU_REGION_ID = 110101L;
    private static final long JEJU_REGION_ID = 110102L;
    private static final long HEALING_TAG_ID = 110201L;
    private static final long HISTORY_TAG_ID = 110202L;
    private static final long POPULAR_ETF_ID = 110301L;
    private static final long LATEST_ETF_ID = 110302L;
    private static final long OTHER_REGION_ETF_ID = 110303L;
    private static final long PRIVATE_ETF_ID = 110304L;

    @Autowired
    private TravelEtfRepository travelEtfRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpFixtures() {
        jdbcTemplate.update("""
                INSERT INTO users (id, email, password_hash, nickname, status)
                VALUES (?, ?, ?, ?, ?)
                """, OWNER_ID, "search-owner@example.com", "encoded-password", "탐색 소유자", "ACTIVE");
        insertRegion(GONGJU_REGION_ID, "공주", 36.4467, 127.1190);
        insertRegion(JEJU_REGION_ID, "제주", 33.4996, 126.5312);
        jdbcTemplate.update("INSERT INTO style_tags (id, name) VALUES (?, ?)", HEALING_TAG_ID, "힐링");
        jdbcTemplate.update("INSERT INTO style_tags (id, name) VALUES (?, ?)", HISTORY_TAG_ID, "역사");

        insertEtf(POPULAR_ETF_ID, GONGJU_REGION_ID, "공주 로컬 힐링 ETF", "시장 중심 여행", "PUBLIC",
                200000, 2, 30, 20, 10, 8, new BigDecimal("4.50"),
                OffsetDateTime.parse("2026-06-01T10:00:00+09:00"));
        insertEtf(LATEST_ETF_ID, GONGJU_REGION_ID, "부여 역사 ETF", "공주 근교 역사 여행", "PUBLIC",
                400000, 3, 10, 8, 5, 3, new BigDecimal("4.20"),
                OffsetDateTime.parse("2026-07-01T10:00:00+09:00"));
        insertEtf(OTHER_REGION_ETF_ID, JEJU_REGION_ID, "제주 카페 ETF", "제주 카페 여행", "PUBLIC",
                700000, 4, 50, 40, 20, 10, new BigDecimal("4.80"),
                OffsetDateTime.parse("2026-05-01T10:00:00+09:00"));
        insertEtf(PRIVATE_ETF_ID, GONGJU_REGION_ID, "비공개 공주 ETF", "외부 노출 금지", "PRIVATE",
                100000, 1, 100, 100, 100, 100, new BigDecimal("5.00"),
                OffsetDateTime.parse("2026-07-02T10:00:00+09:00"));

        insertTagMapping(POPULAR_ETF_ID, HEALING_TAG_ID);
        insertTagMapping(POPULAR_ETF_ID, HISTORY_TAG_ID);
        insertTagMapping(LATEST_ETF_ID, HISTORY_TAG_ID);
        insertTagMapping(PRIVATE_ETF_ID, HEALING_TAG_ID);
    }

    @Test
    void searchAppliesAllFiltersAndExcludesNonPublicEtfs() {
        EtfSearchCriteria criteria = new EtfSearchCriteria(
                "힐링",
                GONGJU_REGION_ID,
                List.of(HISTORY_TAG_ID),
                100000,
                300000,
                1,
                2,
                EtfSearchSort.POPULAR
        );

        Page<TravelEtf> result = travelEtfRepository.search(criteria, PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(TravelEtf::getId).containsExactly(POPULAR_ETF_ID);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void searchMatchesAnyStyleTagWithoutDuplicateEtfs() {
        EtfSearchCriteria criteria = emptyCriteria(EtfSearchSort.POPULAR, List.of(HEALING_TAG_ID, HISTORY_TAG_ID));

        Page<TravelEtf> result = travelEtfRepository.search(criteria, PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(TravelEtf::getId)
                .containsExactly(POPULAR_ETF_ID, LATEST_ETF_ID);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void searchSortsPopularAndPaginatesWithStableTotalCount() {
        EtfSearchCriteria criteria = emptyCriteria(EtfSearchSort.POPULAR, List.of());

        Page<TravelEtf> result = travelEtfRepository.search(criteria, PageRequest.of(0, 2));

        assertThat(result.getContent()).extracting(TravelEtf::getId)
                .containsExactly(OTHER_REGION_ETF_ID, POPULAR_ETF_ID);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    void searchSortsLatestByCreatedAtDescending() {
        EtfSearchCriteria criteria = emptyCriteria(EtfSearchSort.LATEST, List.of());

        Page<TravelEtf> result = travelEtfRepository.search(criteria, PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(TravelEtf::getId)
                .containsExactly(LATEST_ETF_ID, POPULAR_ETF_ID, OTHER_REGION_ETF_ID);
    }

    @Test
    void searchUsesIdDescendingAsFinalTieBreakerForPopularAndLatest() {
        long lowerId = 110401L;
        long higherId = 110402L;
        OffsetDateTime sameCreatedAt = OffsetDateTime.parse("2026-06-15T10:00:00+09:00");
        insertEtf(lowerId, GONGJU_REGION_ID, "동률 ETF A", "정렬 동률 검증", "PUBLIC",
                250000, 2, 15, 10, 5, 2, new BigDecimal("4.30"), sameCreatedAt);
        insertEtf(higherId, GONGJU_REGION_ID, "동률 ETF B", "정렬 동률 검증", "PUBLIC",
                250000, 2, 15, 10, 5, 2, new BigDecimal("4.30"), sameCreatedAt);

        EtfSearchCriteria popularCriteria = new EtfSearchCriteria(
                "동률", null, List.of(), null, null, null, null, EtfSearchSort.POPULAR
        );
        EtfSearchCriteria latestCriteria = new EtfSearchCriteria(
                "동률", null, List.of(), null, null, null, null, EtfSearchSort.LATEST
        );

        Page<TravelEtf> popularResult = travelEtfRepository.search(popularCriteria, PageRequest.of(0, 20));
        Page<TravelEtf> latestResult = travelEtfRepository.search(latestCriteria, PageRequest.of(0, 20));

        assertThat(popularResult.getContent()).extracting(TravelEtf::getId)
                .containsExactly(higherId, lowerId);
        assertThat(latestResult.getContent()).extracting(TravelEtf::getId)
                .containsExactly(higherId, lowerId);
    }

    @Test
    void searchRejectsOffsetOutsideJpaSupportedIntegerRange() {
        EtfSearchCriteria criteria = emptyCriteria(EtfSearchSort.POPULAR, List.of());

        assertThatThrownBy(() -> travelEtfRepository.search(
                criteria,
                PageRequest.of(Integer.MAX_VALUE, 100)
        )).isInstanceOfSatisfying(GeneralException.class, exception ->
                assertThat(exception.getCode()).isEqualTo(GeneralErrorCode.BAD_REQUEST));
    }

    private EtfSearchCriteria emptyCriteria(EtfSearchSort sort, List<Long> styleTagIds) {
        return new EtfSearchCriteria(null, null, styleTagIds, null, null, null, null, sort);
    }

    private void insertRegion(long id, String name, double latitude, double longitude) {
        jdbcTemplate.update("""
                INSERT INTO regions (id, name, region_type, latitude, longitude)
                VALUES (?, ?, ?, ?, ?)
                """, id, name, "CITY", latitude, longitude);
    }

    private void insertEtf(
            long id,
            long regionId,
            String title,
            String summary,
            String status,
            int budget,
            int durationDays,
            int likeCount,
            int scrapCount,
            int followCount,
            int verificationCount,
            BigDecimal ratingAverage,
            OffsetDateTime createdAt
    ) {
        jdbcTemplate.update("""
                INSERT INTO travel_etfs (
                    id, owner_id, region_id, title, summary, status, total_budget, duration_days,
                    like_count, scrap_count, follow_count, verification_count, rating_average,
                    created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, OWNER_ID, regionId, title, summary, status, budget, durationDays,
                likeCount, scrapCount, followCount, verificationCount, ratingAverage, createdAt, createdAt);
    }

    private void insertTagMapping(long etfId, long tagId) {
        jdbcTemplate.update("""
                INSERT INTO travel_etf_style_tags (travel_etf_id, style_tag_id)
                VALUES (?, ?)
                """, etfId, tagId);
    }
}
