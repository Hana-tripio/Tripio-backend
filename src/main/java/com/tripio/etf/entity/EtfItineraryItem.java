package com.tripio.etf.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "etf_itinerary_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EtfItineraryItem {

    @Id
    private Long id;

    @Column(name = "itinerary_day_id", nullable = false)
    private Long itineraryDayId;

    @Column(name = "place_id", nullable = false)
    private Long placeId;

    @Column(nullable = false)
    private Integer sequence;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "estimated_cost", nullable = false)
    private Integer estimatedCost;

    @Column(name = "is_core", nullable = false)
    private Boolean core;

    @Column
    private String memo;
}
