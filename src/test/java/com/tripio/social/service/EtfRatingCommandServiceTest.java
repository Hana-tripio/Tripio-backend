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
import com.tripio.social.dto.EtfRatingResponse;
import com.tripio.social.entity.EtfRating;
import com.tripio.social.repository.EtfRatingRepository;
import com.tripio.social.type.RatingErrorCode;
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
class EtfRatingCommandServiceTest {

    private static final long USER_ID = 10L;
    private static final long ETF_ID = 20L;

    @Mock
    private TravelEtfRepository travelEtfRepository;

    @Mock
    private EtfRatingRepository etfRatingRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EtfRatingCommandService etfRatingCommandService;

    @Test
    void createRatingPersistsOneRatingAndSynchronizesAverage() {
        TravelEtf etf = createEtf();
        givenUserAndPublicEtf(etf);
        given(etfRatingRepository.existsByUserIdAndTravelEtfId(USER_ID, ETF_ID)).willReturn(false);
        given(etfRatingRepository.calculateAverageByTravelEtfId(ETF_ID))
                .willReturn(new BigDecimal("4"));

        EtfRatingResponse response = etfRatingCommandService.createRating(USER_ID, ETF_ID, 4);

        assertThat(response).isEqualTo(new EtfRatingResponse(ETF_ID, true, 4, new BigDecimal("4.00")));
        assertThat(etf.getRatingAverage()).isEqualByComparingTo("4.00");
        verify(etfRatingRepository).saveAndFlush(org.mockito.ArgumentMatchers.any(EtfRating.class));
    }

    @Test
    void duplicateCreateReturnsConflictWithoutInsert() {
        TravelEtf etf = createEtf();
        givenUserAndPublicEtf(etf);
        given(etfRatingRepository.existsByUserIdAndTravelEtfId(USER_ID, ETF_ID)).willReturn(true);

        assertThatThrownBy(() -> etfRatingCommandService.createRating(USER_ID, ETF_ID, 4))
                .isInstanceOfSatisfying(GeneralException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(RatingErrorCode.RATING_ALREADY_EXISTS));

        verify(etfRatingRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any(EtfRating.class));
    }

    @Test
    void updateRatingChangesScoreAndSynchronizesRoundedAverage() {
        TravelEtf etf = createEtf();
        EtfRating rating = new EtfRating(USER_ID, ETF_ID, 5);
        givenUserAndPublicEtf(etf);
        given(etfRatingRepository.findByUserIdAndTravelEtfId(USER_ID, ETF_ID))
                .willReturn(Optional.of(rating));
        given(etfRatingRepository.calculateAverageByTravelEtfId(ETF_ID))
                .willReturn(new BigDecimal("3.6666666667"));

        EtfRatingResponse response = etfRatingCommandService.updateRating(USER_ID, ETF_ID, 3);

        assertThat(rating.getScore()).isEqualTo(3);
        assertThat(response.ratingAverage()).isEqualByComparingTo("3.67");
        assertThat(etf.getRatingAverage()).isEqualByComparingTo("3.67");
        verify(etfRatingRepository).flush();
    }

    @Test
    void updateMissingRatingReturnsRatingNotFound() {
        TravelEtf etf = createEtf();
        givenUserAndPublicEtf(etf);
        given(etfRatingRepository.findByUserIdAndTravelEtfId(USER_ID, ETF_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> etfRatingCommandService.updateRating(USER_ID, ETF_ID, 3))
                .isInstanceOfSatisfying(GeneralException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(RatingErrorCode.RATING_NOT_FOUND));
    }

    @Test
    void repeatedDeleteIsIdempotentAndSynchronizesZeroAverage() {
        TravelEtf etf = createEtf();
        givenUserAndPublicEtf(etf);
        given(etfRatingRepository.calculateAverageByTravelEtfId(ETF_ID))
                .willReturn(BigDecimal.ZERO);

        EtfRatingResponse response = etfRatingCommandService.deleteRating(USER_ID, ETF_ID);

        assertThat(response).isEqualTo(new EtfRatingResponse(ETF_ID, false, null, new BigDecimal("0.00")));
        assertThat(etf.getRatingAverage()).isEqualByComparingTo("0.00");
        verify(etfRatingRepository).deleteByUserIdAndTravelEtfId(USER_ID, ETF_ID);
    }

    @Test
    void nonPublicOrMissingEtfReturnsEtfNotFound() {
        given(userRepository.existsById(USER_ID)).willReturn(true);
        given(travelEtfRepository.findByIdAndStatusForUpdate(ETF_ID, "PUBLIC"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> etfRatingCommandService.createRating(USER_ID, ETF_ID, 4))
                .isInstanceOfSatisfying(GeneralException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(EtfErrorCode.ETF_NOT_FOUND));

        verifyNoInteractions(etfRatingRepository);
    }

    @Test
    void missingAuthenticatedUserReturnsUnauthorized() {
        given(userRepository.existsById(USER_ID)).willReturn(false);

        assertThatThrownBy(() -> etfRatingCommandService.createRating(USER_ID, ETF_ID, 4))
                .isInstanceOfSatisfying(GeneralException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(GeneralErrorCode.UNAUTHORIZED));

        verifyNoInteractions(travelEtfRepository, etfRatingRepository);
    }

    private void givenUserAndPublicEtf(TravelEtf etf) {
        given(userRepository.existsById(USER_ID)).willReturn(true);
        given(travelEtfRepository.findByIdAndStatusForUpdate(ETF_ID, "PUBLIC"))
                .willReturn(Optional.of(etf));
    }

    private TravelEtf createEtf() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 2, 12, 0);
        return new TravelEtf(
                ETF_ID,
                USER_ID,
                30L,
                "공주 ETF",
                "평점 테스트",
                "PUBLIC",
                100000,
                1,
                80,
                70,
                0,
                null,
                0,
                0,
                0,
                0,
                new BigDecimal("2.00"),
                now,
                now
        );
    }
}
