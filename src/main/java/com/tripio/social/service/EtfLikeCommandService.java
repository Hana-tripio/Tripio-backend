package com.tripio.social.service;

import com.tripio.etf.entity.TravelEtf;
import com.tripio.etf.repository.TravelEtfRepository;
import com.tripio.etf.type.EtfErrorCode;
import com.tripio.global.apiPayload.code.GeneralErrorCode;
import com.tripio.global.apiPayload.exception.GeneralException;
import com.tripio.social.dto.EtfLikeResponse;
import com.tripio.social.entity.EtfLike;
import com.tripio.social.repository.EtfLikeRepository;
import com.tripio.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class EtfLikeCommandService implements EtfLikeService {

    private static final String PUBLIC_STATUS = "PUBLIC";

    private final TravelEtfRepository travelEtfRepository;
    private final EtfLikeRepository etfLikeRepository;
    private final UserRepository userRepository;

    @Override
    public EtfLikeResponse addLike(Long userId, Long etfId) {
        validateUser(userId);
        TravelEtf travelEtf = findPublicEtfForUpdate(etfId);

        if (!etfLikeRepository.existsByUserIdAndTravelEtfId(userId, etfId)) {
            etfLikeRepository.saveAndFlush(new EtfLike(userId, etfId));
        }

        return synchronizeLikeCount(travelEtf, true);
    }

    @Override
    public EtfLikeResponse removeLike(Long userId, Long etfId) {
        validateUser(userId);
        TravelEtf travelEtf = findPublicEtfForUpdate(etfId);

        etfLikeRepository.deleteByUserIdAndTravelEtfId(userId, etfId);
        return synchronizeLikeCount(travelEtf, false);
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

    private EtfLikeResponse synchronizeLikeCount(TravelEtf travelEtf, boolean liked) {
        long likeCount = etfLikeRepository.countByTravelEtfId(travelEtf.getId());
        travelEtf.synchronizeLikeCount(likeCount);
        return new EtfLikeResponse(travelEtf.getId(), liked, travelEtf.getLikeCount());
    }
}
