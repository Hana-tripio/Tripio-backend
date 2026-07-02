package com.tripio.etf.repository;

import com.tripio.etf.type.EtfSearchSort;
import java.util.List;

public record EtfSearchCriteria(
        String keyword,
        Long regionId,
        List<Long> styleTagIds,
        Integer minBudget,
        Integer maxBudget,
        Integer minDurationDays,
        Integer maxDurationDays,
        EtfSearchSort sort
) {
}
