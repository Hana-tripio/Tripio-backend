package com.tripio.social.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.tripio.etf.entity.TravelEtf;
import com.tripio.etf.repository.TravelEtfRepository;
import com.tripio.etf.type.EtfErrorCode;
import com.tripio.global.apiPayload.code.GeneralErrorCode;
import com.tripio.global.apiPayload.exception.GeneralException;
import com.tripio.social.dto.EtfLikeResponse;
import com.tripio.social.entity.EtfLike;
import com.tripio.social.repository.EtfLikeRepository;
import com.tripio.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EtfLikeCommandServiceTest {

    private static final long USER_ID = 10L;
    private static final long ETF_ID = 20L;

    @Mock
    private TravelEtfRepository travelEtfRepository;

    @Mock
    private EtfLikeRepository etfLikeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EtfLikeCommandService etfLikeCommandService;

    @Test
    void addLikeCreatesOnlyOneLikeAndSynchronizesCount() {
        TravelEtf etf = createEtf(0);
        given(userRepository.existsById(USER_ID)).willReturn(true);
        given(travelEtfRepository.findByIdAndStatusForUpdate(ETF_ID, "PUBLIC"))
                .willReturn(Optional.of(etf));
        given(etfLikeRepository.existsByUserIdAndTravelEtfId(USER_ID, ETF_ID)).willReturn(false);
        given(etfLikeRepository.countByTravelEtfId(ETF_ID)).willReturn(1L);

        EtfLikeResponse response = etfLikeCommandService.addLike(USER_ID, ETF_ID);

        assertThat(response).isEqualTo(new EtfLikeResponse(ETF_ID, true, 1));
        assertThat(etf.getLikeCount()).isEqualTo(1);
        verify(etfLikeRepository).saveAndFlush(org.mockito.ArgumentMatchers.any(EtfLike.class));
    }

    @Test
    void duplicateAddLikeDoesNotInsertAgainAndKeepsActualCount() {
        TravelEtf etf = createEtf(1);
        given(userRepository.existsById(USER_ID)).willReturn(true);
        given(travelEtfRepository.findByIdAndStatusForUpdate(ETF_ID, "PUBLIC"))
                .willReturn(Optional.of(etf));
        given(etfLikeRepository.existsByUserIdAndTravelEtfId(USER_ID, ETF_ID)).willReturn(true);
        given(etfLikeRepository.countByTravelEtfId(ETF_ID)).willReturn(1L);

        EtfLikeResponse response = etfLikeCommandService.addLike(USER_ID, ETF_ID);

        assertThat(response).isEqualTo(new EtfLikeResponse(ETF_ID, true, 1));
        verify(etfLikeRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any(EtfLike.class));
    }

    @Test
    void repeatedRemoveLikeReturnsUnlikedAndNeverMakesCountNegative() {
        TravelEtf etf = createEtf(0);
        given(userRepository.existsById(USER_ID)).willReturn(true);
        given(travelEtfRepository.findByIdAndStatusForUpdate(ETF_ID, "PUBLIC"))
                .willReturn(Optional.of(etf));
        given(etfLikeRepository.countByTravelEtfId(ETF_ID)).willReturn(0L);

        EtfLikeResponse response = etfLikeCommandService.removeLike(USER_ID, ETF_ID);

        assertThat(response).isEqualTo(new EtfLikeResponse(ETF_ID, false, 0));
        assertThat(etf.getLikeCount()).isZero();
        verify(etfLikeRepository).deleteByUserIdAndTravelEtfId(USER_ID, ETF_ID);
    }

    @Test
    void nonPublicOrMissingEtfReturnsEtfNotFound() {
        given(userRepository.existsById(USER_ID)).willReturn(true);
        given(travelEtfRepository.findByIdAndStatusForUpdate(ETF_ID, "PUBLIC"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> etfLikeCommandService.addLike(USER_ID, ETF_ID))
                .isInstanceOfSatisfying(GeneralException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(EtfErrorCode.ETF_NOT_FOUND));

        verifyNoInteractions(etfLikeRepository);
    }

    @Test
    void missingAuthenticatedUserReturnsUnauthorized() {
        given(userRepository.existsById(USER_ID)).willReturn(false);

        assertThatThrownBy(() -> etfLikeCommandService.addLike(USER_ID, ETF_ID))
                .isInstanceOfSatisfying(GeneralException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(GeneralErrorCode.UNAUTHORIZED));

        verifyNoInteractions(travelEtfRepository, etfLikeRepository);
    }

    private TravelEtf createEtf(int likeCount) {
        LocalDateTime now = LocalDateTime.of(2026, 7, 2, 12, 0);
        return new TravelEtf(
                ETF_ID,
                USER_ID,
                30L,
                "공주 ETF",
                "좋아요 테스트",
                "PUBLIC",
                100000,
                1,
                80,
                70,
                0,
                null,
                likeCount,
                0,
                0,
                0,
                BigDecimal.ZERO,
                now,
                now
        );
    }
}
