package com.tripio.social.service;

import com.tripio.etf.entity.TravelEtf;
import com.tripio.etf.repository.TravelEtfRepository;
import com.tripio.etf.type.EtfErrorCode;
import com.tripio.global.apiPayload.code.GeneralErrorCode;
import com.tripio.global.apiPayload.exception.GeneralException;
import com.tripio.social.dto.EtfScrapResponse;
import com.tripio.social.entity.EtfScrap;
import com.tripio.social.repository.EtfScrapRepository;
import com.tripio.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class EtfScrapCommandService implements EtfScrapService {

    private static final String PUBLIC_STATUS = "PUBLIC";

    private final TravelEtfRepository travelEtfRepository;
    private final EtfScrapRepository etfScrapRepository;
    private final UserRepository userRepository;

    @Override
    public EtfScrapResponse addScrap(Long userId, Long etfId) {
        validateUser(userId);
        TravelEtf travelEtf = findPublicEtfForUpdate(etfId);

        if (!etfScrapRepository.existsByUserIdAndTravelEtfId(userId, etfId)) {
            etfScrapRepository.saveAndFlush(new EtfScrap(userId, etfId));
        }

        return synchronizeScrapCount(travelEtf, true);
    }

    @Override
    public EtfScrapResponse removeScrap(Long userId, Long etfId) {
        validateUser(userId);
        TravelEtf travelEtf = findPublicEtfForUpdate(etfId);

        etfScrapRepository.deleteByUserIdAndTravelEtfId(userId, etfId);
        return synchronizeScrapCount(travelEtf, false);
    }

    private void validateUser(Long userId) {
        if (userId == null || !userRepository.existsById(userId)) {
            throw new GeneralException(GeneralErrorCode.UNAUTHORIZED);
        }
    }

    private TravelEtf findPublicEtfForUpdate(Long etfId) {
        return travelEtfRepository.findByIdAndStatusForUpdate(etfId, PUBLIC_STATUS)
                .orElseThrow(() -> new GeneralException(EtfErrorCode.ETF_NOT_FOUND));
    }

    private EtfScrapResponse synchronizeScrapCount(TravelEtf travelEtf, boolean scrapped) {
        long scrapCount = etfScrapRepository.countByTravelEtfId(travelEtf.getId());
        travelEtf.synchronizeScrapCount(scrapCount);
        return new EtfScrapResponse(travelEtf.getId(), scrapped, travelEtf.getScrapCount());
    }
}
