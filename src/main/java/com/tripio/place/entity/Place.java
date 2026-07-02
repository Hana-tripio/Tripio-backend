package com.tripio.place.entity;

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
@Table(name = "places")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place {

    @Id
    private Long id;

    @Column(name = "region_id", nullable = false)
    private Long regionId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(nullable = false, length = 40)
    private String category;

    @Column(name = "is_local", nullable = false)
    private boolean local;

    @Column(name = "is_core_spot", nullable = false)
    private boolean coreSpot;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "estimated_cost", nullable = false)
    private Integer estimatedCost;

    public Place(
            Long id,
            Long regionId,
            String name,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            String category,
            boolean local,
            boolean coreSpot,
            String imageUrl,
            Integer estimatedCost
    ) {
        this.id = id;
        this.regionId = regionId;
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.category = category;
        this.local = local;
        this.coreSpot = coreSpot;
        this.imageUrl = imageUrl;
        this.estimatedCost = estimatedCost;
    }
}
