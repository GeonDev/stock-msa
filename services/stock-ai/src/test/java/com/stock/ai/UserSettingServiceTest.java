package com.stock.ai;

import com.stock.ai.domain.UserAlert;
import com.stock.ai.domain.UserWatchlist;
import com.stock.ai.repository.UserAlertRepository;
import com.stock.ai.repository.UserWatchlistRepository;
import com.stock.ai.service.UserSettingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(UserSettingService.class)
@ActiveProfiles("test")
public class UserSettingServiceTest {

    @Autowired
    private UserSettingService userSettingService;

    @Autowired
    private UserWatchlistRepository watchlistRepository;

    @Autowired
    private UserAlertRepository alertRepository;

    @Test
    @DisplayName("관심 종목 추가 및 조회가 정상적으로 동작한다")
    void watchlistTest() {
        // given
        String chatId = "123456789";
        String ticker = "005930";

        // when
        userSettingService.addToWatchlist(chatId, ticker);

        // then
        List<String> watchlist = userSettingService.getWatchlist(chatId);
        assertThat(watchlist).hasSize(1);
        assertThat(watchlist.get(0)).isEqualTo(ticker);
    }

    @Test
    @DisplayName("관심 종목 삭제가 정상적으로 동작한다")
    void removeFromWatchlistTest() {
        // given
        String chatId = "123456789";
        String ticker = "005930";
        userSettingService.addToWatchlist(chatId, ticker);

        // when
        userSettingService.removeFromWatchlist(chatId, ticker);

        // then
        List<String> watchlist = userSettingService.getWatchlist(chatId);
        assertThat(watchlist).isEmpty();
    }

    @Test
    @DisplayName("지표 알림 추가 및 활성 알림 조회가 정상적으로 동작한다")
    void alertTest() {
        // given
        String chatId = "123456789";
        String ticker = "005930";
        userSettingService.addAlert(chatId, ticker, "PER", "LOWER", new BigDecimal("10.0"));

        // when
        List<UserAlert> activeAlerts = userSettingService.getActiveAlerts();

        // then
        assertThat(activeAlerts).isNotEmpty();
        UserAlert alert = activeAlerts.get(0);
        assertThat(alert.getTicker()).isEqualTo(ticker);
        assertThat(alert.getIndicatorName()).isEqualTo("PER");
        assertThat(alert.getConditionOperator()).isEqualTo("LOWER");
        assertThat(alert.getTargetValue()).isEqualByComparingTo("10.0");
    }
}
