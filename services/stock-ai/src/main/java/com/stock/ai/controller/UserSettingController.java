package com.stock.ai.controller;

import com.stock.ai.domain.UserAlert;
import com.stock.ai.service.UserSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ai/user")
@RequiredArgsConstructor
public class UserSettingController {

    private final UserSettingService userSettingService;

    @PostMapping("/watchlist/{chatId}/{ticker}")
    public void addToWatchlist(@PathVariable String chatId, @PathVariable String ticker) {
        userSettingService.addToWatchlist(chatId, ticker);
    }

    @DeleteMapping("/watchlist/{chatId}/{ticker}")
    public void removeFromWatchlist(@PathVariable String chatId, @PathVariable String ticker) {
        userSettingService.removeFromWatchlist(chatId, ticker);
    }

    @GetMapping("/watchlist/{chatId}")
    public List<String> getWatchlist(@PathVariable String chatId) {
        return userSettingService.getWatchlist(chatId);
    }

    @PostMapping("/alert")
    public void addAlert(@RequestParam String chatId, @RequestParam String ticker, 
                         @RequestParam String indicator, @RequestParam String operator, 
                         @RequestParam BigDecimal target) {
        userSettingService.addAlert(chatId, ticker, indicator, operator, target);
    }

    @GetMapping("/alerts/active")
    public List<UserAlert> getActiveAlerts() {
        return userSettingService.getActiveAlerts();
    }
}
