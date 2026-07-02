package com.tripio.discovery.repository;

import com.tripio.discovery.type.HomeDiscoverySection;
import com.tripio.etf.entity.TravelEtf;
import java.util.List;

public interface HomeDiscoveryRepository {

    List<TravelEtf> findTop(HomeDiscoverySection section, int limit);
}
