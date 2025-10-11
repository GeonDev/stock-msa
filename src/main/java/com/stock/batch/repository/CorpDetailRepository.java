package com.stock.batch.repository;

import com.stock.batch.entity.CorpDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CorpDetailRepository extends JpaRepository<CorpDetail, String> {

    Optional<CorpDetail> findByCorpCode(String corpCode);
}
