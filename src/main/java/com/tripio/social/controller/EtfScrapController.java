package com.tripio.social.controller;

import com.tripio.etf.dto.EtfListResponse;
import com.tripio.global.apiPayload.ApiResponse;
import com.tripio.global.apiPayload.code.GeneralErrorCode;
import com.tripio.global.apiPayload.code.GeneralSuccessCode;
import com.tripio.global.apiPayload.exception.GeneralException;
import com.tripio.social.dto.EtfScrapResponse;
import com.tripio.social.dto.ScrappedEtfListRequest;
import com.tripio.social.service.EtfScrapService;
import com.tripio.social.service.ScrappedEtfService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
public class EtfScrapController {

    private final EtfScrapService etfScrapService;
    private final ScrappedEtfService scrappedEtfService;

    @PostMapping("/api/etfs/{etfId}/scraps")
    public ResponseEntity<ApiResponse<EtfScrapResponse>> addScrap(
            @PathVariable @Positive(message = "etfId는 1 이상이어야 합니다.") Long etfId,
            Authentication authentication
    ) {
        EtfScrapResponse response = etfScrapService.addScrap(resolveUserId(authentication), etfId);

        return ResponseEntity
                .status(GeneralSuccessCode.OK.getStatus())
                .body(ApiResponse.onSuccess(GeneralSuccessCode.OK, response));
    }

    @DeleteMapping("/api/etfs/{etfId}/scraps")
    public ResponseEntity<ApiResponse<EtfScrapResponse>> removeScrap(
            @PathVariable @Positive(message = "etfId는 1 이상이어야 합니다.") Long etfId,
            Authentication authentication
    ) {
        EtfScrapResponse response = etfScrapService.removeScrap(resolveUserId(authentication), etfId);

        return ResponseEntity
                .status(GeneralSuccessCode.OK.getStatus())
                .body(ApiResponse.onSuccess(GeneralSuccessCode.OK, response));
    }

    @GetMapping("/api/users/me/scrapped-etfs")
    public ResponseEntity<ApiResponse<EtfListResponse>> getScrappedEtfs(
            @Valid @ModelAttribute ScrappedEtfListRequest request,
            Authentication authentication
    ) {
        EtfListResponse response = scrappedEtfService.getScrappedEtfs(resolveUserId(authentication), request);

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
