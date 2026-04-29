package com.example.cab302project;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * The main entry point for the RADIUS JavaFX application.
 * This class handles the initial window setup, global configuration constants,
 * and maintains the singleton-style database access point for the entire application.
 */
public class HelloApplication extends Application {

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
     * Initializes and displays the primary stage (window) of the application.
     * Loads the initial login view from FXML and applies global window settings.
     *
     * @param stage The primary stage for this application, onto which the scene is set.
     * @throws IOException If the initial FXML view file cannot be located or loaded.
     */
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), WIDTH, HEIGHT);
        stage.setTitle(TITLE);
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }
}