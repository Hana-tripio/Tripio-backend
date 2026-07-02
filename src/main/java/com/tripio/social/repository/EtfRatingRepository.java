package com.tripio.social.repository;

import com.tripio.social.entity.EtfRating;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EtfRatingRepository extends JpaRepository<EtfRating, Long> {

    boolean existsByUserIdAndTravelEtfId(Long userId, Long travelEtfId);

    Optional<EtfRating> findByUserIdAndTravelEtfId(Long userId, Long travelEtfId);

    @Modifying
    @Query("""
            delete from EtfRating rating
            where rating.userId = :userId
              and rating.travelEtfId = :travelEtfId
            """)
    int deleteByUserIdAndTravelEtfId(
            @Param("userId") Long userId,
            @Param("travelEtfId") Long travelEtfId
    );

    @Query(
            value = """
                    select coalesce(avg(score), 0)
                    from etf_ratings
                    where travel_etf_id = :travelEtfId
                    """,
            nativeQuery = true
    )
    BigDecimal calculateAverageByTravelEtfId(@Param("travelEtfId") Long travelEtfId);
}
