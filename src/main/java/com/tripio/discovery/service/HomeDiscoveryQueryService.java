package com.tripio.discovery.service;

import com.tripio.discovery.dto.HomeDiscoveryResponse;
import com.tripio.discovery.repository.HomeDiscoveryRepository;
import com.tripio.discovery.type.HomeDiscoverySection;
import com.tripio.etf.dto.EtfCardResponse;
import com.tripio.etf.entity.TravelEtf;
import com.tripio.etf.repository.EtfStyleTagRow;
import com.tripio.etf.repository.TravelEtfStyleTagRepository;
import com.tripio.etf.type.EtfErrorCode;
import com.tripio.global.apiPayload.exception.GeneralException;
import com.tripio.region.entity.Region;
import com.tripio.region.repository.RegionRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeDiscoveryQueryService implements HomeDiscoveryService {

    private static final int DEFAULT_SECTION_SIZE = 10;

    private final HomeDiscoveryRepository homeDiscoveryRepository;
    private final RegionRepository regionRepository;
    private final TravelEtfStyleTagRepository styleTagRepository;

    @Override
    public HomeDiscoveryResponse getHomeDiscovery() {
        List<TravelEtf> popularEtfs = homeDiscoveryRepository.findTop(
                HomeDiscoverySection.POPULAR,
                DEFAULT_SECTION_SIZE
        );
        List<TravelEtf> risingEtfs = homeDiscoveryRepository.findTop(
                HomeDiscoverySection.RISING,
                DEFAULT_SECTION_SIZE
        );
        List<TravelEtf> undervaluedEtfs = homeDiscoveryRepository.findTop(
                HomeDiscoverySection.UNDERVALUED,
                DEFAULT_SECTION_SIZE
        );

        LinkedHashMap<Long, TravelEtf> uniqueEtfs = collectUniqueEtfs(
                popularEtfs,
                risingEtfs,
                undervaluedEtfs
        );
        if (uniqueEtfs.isEmpty()) {
            return new HomeDiscoveryResponse(List.of(), List.of(), List.of());
        }

        Map<Long, EtfCardResponse> cardsByEtfId = loadCards(uniqueEtfs);
        return new HomeDiscoveryResponse(
                toOrderedCards(popularEtfs, cardsByEtfId),
                toOrderedCards(risingEtfs, cardsByEtfId),
                toOrderedCards(undervaluedEtfs, cardsByEtfId)
        );
    }

    private LinkedHashMap<Long, TravelEtf> collectUniqueEtfs(
            List<TravelEtf> popularEtfs,
            List<TravelEtf> risingEtfs,
            List<TravelEtf> undervaluedEtfs
    ) {
        LinkedHashMap<Long, TravelEtf> uniqueEtfs = new LinkedHashMap<>();
        popularEtfs.forEach(etf -> uniqueEtfs.putIfAbsent(etf.getId(), etf));
        risingEtfs.forEach(etf -> uniqueEtfs.putIfAbsent(etf.getId(), etf));
        undervaluedEtfs.forEach(etf -> uniqueEtfs.putIfAbsent(etf.getId(), etf));
        return uniqueEtfs;
    }

    private Map<Long, EtfCardResponse> loadCards(LinkedHashMap<Long, TravelEtf> uniqueEtfs) {
        List<Long> regionIds = uniqueEtfs.values().stream()
                .map(TravelEtf::getRegionId)
                .distinct()
                .toList();
        Map<Long, Region> regionsById = regionRepository.findAllById(regionIds).stream()
                .collect(Collectors.toMap(Region::getId, Function.identity()));

        List<Long> etfIds = List.copyOf(uniqueEtfs.keySet());
        Map<Long, List<String>> styleTagsByEtfId = styleTagRepository.findTagRowsByTravelEtfIds(etfIds).stream()
                .collect(Collectors.groupingBy(
                        EtfStyleTagRow::etfId,
                        LinkedHashMap::new,
                        Collectors.mapping(EtfStyleTagRow::tagName, Collectors.toList())
                ));

        LinkedHashMap<Long, EtfCardResponse> cardsByEtfId = new LinkedHashMap<>();
        uniqueEtfs.forEach((etfId, etf) -> cardsByEtfId.put(
                etfId,
                toCardResponse(etf, regionsById, styleTagsByEtfId)
        ));
        return cardsByEtfId;
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

    private List<EtfCardResponse> toOrderedCards(
            List<TravelEtf> etfs,
            Map<Long, EtfCardResponse> cardsByEtfId
    ) {
        return etfs.stream()
                .map(etf -> cardsByEtfId.get(etf.getId()))
                .toList();
    }
}
