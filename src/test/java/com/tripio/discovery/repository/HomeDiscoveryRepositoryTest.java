package com.tripio.discovery.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.tripio.discovery.type.HomeDiscoverySection;
import com.tripio.etf.entity.TravelEtf;
import com.tripio.support.IntegrationTestSupport;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class HomeDiscoveryRepositoryTest extends IntegrationTestSupport {

    private static final long OWNER_ID = 210001L;
    private static final long REGION_ID = 210101L;

    @Autowired
    private HomeDiscoveryRepository homeDiscoveryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpBaseFixtures() {
        jdbcTemplate.update("""
                INSERT INTO users (id, email, password_hash, nickname, status)
                VALUES (?, ?, ?, ?, ?)
                """, OWNER_ID, "home-repository@example.com", "encoded-password", "홈 Repository", "ACTIVE");
        jdbcTemplate.update("""
                INSERT INTO regions (id, name, region_type, latitude, longitude)
                VALUES (?, ?, ?, ?, ?)
                """, REGION_ID, "공주", "CITY", 36.4467, 127.1190);
    }

    @Test
    void findTopPopularOrdersByReactionCountsAndExcludesNonPublicEtfs() {
        insertEtf(210301L, "인기 ETF", "PUBLIC", 70, 30, 20, 10, 80, new BigDecimal("4.50"),
                OffsetDateTime.parse("2026-06-01T10:00:00+09:00"));
        insertEtf(210302L, "일반 ETF", "PUBLIC", 10, 5, 3, 1, 80, new BigDecimal("4.00"),
                OffsetDateTime.parse("2026-06-02T10:00:00+09:00"));
        insertEtf(210303L, "비공개 인기 ETF", "PRIVATE", 100, 100, 100, 100, 100, new BigDecimal("5.00"),
                OffsetDateTime.parse("2026-07-02T10:00:00+09:00"));

        List<TravelEtf> result = homeDiscoveryRepository.findTop(HomeDiscoverySection.POPULAR, 10);

        assertThat(result).extracting(TravelEtf::getId).containsExactly(210301L, 210302L);
    }

    @Test
    void findTopRisingUsesLatestCreatedAtAsMvpProxy() {
        insertEtf(210311L, "기존 고반응 ETF", "PUBLIC", 80, 50, 30, 20, 80, new BigDecimal("4.80"),
                OffsetDateTime.parse("2026-06-01T10:00:00+09:00"));
        insertEtf(210312L, "신규 ETF", "PUBLIC", 1, 1, 0, 0, 70, new BigDecimal("3.00"),
                OffsetDateTime.parse("2026-07-01T10:00:00+09:00"));
        insertEtf(210313L, "비공개 신규 ETF", "PRIVATE", 100, 100, 100, 100, 100, new BigDecimal("5.00"),
                OffsetDateTime.parse("2026-07-02T10:00:00+09:00"));

        List<TravelEtf> result = homeDiscoveryRepository.findTop(HomeDiscoverySection.RISING, 10);

        assertThat(result).extracting(TravelEtf::getId).containsExactly(210312L, 210311L);
    }

    @Test
    void findTopUndervaluedPrioritizesHighRegionValueAndLowTotalReactions() {
        insertEtf(210321L, "고가치 저반응 ETF", "PUBLIC", 1, 1, 1, 1, 90, new BigDecimal("4.50"),
                OffsetDateTime.parse("2026-06-01T10:00:00+09:00"));
        insertEtf(210322L, "고가치 고반응 ETF", "PUBLIC", 20, 20, 20, 20, 90, new BigDecimal("4.80"),
                OffsetDateTime.parse("2026-06-02T10:00:00+09:00"));
        insertEtf(210323L, "중간가치 무반응 ETF", "PUBLIC", 0, 0, 0, 0, 80, BigDecimal.ZERO,
                OffsetDateTime.parse("2026-06-03T10:00:00+09:00"));
        insertEtf(210324L, "비공개 저평가 ETF", "PRIVATE", 0, 0, 0, 0, 100, new BigDecimal("5.00"),
                OffsetDateTime.parse("2026-07-02T10:00:00+09:00"));

        List<TravelEtf> result = homeDiscoveryRepository.findTop(HomeDiscoverySection.UNDERVALUED, 10);

        assertThat(result).extracting(TravelEtf::getId).containsExactly(210321L, 210322L, 210323L);
    }

    @Test
    void findTopNeverReturnsMoreThanTenEtfs() {
        for (int index = 1; index <= 12; index++) {
            insertEtf(
                    210400L + index,
                    "제한 검증 ETF " + index,
                    "PUBLIC",
                    index,
                    index,
                    index,
                    index,
                    50,
                    new BigDecimal("4.00"),
                    OffsetDateTime.parse("2026-06-01T10:00:00+09:00").plusDays(index)
            );
        }

        List<TravelEtf> result = homeDiscoveryRepository.findTop(HomeDiscoverySection.POPULAR, 20);

        assertThat(result).hasSize(10);
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
                """, id, OWNER_ID, REGION_ID, title, "홈 큐레이션 테스트", status, 200000, 2,
                75, regionValueScore, likeCount, scrapCount, followCount, verificationCount, ratingAverage,
                "https://example.com/thumbnail.jpg", createdAt, createdAt);
    }
}
