package com.tripio.etf.repository;

import com.tripio.etf.entity.EtfItineraryItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EtfItineraryItemRepository extends JpaRepository<EtfItineraryItem, Long> {

    @Query("""
            select new com.tripio.etf.repository.EtfItineraryItemRow(
                item.id,
                place.id,
                place.name,
                place.category,
                item.sequence,
                item.startTime,
                item.endTime,
                item.estimatedCost,
                item.core,
                item.memo
            )
            from EtfItineraryItem item
            join Place place on place.id = item.placeId
            where item.itineraryDayId = :itineraryDayId
            order by item.sequence
            """)
    List<EtfItineraryItemRow> findRowsByItineraryDayId(@Param("itineraryDayId") Long itineraryDayId);
}
