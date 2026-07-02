package com.tripio.etf.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EtfSearchRequest {

    @Size(max = 100, message = "keyword는 100자 이하여야 합니다.")
    private String keyword;

    @Positive(message = "regionId는 1 이상이어야 합니다.")
    private Long regionId;

    @Size(max = 10, message = "styleTagIds는 최대 10개까지 조회할 수 있습니다.")
    private List<@Positive(message = "styleTagId는 1 이상이어야 합니다.") Long> styleTagIds = new ArrayList<>();

    @PositiveOrZero(message = "minBudget은 0 이상이어야 합니다.")
    private Integer minBudget;

    @PositiveOrZero(message = "maxBudget은 0 이상이어야 합니다.")
    private Integer maxBudget;

    @Positive(message = "minDurationDays는 1 이상이어야 합니다.")
    private Integer minDurationDays;

    @Positive(message = "maxDurationDays는 1 이상이어야 합니다.")
    private Integer maxDurationDays;

    private String sort = "popular";

    @NotNull(message = "page는 필수입니다.")
    @Min(value = 0, message = "page는 0 이상이어야 합니다.")
    private Integer page = 0;

    @NotNull(message = "size는 필수입니다.")
    @Min(value = 1, message = "size는 1 이상이어야 합니다.")
    @Max(value = 100, message = "size는 100 이하여야 합니다.")
    private Integer size = 20;

    @AssertTrue(message = "minBudget은 maxBudget보다 클 수 없습니다.")
    public boolean isBudgetRangeValid() {
        return minBudget == null || maxBudget == null || minBudget <= maxBudget;
    }

    @AssertTrue(message = "minDurationDays는 maxDurationDays보다 클 수 없습니다.")
    public boolean isDurationRangeValid() {
        return minDurationDays == null || maxDurationDays == null || minDurationDays <= maxDurationDays;
    }

    @AssertTrue(message = "page와 size로 계산한 offset이 허용 범위를 초과했습니다.")
    public boolean isOffsetRangeValid() {
        return page == null || size == null || (long) page * size <= Integer.MAX_VALUE;
    }
}
