package com.tripio.region.repository;

import com.tripio.region.entity.Region;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionRepository extends JpaRepository<Region, Long> {

    List<Region> findByParentRegionIdIsNullOrderByNameAsc();

    List<Region> findByParentRegionIdOrderByNameAsc(Long parentRegionId);

    List<Region> findByParentRegionIdIn(Collection<Long> parentRegionIds);
}
