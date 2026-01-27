package com.stock.batch.corpInfo.repository;

import com.stock.batch.corpInfo.entity.CorpDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CorpDetailRepository extends JpaRepository<CorpDetail, String> {

    Optional<CorpDetail> findByCorpCode(String corpCode);
}
