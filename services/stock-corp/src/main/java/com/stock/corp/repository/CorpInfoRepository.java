package com.stock.corp.repository;

import com.stock.corp.entity.CorpInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CorpInfoRepository extends JpaRepository<CorpInfo, String> {
    List<CorpInfo> findAllByStockCodeIsNotNull();
    List<CorpInfo> findAllByMarket(String market);
}
