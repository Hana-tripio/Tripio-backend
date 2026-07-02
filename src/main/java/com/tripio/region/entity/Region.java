package com.tripio.region.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "regions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Region {

    @Id
    private Long id;

    @Column(name = "parent_region_id")
    private Long parentRegionId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "region_type", nullable = false, length = 30)
    private String regionType;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "per_score", nullable = false)
    private Integer perScore;

    @Column(name = "local_contribution_base_score", nullable = false)
    private Integer localContributionBaseScore;

    public Region(Long id, Long parentRegionId, String name, String regionType) {
        this(id, parentRegionId, name, regionType, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0);
    }

    public Region(
            Long id,
            Long parentRegionId,
            String name,
            String regionType,
            BigDecimal latitude,
            BigDecimal longitude,
            Integer perScore,
            Integer localContributionBaseScore
    ) {
        this.id = id;
        this.parentRegionId = parentRegionId;
        this.name = name;
        this.regionType = regionType;
        this.latitude = latitude;
        this.longitude = longitude;
        this.perScore = perScore;
        this.localContributionBaseScore = localContributionBaseScore;
    }
}
