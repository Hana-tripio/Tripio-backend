package com.tripio.social.service;

import com.tripio.social.dto.EtfScrapResponse;

public interface EtfScrapService {

    EtfScrapResponse addScrap(Long userId, Long etfId);

    EtfScrapResponse removeScrap(Long userId, Long etfId);
}
