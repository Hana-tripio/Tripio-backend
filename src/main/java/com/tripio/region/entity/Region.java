package com.tripio.region.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

    public Region(Long id, Long parentRegionId, String name, String regionType) {
        this.id = id;
        this.parentRegionId = parentRegionId;
        this.name = name;
        this.regionType = regionType;
    }
}
