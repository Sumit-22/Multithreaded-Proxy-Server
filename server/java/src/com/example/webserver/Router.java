package com.example.webserver;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Router {
    public interface Handler {
        HttpResponse handle(HttpRequest ctx) throws Exception;
    }

    private final Map<String, Handler> routes = new ConcurrentHashMap<>();

    private static String key(String method, String path) {
        return method + " " + path;
    }

    public void get(String path, Handler h) { routes.put(key("GET", path), h); }
    public void post(String path, Handler h) { routes.put(key("POST", path), h); }
    public void put(String path, Handler h) { routes.put(key("PUT", path), h); }
    public void delete(String path, Handler h) { routes.put(key("DELETE", path), h); }

    public HttpResponse handle(HttpRequest req) {
        try {
            Handler h = routes.getOrDefault(key(req.method(), req.path()), null);
            if (h == null) return HttpResponse.notFound();
            return h.handle(req);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return HttpResponse.internalError("Unhandled error");
        }
    }
}
