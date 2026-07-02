package com.tripio.social.service;

import com.tripio.etf.dto.EtfCardResponse;
import com.tripio.etf.dto.EtfListResponse;
import com.tripio.etf.entity.TravelEtf;
import com.tripio.etf.repository.EtfStyleTagRow;
import com.tripio.etf.repository.TravelEtfStyleTagRepository;
import com.tripio.etf.type.EtfErrorCode;
import com.tripio.global.apiPayload.code.GeneralErrorCode;
import com.tripio.global.apiPayload.exception.GeneralException;
import com.tripio.region.entity.Region;
import com.tripio.region.repository.RegionRepository;
import com.tripio.social.dto.ScrappedEtfListRequest;
import com.tripio.social.repository.EtfScrapRepository;
import com.tripio.user.repository.UserRepository;
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
public class ScrappedEtfQueryService implements ScrappedEtfService {

    private static final String PUBLIC_STATUS = "PUBLIC";

    private final EtfScrapRepository etfScrapRepository;
    private final UserRepository userRepository;
    private final RegionRepository regionRepository;
    private final TravelEtfStyleTagRepository styleTagRepository;

    @Override
    public EtfListResponse getScrappedEtfs(Long userId, ScrappedEtfListRequest request) {
        validateUser(userId);
        PageRequest pageRequest = PageRequest.of(request.getPage(), request.getSize());
        Page<TravelEtf> etfPage = etfScrapRepository.findScrappedEtfsByUserIdAndStatus(
                userId,
                PUBLIC_STATUS,
                pageRequest
        );

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

    private void validateUser(Long userId) {
        if (userId == null || !userRepository.existsById(userId)) {
            throw new GeneralException(GeneralErrorCode.UNAUTHORIZED);
        }
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
