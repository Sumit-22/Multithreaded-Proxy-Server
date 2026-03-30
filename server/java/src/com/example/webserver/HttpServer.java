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
                       // t.setDaemon(true);
                        t.setDaemon(false);
                        return t;
                    }
                }, // Keep the default AbortPolicy; we'll handle RejectedExecutionException after accept().
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
                    socket.setSoTimeout(15000); // read timeout
                     try {
                        pool.execute(() -> handle(socket));
                    } catch (RejectedExecutionException rex) {
                    // Saturated; drop connection gracefully
                    metrics.incDropped();
                    try{
                      // Optionally send a short 503 response. Keep it simple: close the socket.
                            socket.close();
                        } catch (IOException ignored) {}
                }
            } catch(SocketException se) {
                    // This happens when serverSocket.close() is called in stop().
                    // If we're stopping, break quietly; otherwise surface the error.
                  if (running) {
                        metrics.incErrors();
                        throw se;
                    } else {
                        break;
                    }
                } catch (IOException ioe) {
                    // Transient IO error on accept - count it and continue
                    metrics.incErrors();
                }
            }
        } finally {
            // graceful shutdown: stop accepting new tasks, wait a bit, then force-kill remaining
            pool.shutdown();
            try {
                if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                pool.shutdownNow();
            }
        }
    }

    private void handle(Socket socket) {
        metrics.incConnections();
        try (socket;
             InputStream in = new BufferedInputStream(socket.getInputStream());
             OutputStream out = new BufferedOutputStream(socket.getOutputStream())) {

            long start = System.nanoTime();
            HttpRequest req = HttpRequest.parse(in);
            if (req == null) { //// best-effort: write a response, but if it fails, just close.
                 try { 
                    HttpResponseWriter.write(out, HttpResponse.badRequest("Malformed request"));
                 }
                 catch (IOException ignored) {}
                    return;
            }

            String client = clientKey(socket);
            if (!rateLimiter.allow(client)) {
                metrics.incRateLimited();
                try{
                    HttpResponseWriter.write(out, HttpResponse.tooManyRequests("Rate limit exceeded"));
                }
                catch (IOException ignored) {}
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
         // optionally close socket (try-with-resources will close it)
        } catch (IOException ioe) {
            metrics.incErrors();
            // Try to send internal error if possible
            try {
                OutputStream out = socket.getOutputStream();
                HttpResponseWriter.write(out, HttpResponse.internalError("I/O error"));
            } catch (Exception ignored) {}
        } catch (Exception e) {
            metrics.incErrors();
            // Try to send internal error if possible
            try {
                OutputStream out = socket.getOutputStream();
                HttpResponseWriter.write(out, HttpResponse.internalError("Internal server error"));
            } catch (Exception ignored) {}
        }
    }

    private static String clientKey(Socket s) {
        try{ // Prefer IP only (no ephemeral port) for client-level rate limiting
            SocketAddress addr = s.getRemoteSocketAddress();
            if (addr instanceof InetSocketAddress isa) {
                InetAddress ia = isa.getAddress();
                return ia == null ? isa.toString() : ia.getHostAddress();
            }
        return addr == null ? "unknown" : addr.toString();
        } catch(Exception e){
            return "unknown";
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close(); // will cause accept() to throw SocketException and exit loop
            }
        } catch (IOException ignored) {}
        // prefer orderly shutdown; start()'s finally block will await termination
        pool.shutdown();
    }
}
