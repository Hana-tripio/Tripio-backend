package com.tripio.social.controller;

import com.tripio.global.apiPayload.ApiResponse;
import com.tripio.global.apiPayload.code.GeneralErrorCode;
import com.tripio.global.apiPayload.code.GeneralSuccessCode;
import com.tripio.global.apiPayload.exception.GeneralException;
import com.tripio.social.dto.EtfRatingRequest;
import com.tripio.social.dto.EtfRatingResponse;
import com.tripio.social.service.EtfRatingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/etfs/{etfId}/ratings")
public class EtfRatingController {

    private final EtfRatingService etfRatingService;

    @PostMapping
    public ResponseEntity<ApiResponse<EtfRatingResponse>> createRating(
            @PathVariable @Positive(message = "etfId는 1 이상이어야 합니다.") Long etfId,
            @Valid @RequestBody EtfRatingRequest request,
            Authentication authentication
    ) {
        EtfRatingResponse response = etfRatingService.createRating(
                resolveUserId(authentication),
                etfId,
                request.score()
        );

        return ResponseEntity
                .status(GeneralSuccessCode.OK.getStatus())
                .body(ApiResponse.onSuccess(GeneralSuccessCode.OK, response));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<EtfRatingResponse>> updateRating(
            @PathVariable @Positive(message = "etfId는 1 이상이어야 합니다.") Long etfId,
            @Valid @RequestBody EtfRatingRequest request,
            Authentication authentication
    ) {
        EtfRatingResponse response = etfRatingService.updateRating(
                resolveUserId(authentication),
                etfId,
                request.score()
        );

        return ResponseEntity
                .status(GeneralSuccessCode.OK.getStatus())
                .body(ApiResponse.onSuccess(GeneralSuccessCode.OK, response));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<EtfRatingResponse>> deleteRating(
            @PathVariable @Positive(message = "etfId는 1 이상이어야 합니다.") Long etfId,
            Authentication authentication
    ) {
        EtfRatingResponse response = etfRatingService.deleteRating(resolveUserId(authentication), etfId);

        return ResponseEntity
                .status(GeneralSuccessCode.OK.getStatus())
                .body(ApiResponse.onSuccess(GeneralSuccessCode.OK, response));
    }

    private Long resolveUserId(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new GeneralException(GeneralErrorCode.UNAUTHORIZED);
        }

        try {
            long userId = Long.parseLong(authentication.getName());
            if (userId <= 0) {
                throw new GeneralException(GeneralErrorCode.UNAUTHORIZED);
            }
            return userId;
        } catch (NumberFormatException exception) {
            throw new GeneralException(GeneralErrorCode.UNAUTHORIZED);
        }
    }
}
