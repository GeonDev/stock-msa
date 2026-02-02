package com.stock.stock.batchJob;

import com.stock.stock.stock.batchJob.StockPriceBatch;
import com.stock.stock.stock.entity.StockIndicator;
import com.stock.stock.stock.entity.StockPrice;
import com.stock.stock.stock.repository.StockPriceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.ItemProcessor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockPriceBatchTest {

    @Mock
    private StockPriceRepository stockPriceRepository;

    @InjectMocks
    private StockPriceBatch stockPriceBatch;

    private ItemProcessor<StockPrice, StockPrice> itemProcessor;

    @BeforeEach
    void setUp() {
        itemProcessor = stockPriceBatch.stockItemProcessor();
    }

    @Test
    void testStockItemProcessor() throws Exception {
        // given
        StockPrice item = StockPrice.builder()
                .stockCode("005930")
                .basDt(LocalDate.now())
                .endPrice(70000)
                .build();

        List<StockPrice> historicalPrices = new ArrayList<>();
        for (int i = 0; i < 300; i++) {
            historicalPrices.add(StockPrice.builder().endPrice(60000 + i * 10).build());
        }
        when(stockPriceRepository.findTop200ByStockCodeAndBasDtBeforeOrderByBasDtDesc(anyString(), any(LocalDate.class)))
                .thenReturn(historicalPrices);

        // when
        StockPrice processedItem = itemProcessor.process(item);

        // then
        assertNotNull(processedItem);
        StockIndicator stockIndicator = processedItem.getStockIndicator();
        assertNotNull(stockIndicator);
        assertEquals(processedItem, stockIndicator.getStockPrice());

        assertNotNull(stockIndicator.getMa5());
        assertNotNull(stockIndicator.getMa20());
        assertNotNull(stockIndicator.getMa60());
        assertNotNull(stockIndicator.getMa120());
        assertNotNull(stockIndicator.getMa200());
        assertNotNull(stockIndicator.getMa250());
        assertNotNull(stockIndicator.getMomentum1m());
        assertNotNull(stockIndicator.getMomentum3m());
        assertNotNull(stockIndicator.getMomentum6m());
    }

    @Test
    void testStockItemProcessor_NotEnoughData() throws Exception {
        // given
        StockPrice item = StockPrice.builder()
                .stockCode("005930")
                .basDt(LocalDate.now())
                .endPrice(70000)
                .build();

        List<StockPrice> historicalPrices = new ArrayList<>();
        // No historical data
        when(stockPriceRepository.findTop200ByStockCodeAndBasDtBeforeOrderByBasDtDesc(anyString(), any(LocalDate.class)))
                .thenReturn(historicalPrices);

        // when
        StockPrice processedItem = itemProcessor.process(item);

        // then
        assertNotNull(processedItem);
        assertNull(processedItem.getStockIndicator());
    }
}
