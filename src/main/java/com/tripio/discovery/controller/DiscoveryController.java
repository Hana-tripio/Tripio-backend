package com.tripio.discovery.controller;

import com.tripio.discovery.dto.HomeDiscoveryResponse;
import com.tripio.discovery.service.HomeDiscoveryService;
import com.tripio.global.apiPayload.ApiResponse;
import com.tripio.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/discovery")
public class DiscoveryController {

    private final HomeDiscoveryService homeDiscoveryService;

    @Operation(
            summary = "홈 화면 ETF 큐레이션 조회",
            description = "인기, 급상승, 저평가 ETF를 반환합니다. "
                    + "급상승 ETF는 실제 기간별 반응 증가량이 아닌 최근 생성일과 현재 반응 수를 사용하는 MVP 임시 기준입니다."
    )
    @GetMapping("/home")
    public ResponseEntity<ApiResponse<HomeDiscoveryResponse>> getHomeDiscovery() {
        HomeDiscoveryResponse response = homeDiscoveryService.getHomeDiscovery();

        return ResponseEntity
                .status(GeneralSuccessCode.OK.getStatus())
                .body(ApiResponse.onSuccess(GeneralSuccessCode.OK, response));
    }
}
