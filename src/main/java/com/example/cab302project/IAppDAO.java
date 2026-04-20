package com.example.cab302project;

import java.util.List;

public interface IAppDAO {
    // User Operations

    /**
     * Used to Validate a username and password, such as during login
     * @param username String containing username to validate
     * @param password String containing validation password
     * @return Returns User object if validation was successful, else returns null
     */
    User validateUser(String username, String password);

    /**
     * Used to add a new user to the database
     * @param user User object to be added
     * @return Returns true if user was successfully added, or false otherwise
     */
    boolean addUser(User user);

    /**
     * Used to update stored user details
     * @param user User object containing updated user data
     * @return Returns true if user was successfully updated, or false otherwise
     */
    boolean updateUser(User user);

    // Crime Operations

    /**
     * Used to add a new crime to the database
     * @param crime CrimeRecord object containing crime details
     * @return Returns true if crime was successfully added, or false otherwise
     */
    boolean addCrime(CrimeRecord crime);

    /**
     * Used to update stored crime details
     * @param crime CrimeRecord object containing updated user data
     * @return Returns true if crime was successfully updated, or false otherwise
     */
    boolean updateCrime(CrimeRecord crime);

    /**
     * Used to delete stored crime details
     * @param id ID value of the crime to be deleted
     * @return Returns true if the crime was deleted successfully, or false otherwise
     */
    boolean deleteCrime(int id);

    /**
     * Used to return CrimeRecord containing the crime with the specified ID
     * @param id ID value of the desired crime
     * @return Returns a CrimeRecord object if the ID is valid, or null otherwise
     */
    CrimeRecord getCrimeById(int id);

    /**
     * Used to return a list containing all CrimeRecords submitted by the specified user
     * @param username The username to search for
     * @return Returns a list of CrimeRecord objects if username is found, or an empty list otherwise
     */
    List<CrimeRecord> getCrimesByUser(String username);

    /**
     * Used to return a list containing all reported crimes
     * @return Returns a list of CrimeRecord objects if any exist, or an empty list otherwise
     */
    List<CrimeRecord> getAllCrimes();
}