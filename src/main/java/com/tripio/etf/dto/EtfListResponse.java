package com.tripio.etf.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record EtfListResponse(
        List<EtfCardResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {

    public static EtfListResponse from(Page<?> pageResult, List<EtfCardResponse> content) {
        return new EtfListResponse(
                content,
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.hasNext()
        );
    }
}
