package com.stock.batch.repository;

import com.stock.batch.entity.CorpInfo;
import com.stock.batch.entity.StockPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CorpInfoRepository extends JpaRepository<CorpInfo, String> {
    List<CorpInfo> findAllByStockCodeIsNotNull();
}
