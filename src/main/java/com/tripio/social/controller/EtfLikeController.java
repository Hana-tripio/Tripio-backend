package com.tripio.social.controller;

import com.tripio.global.apiPayload.ApiResponse;
import com.tripio.global.apiPayload.code.GeneralErrorCode;
import com.tripio.global.apiPayload.code.GeneralSuccessCode;
import com.tripio.global.apiPayload.exception.GeneralException;
import com.tripio.social.dto.EtfLikeResponse;
import com.tripio.social.service.EtfLikeService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/etfs/{etfId}/likes")
public class EtfLikeController {

    private final EtfLikeService etfLikeService;

    @PostMapping
    public ResponseEntity<ApiResponse<EtfLikeResponse>> addLike(
            @PathVariable @Positive(message = "etfId는 1 이상이어야 합니다.") Long etfId,
            Authentication authentication
    ) {
        EtfLikeResponse response = etfLikeService.addLike(resolveUserId(authentication), etfId);

        return ResponseEntity
                .status(GeneralSuccessCode.OK.getStatus())
                .body(ApiResponse.onSuccess(GeneralSuccessCode.OK, response));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<EtfLikeResponse>> removeLike(
            @PathVariable @Positive(message = "etfId는 1 이상이어야 합니다.") Long etfId,
            Authentication authentication
    ) {
        EtfLikeResponse response = etfLikeService.removeLike(resolveUserId(authentication), etfId);

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
