package com.example.webserver;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class Metrics {
    private final AtomicLong connections = new AtomicLong();
    private final AtomicLong timeouts = new AtomicLong();
    private final AtomicLong errors = new AtomicLong();
    private final AtomicLong dropped = new AtomicLong();
    private final AtomicLong rateLimited = new AtomicLong();
    private final AtomicLong cacheHit = new AtomicLong();
    private final AtomicLong cacheStore = new AtomicLong();
    private final Map<Integer, AtomicLong> statuses = new ConcurrentHashMap<>();

    public void incConnections() { connections.incrementAndGet(); }
    public void incTimeouts() { timeouts.incrementAndGet(); }
    public void incErrors() { errors.incrementAndGet(); }
    public void incDropped() { dropped.incrementAndGet(); }
    public void incRateLimited() { rateLimited.incrementAndGet(); }
    public void incCacheHit() { cacheHit.incrementAndGet(); }
    public void incCacheStore() { cacheStore.incrementAndGet(); }

    public void observeRequest(String method, int status, long nanos) {
        statuses.computeIfAbsent(status, s -> new AtomicLong()).incrementAndGet();
        // You can hook this into logs or JMX if desired
        if ((connections.get() % 1000) == 0) {
            System.out.println(summary());
        }
    }

    public String summary() {
        return "[metrics] conns=" + connections.get()
                + " timeouts=" + timeouts.get()
                + " errors=" + errors.get()
                + " dropped=" + dropped.get()
                + " ratelimited=" + rateLimited.get()
                + " cache(hit/store)=" + cacheHit.get() + "/" + cacheStore.get()
                + " statuses=" + statuses.toString();
    }
}
