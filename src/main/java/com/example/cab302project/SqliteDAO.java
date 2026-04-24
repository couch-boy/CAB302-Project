package com.example.cab302project;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SqliteDAO implements IAppDAO{
    //database connection
    private Connection connection;

    //created DAO and get db instance, then ensure tables exist
    //populate tables with sample data
    public SqliteDAO() {
        connection = SqliteConnection.getInstance();
        createUserTable();
        createCrimesTable();

        //uncomment to insert sample data to data.db file
        //insertSampleUserData();
        //insertSampleCrimeData();
    }

    //create user table if it does not exist
    private void createUserTable() {
        try (Statement statement = connection.createStatement()) {
            String query = "CREATE TABLE IF NOT EXISTS users ("
                    + "username VARCHAR PRIMARY KEY NOT NULL,"
                    + "password VARCHAR NOT NULL,"
                    + "email VARCHAR NOT NULL,"
                    + "phone VARCHAR NOT NULL,"
                    + "homelatitude REAL,"
                    + "homelongitude REAL,"
                    + "darkmode BOOLEAN DEFAULT FALSE NOT NULL,"
                    + "usertype VARCHAR NOT NULL"
                    + ")";
            statement.execute(query);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //create crimes table if it does not exist
    private void createCrimesTable() {
        try (Statement statement = connection.createStatement()) {

            String query = "CREATE TABLE IF NOT EXISTS crimes ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "category VARCHAR NOT NULL,"
                    + "timestamp VARCHAR NOT NULL,"
                    + "latitude REAL NOT NULL,"
                    + "longitude REAL NOT NULL,"
                    + "description TEXT,"
                    + "reporter VARCHAR,"
                    + "actioned BOOLEAN DEFAULT FALSE NOT NULL"
                    + ")";
            statement.execute(query);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //insert sample user data for testing
    private void insertSampleUserData() {
        try (Statement statement = connection.createStatement()) {
            // Clear before inserting
            statement.execute("DELETE FROM users");
            String insertQuery = "INSERT INTO users (username, password, email, phone, homelatitude, homelongitude, darkmode, usertype) VALUES "
                    + "('QPS123', 'Password', 'police@officer.com', '0412356798', '-27.4760', '153.0280', FALSE, 'POLICE'),"
                    + "('test1', 'test', 'johndoe@example.com', '0423423423', '-27.4765', '153.0285', FALSE, 'REGULAR'),"
                    + "('test2', 'test', 'janedoe@example.com', '0423423424', '-27.4770', '153.0290', TRUE, 'REGULAR'),"
                    + "('test3', 'test', 'jaydoe@example.com', '0423423425', '-27.4775', '153.0295', FALSE, 'REGULAR')";
            statement.execute(insertQuery);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //insert sample user data for testing
    private void insertSampleCrimeData() {
        try (Statement statement = connection.createStatement()) {
            // Clear before inserting
            statement.execute("DELETE FROM crimes");
            String insertQuery = "INSERT INTO crimes (category, timestamp, latitude, longitude, description, reporter, actioned) VALUES "
                    + "('HOMICIDE', '2026-04-18 10:00:00', '-27.4765', '153.0285', 'Sample Description','test1', FALSE),"
                    + "('ARSON', '2026-04-18 10:15:00', '-27.4770', '153.0290', 'Sample Description','test1', TRUE),"
                    + "('NOISE', '2026-04-18 10:30:00', '-27.4775', '153.0295', 'Sample Description','test2', FALSE),"
                    + "('LOITERING', '2026-04-18 10:45:00', '-27.4780', '153.0300', 'Sample Description','test2', TRUE),"
                    + "('ASSAULT', '2026-04-18 11:00:00', '-27.4785', '153.0305', 'Sample Description','test3', TRUE),"
                    + "('TRESPASSING', '2026-04-18 11:15:00', '-27.4790', '153.0310', 'Sample Description','test3', FALSE)";
            statement.execute(insertQuery);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // User validation used for login request
    public User validateUser(String username, String password) {
        String query = "SELECT * FROM users WHERE username = ? AND password = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, password);

            ResultSet resultSet = preparedStatement.executeQuery();

            //if user is found, create a User object with the data from db
            if (resultSet.next()) {
                return mapResultSetToUser(resultSet);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // Add new user for user registration
    public boolean addUser(User user) {
        String query = "INSERT INTO users (username, password, email, phone, homelatitude, homelongitude, darkmode, usertype) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, user.getUsername());
            preparedStatement.setString(2, user.getPassword());
            preparedStatement.setString(3, user.getEmail());
            preparedStatement.setString(4, user.getPhone());
            preparedStatement.setDouble(5, user.getHomeLatitude());
            preparedStatement.setDouble(6, user.getHomeLongitude());
            preparedStatement.setBoolean(7, user.isDarkMode()); // This will be false by default
            preparedStatement.setString(8, user.getUserType().name()); // Ensure only regular users are added to the database

            preparedStatement.executeUpdate();
            return true;
        } catch (SQLException e) {
            //this will trigger if the username (primary key) already exists
            e.printStackTrace();
            return false;
        }
    }

    // Update stored user details
    public boolean updateUser(User user) {
        String query = "UPDATE users SET password = ?, email = ?, phone = ?, " +
                "homelatitude = ?, homelongitude = ?, darkmode = ?, usertype = ? WHERE username = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, user.getPassword());
            preparedStatement.setString(2, user.getEmail());
            preparedStatement.setString(3, user.getPhone());
            preparedStatement.setDouble(4, user.getHomeLatitude());
            preparedStatement.setDouble(5, user.getHomeLongitude());
            preparedStatement.setBoolean(6, user.isDarkMode());
            preparedStatement.setString(7, user.getUserType().name());
            preparedStatement.setString(8, user.getUsername());

            return preparedStatement.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Add new crime entry
    public boolean addCrime(CrimeRecord crime) {

        String query = "INSERT INTO crimes (category, timestamp, latitude, longitude, description, reporter) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, crime.getCategory().name());
            preparedStatement.setString(2, crime.getTimestampForDb());
            preparedStatement.setDouble(3, crime.getLatitude());
            preparedStatement.setDouble(4, crime.getLongitude());
            preparedStatement.setString(5, crime.getDescription());
            preparedStatement.setString(6, crime.getReporter());

            preparedStatement.executeUpdate();
            return true;
        } catch (SQLException e) {
            //this will trigger if the username (primary key) already exists
            e.printStackTrace();
            return false;
        }
    }

    // Update stored crime details
    public boolean updateCrime(CrimeRecord crime) {
        //sql statement to update the fields for a specific id
        String query = "UPDATE crimes SET category = ?, timestamp = ?, latitude = ?, longitude = ?, description = ?, reporter = ?, actioned = ? WHERE id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            // Map the object data to the query parameters
            preparedStatement.setString(1, crime.getCategory().name());
            preparedStatement.setString(2, crime.getTimestampForDb());
            preparedStatement.setDouble(3, crime.getLatitude());
            preparedStatement.setDouble(4, crime.getLongitude());
            preparedStatement.setString(5, crime.getDescription());
            preparedStatement.setString(6, crime.getReporter());
            preparedStatement.setBoolean(7, crime.isActioned());
            //get key to find the right row
            preparedStatement.setInt(8, crime.getId());

            //executeUpdate returns the number of rows affected
            int rowsAffected = preparedStatement.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error updating crime: " + e.getMessage());
            return false;
        }
    }

    // Delete a crime record via crime ID
    public boolean deleteCrime(int id) {
        String query = "DELETE FROM crimes WHERE id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, id);
            return preparedStatement.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Get CrimeRecord by crime ID
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

    // Get list of CrimeRecord objects reported by a specific user
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

    // Get a list of all CrimeRecord objects
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

    // Helper method to parse sql data into CrimeRecord object
    private CrimeRecord mapResultSetToCrime(ResultSet resultSet) throws SQLException {
        return new CrimeRecord(
                resultSet.getInt("id"),
                CrimeCategory.valueOf(resultSet.getString("category")),
                UIUtils.parseFromDb(resultSet.getString("timestamp")),
                resultSet.getDouble("latitude"),
                resultSet.getDouble("longitude"),
                resultSet.getString("description"),
                resultSet.getString("reporter"),
                resultSet.getBoolean("actioned")
        );
    }

    // Helper method to parse sql data into user object
    private User mapResultSetToUser(ResultSet resultSet) throws SQLException {
        return new User(
                resultSet.getString("username"),
                resultSet.getString("password"),
                resultSet.getString("email"),
                resultSet.getString("phone"),
                resultSet.getDouble("homelatitude"),
                resultSet.getDouble("homelongitude"),
                resultSet.getBoolean("darkmode"),
                UserType.valueOf(resultSet.getString("usertype"))
        );
    }

}
