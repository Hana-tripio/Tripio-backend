package com.tripio.social.type;

import com.tripio.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum RatingErrorCode implements BaseErrorCode {

    RATING_NOT_FOUND(HttpStatus.NOT_FOUND, "RATING404", "평점을 찾을 수 없습니다."),
    RATING_ALREADY_EXISTS(HttpStatus.CONFLICT, "RATING409", "이미 등록된 평점이 있습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
