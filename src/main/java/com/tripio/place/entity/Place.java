package com.tripio.place.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

    @Column(nullable = false, length = 40)
    private String category;
}
