package com.tripio.place.repository;

import com.tripio.place.entity.Place;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    List<Place> findByRegionIdOrderByCoreSpotDescNameAsc(Long regionId);
}
