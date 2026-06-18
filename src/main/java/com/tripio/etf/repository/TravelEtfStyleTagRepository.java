package com.tripio.etf.repository;

import com.tripio.etf.entity.TravelEtfStyleTag;
import com.tripio.etf.entity.TravelEtfStyleTagId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TravelEtfStyleTagRepository extends JpaRepository<TravelEtfStyleTag, TravelEtfStyleTagId> {

    @Query("""
            select tag.name
            from TravelEtfStyleTag mapping
            join StyleTag tag on tag.id = mapping.styleTagId
            where mapping.travelEtfId = :travelEtfId
            order by tag.name
            """)
    List<String> findTagNamesByTravelEtfId(@Param("travelEtfId") Long travelEtfId);
}
