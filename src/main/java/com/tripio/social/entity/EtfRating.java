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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Entity
@Table(
        name = "etf_ratings",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_etf_ratings_user_etf",
                columnNames = {"user_id", "travel_etf_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EtfRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "travel_etf_id", nullable = false)
    private Long travelEtfId;

    @Column(nullable = false)
    private Integer score;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public EtfRating(Long userId, Long travelEtfId, Integer score) {
        this.userId = userId;
        this.travelEtfId = travelEtfId;
        this.score = score;
    }

    public void updateScore(Integer score) {
        this.score = score;
    }
}
