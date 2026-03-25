package com.example.cab302project;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

//class for establishing sqlite database connection with singleton instance
public class SqliteConnection {
    //private connection instance
    private static Connection instance = null;

    //private method to establish connection instance if noot already instantiated
    private SqliteConnection() {
        String url = "jdbc:sqlite:data.db";
        try {
            instance = DriverManager.getConnection(url);
        } catch (SQLException sqlEx) {
            System.err.println(sqlEx);
        }
    }

    //public method to return existing instance, or create one if not instantiated
    public synchronized static Connection getInstance() {
        if (instance == null) {
            new SqliteConnection();
        }
        return instance;
    }
}