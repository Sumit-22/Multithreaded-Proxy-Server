package com.example.webserver;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public record HttpRequest(
        String method,
        String path,
        String version,
        Map<String, String> headers,
        byte[] body
) {

    private static final int MAX_HEADER_SIZE = 64 * 1024;
    private static final int MAX_BODY_SIZE = 1 * 1024 * 1024; // 1 MB

    public static HttpRequest parse(InputStream in) throws IOException {
        byte[] headerBytes = readUntilHeaderEnd(in);
        if (headerBytes == null || headerBytes.length == 0) return null;

        String headerStr = new String(headerBytes, StandardCharsets.US_ASCII);
        String[] lines = headerStr.split("\r?\n");
        if (lines.length == 0) return null;

        String[] parts = lines[0].split(" ", 3);
        if (parts.length < 3) return null;

        String method = parts[0].trim();
        String rawPath = parts[1].trim();
        String path = decodePath(rawPath);
        String version = parts[2].trim();

        Map<String, String> headers = new LinkedHashMap<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty()) break;
            int idx = line.indexOf(':');
            if (idx > 0) {
                headers.put(
                        line.substring(0, idx).trim(),
                        line.substring(idx + 1).trim()
                );
            }
        }

        // Reject chunked encoding (not supported)
        if ("chunked".equalsIgnoreCase(headers.get("Transfer-Encoding"))) {
            throw new IOException("Chunked transfer encoding not supported");
        }

        int contentLength = parseContentLength(headers);
        if (contentLength > MAX_BODY_SIZE) {
            throw new IOException("Request body too large");
        }

        byte[] body = readExactBytes(in, contentLength);

        return new HttpRequest(method, path, version, headers, body);
    }

    private static int parseContentLength(Map<String, String> headers) {
        String v = headers.get("Content-Length");
        if (v == null) return 0;
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static byte[] readExactBytes(InputStream in, int len) throws IOException {
        if (len == 0) return new byte[0];

        byte[] body = in.readNBytes(len);
        if (body.length != len) {
            throw new EOFException("Unexpected end of request body");
        }
        return body;
    }

    private static String decodePath(String path) {
        try {
            return URLDecoder.decode(path, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return path;
        }
    }

    private static byte[] readUntilHeaderEnd(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
        int prev = -1;
        int curr;
        int newlineCount = 0;

        while ((curr = in.read()) != -1) {
            bos.write(curr);

            if (curr == '\n') {
                newlineCount++;
                if (newlineCount == 2) {
                    return bos.toByteArray();
                }
            } else if (curr != '\r') {
                newlineCount = 0;
            }

            if (bos.size() > MAX_HEADER_SIZE) {
                throw new IOException("Header too large");
            }

            prev = curr;
        }
        return bos.toByteArray();
    }

    public byte[] body() {
        return body.clone();
    }
}
