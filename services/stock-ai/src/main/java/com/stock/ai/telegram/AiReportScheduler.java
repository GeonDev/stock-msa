package com.stock.ai.telegram;

import com.stock.ai.entity.UserAlert;
import com.stock.ai.repository.UserWatchlistRepository;
import com.stock.ai.service.AiInsightService;
import com.stock.ai.service.UserSettingService;
import com.stock.common.dto.CorpFinanceIndicatorDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class AiReportScheduler {

    private final TelegramBotService telegramBotService;
    private final AiInsightService aiInsightService;
    private final UserSettingService userSettingService;
    private final UserWatchlistRepository watchlistRepository;
    private final com.stock.ai.client.FinanceClient financeClient;

    @Scheduled(cron = "0 30 8 * * MON-FRI") // 08:30 AM KST
    public void morningBriefing() {
        log.info("Sending morning briefing to all users...");
        List<String> chatIds = watchlistRepository.findDistinctChatIds();

        for (String chatId : chatIds) {
            List<String> tickers = userSettingService.getWatchlist(chatId);
            if (tickers.isEmpty())
                continue;

            telegramBotService.sendMessage(chatId, "☀️ <b>Morning Briefing</b>\n당신의 관심 종목 리포트입니다.").subscribe();
            for (String ticker : tickers) {
                aiInsightService.getFinancialVisualReport(ticker)
                        .subscribe(report -> telegramBotService.sendPhoto(chatId, report.chartImage(),
                                "<b>[" + ticker + "]</b> Morning Analysis\n" + report.text()).subscribe());
            }
        }
    }

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void checkAlerts() {
        log.info("Checking user alerts...");
        List<UserAlert> alerts = userSettingService.getActiveAlerts();

        for (UserAlert alert : alerts) {
            financeClient.getLatestIndicator(alert.getTicker())
                    .subscribe(indicator -> {
                        if (checkCondition(alert, indicator)) {
                            String message = String.format("🔔 <b>Alert!</b>\n%s %s is %s than %s",
                                    alert.getTicker(), alert.getIndicatorName(), alert.getConditionOperator(),
                                    alert.getTargetValue());
                            telegramBotService.sendMessage(alert.getChatId(), message).subscribe();
                            // Optional: Deactivate alert after firing
                        }
                    });
        }
    }

    private boolean checkCondition(UserAlert alert, CorpFinanceIndicatorDto indicator) {
        BigDecimal currentValue = switch (alert.getIndicatorName()) {
            case "PER" -> indicator.getPer();
            case "PBR" -> indicator.getPbr();
            case "ROE" -> indicator.getRoe();
            default -> null;
        };

        if (currentValue == null)
            return false;

        if ("UPPER".equalsIgnoreCase(alert.getConditionOperator())) {
            return currentValue.compareTo(alert.getTargetValue()) >= 0;
        } else {
            return currentValue.compareTo(alert.getTargetValue()) <= 0;
        }
    }

    @Scheduled(cron = "0 0 16 * * MON-FRI") // 04:00 PM KST
    public void marketCloseReport() {
        log.info("Sending market close report to all users...");
        List<String> chatIds = watchlistRepository.findDistinctChatIds();

        for (String chatId : chatIds) {
            List<String> tickers = userSettingService.getWatchlist(chatId);
            if (tickers.isEmpty())
                continue;

            telegramBotService.sendMessage(chatId, "🔔 <b>Market Close Report</b>\n장 마감 요약 및 특이점 보고서입니다.").subscribe();
            for (String ticker : tickers) {
                aiInsightService.getFinancialVisualReport(ticker)
                        .subscribe(report -> telegramBotService.sendPhoto(chatId, report.chartImage(),
                                "<b>[" + ticker + "]</b> Closing Insight\n" + report.text()).subscribe());
            }
        }
    }
}
