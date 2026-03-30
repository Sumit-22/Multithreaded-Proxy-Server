package com.example.webserver;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class CacheEntry {

    private final byte[] body;
    private final Map<String, String> headers;
    private final long expiresAtNanos;

    CacheEntry(byte[] body, Map<String, String> headers, long ttlMillis) {
        this.body = body.clone();
        this.headers = Collections.unmodifiableMap(
                new LinkedHashMap<>(headers)
        );

        if (ttlMillis > 0) {
            this.expiresAtNanos =
                    System.nanoTime() + ttlMillis * 1_000_000L;
        } else {
            this.expiresAtNanos = Long.MAX_VALUE;
        }
    }

    public static CacheEntry from(HttpResponse r, long ttlMillis) {
        return new CacheEntry(
                r.body(),
                r.headers(),
                ttlMillis
        );
    }

    public boolean isExpired() {
        return System.nanoTime() > expiresAtNanos;
    }

    public byte[] body() {
        return body.clone();
    }

    public Map<String, String> headers() {
        return headers;
    }
}