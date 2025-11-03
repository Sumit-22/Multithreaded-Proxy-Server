package com.example.proxy;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;

/**
 * Multithreaded HTTP/HTTPS proxy server with LRU cache.
 * Handles concurrent client connections via ExecutorService thread pool.
 */
public class ProxyServer {
    private static final Logger logger = Logger.getLogger(ProxyServer.class.getName());

    private final int port;
    private final int threadPoolSize;
    private final ExecutorService threadPool;
    private final ProxyCache cache;
    private final ProxyMetrics metrics;
    private volatile boolean running = false;
    private ServerSocket serverSocket;

    public ProxyServer(int port, int threadPoolSize, int cacheSize) {
        this.port = port;
        this.threadPoolSize = threadPoolSize;
        this.threadPool = Executors.newFixedThreadPool(threadPoolSize);
        this.cache = new ProxyCache(cacheSize);
        this.metrics = new ProxyMetrics();
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        logger.info("Proxy Server started on port " + port + " with thread pool size: " + threadPoolSize);

        // Accept connections in main thread
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                metrics.incrementConnectionsReceived();
                threadPool.execute(new ProxyHandler(clientSocket, cache, metrics));
            } catch (SocketException e) {
                if (running) {
                    logger.log(Level.WARNING, "Error accepting connection", e);
                }
            }
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error closing server socket", e);
        }
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("Proxy Server stopped");
    }

    public ProxyMetrics getMetrics() {
        return metrics;
    }

    public static void main(String[] args) throws IOException {
        // ✅ Ensure INFO-level logs are visible in console
        Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(Level.INFO);
        for (Handler h : rootLogger.getHandlers()) {
            h.setLevel(Level.INFO);
        }

        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9090;
        int threadPoolSize = args.length > 1 ? Integer.parseInt(args[1]) : 100;
        int cacheSize = args.length > 2 ? Integer.parseInt(args[2]) : 1000;

        ProxyServer server = new ProxyServer(port, threadPoolSize, cacheSize);

        // Graceful shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down proxy server...");
            server.stop();
        }));

        server.start();
    }

}
