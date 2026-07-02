package com.tripio.social.service;

import com.tripio.etf.dto.EtfListResponse;
import com.tripio.social.dto.ScrappedEtfListRequest;

public interface ScrappedEtfService {

    EtfListResponse getScrappedEtfs(Long userId, ScrappedEtfListRequest request);
}
