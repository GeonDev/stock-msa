package com.stock.ai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false)
    private String chatId;

    @Column(nullable = false)
    private String ticker;

    @Column(name = "indicator_name", nullable = false)
    private String indicatorName; // e.g., PER, PBR, ROE

    @Column(name = "condition_operator", nullable = false)
    private String conditionOperator; // UPPER (above), LOWER (below)

    @Column(name = "target_value", nullable = false)
    private BigDecimal targetValue;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isActive == null) isActive = true;
    }
}
