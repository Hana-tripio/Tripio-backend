package com.tripio.etf.type;

import com.tripio.global.apiPayload.code.GeneralErrorCode;
import com.tripio.global.apiPayload.exception.GeneralException;
import java.util.Arrays;

public enum EtfSearchSort {

    POPULAR("popular"),
    LATEST("latest");

    private final String value;

    EtfSearchSort(String value) {
        this.value = value;
    }

    public static EtfSearchSort from(String value) {
        return Arrays.stream(values())
                .filter(sort -> sort.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.BAD_REQUEST));
    }
}
