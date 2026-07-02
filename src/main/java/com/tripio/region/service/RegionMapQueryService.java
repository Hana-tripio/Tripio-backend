package com.tripio.region.service;

import com.tripio.global.apiPayload.code.GeneralErrorCode;
import com.tripio.global.apiPayload.exception.GeneralException;
import com.tripio.region.dto.MapRegionListResponse;
import com.tripio.region.dto.MapRegionResponse;
import com.tripio.region.entity.Region;
import com.tripio.region.repository.RegionRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegionMapQueryService implements RegionMapService {

    private final RegionRepository regionRepository;

    @Override
    public MapRegionListResponse getMapRegions(Long parentRegionId) {
        List<Region> regions = loadRegions(parentRegionId);
        Set<Long> parentIdsWithChildren = loadParentIdsWithChildren(regions);

        return new MapRegionListResponse(regions.stream()
                .map(region -> toResponse(region, parentIdsWithChildren))
                .toList());
    }

    private List<Region> loadRegions(Long parentRegionId) {
        if (parentRegionId == null) {
            return regionRepository.findByParentRegionIdIsNullOrderByNameAsc();
        }

        regionRepository.findById(parentRegionId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.NOT_FOUND));
        return regionRepository.findByParentRegionIdOrderByNameAsc(parentRegionId);
    }

    private Set<Long> loadParentIdsWithChildren(List<Region> regions) {
        List<Long> regionIds = regions.stream()
                .map(Region::getId)
                .toList();
        if (regionIds.isEmpty()) {
            return Set.of();
        }

        return regionRepository.findByParentRegionIdIn(regionIds).stream()
                .map(Region::getParentRegionId)
                .collect(Collectors.toSet());
    }

    private MapRegionResponse toResponse(Region region, Set<Long> parentIdsWithChildren) {
        return new MapRegionResponse(
                region.getId(),
                region.getName(),
                region.getRegionType(),
                region.getLatitude(),
                region.getLongitude(),
                region.getPerScore(),
                region.getLocalContributionBaseScore(),
                parentIdsWithChildren.contains(region.getId())
        );
    }
}
