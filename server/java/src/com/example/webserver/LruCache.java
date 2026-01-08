package com.example.webserver;

import java.util.LinkedHashMap;
import java.util.Map;

public class LruCache<K, V extends CacheEntry> {

    private final int maxSize;
    private final long ttlNanos;

    private final Map<K, V> map;

    public LruCache(int maxSize, long ttlMillis) {
        this.maxSize = Math.max(1, maxSize);
        this.ttlNanos = ttlMillis > 0
                ? ttlMillis * 1_000_000L
                : 0;

        this.map = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > LruCache.this.maxSize;
            }
        };
    }

    public synchronized V get(K key) {
        V entry = map.get(key);
        if (entry == null) return null;

        if (entry.isExpired()) {
            map.remove(key);
            return null;
        }
        return entry;
    }

    public synchronized void put(K key, V value) {
        map.put(key, value);
    }

    public synchronized int size() {
        return map.size();
    }

    public long getTtlMillis() {
        return ttlNanos / 1_000_000L;
    }
}
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
