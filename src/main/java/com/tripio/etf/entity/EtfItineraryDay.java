package com.tripio.etf.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "etf_itinerary_days")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EtfItineraryDay {

    @Id
    private Long id;

    @Column(name = "travel_etf_id", nullable = false)
    private Long travelEtfId;

    @Column(name = "day_number", nullable = false)
    private Integer dayNumber;

    public EtfItineraryDay(Long id, Long travelEtfId, Integer dayNumber) {
        this.id = id;
        this.travelEtfId = travelEtfId;
        this.dayNumber = dayNumber;
    }
}
