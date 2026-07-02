package com.tripio.etf.repository;

import com.tripio.etf.entity.TravelEtf;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TravelEtfRepository extends JpaRepository<TravelEtf, Long>, TravelEtfSearchRepository {

    Optional<TravelEtf> findByIdAndStatus(Long id, String status);
}
