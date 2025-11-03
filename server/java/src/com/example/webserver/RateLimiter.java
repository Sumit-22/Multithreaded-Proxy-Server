package com.example.webserver;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimiter {
    private static final class Bucket {
        double tokens;
        long lastRefillNanos;
        final double ratePerSecond;
        final double burst;

        Bucket(double ratePerSecond, double burst) {
            this.ratePerSecond = ratePerSecond;
            this.burst = burst;
            this.tokens = burst;
            this.lastRefillNanos = System.nanoTime();
        }

        synchronized boolean allow() {
            long now = System.nanoTime();
            double elapsed = (now - lastRefillNanos) / 1_000_000_000.0;
            tokens = Math.min(burst, tokens + elapsed * ratePerSecond);
            lastRefillNanos = now;
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }
    }

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final double ratePerSecond;
    private final double burst;

    public RateLimiter(double ratePerSecond, double burst) {
        this.ratePerSecond = ratePerSecond;
        this.burst = burst;
    }

    public boolean allow(String key) {
        return buckets.computeIfAbsent(key, k -> new Bucket(ratePerSecond, burst)).allow();
    }
}
