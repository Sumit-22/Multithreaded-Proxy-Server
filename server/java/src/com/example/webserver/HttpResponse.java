package com.example.webserver;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class HttpResponse {
    private final int status;
    private final String reason;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private byte[] body;

    public HttpResponse(int status, String reason) {
        this.status = status;
        this.reason = reason;
    }

    public int status() { return status; }
    public String reason() { return reason; }
    public Map<String, String> headers() { return headers; }
    public byte[] body() { return body; }

    public HttpResponse body(byte[] body) {
        this.body = body;
        return this;
    }

    public static HttpResponse okText(String s) {
        HttpResponse r = new HttpResponse(200, "OK");
        r.headers.put("Content-Type", "text/plain; charset=utf-8");
        r.body = s.getBytes(StandardCharsets.UTF_8);
        return r;
    }

    public static HttpResponse okJson(String s) {
        HttpResponse r = new HttpResponse(200, "OK");
        r.headers.put("Content-Type", "application/json; charset=utf-8");
        r.body = s.getBytes(StandardCharsets.UTF_8);
        return r;
    }

    public static HttpResponse okBytes(byte[] b) {
        HttpResponse r = new HttpResponse(200, "OK");
        r.headers.put("Content-Type", "application/octet-stream");
        r.body = b;
        return r;
    }

    public static HttpResponse notFound() {
        return badRequest(404, "Not Found");
    }

    public static HttpResponse badRequest(String msg) {
        return badRequest(400, msg);
    }

    public static HttpResponse badRequest(int code, String msg) {
        HttpResponse r = new HttpResponse(code, code == 404 ? "Not Found" : "Bad Request");
        r.headers.put("Content-Type", "text/plain; charset=utf-8");
        r.body = msg.getBytes(StandardCharsets.UTF_8);
        return r;
    }

    public static HttpResponse tooManyRequests(String msg) {
        HttpResponse r = new HttpResponse(429, "Too Many Requests");
        r.headers.put("Content-Type", "text/plain; charset=utf-8");
        r.body = msg.getBytes(StandardCharsets.UTF_8);
        return r;
    }

    public static HttpResponse internalError(String msg) {
        HttpResponse r = new HttpResponse(500, "Internal Server Error");
        r.headers.put("Content-Type", "text/plain; charset=utf-8");
        r.body = msg.getBytes(StandardCharsets.UTF_8);
        return r;
    }
}
