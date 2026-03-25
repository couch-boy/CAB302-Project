package com.example.cab302project;

import java.sql.*;

public class SqliteDAO {
    //database connection
    private Connection connection;

    //created DAO and get db instance, then ensure tables exist
    //populate tables with sample data
    public SqliteDAO() {
        connection = SqliteConnection.getInstance();
        createUserTable();
        //uncomment to insert sample user data to data.db file
        //insertSampleUserData();
    }

    //create user table if it does not exist
    private void createUserTable() {
        try {
            Statement statement = connection.createStatement();
            String query = "CREATE TABLE IF NOT EXISTS users ("
                    + "username VARCHAR PRIMARY KEY NOT NULL,"
                    + "password VARCHAR NOT NULL,"
                    + "email VARCHAR NOT NULL,"
                    + "phone VARCHAR NOT NULL"
                    + ")";
            statement.execute(query);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //insert sample user data for testing
    private void insertSampleUserData() {
        try {
            // Clear before inserting
            Statement statement = connection.createStatement();
            statement.execute("DELETE FROM users");
            String insertQuery = "INSERT INTO users (username, password, email, phone) VALUES "
                    + "('test1', 'test', 'johndoe@example.com', '0423423423'),"
                    + "('test2', 'test', 'janedoe@example.com', '0423423424'),"
                    + "('test3', 'test', 'jaydoe@example.com', '0423423425')";
            statement.execute(insertQuery);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public User validateUser(String username, String password) {
        String query = "SELECT * FROM users WHERE username = ? AND password = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, password);

            ResultSet resultSet = preparedStatement.executeQuery();

            //if user is found, create a User object with the data from db
            if (resultSet.next()) {
                return new User(
                        resultSet.getString("username"),
                        resultSet.getString("password"),
                        resultSet.getString("email"),
                        resultSet.getString("phone")
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean addUser(String username, String password, String email, String phone) {
        String query = "INSERT INTO users (username, password, email, phone) VALUES (?, ?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, password);
            preparedStatement.setString(3, email);
            preparedStatement.setString(4, phone);

            preparedStatement.executeUpdate();
            return true;
        } catch (SQLException e) {
            //this will trigger if the username (primary key) already exists
            e.printStackTrace();
            return false;
        }
    }

}
