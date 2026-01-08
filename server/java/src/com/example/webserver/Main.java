package com.example.webserver;

public class Main {

    public static void main(String[] args) throws Exception {
        int port = parsePort(args);

        // Global response cache: 1024 entries, 30s TTL
        LruCache<String, CacheEntry> cache =
                new LruCache<>(1024, 30_000);

        // Rate limit: 50 req/s, burst up to 100 per key
        RateLimiter rateLimiter =
                new RateLimiter(50.0, 100.0);

        Metrics metrics = new Metrics();

        Router router = new Router();

        // Demo routes
        router.get("/", ctx ->
                HttpResponse.okText("Welcome to the Java Multithreaded Web Server!")
        );

        router.get("/healthz", ctx ->
                HttpResponse.okText("ok")
        );

        router.get("/time", ctx ->
                HttpResponse.okJson(jsonField(
                        "epochMillis",
                        System.currentTimeMillis()
                ))
        );

        router.post("/echo", ctx ->
                HttpResponse.okBytes(ctx.body())
        );

        HttpServer server =
                new HttpServer(port, router, cache, rateLimiter, metrics);

        System.out.println("[server] Starting on port " + port);
        server.start();
    }

    private static int parsePort(String[] args) {
        if (args.length == 0) return 8080;

        try {
            return Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid port: " + args[0]);
            System.exit(1);
            return -1; // unreachable
        }
    }

    private static String jsonField(String key, long value) {
        return "{\"" + key + "\":" + value + "}";
    }
}
