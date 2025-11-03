package com.example.proxy;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Parses HTTP requests for proxy forwarding.
 */
public class HttpProxyRequest {
    private String method;
    private String path;
    private String host;
    private int port;
    private Map<String, String> headers;
    private byte[] body;
    
    public static HttpProxyRequest parse(InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        
        String requestLine = reader.readLine();
        if (requestLine == null || requestLine.isEmpty()) {
            return null;
        }
        
        String[] parts = requestLine.split(" ");
        if (parts.length < 3) {
            return null;
        }
        
        HttpProxyRequest req = new HttpProxyRequest();
        req.method = parts[0].toUpperCase();
        req.path = parts[1];
        req.headers = new HashMap<>();
        
        // Parse headers
        String headerLine;
        int contentLength = 0;
        while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
            int colonIdx = headerLine.indexOf(':');
            if (colonIdx > 0) {
                String key = headerLine.substring(0, colonIdx).trim();
                String value = headerLine.substring(colonIdx + 1).trim();
                req.headers.put(key.toLowerCase(), value);
                
                if ("content-length".equalsIgnoreCase(key)) {
                    contentLength = Integer.parseInt(value);
                }
            }
        }
        
        // Parse Host header to extract target server
        String hostHeader = req.headers.get("host");
        if (hostHeader == null) {
            return null;
        }
        
        String[] hostParts = hostHeader.split(":");
        req.host = hostParts[0];
        req.port = hostParts.length > 1 ? Integer.parseInt(hostParts[1]) : 80;
        
        // Read body if present
        if (contentLength > 0) {
            req.body = new byte[contentLength];
            int totalRead = 0;
            while (totalRead < contentLength) {
                int nRead = input.read(req.body, totalRead, contentLength - totalRead);
                if (nRead == -1) break;
                totalRead += nRead;
            }
        }
        
        return req;
    }
    
    public void writeTo(OutputStream output) throws IOException {
        StringBuilder request = new StringBuilder();
        request.append(method).append(" ").append(path).append(" HTTP/1.1\r\n");
        
        for (Map.Entry<String, String> header : headers.entrySet()) {
            request.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
        }
        request.append("\r\n");
        
        output.write(request.toString().getBytes());
        if (body != null && body.length > 0) {
            output.write(body);
        }
        output.flush();
    }
    
    public String getCacheKey() {
        return method + ":" + host + ":" + port + ":" + path;
    }
    
    public String getMethod() { return method; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getUrl() {
        StringBuilder sb = new StringBuilder("http://");
        sb.append(host);
        if (port != 80) {
            sb.append(":").append(port);
        }
        sb.append(path);
        return sb.toString();
    }

}
