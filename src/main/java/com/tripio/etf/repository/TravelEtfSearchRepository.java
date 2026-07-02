package com.tripio.etf.repository;

import com.tripio.etf.entity.TravelEtf;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TravelEtfSearchRepository {

    Page<TravelEtf> search(EtfSearchCriteria criteria, Pageable pageable);
}
