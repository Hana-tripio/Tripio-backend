package com.tripio.global.apiPayload.exception;

import com.tripio.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GeneralException extends RuntimeException {

    private final BaseErrorCode code;

    @Override
    public String getMessage() {
        return code.getMessage();
    }
}
