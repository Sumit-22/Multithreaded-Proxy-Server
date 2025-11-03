package com.example.proxy;

import java.util.concurrent.atomic.*;

/**
 * Thread-safe metrics collector for proxy server performance monitoring.
 */
public class ProxyMetrics {
    private final AtomicLong connectionsReceived = new AtomicLong(0);
    private final AtomicLong requestsProcessed = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong badRequests = new AtomicLong(0);
    private final AtomicLong errors = new AtomicLong(0);
    private final AtomicLong timeouts = new AtomicLong(0);
    private final AtomicLong totalLatency = new AtomicLong(0);
    private final AtomicLong requestCount = new AtomicLong(0);
    
    public void incrementConnectionsReceived() {
        connectionsReceived.incrementAndGet();
    }
    
    public void incrementRequestsProcessed() {
        requestsProcessed.incrementAndGet();
    }
    
    public void incrementCacheHits() {
        cacheHits.incrementAndGet();
    }
    
    public void incrementCacheMisses() {
        cacheMisses.incrementAndGet();
    }
    
    public void incrementBadRequests() {
        badRequests.incrementAndGet();
    }
    
    public void incrementErrors() {
        errors.incrementAndGet();
    }
    
    public void incrementTimeouts() {
        timeouts.incrementAndGet();
    }
    
    public void recordLatency(long latencyMs) {
        totalLatency.addAndGet(latencyMs);
        requestCount.incrementAndGet();
    }
    
    public void printMetrics() {
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long total = hits + misses;
        double cacheHitRate = total > 0 ? (100.0 * hits / total) : 0;
        long avgLatency = requestCount.get() > 0 ? totalLatency.get() / requestCount.get() : 0;
        
        System.out.println("\n=== PROXY SERVER METRICS ===");
        System.out.println("Connections Received: " + connectionsReceived.get());
        System.out.println("Requests Processed: " + requestsProcessed.get());
        System.out.println("Cache Hits: " + hits);
        System.out.println("Cache Misses: " + misses);
        System.out.println("Cache Hit Rate: " + String.format("%.2f%%", cacheHitRate));
        System.out.println("Bad Requests: " + badRequests.get());
        System.out.println("Errors: " + errors.get());
        System.out.println("Timeouts: " + timeouts.get());
        System.out.println("Average Latency: " + avgLatency + "ms");
    }
}
