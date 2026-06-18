package com.tripio.etf.entity;

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
@Table(name = "etf_portfolio_ratios")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EtfPortfolioRatio {

    @Id
    private Long id;

    @Column(name = "travel_etf_id", nullable = false, unique = true)
    private Long travelEtfId;

    @Column(name = "lodging_ratio", nullable = false, precision = 5, scale = 2)
    private BigDecimal lodgingRatio;

    @Column(name = "food_ratio", nullable = false, precision = 5, scale = 2)
    private BigDecimal foodRatio;

    @Column(name = "cafe_ratio", nullable = false, precision = 5, scale = 2)
    private BigDecimal cafeRatio;

    @Column(name = "activity_ratio", nullable = false, precision = 5, scale = 2)
    private BigDecimal activityRatio;

    @Column(name = "festival_ratio", nullable = false, precision = 5, scale = 2)
    private BigDecimal festivalRatio;

    @Column(name = "local_store_ratio", nullable = false, precision = 5, scale = 2)
    private BigDecimal localStoreRatio;

    @Column(name = "transport_ratio", nullable = false, precision = 5, scale = 2)
    private BigDecimal transportRatio;

    public EtfPortfolioRatio(
            Long id,
            Long travelEtfId,
            BigDecimal lodgingRatio,
            BigDecimal foodRatio,
            BigDecimal cafeRatio,
            BigDecimal activityRatio,
            BigDecimal festivalRatio,
            BigDecimal localStoreRatio,
            BigDecimal transportRatio
    ) {
        this.id = id;
        this.travelEtfId = travelEtfId;
        this.lodgingRatio = lodgingRatio;
        this.foodRatio = foodRatio;
        this.cafeRatio = cafeRatio;
        this.activityRatio = activityRatio;
        this.festivalRatio = festivalRatio;
        this.localStoreRatio = localStoreRatio;
        this.transportRatio = transportRatio;
    }
}
