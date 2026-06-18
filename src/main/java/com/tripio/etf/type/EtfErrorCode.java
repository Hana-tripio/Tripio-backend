package com.tripio.etf.type;

import com.tripio.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum EtfErrorCode implements BaseErrorCode {

    ETF_NOT_FOUND(HttpStatus.NOT_FOUND, "ETF404", "ETF를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
