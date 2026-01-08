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
    /* 
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
    */
   public static HttpProxyRequest parse(InputStream in) throws IOException {
    byte[] headerBytes = readUntilDoubleCRLF(in);
    if (headerBytes == null) return null;

    String headerText = new String(headerBytes, StandardCharsets.US_ASCII);
    String[] lines = headerText.split("\r?\n");

    String[] requestLine = lines[0].split(" ", 3);
    if (requestLine.length < 3) return null;

    HttpProxyRequest req = new HttpProxyRequest();
    req.method = requestLine[0].toUpperCase();
    String rawPath = requestLine[1];
    req.headers = new LinkedHashMap<>();

    int contentLength = 0;

    for (int i = 1; i < lines.length; i++) {
        if (lines[i].isEmpty()) break;
        int idx = lines[i].indexOf(':');
        if (idx > 0) {
            String k = lines[i].substring(0, idx).trim();
            String v = lines[i].substring(idx + 1).trim();
            req.headers.put(k, v);
            if (k.equalsIgnoreCase("Content-Length")) {
                contentLength = Integer.parseInt(v);
            }
            if (k.equalsIgnoreCase("Transfer-Encoding")
                    && v.equalsIgnoreCase("chunked")) {
                throw new IOException("Chunked encoding not supported");
            }
        }
    }

    // Handle absolute-form URL
    if (rawPath.startsWith("http://")) {
        URL url = new URL(rawPath);
        req.host = url.getHost();
        req.port = url.getPort() != -1 ? url.getPort() : 80;
        req.path = url.getFile();
    } else {
        req.path = rawPath;
        String hostHeader = req.headers.get("Host");
        if (hostHeader == null) return null;
        String[] hostParts = hostHeader.split(":");
        req.host = hostParts[0];
        req.port = hostParts.length > 1 ? Integer.parseInt(hostParts[1]) : 80;
    }

    // Body read with limit
    if (contentLength > 0) {
        if (contentLength > 1024 * 1024) {
            throw new IOException("Body too large");
        }
        req.body = in.readNBytes(contentLength);
        if (req.body.length != contentLength) {
            throw new EOFException("Incomplete request body");
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
