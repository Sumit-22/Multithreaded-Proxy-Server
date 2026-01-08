package com.example.webserver;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class Metrics {
    private final AtomicLong connections = new AtomicLong();
    private final AtomicLong requests = new AtomicLong();
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

    /* ===== Request observation ===== */

    public void observeRequest(String method, int status, long nanos) {
        requests.incrementAndGet();
        statuses
                .computeIfAbsent(status, s -> new AtomicLong())
                .incrementAndGet();

        if ((requests.get() % 1000) == 0) {
            System.out.println(summary());
        }
    }

    public String summary() {
        Map<Integer, Long> statusSnapshot = snapshotStatuses();
        return "[metrics] conns=" + connections.get()
                + " reqs=" + requests.get()
                + " timeouts=" + timeouts.get()
                + " errors=" + errors.get()
                + " dropped=" + dropped.get()
                + " ratelimited=" + rateLimited.get()
                + " cache(hit/store)=" + cacheHit.get() + "/" + cacheStore.get()
                + " statuses=" + statusSnapshot;
    }
    private Map<Integer, Long> snapshotStatuses() {
        Map<Integer, Long> snapshot = new HashMap<>();
        for (Map.Entry<Integer, AtomicLong> e : statuses.entrySet()) {
            snapshot.put(e.getKey(), e.getValue().get());
        }
        return snapshot;
    }
}
