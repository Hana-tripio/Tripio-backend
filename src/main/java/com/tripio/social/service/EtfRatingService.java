package com.tripio.social.service;

import com.tripio.social.dto.EtfRatingResponse;

public interface EtfRatingService {

    EtfRatingResponse createRating(Long userId, Long etfId, Integer score);

    EtfRatingResponse updateRating(Long userId, Long etfId, Integer score);

    EtfRatingResponse deleteRating(Long userId, Long etfId);
}
