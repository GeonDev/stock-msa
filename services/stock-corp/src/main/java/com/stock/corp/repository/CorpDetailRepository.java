package com.stock.corp.repository;

import com.stock.corp.entity.CorpDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CorpDetailRepository extends JpaRepository<CorpDetail, String> {

    Optional<CorpDetail> findByCorpCode(String corpCode);
}
