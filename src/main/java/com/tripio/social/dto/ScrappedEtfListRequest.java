package com.tripio.social.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScrappedEtfListRequest {

    @NotNull(message = "page는 필수입니다.")
    @Min(value = 0, message = "page는 0 이상이어야 합니다.")
    private Integer page = 0;

    @NotNull(message = "size는 필수입니다.")
    @Min(value = 1, message = "size는 1 이상이어야 합니다.")
    @Max(value = 100, message = "size는 100 이하여야 합니다.")
    private Integer size = 20;

    @AssertTrue(message = "page와 size로 계산한 offset이 허용 범위를 초과했습니다.")
    public boolean isOffsetRangeValid() {
        return page == null || size == null || (long) page * size <= Integer.MAX_VALUE;
    }
}
