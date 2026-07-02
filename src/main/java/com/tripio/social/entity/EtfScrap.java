package com.tripio.social.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "etf_scraps",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_etf_scraps_user_etf",
                columnNames = {"user_id", "travel_etf_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EtfScrap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "travel_etf_id", nullable = false)
    private Long travelEtfId;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public EtfScrap(Long userId, Long travelEtfId) {
        this.userId = userId;
        this.travelEtfId = travelEtfId;
    }
}
