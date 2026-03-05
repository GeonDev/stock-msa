package com.stock.ai.repository;

import com.stock.ai.domain.UserWatchlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface UserWatchlistRepository extends JpaRepository<UserWatchlist, Long> {
    List<UserWatchlist> findByChatId(String chatId);
    Optional<UserWatchlist> findByChatIdAndTicker(String chatId, String ticker);
    void deleteByChatIdAndTicker(String chatId, String ticker);

    @Query("SELECT DISTINCT w.chatId FROM UserWatchlist w")
    List<String> findDistinctChatIds();
}
