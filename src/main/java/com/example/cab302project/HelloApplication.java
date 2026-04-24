package com.example.cab302project;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {

    public static final String TITLE = "RADIUS";
    public static final int WIDTH = 420;
    public static final int HEIGHT = 700;

    // Single shared DAO instance
    public static final IAppDAO DATABASE = new SqliteDAO();

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