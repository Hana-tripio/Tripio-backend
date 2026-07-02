package com.tripio.social.service;

import com.tripio.social.dto.EtfLikeResponse;

public interface EtfLikeService {

    EtfLikeResponse addLike(Long userId, Long etfId);

    EtfLikeResponse removeLike(Long userId, Long etfId);
}
