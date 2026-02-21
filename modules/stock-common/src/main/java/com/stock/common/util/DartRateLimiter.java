package com.stock.common.util;

import com.stock.common.consts.ApplicationConstants;
import lombok.extern.slf4j.Slf4j;

/**
 * DART API Rate Limiter
 * - ApplicationConstants.DART_MAX_CALLS_PER_MINUTE 설정을 기반으로 호출 간격을 제어합니다.
 */
@Slf4j
public class DartRateLimiter {

    private long lastCallTime = 0;
    
    // 분당 호출 횟수를 기반으로 최소 간격 계산 (예: 800회 -> 약 75ms)
    private static final long MIN_INTERVAL_MILLIS = 60000L / ApplicationConstants.DART_MAX_CALLS_PER_MINUTE;

    /**
     * API 호출 전 rate limit 체크 및 대기
     * 마지막 호출로부터 최소 간격(MIN_INTERVAL_MILLIS)이 지났는지 확인하고, 부족하면 그만큼 대기합니다.
     */
    public synchronized void acquire() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastCallTime;

        if (elapsed < MIN_INTERVAL_MILLIS) {
            long waitTime = MIN_INTERVAL_MILLIS - elapsed;
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Rate limiter interrupted", e);
            }
        }
        
        // 실행 시점을 기록 (대기했을 경우 대기 후의 시점 기준)
        lastCallTime = System.currentTimeMillis();
    }
}
