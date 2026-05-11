package com.example.cab302project;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * The main entry point for the RADIUS JavaFX application.
 * This class handles the initial window setup, global configuration constants,
 * and maintains the singleton-style database access point for the entire application.
 *
 * It also manages the lifecycle of the local tile proxy server, which is required
 * for reliable OSM map tile loading on both macOS and Windows via JavaFX WebView.
 */
public class HelloApplication extends Application {

    private static final Logger LOG = Logger.getLogger(HelloApplication.class.getName());

    /** The display title of the application window. */
    public static final String TITLE = "RADIUS";

    /** The default width of the application window in pixels. */
    public static final int WIDTH = 420;

    /** The default height of the application window in pixels. */
    public static final int HEIGHT = 700;

    /**
     * The shared Data Access Object instance used across the application.
     * All controllers should reference this static instance to interact with the database.
     */
    public static final IAppDAO DATABASE = new SqliteDAO();

    /**
     * The local tile proxy server instance. Started before the first scene loads,
     * stopped when the application exits. Controllers use getTileServerPort() to
     * build the tile URL for WebView.
     */
    private static TileProxyServer tileProxyServer;

    /**
     * Returns the port the local OSM tile proxy is running on.
     * Returns -1 if the server failed to start.
     */
    public static int getTileServerPort() {
        if (tileProxyServer == null) return -1;
        return tileProxyServer.getPort();
    }

    /**
     * Initializes and displays the primary stage (window) of the application.
     * Loads the initial login view from FXML and applies global window settings.
     *
     * @param stage The primary stage for this application, onto which the scene is set.
     * @throws IOException If the initial FXML view file cannot be located or loaded.
     */
    @Override
    public void start(Stage stage) throws IOException {
        // Start the tile proxy server before loading any scene with a map
        startTileProxy();

        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), WIDTH, HEIGHT);
        stage.setTitle(TITLE);
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Called by the JavaFX runtime when the application is shutting down.
     * Ensures the tile proxy server is properly stopped to free the port.
     */
    @Override
    public void stop() {
        stopTileProxy();
    }

    /**
     * Creates and starts the TileProxyServer on a random free port.
     * If startup fails, tileProxyServer is left null and maps fall back to direct OSM.
     */
    private void startTileProxy() {
        tileProxyServer = new TileProxyServer();
        try {
            tileProxyServer.start();
            LOG.info("OSM tile proxy started on port " + tileProxyServer.getPort());
        } catch (IOException e) {
            LOG.warning("Could not start tile proxy server: " + e.getMessage()
                    + " Maps may not load on macOS.");
            tileProxyServer = null;
        }
    }

    /**
     * Stops the TileProxyServer if it is running.
     */
    private void stopTileProxy() {
        if (tileProxyServer != null) {
            tileProxyServer.stop();
            tileProxyServer = null;
        }
    }
}