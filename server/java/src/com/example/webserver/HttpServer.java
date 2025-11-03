package com.example.webserver;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class HttpServer {
    private final int port;
    private final Router router;
    private final LruCache<String, CacheEntry> cache;
    private final RateLimiter rateLimiter;
    private final Metrics metrics;

    private volatile boolean running = true;
    private ServerSocket serverSocket;
    private final ExecutorService pool;

    public HttpServer(int port, Router router, LruCache<String, CacheEntry> cache, RateLimiter rateLimiter, Metrics metrics) {
        this.port = port;
        this.router = router;
        this.cache = cache;
        this.rateLimiter = rateLimiter;
        this.metrics = metrics;
        int threads = Math.max(4, Runtime.getRuntime().availableProcessors() * 2);
        this.pool = new ThreadPoolExecutor(
                threads, threads,
                60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1024),
                new ThreadFactory() {
                    private final ThreadFactory def = Executors.defaultThreadFactory();
                    @Override public Thread newThread(Runnable r) {
                        Thread t = def.newThread(r);
                        t.setName("http-worker-" + t.getId());
                        t.setDaemon(true);
                        return t;
                    }
                },
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    public void start() throws IOException {
        try (ServerSocket ss = new ServerSocket()) {
            this.serverSocket = ss;
            ss.setReuseAddress(true);
            ss.bind(new InetSocketAddress(port));
            while (running) {
                try {
                    final Socket socket = ss.accept();
                    socket.setTcpNoDelay(true);
                    socket.setSoTimeout(15_000); // read timeout
                    pool.execute(() -> handle(socket));
                } catch (RejectedExecutionException rex) {
                    // Saturated; drop connection gracefully
                    metrics.incDropped();
                }
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private void handle(Socket socket) {
        metrics.incConnections();
        try (socket;
             InputStream in = new BufferedInputStream(socket.getInputStream());
             OutputStream out = new BufferedOutputStream(socket.getOutputStream())) {

            long start = System.nanoTime();
            HttpRequest req = HttpRequest.parse(in);
            if (req == null) {
                HttpResponseWriter.write(out, HttpResponse.badRequest("Malformed request"));
                return;
            }

            String client = clientKey(socket);
            if (!rateLimiter.allow(client)) {
                metrics.incRateLimited();
                HttpResponseWriter.write(out, HttpResponse.tooManyRequests("Rate limit exceeded"));
                return;
            }

            HttpResponse resp;
            boolean cacheable = "GET".equals(req.method());
            String cacheKey = req.method() + " " + req.path();

            if (cacheable) {
                CacheEntry cached = cache.get(cacheKey);
                if (cached != null && !cached.isExpired()) {
                    resp = HttpResponse.okBytes(cached.body());
                    resp.headers().putAll(cached.headers());
                    resp.headers().putIfAbsent("X-Cache", "HIT");
                    metrics.incCacheHit();
                } else {
                    resp = router.handle(req);
                    resp.headers().put("X-Cache", "MISS");
                    if (resp.status() == 200 && resp.body() != null && resp.body().length < 1_000_000) {
                        cache.put(cacheKey, CacheEntry.from(resp, cache.getTtlMillis()));
                        metrics.incCacheStore();
                    }
                }
            } else {
                resp = router.handle(req);
            }

            HttpResponseWriter.write(out, resp);
            long took = System.nanoTime() - start;
            metrics.observeRequest(req.method(), resp.status(), took);
        } catch (SocketTimeoutException ste) {
            metrics.incTimeouts();
        } catch (IOException ioe) {
            metrics.incErrors();
        } catch (Exception e) {
            metrics.incErrors();
        }
    }

    private static String clientKey(Socket s) {
        SocketAddress addr = s.getRemoteSocketAddress();
        return addr == null ? "unknown" : addr.toString();
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {}
        pool.shutdown();
    }
}
