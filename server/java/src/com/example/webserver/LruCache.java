package com.example.webserver;

import java.util.LinkedHashMap;
import java.util.Map;

public class LruCache<K, V> extends LinkedHashMap<K, V> {
    private final int maxSize;
    private final long ttlMillis; // 0 means no TTL

    public LruCache(int maxSize, long ttlMillis) {
        super(16, 0.75f, true);
        this.maxSize = Math.max(1, maxSize);
        this.ttlMillis = Math.max(0, ttlMillis);
    }

    public long getTtlMillis() { return ttlMillis; }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxSize;
    }

    @Override
    public synchronized V get(Object key) {
        return super.get(key);
    }

    @Override
    public synchronized V put(K key, V value) {
        return super.put(key, value);
    }
}

class CacheEntry {
    private final byte[] body;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private final long expiresAt;

    CacheEntry(byte[] body, long ttlMillis) {
        this.body = body;
        this.expiresAt = ttlMillis > 0 ? System.currentTimeMillis() + ttlMillis : Long.MAX_VALUE;
    }

    public static CacheEntry from(HttpResponse r, long ttlMillis) {
        CacheEntry ce = new CacheEntry(r.body(), ttlMillis);
        ce.headers.putAll(r.headers());
        return ce;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    public byte[] body() { return body; }
    public Map<String, String> headers() { return headers; }
}
