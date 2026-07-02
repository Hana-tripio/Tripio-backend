package com.tripio.social.repository;

import com.tripio.etf.entity.TravelEtf;
import com.tripio.social.entity.EtfScrap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EtfScrapRepository extends JpaRepository<EtfScrap, Long> {

    boolean existsByUserIdAndTravelEtfId(Long userId, Long travelEtfId);

    long countByTravelEtfId(Long travelEtfId);

    @Modifying
    @Query("""
            delete from EtfScrap scrap
            where scrap.userId = :userId
              and scrap.travelEtfId = :travelEtfId
            """)
    int deleteByUserIdAndTravelEtfId(
            @Param("userId") Long userId,
            @Param("travelEtfId") Long travelEtfId
    );

    @Query(
            value = """
                    select etf
                    from EtfScrap scrap
                    join TravelEtf etf on etf.id = scrap.travelEtfId
                    where scrap.userId = :userId
                      and etf.status = :status
                    order by scrap.createdAt desc, scrap.id desc
                    """,
            countQuery = """
                    select count(scrap)
                    from EtfScrap scrap
                    join TravelEtf etf on etf.id = scrap.travelEtfId
                    where scrap.userId = :userId
                      and etf.status = :status
                    """
    )
    Page<TravelEtf> findScrappedEtfsByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") String status,
            Pageable pageable
    );
}
