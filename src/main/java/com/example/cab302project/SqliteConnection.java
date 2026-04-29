package com.example.cab302project;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages a single, shared connection to the SQLite database.
 * This class implements a thread-safe Singleton pattern to ensure that the
 * application maintains only one active connection to the database file at any time,
 * preventing file-locking issues.
 */
public class SqliteConnection {

    /** The single active connection instance. */
    private static Connection instance = null;

    /**
     * Private constructor to prevent external instantiation.
     * Enforces the use of {@link #getInstance()} to access the database.
     */
    private SqliteConnection() {}

    /**
     * Retrieves the active database connection.
     * If the connection does not exist or has been closed, a new one is established.
     *
     * <p>The method is <b>synchronized</b> to ensure that multiple threads (e.g.,
     * background tasks) do not attempt to open separate connections simultaneously.</p>
     *
     * @return The active {@link Connection} to 'data.db'; returns null if a
     *         connection could not be established.
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