package com.example.proxy;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe LRU cache for HTTP responses.
 * Achieves ~60% faster response times on repeated requests.
 
public class ProxyCache {
    private static class CacheEntry {
        byte[] data;
        long timestamp;
        
        CacheEntry(byte[] data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    private final int maxSize;
    private final long ttlMillis;
    private final Map<String, CacheEntry> cache;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    
    public ProxyCache(int maxSize) {
        this(maxSize, 300000); // 5 minute default TTL
    }
    
    public ProxyCache(int maxSize, long ttlMillis) {
        this.maxSize = maxSize;
        this.ttlMillis = ttlMillis;
        this.cache = new LinkedHashMap<String, CacheEntry>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                return size() > maxSize;
            }
        };
    }
    
    public void put(String key, byte[] value) {
        lock.writeLock().lock();
        try {
            cache.put(key, new CacheEntry(value));
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public byte[] get(String key) {
        lock.readLock().lock();
        try {
            CacheEntry entry = cache.get(key);
            if (entry == null) {
                return null;
            }
            
            // Check if expired
            if (System.currentTimeMillis() - entry.timestamp > ttlMillis) {
                lock.readLock().unlock();
                lock.writeLock().lock();
                try {
                    cache.remove(key);
                    return null;
                } finally {
                    lock.writeLock().unlock();
                    lock.readLock().lock();
                }
            }
            
            return entry.data;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public void clear() {
        lock.writeLock().lock();
        try {
            cache.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public int size() {
        lock.readLock().lock();
        try {
            return cache.size();
        } finally {
            lock.readLock().unlock();
        }
    }
}
*/
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class ProxyCache {

    private static final class CacheEntry {
        final byte[] data;
        final long expiresAtNanos;

        CacheEntry(byte[] data, long ttlMillis) {
            this.data = data.clone();
            this.expiresAtNanos = ttlMillis > 0
                    ? System.nanoTime() + ttlMillis * 1_000_000L
                    : Long.MAX_VALUE;
        }

        boolean isExpired() {
            return System.nanoTime() > expiresAtNanos;
        }
    }

    private final int maxSize;
    private final long ttlMillis;
    private final Map<String, CacheEntry> cache;
    private final ReentrantLock lock = new ReentrantLock();

    public ProxyCache(int maxSize) {
        this(maxSize, 300_000);
    }

    public ProxyCache(int maxSize, long ttlMillis) {
        this.maxSize = maxSize;
        this.ttlMillis = ttlMillis;

        this.cache = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                return size() > ProxyCache.this.maxSize;
            }
        };
    }

    public byte[] get(String key) {
        lock.lock();
        try {
            CacheEntry entry = cache.get(key);
            if (entry == null) return null;

            if (entry.isExpired()) {
                cache.remove(key);
                return null;
            }

            return entry.data.clone();
        } finally {
            lock.unlock();
        }
    }

    public void put(String key, byte[] value) {
        lock.lock();
        try {
            cache.put(key, new CacheEntry(value, ttlMillis));
        } finally {
            lock.unlock();
        }
    }

    public void clear() {
        lock.lock();
        try {
            cache.clear();
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return cache.size();
        } finally {
            lock.unlock();
        }
    }
}
