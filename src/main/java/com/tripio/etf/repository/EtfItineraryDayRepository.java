package com.tripio.etf.repository;

import com.tripio.etf.entity.EtfItineraryDay;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EtfItineraryDayRepository extends JpaRepository<EtfItineraryDay, Long> {

    List<EtfItineraryDay> findByTravelEtfIdOrderByDayNumber(Long travelEtfId);
}
