package com.tripio.social.service;

import com.tripio.etf.entity.TravelEtf;
import com.tripio.etf.repository.TravelEtfRepository;
import com.tripio.etf.type.EtfErrorCode;
import com.tripio.global.apiPayload.code.GeneralErrorCode;
import com.tripio.global.apiPayload.exception.GeneralException;
import com.tripio.social.dto.EtfRatingResponse;
import com.tripio.social.entity.EtfRating;
import com.tripio.social.repository.EtfRatingRepository;
import com.tripio.social.type.RatingErrorCode;
import com.tripio.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class EtfRatingCommandService implements EtfRatingService {

    private static final String PUBLIC_STATUS = "PUBLIC";
    private static final int RATING_SCALE = 2;

    private final TravelEtfRepository travelEtfRepository;
    private final EtfRatingRepository etfRatingRepository;
    private final UserRepository userRepository;

    @Override
    public EtfRatingResponse createRating(Long userId, Long etfId, Integer score) {
        validateUser(userId);
        TravelEtf travelEtf = findPublicEtfForUpdate(etfId);
        if (etfRatingRepository.existsByUserIdAndTravelEtfId(userId, etfId)) {
            throw new GeneralException(RatingErrorCode.RATING_ALREADY_EXISTS);
        }

        etfRatingRepository.saveAndFlush(new EtfRating(userId, etfId, score));
        BigDecimal ratingAverage = synchronizeRatingAverage(travelEtf);
        return new EtfRatingResponse(etfId, true, score, ratingAverage);
    }

    @Override
    public EtfRatingResponse updateRating(Long userId, Long etfId, Integer score) {
        validateUser(userId);
        TravelEtf travelEtf = findPublicEtfForUpdate(etfId);
        EtfRating rating = etfRatingRepository.findByUserIdAndTravelEtfId(userId, etfId)
                .orElseThrow(() -> new GeneralException(RatingErrorCode.RATING_NOT_FOUND));

        rating.updateScore(score);
        etfRatingRepository.flush();
        BigDecimal ratingAverage = synchronizeRatingAverage(travelEtf);
        return new EtfRatingResponse(etfId, true, score, ratingAverage);
    }

    @Override
    public EtfRatingResponse deleteRating(Long userId, Long etfId) {
        validateUser(userId);
        TravelEtf travelEtf = findPublicEtfForUpdate(etfId);
        etfRatingRepository.deleteByUserIdAndTravelEtfId(userId, etfId);

        BigDecimal ratingAverage = synchronizeRatingAverage(travelEtf);
        return new EtfRatingResponse(etfId, false, null, ratingAverage);
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

    private BigDecimal synchronizeRatingAverage(TravelEtf travelEtf) {
        BigDecimal ratingAverage = etfRatingRepository.calculateAverageByTravelEtfId(travelEtf.getId())
                .setScale(RATING_SCALE, RoundingMode.HALF_UP);
        travelEtf.synchronizeRatingAverage(ratingAverage);
        return ratingAverage;
    }
}
