package com.example.cab302project;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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
        //insertSampleCrimeData();
        createCrimesTable();
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

    //create crimes table if it does not exist
    private void createCrimesTable() {
        try {
            Statement statement = connection.createStatement();

            //enable foreign key support
            statement.execute("PRAGMA foreign_keys = ON;");

            String query = "CREATE TABLE IF NOT EXISTS crimes ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "category VARCHAR NOT NULL,"
                    + "severity VARCHAR NOT NULL,"
                    + "reporter VARCHAR NOT NULL,"
                    + "description TEXT,"
                    + "latitude REAL NOT NULL,"
                    + "longitude REAL NOT NULL,"
                    + "FOREIGN KEY (reporter) REFERENCES users(username)"
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

    //insert sample user data for testing
    private void insertSampleCrimeData() {
        try {
            // Clear before inserting
            Statement statement = connection.createStatement();
            statement.execute("DELETE FROM crimes");
            String insertQuery = "INSERT INTO crimes (category, severity, reporter, description, latitude, longitude) VALUES "
                    + "('THEFT', 'LOW', 'test1', 'Sample Description', '-27.4765', '153.0288'),"
                    + "('VANDALISM', 'LOW', 'test1', 'Sample Description', '-27.4818', '153.0230'),"
                    + "('THEFT', 'MEDIUM', 'test2', 'Sample Description', '-27.4750', '153.0250'),"
                    + "('OTHER', 'LOW', 'test2', 'Sample Description', '-27.4800', '153.0300'),"
                    + "('ASSAULT', 'HIGH', 'test3', 'Sample Description', '-27.4850', '153.0350'),"
                    + "('THEFT', 'MEDIUM', 'test3', 'Sample Description', '-27.4900', '153.040')";
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

    public boolean addCrime(CrimeCategory category, CrimeSeverity severity,
                            String description, double latitude, double longitude) {

        String currentUsername = UserSession.getInstance().getUser().getUsername();
        String query = "INSERT INTO crimes (category, severity, reporter, description, latitude, longitude) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, category.name());
            preparedStatement.setString(2, severity.name());
            preparedStatement.setString(3, currentUsername);
            preparedStatement.setString(4, description);
            preparedStatement.setDouble(5, latitude);
            preparedStatement.setDouble(6, longitude);

            preparedStatement.executeUpdate();
            return true;
        } catch (SQLException e) {
            //this will trigger if the username (primary key) already exists
            e.printStackTrace();
            return false;
        }
    }

    public CrimeRecord getCrimeById(int id) {
        String query = "SELECT * FROM crimes WHERE id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, id);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {

                //if crime is found, create a crimerecord object with the data from db
                if (resultSet.next()) {
                    return mapResultSetToCrime(resultSet);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<CrimeRecord> getCrimesByUser(String username) {
        String query = "SELECT * FROM crimes WHERE reporter = ?";
        List<CrimeRecord> crimesList = new ArrayList<>();

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, username);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {

                //if crime is found, create a crimerecord object with the data from db
                while (resultSet.next()) {
                    crimesList.add(mapResultSetToCrime(resultSet));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return crimesList;
    }

    public List<CrimeRecord> getAllCrimes() {
        String query = "SELECT * FROM crimes";
        List<CrimeRecord> crimesList = new ArrayList<>();

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            try (ResultSet resultSet = preparedStatement.executeQuery()) {

                //if crime is found, create a crimerecord object with the data from db
                while (resultSet.next()) {
                    crimesList.add(mapResultSetToCrime(resultSet));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return crimesList;
    }

    public boolean updateCrime(CrimeRecord crime) {
        //sql statement to update the fields for a specific id
        String query = "UPDATE crimes SET category = ?, severity = ?, description = ?, latitude = ?, longitude = ? WHERE id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            // Map the object data to the query parameters
            preparedStatement.setString(1, crime.getCategory().name());
            preparedStatement.setString(2, crime.getSeverity().name());
            preparedStatement.setString(3, crime.getDescription());
            preparedStatement.setDouble(4, crime.getLatitude());
            preparedStatement.setDouble(5, crime.getLongitude());
            //get key to find the right row
            preparedStatement.setInt(6, crime.getId());

            //executeUpdate returns the number of rows affected
            int rowsAffected = preparedStatement.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error updating crime: " + e.getMessage());
            return false;
        }
    }

    private CrimeRecord mapResultSetToCrime(ResultSet resultSet) throws SQLException {
        return new CrimeRecord(
                resultSet.getInt("id"),
                CrimeCategory.valueOf(resultSet.getString("category")),
                CrimeSeverity.valueOf(resultSet.getString("severity")),
                resultSet.getString("reporter"), // Note: Ensure this matches your DB column name
                resultSet.getString("description"),
                resultSet.getDouble("latitude"),
                resultSet.getDouble("longitude")
        );
    }

}
