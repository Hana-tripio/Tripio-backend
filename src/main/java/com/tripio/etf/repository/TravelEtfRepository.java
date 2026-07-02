package com.tripio.etf.repository;

import com.tripio.etf.entity.TravelEtf;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TravelEtfRepository extends JpaRepository<TravelEtf, Long>, TravelEtfSearchRepository {

    Optional<TravelEtf> findByIdAndStatus(Long id, String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select etf
            from TravelEtf etf
            where etf.id = :id
              and etf.status = :status
            """)
    Optional<TravelEtf> findByIdAndStatusForUpdate(
            @Param("id") Long id,
            @Param("status") String status
    );
}
