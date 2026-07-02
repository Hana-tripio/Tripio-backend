package com.tripio.etf.service;

import com.tripio.etf.dto.EtfListResponse;
import com.tripio.etf.dto.EtfSearchRequest;

public interface EtfSearchService {

    EtfListResponse searchEtfs(EtfSearchRequest request);
}
