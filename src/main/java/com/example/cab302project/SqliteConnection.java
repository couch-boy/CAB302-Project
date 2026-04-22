package com.example.cab302project;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SqliteConnection {

    // Private connection instance
    private static Connection instance = null;

    // Constructor: Private for singleton database connection
    private SqliteConnection() {}

    /**
     *
     * @return
     */
    public synchronized static Connection getInstance() {
        try {
            // Check if instance is null OR if the connection was closed externally
            if (instance == null || instance.isClosed()) {
                String url = "jdbc:sqlite:data.db";
                // Re-establish connection instance
                instance = DriverManager.getConnection(url);
            }
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
        }
        // Return existing database connection instance
        return instance;
    }
}