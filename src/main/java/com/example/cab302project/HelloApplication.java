package com.example.cab302project;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;


public class HelloApplication extends Application {


    // Constants defining the window title and size
    public static final String TITLE = "CAB302 Project";
    public static final int WIDTH = 640;
    public static final int HEIGHT = 360;

    // Create a single instance of the DAO
    public static final SqliteDAO DATABASE = new SqliteDAO();

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), WIDTH, HEIGHT);
        String css = HelloApplication.class.getResource("style.css").toExternalForm();
        scene.getStylesheets().add(css);
        stage.setTitle(TITLE);
        stage.setScene(scene);
        stage.show();
    }
}
