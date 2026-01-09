package com.example.proxy;

import java.io.*;
import java.net.*;
import java.util.logging.*;

public class ProxyHandler implements Runnable {

    private static final Logger logger =
            Logger.getLogger(ProxyHandler.class.getName());

    private static final int READ_TIMEOUT = 10_000;
    private static final int CONNECT_TIMEOUT = 5_000;
    private static final int MAX_CACHE_SIZE = 512 * 1024; // 512 KB

    private final Socket clientSocket;
    private final ProxyCache cache;
    private final ProxyMetrics metrics;

    public ProxyHandler(Socket clientSocket,
                        ProxyCache cache,
                        ProxyMetrics metrics) {
        this.clientSocket = clientSocket;
        this.cache = cache;
        this.metrics = metrics;
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();

        try {
            clientSocket.setSoTimeout(READ_TIMEOUT);

            InputStream clientIn = clientSocket.getInputStream();
            OutputStream clientOut = clientSocket.getOutputStream();

            HttpProxyRequest request = HttpProxyRequest.parse(clientIn);
            if (request == null) {
                metrics.incrementBadRequests();
                sendBadRequest(clientOut);
                return;
            }

            // HTTPS tunneling not supported
            if ("CONNECT".equalsIgnoreCase(request.getMethod())) {
                sendNotImplemented(clientOut);
                return;
            }

            metrics.incrementRequests();
            String cacheKey = request.getCacheKey();

            // ---------- CACHE ----------
            if ("GET".equalsIgnoreCase(request.getMethod())) {
                byte[] cached = cache.get(cacheKey);
                metrics.recordCacheLookup(cached != null);

            if (cached != null) {
                clientOut.write(cached);
                clientOut.flush();
                return;
    }
}

            // ---------- CONNECT TO TARGET ----------
            Socket target = new Socket();
            target.connect(
                    new InetSocketAddress(
                            request.getHost(),
                            request.getPort()
                    ),
                    CONNECT_TIMEOUT
            );
            target.setSoTimeout(READ_TIMEOUT);

            InputStream targetIn = target.getInputStream();
            OutputStream targetOut = target.getOutputStream();

            // Normalize headers
            request.removeHopByHopHeaders();
            request.addHeader("Connection", "close");

            request.writeTo(targetOut);

            // ---------- STREAM RESPONSE ----------
            ByteArrayOutputStream cacheBuffer = new ByteArrayOutputStream();
            boolean cacheable = "GET".equalsIgnoreCase(request.getMethod());

            byte[] buf = new byte[8192];
            int n;
            int total = 0;

            while ((n = targetIn.read(buf)) != -1) {
                clientOut.write(buf, 0, n);

                if (cacheable && total < MAX_CACHE_SIZE) {
                    cacheBuffer.write(buf, 0, n);
                }

                total += n;
            }

            clientOut.flush();

            if (cacheable && total <= MAX_CACHE_SIZE) {
                cache.put(cacheKey, cacheBuffer.toByteArray());
            }

            target.close();
            metrics.recordLatency(System.currentTimeMillis() - start);

        } catch (SocketTimeoutException e) {
            metrics.incrementTimeouts();
            logger.warning("Timeout handling request");
        } catch (IOException e) {
            metrics.incrementErrors();
            logger.log(Level.WARNING, "Proxy error", e);
        } finally {
            try {
                clientSocket.close();
            } catch (IOException ignored) {}
        }
    }

    // ---------- ERROR RESPONSES ----------

    private void sendBadRequest(OutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 400 Bad Request\r\n" +
                "Connection: close\r\n\r\n"
        ).getBytes());
        out.flush();
    }

    private void sendNotImplemented(OutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 501 Not Implemented\r\n" +
                "Connection: close\r\n\r\n" +
                "CONNECT not supported"
        ).getBytes());
        out.flush();
    }
}
