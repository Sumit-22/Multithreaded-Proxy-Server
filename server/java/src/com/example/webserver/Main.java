package com.example.webserver;

public class Main {
    public static void main(String[] args) throws Exception {
        int port = 8080;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }

        // LRU cache: capacity 1024 entries, TTL 30 seconds
        LruCache<String, CacheEntry> cache = new LruCache<>(1024, 30_000);
        RateLimiter rateLimiter = new RateLimiter( // 50 req/s burst 100 per IP
                50.0, 100.0
        );
        Metrics metrics = new Metrics();

        Router router = new Router();
        // Register demo routes
        router.get("/", ctx -> HttpResponse.okText("Welcome to the Java Multithreaded Web Server!"));
        router.get("/healthz", ctx -> HttpResponse.okText("ok"));
        router.get("/time", ctx -> HttpResponse.okJson("{\"epochMillis\":" + System.currentTimeMillis() + "}"));
        router.post("/echo", ctx -> HttpResponse.okBytes(ctx.body()));

        HttpServer server = new HttpServer(port, router, cache, rateLimiter, metrics);
        System.out.println("[server] Starting on port " + port);
        server.start();
    }
}
