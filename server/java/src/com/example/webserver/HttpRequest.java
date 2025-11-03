package com.example.webserver;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public record HttpRequest(String method, String path, String version, Map<String, String> headers, byte[] body) {

    public static HttpRequest parse(InputStream in) throws IOException {
        // Read raw headers until CRLF CRLF
        byte[] headerBytes = readUntilDoubleCRLF(in);
        if (headerBytes == null || headerBytes.length == 0) return null;

        String headerStr = new String(headerBytes, java.nio.charset.StandardCharsets.US_ASCII);
        String[] lines = headerStr.split("\r\n");
        if (lines.length == 0) return null;

        String[] parts = lines[0].split(" ", 3);
        if (parts.length < 3) return null;

        String method = parts[0].trim();
        String path = parts[1].trim();
        String version = parts[2].trim();

        Map<String, String> headers = new LinkedHashMap<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty()) break;
            int idx = line.indexOf(':');
            if (idx > 0) {
                String k = line.substring(0, idx).trim();
                String v = line.substring(idx + 1).trim();
                headers.put(k, v);
            }
        }

        int len = 0;
        if (headers.containsKey("Content-Length")) {
            try {
                len = Integer.parseInt(headers.get("Content-Length"));
            } catch (NumberFormatException ignored) {}
        }

        byte[] body = new byte[0];
        if (len > 0) {
            // Read raw bytes for the body; do not use character readers
            body = in.readNBytes(len);
        }

        return new HttpRequest(method, path, version, headers, body);
    }

    private static byte[] readUntilDoubleCRLF(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream(1024);
        int state = 0;
        int b;
        while ((b = in.read()) != -1) {
            bos.write(b);
            // detect \r\n\r\n
            switch (state) {
                case 0: state = (b == '\r') ? 1 : 0; break;
                case 1: state = (b == '\n') ? 2 : 0; break;
                case 2: state = (b == '\r') ? 3 : 0; break;
                case 3:
                    if (b == '\n') return bos.toByteArray();
                    state = 0; break;
            }
            // Safety: cap header size (e.g., 64KB) to avoid abuse
            if (bos.size() > 64 * 1024) throw new IOException("Header too large");
        }
        return bos.toByteArray();
    }

    public byte[] body() { return body; }
}
