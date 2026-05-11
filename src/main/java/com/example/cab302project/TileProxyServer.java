package com.example.cab302project;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * A lightweight local HTTP tile proxy server for JavaFX WebView.
 *
 * JavaFX's WebView on macOS (WebKit) blocks external network requests to
 * tile servers (e.g. openstreetmap.org), causing the map tiles to fail silently.
 * On Windows the tiles load but slowly due to WebKit's limited concurrency.
 *
 * This server binds to localhost on a random available port and proxies
 * OSM tile requests using Java's HttpURLConnection, which has no such
 * restrictions. WebView talks to localhost (always allowed), and this
 * server fetches the real tiles transparently.
 *
 * Usage:
 *   TileProxyServer server = new TileProxyServer();
 *   server.start();
 *   int port = server.getPort();
 *   server.stop();
 */
public class TileProxyServer {

    private static final Logger LOG = Logger.getLogger(TileProxyServer.class.getName());

    /** OSM subdomains to round-robin across for load balancing, as per OSM tile usage policy. */
    private static final String[] OSM_HOSTS = {"a", "b", "c"};

    /** User-Agent required by OSM tile usage policy. */
    private static final String USER_AGENT = "RADIUS-App/1.0 (CAB302 University Project)";

    private HttpServer server;
    private int port;
    private int hostIndex = 0;

    /**
     * Starts the tile proxy on a random available port.
     *
     * @throws IOException if the server cannot bind to a port.
     */
    public void start() throws IOException {
        // Port 0 tells the OS to assign any free port automatically
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();

        server.createContext("/tile", new TileHandler());
        server.createContext("/health", exchange -> {
            byte[] body = "OK".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        // Use a thread pool so concurrent tile requests during panning/zooming are handled efficiently
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        LOG.info("TileProxyServer started on port " + port);
    }

    /**
     * Stops the tile proxy server.
     */
    public void stop() {
        if (server != null) {
            server.stop(1);
            LOG.info("TileProxyServer stopped");
        }
    }

    /**
     * Returns the port the server is listening on.
     */
    public int getPort() {
        return port;
    }

    /**
     * Handles tile requests in the form: /tile/{z}/{x}/{y}.png
     * Fetches the tile from OSM and returns it with CORS headers so WebView accepts it.
     */
    private class TileHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Add CORS headers required for WebView's same-origin checks
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Cache-Control", "public, max-age=86400");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            // Path format: /tile/{z}/{x}/{y}.png — strip the leading /tile/ prefix
            String path = exchange.getRequestURI().getPath();
            String tilePath = path.replaceFirst("^/tile/", "");

            // Pick OSM subdomain via round-robin to spread load across a, b, c subdomains
            String host;
            synchronized (TileProxyServer.this) {
                host = OSM_HOSTS[hostIndex % OSM_HOSTS.length];
                hostIndex++;
            }

            String osmUrl = "https://" + host + ".tile.openstreetmap.org/" + tilePath;

            try {
                URL url = new URL(osmUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", USER_AGENT);
                conn.setRequestProperty("Referer", "http://localhost:" + port + "/");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(10000);
                conn.connect();

                int responseCode = conn.getResponseCode();

                if (responseCode == 200) {
                    String contentType = conn.getContentType();
                    if (contentType == null) contentType = "image/png";

                    byte[] tileData = conn.getInputStream().readAllBytes();

                    exchange.getResponseHeaders().add("Content-Type", contentType);
                    exchange.sendResponseHeaders(200, tileData.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(tileData);
                    }
                } else {
                    // Forward non-200 responses (e.g. 404 for out-of-range tiles)
                    exchange.sendResponseHeaders(responseCode, -1);
                }

                conn.disconnect();

            } catch (Exception e) {
                LOG.warning("Tile fetch failed for " + osmUrl + ": " + e.getMessage());
                exchange.sendResponseHeaders(502, -1);
            }
        }
    }
}