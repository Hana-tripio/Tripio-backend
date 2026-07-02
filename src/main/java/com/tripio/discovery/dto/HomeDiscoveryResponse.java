package com.tripio.discovery.dto;

import com.tripio.etf.dto.EtfCardResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record HomeDiscoveryResponse(
        @Schema(description = "현재 반응 수 기준 인기 ETF 목록")
        List<EtfCardResponse> popularEtfs,
        @Schema(description = "MVP 임시 급상승 ETF 목록. 실제 기간별 반응 증가량이 아닌 최근 생성일과 현재 반응 수를 기준으로 합니다.")
        List<EtfCardResponse> risingEtfs,
        @Schema(description = "지역 가치 점수가 높고 현재 반응 수가 상대적으로 낮은 ETF 목록")
        List<EtfCardResponse> undervaluedEtfs
) {
}
