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
import com.tripio.social.dto.EtfScrapResponse;
import com.tripio.social.entity.EtfScrap;
import com.tripio.social.repository.EtfScrapRepository;
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
class EtfScrapCommandServiceTest {

    private static final long USER_ID = 10L;
    private static final long ETF_ID = 20L;

    @Mock
    private TravelEtfRepository travelEtfRepository;

    @Mock
    private EtfScrapRepository etfScrapRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EtfScrapCommandService etfScrapCommandService;

    @Test
    void addScrapCreatesOnlyOneScrapAndSynchronizesCount() {
        TravelEtf etf = createEtf(0);
        given(userRepository.existsById(USER_ID)).willReturn(true);
        given(travelEtfRepository.findByIdAndStatusForUpdate(ETF_ID, "PUBLIC"))
                .willReturn(Optional.of(etf));
        given(etfScrapRepository.existsByUserIdAndTravelEtfId(USER_ID, ETF_ID)).willReturn(false);
        given(etfScrapRepository.countByTravelEtfId(ETF_ID)).willReturn(1L);

        EtfScrapResponse response = etfScrapCommandService.addScrap(USER_ID, ETF_ID);

        assertThat(response).isEqualTo(new EtfScrapResponse(ETF_ID, true, 1));
        assertThat(etf.getScrapCount()).isEqualTo(1);
        verify(etfScrapRepository).saveAndFlush(org.mockito.ArgumentMatchers.any(EtfScrap.class));
    }

    @Test
    void duplicateAddScrapDoesNotInsertAgainAndKeepsActualCount() {
        TravelEtf etf = createEtf(7);
        given(userRepository.existsById(USER_ID)).willReturn(true);
        given(travelEtfRepository.findByIdAndStatusForUpdate(ETF_ID, "PUBLIC"))
                .willReturn(Optional.of(etf));
        given(etfScrapRepository.existsByUserIdAndTravelEtfId(USER_ID, ETF_ID)).willReturn(true);
        given(etfScrapRepository.countByTravelEtfId(ETF_ID)).willReturn(1L);

        EtfScrapResponse response = etfScrapCommandService.addScrap(USER_ID, ETF_ID);

        assertThat(response).isEqualTo(new EtfScrapResponse(ETF_ID, true, 1));
        verify(etfScrapRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any(EtfScrap.class));
    }

    @Test
    void repeatedRemoveScrapReturnsUnscrappedAndNeverMakesCountNegative() {
        TravelEtf etf = createEtf(7);
        given(userRepository.existsById(USER_ID)).willReturn(true);
        given(travelEtfRepository.findByIdAndStatusForUpdate(ETF_ID, "PUBLIC"))
                .willReturn(Optional.of(etf));
        given(etfScrapRepository.countByTravelEtfId(ETF_ID)).willReturn(0L);

        EtfScrapResponse response = etfScrapCommandService.removeScrap(USER_ID, ETF_ID);

        assertThat(response).isEqualTo(new EtfScrapResponse(ETF_ID, false, 0));
        assertThat(etf.getScrapCount()).isZero();
        verify(etfScrapRepository).deleteByUserIdAndTravelEtfId(USER_ID, ETF_ID);
    }

    @Test
    void nonPublicOrMissingEtfReturnsEtfNotFound() {
        given(userRepository.existsById(USER_ID)).willReturn(true);
        given(travelEtfRepository.findByIdAndStatusForUpdate(ETF_ID, "PUBLIC"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> etfScrapCommandService.addScrap(USER_ID, ETF_ID))
                .isInstanceOfSatisfying(GeneralException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(EtfErrorCode.ETF_NOT_FOUND));

        verifyNoInteractions(etfScrapRepository);
    }

    @Test
    void missingAuthenticatedUserReturnsUnauthorized() {
        given(userRepository.existsById(USER_ID)).willReturn(false);

        assertThatThrownBy(() -> etfScrapCommandService.addScrap(USER_ID, ETF_ID))
                .isInstanceOfSatisfying(GeneralException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(GeneralErrorCode.UNAUTHORIZED));

        verifyNoInteractions(travelEtfRepository, etfScrapRepository);
    }

    private TravelEtf createEtf(int scrapCount) {
        LocalDateTime now = LocalDateTime.of(2026, 7, 2, 12, 0);
        return new TravelEtf(
                ETF_ID,
                USER_ID,
                30L,
                "공주 ETF",
                "스크랩 테스트",
                "PUBLIC",
                100000,
                1,
                80,
                70,
                0,
                null,
                0,
                scrapCount,
                0,
                0,
                BigDecimal.ZERO,
                now,
                now
        );
    }
}
