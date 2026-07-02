package com.tripio.etf.service;

import com.tripio.etf.dto.EtfDetailResponse;

public interface EtfService {

    EtfDetailResponse getEtfDetail(Long etfId);
}
