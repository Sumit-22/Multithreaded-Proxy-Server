package com.example.webserver;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Router {
    public interface Handler {
        HttpResponse handle(HttpRequest ctx) throws Exception;
    }

    private final Map<String, Handler> routes = new ConcurrentHashMap<>();

    private static String key(String method, String path) {
        String decodedPath = decodePath(path);
        return normalizeMethod(method) + " " + normalizePath(decodedPath);

    }
    private static String decodePath(String path) {
        return URLDecoder.decode(path, StandardCharsets.UTF_8);
    }

    private static String normalizeMethod(String method) {
    return method.toUpperCase();
}

    private static String normalizePath(String path) {
        if (path.length() > 1 && path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
    return path;
    }

    public void get(String path, Handler h) { routes.put(key("GET", path), h); }
    public void post(String path, Handler h) { routes.put(key("POST", path), h); }
    public void put(String path, Handler h) { routes.put(key("PUT", path), h); }
    public void delete(String path, Handler h) { routes.put(key("DELETE", path), h); }
    
    public HttpResponse handle(HttpRequest req) {
        try {
            Handler h = routes.get(key(req.method(), req.path()));
            if (h == null) return HttpResponse.notFound();
            return h.handle(req);
        } catch (HttpException e) {
            return e.response();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return HttpResponse.internalError("Unhandled server error");
        }
    }   

}
