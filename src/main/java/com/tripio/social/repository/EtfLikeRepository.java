package com.tripio.social.repository;

import com.tripio.social.entity.EtfLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EtfLikeRepository extends JpaRepository<EtfLike, Long> {

    boolean existsByUserIdAndTravelEtfId(Long userId, Long travelEtfId);

    long countByTravelEtfId(Long travelEtfId);

    @Modifying
    @Query("""
            delete from EtfLike like
            where like.userId = :userId
              and like.travelEtfId = :travelEtfId
            """)
    int deleteByUserIdAndTravelEtfId(
            @Param("userId") Long userId,
            @Param("travelEtfId") Long travelEtfId
    );
}
