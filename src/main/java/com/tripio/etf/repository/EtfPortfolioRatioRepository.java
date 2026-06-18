package com.tripio.etf.repository;

import com.tripio.etf.entity.EtfPortfolioRatio;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EtfPortfolioRatioRepository extends JpaRepository<EtfPortfolioRatio, Long> {

    Optional<EtfPortfolioRatio> findByTravelEtfId(Long travelEtfId);
}
