package com.tripio.etf.controller;

import com.tripio.etf.dto.EtfDetailResponse;
import com.tripio.etf.service.EtfService;
import com.tripio.global.apiPayload.ApiResponse;
import com.tripio.global.apiPayload.code.GeneralSuccessCode;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/etfs")
public class EtfController {

    private final EtfService etfService;

    @GetMapping("/{etfId}")
    public ResponseEntity<ApiResponse<EtfDetailResponse>> getEtfDetail(
            @PathVariable @Positive(message = "etfId는 1 이상이어야 합니다.") Long etfId
    ) {
        EtfDetailResponse response = etfService.getEtfDetail(etfId);

        return ResponseEntity
                .status(GeneralSuccessCode.OK.getStatus())
                .body(ApiResponse.onSuccess(GeneralSuccessCode.OK, response));
    }
}
