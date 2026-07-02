package com.tripio.etf.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "travel_etfs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TravelEtf {

    @Id
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "region_id", nullable = false)
    private Long regionId;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false)
    private String summary;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "total_budget", nullable = false)
    private Integer totalBudget;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @Column(name = "local_contribution_score", nullable = false)
    private Integer localContributionScore;

    @Column(name = "region_value_score", nullable = false)
    private Integer regionValueScore;

    @Column(name = "expected_reward", nullable = false)
    private Integer expectedReward;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "like_count", nullable = false)
    private Integer likeCount;

    @Column(name = "scrap_count", nullable = false)
    private Integer scrapCount;

    @Column(name = "follow_count", nullable = false)
    private Integer followCount;

    @Column(name = "verification_count", nullable = false)
    private Integer verificationCount;

    @Column(name = "rating_average", nullable = false, precision = 3, scale = 2)
    private BigDecimal ratingAverage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public TravelEtf(
            Long id,
            Long ownerId,
            Long regionId,
            String title,
            String summary,
            String status,
            Integer totalBudget,
            Integer durationDays,
            Integer localContributionScore,
            Integer regionValueScore,
            Integer expectedReward,
            String thumbnailUrl,
            Integer likeCount,
            Integer scrapCount,
            Integer followCount,
            Integer verificationCount,
            BigDecimal ratingAverage,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.ownerId = ownerId;
        this.regionId = regionId;
        this.title = title;
        this.summary = summary;
        this.status = status;
        this.totalBudget = totalBudget;
        this.durationDays = durationDays;
        this.localContributionScore = localContributionScore;
        this.regionValueScore = regionValueScore;
        this.expectedReward = expectedReward;
        this.thumbnailUrl = thumbnailUrl;
        this.likeCount = likeCount;
        this.scrapCount = scrapCount;
        this.followCount = followCount;
        this.verificationCount = verificationCount;
        this.ratingAverage = ratingAverage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void synchronizeLikeCount(long likeCount) {
        this.likeCount = Math.toIntExact(likeCount);
    }

    public void synchronizeScrapCount(long scrapCount) {
        this.scrapCount = Math.toIntExact(scrapCount);
    }

    public void synchronizeRatingAverage(BigDecimal ratingAverage) {
        this.ratingAverage = ratingAverage;
    }
}
