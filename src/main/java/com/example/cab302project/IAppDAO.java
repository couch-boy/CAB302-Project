package com.example.cab302project;

import java.util.List;

/**
 * Interface defining the Data Access Object (DAO) contract for the application.
 * Provides abstract methods for CRUD (Create, Read, Update, Delete) operations
 * on {@link User} and {@link CrimeRecord} entities within the persistent data store.
 */
public interface IAppDAO {

    // --- User Operations ---

    /**
     * Validates a user's credentials against the data store.
     * Typically used during the login process to verify identity.
     *
     * @param username The unique username to validate.
     * @param password The password provided for validation.
     * @return A {@link User} object if credentials are correct; null if validation fails.
     */
    User validateUser(String username, String password);

    /**
     * Persists a new user in the data store.
     *
     * @param user The {@link User} object containing the account details to be saved.
     * @return true if the user was successfully added; false if the username is taken or an error occurred.
     */
    boolean addUser(User user);

    /**
     * Updates an existing user's information in the data store.
     *
     * @param user The {@link User} object containing the updated profile data.
     * @return true if the update was successful; false otherwise.
     */
    boolean updateUser(User user);

    // --- Crime Operations ---

    /**
     * Records a new crime incident in the data store.
     *
     * @param crime The {@link CrimeRecord} object containing the incident details.
     * @return true if the record was successfully created; false if an error occurred.
     */
    boolean addCrime(CrimeRecord crime);

    /**
     * Updates the details of an existing crime record.
     *
     * @param crime The {@link CrimeRecord} object containing the updated incident data.
     * @return true if the record was found and updated successfully; false otherwise.
     */
    boolean updateCrime(CrimeRecord crime);

    /**
     * Removes a crime record from the data store based on its unique identifier.
     *
     * @param id The unique primary key of the crime record to be removed.
     * @return true if the record was deleted; false if the ID was not found or an error occurred.
     */
    boolean deleteCrime(int id);

    /**
     * Retrieves a specific crime record by its unique identifier.
     *
     * @param id The unique primary key of the desired crime.
     * @return The {@link CrimeRecord} matching the ID, or null if no such record exists.
     */
    CrimeRecord getCrimeById(int id);

    /**
     * Retrieves all crime incidents reported by a specific user.
     *
     * @param username The username of the reporter to filter by.
     * @return A {@link List} of {@link CrimeRecord} objects; returns an empty list if the user has no reports.
     */
    List<CrimeRecord> getCrimesByUser(String username);

    /**
     * Retrieves every crime record currently stored in the system.
     *
     * @return A {@link List} containing all {@link CrimeRecord} objects in the data store.
     */
    List<CrimeRecord> getAllCrimes();
}