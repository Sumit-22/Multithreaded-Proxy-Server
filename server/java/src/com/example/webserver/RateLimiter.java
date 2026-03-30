package com.example.webserver;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class RateLimiter {

    /* ===== Constants ===== */

    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    // Use fixed-point math: 1 token = 1000000 units
    private static final long TOKEN_SCALE = 1_000_000L;

    // Remove inactive buckets after 5 minutes
    private static final long BUCKET_TTL_NANOS = 5 * 60 * NANOS_PER_SECOND;

    /* ===== Bucket ===== */

    private static final class Bucket {

        private final long maxTokens;
        private final long refillPerNano;

        private final AtomicLong tokens;

        private volatile long lastRefillNanos;
        private volatile long lastAccessNanos;

        Bucket(long refillPerNano, long maxTokens) {
            this.refillPerNano = refillPerNano;
            this.maxTokens = maxTokens;
            this.tokens = new AtomicLong(maxTokens);

            long now = System.nanoTime();
            this.lastRefillNanos = now;
            this.lastAccessNanos = now;
        }

        boolean allow() {
            long now = System.nanoTime();
            lastAccessNanos = now;

            refillTokens(now);

            return tryConsumeOneToken();
        }

        private void refillTokens(long now) {
            long elapsedNanos = now - lastRefillNanos;
            if (elapsedNanos <= 0) return;

            long tokensToAdd = elapsedNanos * refillPerNano;
            if (tokensToAdd <= 0) return;

            long current;
            long updated;

            do {
                current = tokens.get();
                updated = Math.min(maxTokens, current + tokensToAdd);
            } while (!tokens.compareAndSet(current, updated));

            lastRefillNanos = now;
        }

        private boolean tryConsumeOneToken() {
            long oneToken = TOKEN_SCALE;

            while (true) {
                long current = tokens.get();
                if (current < oneToken) {
                    return false;
                }
                if (tokens.compareAndSet(current, current - oneToken)) {
                    return true;
                }
            }
        }
    }

    /* ===== RateLimiter ===== */

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private final long refillPerNano;
    private final long maxTokens;

    public RateLimiter(double ratePerSecond, double burst) {
        this.refillPerNano =
                (long) ((ratePerSecond * TOKEN_SCALE) / NANOS_PER_SECOND);

        this.maxTokens = (long) (burst * TOKEN_SCALE);
    }

    public boolean allow(String key) {
        long now = System.nanoTime();

        Bucket bucket = buckets.computeIfAbsent(
                key,
                k -> new Bucket(refillPerNano, maxTokens)
        );

        boolean allowed = bucket.allow();

        cleanupExpiredBuckets(now);

        return allowed;
    }

    private void cleanupExpiredBuckets(long now) {
        buckets.entrySet().removeIf(entry ->
                now - entry.getValue().lastAccessNanos > BUCKET_TTL_NANOS
        );
    }
}
