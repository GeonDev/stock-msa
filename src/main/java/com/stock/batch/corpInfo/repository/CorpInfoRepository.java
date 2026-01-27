package com.stock.batch.corpInfo.repository;

import com.stock.batch.corpInfo.entity.CorpInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CorpInfoRepository extends JpaRepository<CorpInfo, String> {
    List<CorpInfo> findAllByStockCodeIsNotNull();
}
