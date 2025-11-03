package com.example.proxy;

import java.io.*;
import java.net.*;
import java.util.logging.*;

/**
 * Handles individual client proxy requests in a thread-safe manner.
 */
public class ProxyHandler implements Runnable {
    private static final Logger logger = Logger.getLogger(ProxyHandler.class.getName());
    private static final int READ_TIMEOUT = 10000; // 10 seconds
    private static final int CONNECT_TIMEOUT = 5000; // 5 seconds
    
    private final Socket clientSocket;
    private final ProxyCache cache;
    private final ProxyMetrics metrics;
    
    public ProxyHandler(Socket clientSocket, ProxyCache cache, ProxyMetrics metrics) {
        this.clientSocket = clientSocket;
        this.cache = cache;
        this.metrics = metrics;
    }
    
    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        try {
            clientSocket.setSoTimeout(READ_TIMEOUT);
            
            InputStream clientInput = clientSocket.getInputStream();
            OutputStream clientOutput = clientSocket.getOutputStream();
            
            // Parse HTTP request
            HttpProxyRequest request = HttpProxyRequest.parse(clientInput);
            if (request == null) {
                metrics.incrementBadRequests();
                return;
            }
            
            metrics.incrementRequestsProcessed();
            String cacheKey = request.getCacheKey();
            
            // Check cache for GET requests
            byte[] cachedResponse = null;
            if ("GET".equalsIgnoreCase(request.getMethod())) {
                cachedResponse = cache.get(cacheKey);
                if (cachedResponse != null) {
                    metrics.incrementCacheHits();
                    logger.info("CACHE HIT → " + request.getUrl());
                    clientOutput.write(cachedResponse);
                    clientOutput.flush();
                    return;
                } else {
                    logger.info("CACHE MISS → " + request.getUrl());
                    metrics.incrementCacheMisses();
                }
            }


            // Forward request to target server
            Socket targetSocket = new Socket();
            targetSocket.connect(new InetSocketAddress(request.getHost(), request.getPort()), CONNECT_TIMEOUT);
            
            InputStream targetInput = targetSocket.getInputStream();
            OutputStream targetOutput = targetSocket.getOutputStream();
            
            // Send request to target
            request.writeTo(targetOutput);
            
            // Read response from target
            byte[] responseBytes = readResponseBytes(targetInput);
            
            // Cache successful GET responses
            if ("GET".equalsIgnoreCase(request.getMethod()) && responseBytes.length > 0) {
                cache.put(cacheKey, responseBytes);
                logger.info("CACHED → " + request.getUrl() + " (" + responseBytes.length + " bytes)");
            }


            // Forward response to client
            clientOutput.write(responseBytes);
            clientOutput.flush();
            
            targetSocket.close();
            metrics.recordLatency(System.currentTimeMillis() - startTime);
            
        } catch (SocketTimeoutException e) {
            metrics.incrementTimeouts();
            logger.log(Level.WARNING, "Client read timeout", e);
        } catch (IOException e) {
            metrics.incrementErrors();
            logger.log(Level.WARNING, "Error handling proxy request", e);
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error closing client socket", e);
            }
        }
    }
    
    private byte[] readResponseBytes(InputStream input) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int nRead;
        while ((nRead = input.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }
}
