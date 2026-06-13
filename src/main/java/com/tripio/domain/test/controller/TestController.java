package com.tripio.domain.test.controller;

import com.tripio.global.apiPayload.ApiResponse;
import com.tripio.global.apiPayload.code.GeneralErrorCode;
import com.tripio.global.apiPayload.code.GeneralSuccessCode;
import com.tripio.global.apiPayload.exception.GeneralException;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/success")
    public ResponseEntity<ApiResponse<String>> success() {
        return ResponseEntity
                .status(GeneralSuccessCode.OK.getStatus())
                .body(ApiResponse.onSuccess(GeneralSuccessCode.OK, "test success"));
    }

    @GetMapping("/failure")
    public ResponseEntity<ApiResponse<Void>> failure() {
        throw new GeneralException(GeneralErrorCode.BAD_REQUEST);
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse("UP", Instant.now()));
    }

    public record HealthResponse(String status, Instant timestamp) {
    }
}
