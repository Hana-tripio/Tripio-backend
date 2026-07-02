package com.tripio.etf.service;

import com.tripio.etf.dto.EtfCardResponse;
import com.tripio.etf.dto.EtfListResponse;
import com.tripio.etf.dto.EtfSearchRequest;
import com.tripio.etf.entity.TravelEtf;
import com.tripio.etf.repository.EtfSearchCriteria;
import com.tripio.etf.repository.EtfStyleTagRow;
import com.tripio.etf.repository.TravelEtfRepository;
import com.tripio.etf.repository.TravelEtfStyleTagRepository;
import com.tripio.etf.type.EtfErrorCode;
import com.tripio.etf.type.EtfSearchSort;
import com.tripio.global.apiPayload.exception.GeneralException;
import com.tripio.region.entity.Region;
import com.tripio.region.repository.RegionRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EtfSearchQueryService implements EtfSearchService {

    private final TravelEtfRepository travelEtfRepository;
    private final RegionRepository regionRepository;
    private final TravelEtfStyleTagRepository styleTagRepository;

    @Override
    public EtfListResponse searchEtfs(EtfSearchRequest request) {
        EtfSearchCriteria criteria = toCriteria(request);
        PageRequest pageRequest = PageRequest.of(request.getPage(), request.getSize());
        Page<TravelEtf> etfPage = travelEtfRepository.search(criteria, pageRequest);

        if (etfPage.isEmpty()) {
            return EtfListResponse.from(etfPage, List.of());
        }

        List<Long> etfIds = etfPage.getContent().stream()
                .map(TravelEtf::getId)
                .toList();
        Map<Long, Region> regionsById = findRegionsById(etfPage.getContent());
        Map<Long, List<String>> styleTagsByEtfId = findStyleTagsByEtfId(etfIds);

        List<EtfCardResponse> content = etfPage.getContent().stream()
                .map(etf -> toCardResponse(etf, regionsById, styleTagsByEtfId))
                .toList();

        return EtfListResponse.from(etfPage, content);
    }

    private EtfSearchCriteria toCriteria(EtfSearchRequest request) {
        String keyword = request.getKeyword();
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        List<Long> styleTagIds = request.getStyleTagIds() == null
                ? List.of()
                : request.getStyleTagIds().stream().distinct().toList();

        return new EtfSearchCriteria(
                normalizedKeyword,
                request.getRegionId(),
                styleTagIds,
                request.getMinBudget(),
                request.getMaxBudget(),
                request.getMinDurationDays(),
                request.getMaxDurationDays(),
                EtfSearchSort.from(request.getSort())
        );
    }

    private Map<Long, Region> findRegionsById(List<TravelEtf> etfs) {
        List<Long> regionIds = etfs.stream()
                .map(TravelEtf::getRegionId)
                .distinct()
                .toList();

        return regionRepository.findAllById(regionIds).stream()
                .collect(Collectors.toMap(Region::getId, Function.identity()));
    }

    private Map<Long, List<String>> findStyleTagsByEtfId(List<Long> etfIds) {
        return styleTagRepository.findTagRowsByTravelEtfIds(etfIds).stream()
                .collect(Collectors.groupingBy(
                        EtfStyleTagRow::etfId,
                        LinkedHashMap::new,
                        Collectors.mapping(EtfStyleTagRow::tagName, Collectors.toList())
                ));
    }

    private EtfCardResponse toCardResponse(
            TravelEtf etf,
            Map<Long, Region> regionsById,
            Map<Long, List<String>> styleTagsByEtfId
    ) {
        Region region = regionsById.get(etf.getRegionId());
        if (region == null) {
            throw new GeneralException(EtfErrorCode.ETF_NOT_FOUND);
        }

        return new EtfCardResponse(
                etf.getId(),
                etf.getTitle(),
                region.getId(),
                region.getName(),
                etf.getDurationDays(),
                etf.getTotalBudget(),
                styleTagsByEtfId.getOrDefault(etf.getId(), List.of()),
                etf.getRegionValueScore(),
                etf.getLocalContributionScore(),
                etf.getLikeCount(),
                etf.getScrapCount(),
                etf.getFollowCount(),
                etf.getVerificationCount(),
                etf.getRatingAverage(),
                etf.getThumbnailUrl()
        );
    }
}
