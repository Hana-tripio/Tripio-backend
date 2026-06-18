package com.tripio.etf.service;

import com.tripio.etf.dto.EtfDetailResponse;
import com.tripio.etf.entity.EtfItineraryDay;
import com.tripio.etf.entity.EtfPortfolioRatio;
import com.tripio.etf.entity.TravelEtf;
import com.tripio.etf.repository.EtfItineraryDayRepository;
import com.tripio.etf.repository.EtfItineraryItemRepository;
import com.tripio.etf.repository.EtfItineraryItemRow;
import com.tripio.etf.repository.EtfPortfolioRatioRepository;
import com.tripio.etf.repository.TravelEtfRepository;
import com.tripio.etf.repository.TravelEtfStyleTagRepository;
import com.tripio.etf.type.EtfErrorCode;
import com.tripio.global.apiPayload.exception.GeneralException;
import com.tripio.region.entity.Region;
import com.tripio.region.repository.RegionRepository;
import com.tripio.user.entity.User;
import com.tripio.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EtfDetailQueryService implements EtfService {

    private final TravelEtfRepository travelEtfRepository;
    private final UserRepository userRepository;
    private final RegionRepository regionRepository;
    private final TravelEtfStyleTagRepository styleTagRepository;
    private final EtfItineraryDayRepository itineraryDayRepository;
    private final EtfItineraryItemRepository itineraryItemRepository;
    private final EtfPortfolioRatioRepository portfolioRatioRepository;

    @Override
    public EtfDetailResponse getEtfDetail(Long etfId) {
        TravelEtf travelEtf = travelEtfRepository.findById(etfId)
                .orElseThrow(() -> new GeneralException(EtfErrorCode.ETF_NOT_FOUND));
        User owner = userRepository.findById(travelEtf.getOwnerId())
                .orElseThrow(() -> new GeneralException(EtfErrorCode.ETF_NOT_FOUND));
        Region region = regionRepository.findById(travelEtf.getRegionId())
                .orElseThrow(() -> new GeneralException(EtfErrorCode.ETF_NOT_FOUND));

        List<String> styleTags = styleTagRepository.findTagNamesByTravelEtfId(etfId);
        List<EtfDetailResponse.ItineraryDayResponse> itineraryDays = itineraryDayRepository
                .findByTravelEtfIdOrderByDayNumber(etfId)
                .stream()
                .map(this::toItineraryDayResponse)
                .toList();
        EtfDetailResponse.PortfolioRatioResponse portfolioRatios = portfolioRatioRepository
                .findByTravelEtfId(etfId)
                .map(this::toPortfolioRatioResponse)
                .orElse(null);

        return new EtfDetailResponse(
                travelEtf.getId(),
                travelEtf.getTitle(),
                travelEtf.getSummary(),
                travelEtf.getStatus(),
                new EtfDetailResponse.OwnerResponse(owner.getId(), owner.getNickname()),
                new EtfDetailResponse.RegionResponse(region.getId(), region.getName(), region.getRegionType()),
                travelEtf.getTotalBudget(),
                travelEtf.getDurationDays(),
                new EtfDetailResponse.ScoreResponse(
                        travelEtf.getLocalContributionScore(),
                        travelEtf.getRegionValueScore(),
                        travelEtf.getExpectedReward()
                ),
                travelEtf.getThumbnailUrl(),
                new EtfDetailResponse.ReactionCountResponse(
                        travelEtf.getLikeCount(),
                        travelEtf.getScrapCount(),
                        travelEtf.getFollowCount(),
                        travelEtf.getVerificationCount(),
                        travelEtf.getRatingAverage()
                ),
                styleTags,
                itineraryDays,
                portfolioRatios,
                travelEtf.getCreatedAt(),
                travelEtf.getUpdatedAt()
        );
    }

    private EtfDetailResponse.ItineraryDayResponse toItineraryDayResponse(EtfItineraryDay day) {
        List<EtfDetailResponse.ItineraryItemResponse> items = itineraryItemRepository
                .findRowsByItineraryDayId(day.getId())
                .stream()
                .map(this::toItineraryItemResponse)
                .toList();

        return new EtfDetailResponse.ItineraryDayResponse(day.getId(), day.getDayNumber(), items);
    }

    private EtfDetailResponse.ItineraryItemResponse toItineraryItemResponse(EtfItineraryItemRow item) {
        return new EtfDetailResponse.ItineraryItemResponse(
                item.id(),
                item.placeId(),
                item.placeName(),
                item.placeCategory(),
                item.sequence(),
                item.startTime(),
                item.endTime(),
                item.estimatedCost(),
                item.core(),
                item.memo()
        );
    }

    private EtfDetailResponse.PortfolioRatioResponse toPortfolioRatioResponse(EtfPortfolioRatio ratio) {
        return new EtfDetailResponse.PortfolioRatioResponse(
                ratio.getLodgingRatio(),
                ratio.getFoodRatio(),
                ratio.getCafeRatio(),
                ratio.getActivityRatio(),
                ratio.getFestivalRatio(),
                ratio.getLocalStoreRatio(),
                ratio.getTransportRatio()
        );
    }
}
