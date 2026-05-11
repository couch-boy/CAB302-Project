package com.example.cab302project;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * Utility for loading Leaflet.js into a JavaFX WebEngine.
 *
 * The problem with using a CDN link for Leaflet in JavaFX's WebView:
 *   - On macOS, WebKit's sandbox often blocks external network requests,
 *     so the CDN script never loads and the map is blank.
 *   - On Windows it works but adds startup latency.
 *
 * Solution: bundle leaflet.min.js as a classpath resource and inject it
 * directly via engine.executeScript() once the HTML page has loaded.
 * The local tile proxy (TileProxyServer) is used for tiles on all platforms.
 *
 * The Java side also passes the WebView's measured width and height to the
 * JavaScript on every resize. The HTML uses those explicit pixel values to
 * size the map div rather than relying on percentage-based CSS, which avoids
 * Leaflet measuring the container at the wrong moment and rendering tiles
 * in misaligned positions.
 */
public class LeafletLoader {

    private static final Logger LOG = Logger.getLogger(LeafletLoader.class.getName());

    /** Cached Leaflet JS source so we only read the classpath resource once per app run. */
    private static String leafletJs = null;
    private static String leafletCss = null;

    private LeafletLoader() {}

    /**
     * Loads the given HTML map file into the WebView and, once loaded:
     * 1. Injects the bundled Leaflet CSS and JS from classpath resources.
     * 2. Calls initMap(tileProxyPort, width, height) in the HTML page using
     *    the WebView's measured pixel dimensions, so Leaflet sees the correct size.
     * 3. Registers width and height listeners so the map updates whenever the
     *    WebView is resized by JavaFX layout.
     * 4. Invokes the provided onReady callback so the caller can push data.
     *
     * @param mapView the WebView from a controller (size is read off this node)
     * @param htmlFile the filename of the HTML resource (e.g. "hotspots-map.html")
     * @param onReady callback invoked on the FX thread after initMap() succeeds
     */
    public static void loadMap(WebView mapView, String htmlFile, Runnable onReady) {
        var resource = LeafletLoader.class.getResource("/com/example/cab302project/" + htmlFile);
        if (resource == null) {
            LOG.severe("Map HTML not found: " + htmlFile);
            return;
        }

        WebEngine engine = mapView.getEngine();

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState != Worker.State.SUCCEEDED) return;

            try {
                // Inject Leaflet CSS into the page head
                String css = getLeafletCss();
                if (css != null && !css.isBlank()) {
                    String escapedCss = css.replace("\\", "\\\\").replace("`", "\\`");
                    engine.executeScript(
                            "var s = document.createElement('style');" +
                                    "s.textContent = `" + escapedCss + "`;" +
                                    "document.head.appendChild(s);"
                    );
                }

                // Inject Leaflet JS so the L global is available to the page
                String js = getLeafletJs();
                if (js != null && !js.isBlank()) {
                    engine.executeScript(js);
                }

                // Read the WebView's current measured size and pass it to initMap explicitly.
                // This avoids Leaflet relying on getBoundingClientRect, which can return stale
                // dimensions during JavaFX scene setup.
                int port = HelloApplication.getTileServerPort();
                int width = (int) Math.max(mapView.getWidth(), 100);
                int height = (int) Math.max(mapView.getHeight(), 100);
                engine.executeScript("initMap(" + port + "," + width + "," + height + ")");

                // Listen for WebView size changes. JavaFX often resizes the WebView multiple
                // times during scene setup, and each resize must be propagated to Leaflet
                // so it recalculates tile positions for the new viewport.
                mapView.widthProperty().addListener((o, oldW, newW) -> {
                    int w = (int) Math.max(newW.doubleValue(), 100);
                    int h = (int) Math.max(mapView.getHeight(), 100);
                    Platform.runLater(() -> resizeMapInBrowser(engine, w, h));
                });
                mapView.heightProperty().addListener((o, oldH, newH) -> {
                    int w = (int) Math.max(mapView.getWidth(), 100);
                    int h = (int) Math.max(newH.doubleValue(), 100);
                    Platform.runLater(() -> resizeMapInBrowser(engine, w, h));
                });

                // Notify the controller that the map is ready for data
                if (onReady != null) {
                    onReady.run();
                }

            } catch (Exception e) {
                LOG.severe("Error initialising map in WebView: " + e.getMessage());
            }
        });

        engine.load(resource.toExternalForm());
    }

    /**
     * Tells the in-page JavaScript to resize the map div to the given pixel dimensions
     * and recompute Leaflet tile positions.
     */
    private static void resizeMapInBrowser(WebEngine engine, int width, int height) {
        try {
            engine.executeScript(
                    "if (typeof resizeMap === 'function') { resizeMap(" + width + "," + height + "); }"
            );
        } catch (Exception ex) {
            LOG.warning("resizeMap call failed: " + ex.getMessage());
        }
    }

    /**
     * Returns the cached Leaflet JS string, reading from classpath on first call.
     */
    private static synchronized String getLeafletJs() {
        if (leafletJs == null) {
            leafletJs = readResource("/com/example/cab302project/leaflet/leaflet.min.js");
        }
        return leafletJs;
    }

    /**
     * Returns the cached Leaflet CSS string, reading from classpath on first call.
     */
    private static synchronized String getLeafletCss() {
        if (leafletCss == null) {
            leafletCss = readResource("/com/example/cab302project/leaflet/leaflet.min.css");
        }
        return leafletCss;
    }

    /**
     * Reads a classpath resource to a UTF-8 string.
     *
     * @param path absolute classpath path to the resource
     * @return the resource contents as a string, or null if not found
     */
    private static String readResource(String path) {
        try (InputStream is = LeafletLoader.class.getResourceAsStream(path)) {
            if (is == null) {
                LOG.warning("Resource not found: " + path);
                return null;
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.severe("Could not read resource " + path + ": " + e.getMessage());
            return null;
        }
    }
}