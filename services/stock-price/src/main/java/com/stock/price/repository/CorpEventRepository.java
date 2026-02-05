package com.stock.price.repository;

import com.stock.price.entity.CorpEventHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CorpEventRepository extends JpaRepository<CorpEventHistory, Long> {
    List<CorpEventHistory> findAllByStockCodeOrderByEventDateDesc(String stockCode);
    boolean existsByStockCodeAndEventDateAndEventType(String stockCode, String eventDate, com.stock.common.enums.CorpEventType eventType);
}
