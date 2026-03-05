package com.stock.ai.service;

import com.stock.ai.domain.UserAlert;
import com.stock.ai.domain.UserWatchlist;
import com.stock.ai.repository.UserAlertRepository;
import com.stock.ai.repository.UserWatchlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserSettingService {

    private final UserWatchlistRepository watchlistRepository;
    private final UserAlertRepository alertRepository;

    @Transactional
    public void addToWatchlist(String chatId, String ticker) {
        if (watchlistRepository.findByChatIdAndTicker(chatId, ticker).isEmpty()) {
            watchlistRepository.save(UserWatchlist.builder()
                    .chatId(chatId)
                    .ticker(ticker)
                    .build());
        }
    }

    @Transactional
    public void removeFromWatchlist(String chatId, String ticker) {
        watchlistRepository.deleteByChatIdAndTicker(chatId, ticker);
    }

    public List<String> getWatchlist(String chatId) {
        return watchlistRepository.findByChatId(chatId).stream()
                .map(UserWatchlist::getTicker)
                .collect(Collectors.toList());
    }

    @Transactional
    public void addAlert(String chatId, String ticker, String indicator, String operator, BigDecimal target) {
        alertRepository.save(UserAlert.builder()
                .chatId(chatId)
                .ticker(ticker)
                .indicatorName(indicator.toUpperCase())
                .conditionOperator(operator.toUpperCase())
                .targetValue(target)
                .isActive(true)
                .build());
    }

    public List<UserAlert> getActiveAlerts() {
        return alertRepository.findByIsActiveTrue();
    }
}
