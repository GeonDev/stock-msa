package com.stock.common.util;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * DART API Rate Limiter
 * - 분당 1,000회 제한
 * - Sliding window 방식
 */
@Slf4j
public class DartRateLimiter {
    
    private static final int MAX_CALLS_PER_MINUTE = 1000;
    private static final long ONE_MINUTE_MILLIS = 60_000;
    
    private final ConcurrentLinkedQueue<Long> callTimestamps = new ConcurrentLinkedQueue<>();
    
    /**
     * API 호출 전 rate limit 체크 및 대기
     */
    public synchronized void acquire() {
        long now = System.currentTimeMillis();
        
        // 1분 이전 타임스탬프 제거
        while (!callTimestamps.isEmpty() && now - callTimestamps.peek() > ONE_MINUTE_MILLIS) {
            callTimestamps.poll();
        }
        
        // 1,000회 초과 시 대기
        if (callTimestamps.size() >= MAX_CALLS_PER_MINUTE) {
            long oldestCall = callTimestamps.peek();
            long waitTime = ONE_MINUTE_MILLIS - (now - oldestCall) + 100; // 100ms 버퍼
            
            if (waitTime > 0) {
                log.warn("DART API rate limit reached. Waiting {}ms", waitTime);
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Rate limiter interrupted", e);
                }
                
                // 대기 후 다시 정리
                now = System.currentTimeMillis();
                while (!callTimestamps.isEmpty() && now - callTimestamps.peek() > ONE_MINUTE_MILLIS) {
                    callTimestamps.poll();
                }
            }
        }
        
        // 현재 호출 기록
        callTimestamps.offer(now);
    }
    
    /**
     * 현재 분당 호출 횟수 조회
     */
    public int getCurrentCallCount() {
        long now = System.currentTimeMillis();
        while (!callTimestamps.isEmpty() && now - callTimestamps.peek() > ONE_MINUTE_MILLIS) {
            callTimestamps.poll();
        }
        return callTimestamps.size();
    }
}
