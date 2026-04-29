package com.example.cab302project;

import javafx.application.Application;

/**
 * Entry point for launching the JavaFX application.
 *
 * Delegates to {@link HelloApplication} via {@link Application#launch}.
 */
public class Launcher {
    public static void main(String[] args) {
        Application.launch(HelloApplication.class, args);
    }
}
