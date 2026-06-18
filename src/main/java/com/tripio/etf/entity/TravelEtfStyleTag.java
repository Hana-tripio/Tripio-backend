package com.tripio.etf.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@IdClass(TravelEtfStyleTagId.class)
@Table(name = "travel_etf_style_tags")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TravelEtfStyleTag {

    @Id
    @Column(name = "travel_etf_id", nullable = false)
    private Long travelEtfId;

    @Id
    @Column(name = "style_tag_id", nullable = false)
    private Long styleTagId;
}
