package com.stock.ai.repository;

import com.stock.ai.entity.UserAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserAlertRepository extends JpaRepository<UserAlert, Long> {
    List<UserAlert> findByChatId(String chatId);
    List<UserAlert> findByIsActiveTrue();
}
