package com.tripio.social.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record EtfRatingRequest(
        @NotNull(message = "score는 필수입니다.")
        @Min(value = 1, message = "score는 1 이상이어야 합니다.")
        @Max(value = 5, message = "score는 5 이하여야 합니다.")
        @JsonDeserialize(using = RatingScoreDeserializer.class)
        Integer score
) {
}
