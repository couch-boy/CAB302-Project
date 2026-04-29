package com.example.cab302project;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

public class AppTest {
    /*
    JACK APP TESTING START
     */
    // Test 1: CrimeRecord coordinate validation
    // Checks that setLocation() rejects out-of-range coordinates and keeps
    // the original valid values. Protects map rendering from bad data.
    @Test
    void testCrimeRecordCoordinateValidation() {
        CrimeRecord crime = new CrimeRecord(
                1, CrimeCategory.ASSAULT, LocalDateTime.now(),
                -27.4709, 153.0235, "Test", "user1", false
        );

        // Valid coordinates should update
        crime.setLocation(-33.8688, 151.2093);
        assertEquals(-33.8688, crime.getLatitude(), 0.0001);
        assertEquals(151.2093, crime.getLongitude(), 0.0001);

        // Invalid latitude (outside -90 to 90) should not update
        crime.setLocation(999.0, 153.0);
        assertEquals(-33.8688, crime.getLatitude(), 0.0001);

        // Invalid longitude (outside -180 to 180) should not update
        crime.setLocation(-27.0, 999.0);
        assertEquals(151.2093, crime.getLongitude(), 0.0001);
    }

    // Test 2: CrimeCategory severity mapping
    // Checks that each severity tier maps correctly to its category.
    // Used throughout the UI to colour code crime markers and list items.
    @Test
    void testCrimeCategorySeverityMapping() {
        // Low severity
        assertEquals(CrimeCategory.Severity.LOW, CrimeCategory.NOISE.getSeverity());
        assertEquals(CrimeCategory.Severity.LOW, CrimeCategory.GRAFFITI.getSeverity());

        // Medium severity
        assertEquals(CrimeCategory.Severity.MEDIUM, CrimeCategory.ASSAULT.getSeverity());
        assertEquals(CrimeCategory.Severity.MEDIUM, CrimeCategory.ROBBERY.getSeverity());

        // Critical severity
        assertEquals(CrimeCategory.Severity.CRITICAL, CrimeCategory.HOMICIDE.getSeverity());
        assertEquals(CrimeCategory.Severity.CRITICAL, CrimeCategory.ARSON.getSeverity());
    }

    // Test 3: UserSession login, access, and logout
    // Checks the session lifecycle: login creates a session, isPolice() returns
    // the correct value, and logout clears the session.
    @Test
    void testUserSessionLifecycle() {
        // Create a public user and log in
        User publicUser = new User(
                "testuser", "pass123", "test@email.com",
                "0400000000", -27.4709, 153.0235, false, UserType.REGULAR
        );
        UserSession.login(publicUser);

        assertNotNull(UserSession.getInstance());
        assertEquals("testuser", UserSession.getInstance().getUser().getUsername());
        assertFalse(UserSession.isPolice());

        // Log out and verify session is cleared
        UserSession.logout();
        assertNull(UserSession.getInstance());

        // Log in as police and verify isPolice() returns true
        User policeUser = new User(
                "officer1", "secure", "police@law.com",
                "0411111111", -27.4709, 153.0235, false, UserType.POLICE
        );
        UserSession.login(policeUser);
        assertTrue(UserSession.isPolice());

        // Clean up
        UserSession.logout();
    }

    // Test 4: UIUtils email and phone validation
    // Checks the input validators used on registration and profile screens.
    // Bad input here would allow corrupt data into the database.
    @Test
    void testInputValidation() {
        // Valid email formats
        assertTrue(UIUtils.isValidEmail("user@example.com"));
        assertTrue(UIUtils.isValidEmail("user.name+tag@domain.co"));

        // Invalid email formats
        assertFalse(UIUtils.isValidEmail("notanemail"));
        assertFalse(UIUtils.isValidEmail("missing@"));
        assertFalse(UIUtils.isValidEmail(null));

        // Valid phone - exactly 10 digits
        assertTrue(UIUtils.isValidPhone("0412345678"));
        assertTrue(UIUtils.isValidPhone("0298765432"));

        // Invalid phone formats
        assertFalse(UIUtils.isValidPhone("123"));
        assertFalse(UIUtils.isValidPhone("04123456789")); // 11 digits
        assertFalse(UIUtils.isValidPhone("041234567a")); // contains a letter
        assertFalse(UIUtils.isValidPhone(null));
    }

    // Test 5: CrimeRecord anonymous reporter display
    // Checks that getReporterDisplayName() returns "Anonymous" for null or
    // empty reporters. Displayed in the crime detail panel and list view.
    @Test
    void testReporterDisplayName() {
        // Null reporter should display as Anonymous
        CrimeRecord nullReporter = new CrimeRecord(
                1, CrimeCategory.NOISE, LocalDateTime.now(),
                -27.4709, 153.0235, "desc", null, false
        );
        assertEquals("Anonymous", nullReporter.getReporterDisplayName());

        // Empty string reporter should display as Anonymous
        CrimeRecord emptyReporter = new CrimeRecord(
                2, CrimeCategory.NOISE, LocalDateTime.now(),
                -27.4709, 153.0235, "desc", "", false
        );
        assertEquals("Anonymous", emptyReporter.getReporterDisplayName());

        // Named reporter should display their username
        CrimeRecord namedReporter = new CrimeRecord(
                3, CrimeCategory.NOISE, LocalDateTime.now(),
                -27.4709, 153.0235, "desc", "jack123", false
        );
        assertEquals("jack123", namedReporter.getReporterDisplayName());
    }

    /*
    JACK APP TESTING END
     */

    /*
    JORDAN APP TESTING START
     */

    // Test 6: DAO returns crime list (basic data integrity)
    // Ensures the database layer is returning a non-null list.
    @Test
    void testDAOGetAllCrimesNotNull() {
        IAppDAO dao = HelloApplication.DATABASE;

        assertNotNull(dao.getAllCrimes(), "Crime list should not be null");
    }

    // Test 7: Filtering crimes by current user (MyReports logic)
    // Ensures only crimes reported by the logged-in user are selected.
    @Test
    void testMyReportsFiltering() {
        IAppDAO dao = HelloApplication.DATABASE;

        User user = new User(
                "jordan", "pass", "j@email.com",
                "0400000000", 0, 0, false, UserType.REGULAR
        );
        UserSession.login(user);

        String currentUser = UserSession.getInstance().getUser().getUsername();

        var filtered = dao.getAllCrimes().stream()
                .filter(c -> currentUser.equals(c.getReporter()))
                .toList();

        for (CrimeRecord crime : filtered) {
            assertEquals("jordan", crime.getReporter());
        }

        UserSession.logout();
    }

    // Test 8: CrimeRecord timestamp is stored correctly
    // Ensures timestamps are not altered after creation.
    @Test
    void testCrimeRecordTimestampIntegrity() {
        LocalDateTime now = LocalDateTime.now();

        CrimeRecord crime = new CrimeRecord(
                10, CrimeCategory.ROBBERY, now,
                -27.4, 153.0, "desc", "user", false
        );

        assertEquals(now, crime.getTimestamp());
    }

    // Test 9: Actioned status toggling
    // Ensures the crime status flag behaves correctly.
    @Test
    void testCrimeActionedStatus() {
        CrimeRecord crime = new CrimeRecord(
                5, CrimeCategory.ASSAULT, LocalDateTime.now(),
                -27.4, 153.0, "desc", "user", false
        );

        assertFalse(crime.isActioned());

        crime.setActioned(true);

        assertTrue(crime.isActioned());
    }

    // Test 10: CrimeCategory enum contains expected values
    // Ensures core categories exist (prevents accidental enum removal).
    @Test
    void testCrimeCategoryExists() {
        assertNotNull(CrimeCategory.valueOf("ASSAULT"));
        assertNotNull(CrimeCategory.valueOf("ROBBERY"));
        assertNotNull(CrimeCategory.valueOf("NOISE"));
    }

    /*
    JORDAN APP TESTING END
     */

    /*
    FINN APP TESTING START
     */

    // Test 11: User.setHomeLocation() rejects out-of-range coordinates
    // This test verifies that invalid lat/lon values are silently rejected
    // and the original valid coordinates are preserved
    @Test
    void testUserSetHomeLocationValidation() {
        User user = new User(
                "alice", "pass", "alice@email.com",
                "0400000000", -27.4709, 153.0235, false, UserType.REGULAR
        );

        // Valid coordinates should update
        user.setHomeLocation(-33.8688, 151.2093);
        assertEquals(-33.8688, user.getHomeLatitude(), 0.0001);
        assertEquals(151.2093, user.getHomeLongitude(), 0.0001);

        // Invalid latitude (outside -90 to 90) should not update
        user.setHomeLocation(999.0, 151.2093);
        assertEquals(-33.8688, user.getHomeLatitude(), 0.0001);

        // Invalid longitude (outside -180 to 180) should not update
        user.setHomeLocation(-33.8688, 999.0);
        assertEquals(151.2093, user.getHomeLongitude(), 0.0001);

        // Both invalid should not update either field
        user.setHomeLocation(-999.0, -999.0);
        assertEquals(-33.8688, user.getHomeLatitude(), 0.0001);
        assertEquals(151.2093, user.getHomeLongitude(), 0.0001);
    }

    // Test 12: User.isPolice() returns correct value based on UserType
    // Verifies User.isPolice() returns true only for police type users,
    // and UserType.toString() returns readable display name
    @Test
    void testUserTypeAndIsPolice() {
        User regularUser = new User(
                "bob", "pass", "bob@email.com",
                "0400000001", 0, 0, false, UserType.REGULAR
        );
        User policeUser = new User(
                "officer", "pass", "cop@law.com",
                "0400000002", 0, 0, false, UserType.POLICE
        );

        assertFalse(regularUser.isPolice());
        assertTrue(policeUser.isPolice());

        // UserType.toString() should return display name, not enum name
        assertEquals("Regular User",    UserType.REGULAR.toString());
        assertEquals("Police Officer",  UserType.POLICE.toString());
        assertEquals("Regular User",    UserType.REGULAR.getDisplayName());
    }

    // Test 13: UIUtils database date formatting round-trip
    // Verifies LocalDateTime stays intact through database and back to string
    @Test
    void testDatabaseDateTimeRoundTrip() {
        LocalDateTime original = LocalDateTime.of(2024, 11, 5, 14, 30, 0);

        // Format to DB string then parse back - should match exactly
        String dbString = UIUtils.formatForDb(original);
        assertEquals("2024-11-05 14:30:00", dbString);

        LocalDateTime parsed = UIUtils.parseFromDb(dbString);
        assertEquals(original, parsed);

        // Midnight edge case
        LocalDateTime midnight = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
        assertEquals("2025-01-01 00:00:00", UIUtils.formatForDb(midnight));
        assertEquals(midnight, UIUtils.parseFromDb("2025-01-01 00:00:00"));
    }

    // Test 14: UIUtils.formatLocalDateTime() produces the correct UI display format
    // Verifies date format used and null inputs return empty string
    @Test
    void testUIDisplayDateFormatting() {
        LocalDateTime dt = LocalDateTime.of(2024, 6, 15, 9, 5);
        assertEquals("15/06/2024 09:05", UIUtils.formatLocalDateTime(dt));

        // Single-digit day and month should be zero-padded
        LocalDateTime padded = LocalDateTime.of(2025, 3, 7, 8, 0);
        assertEquals("07/03/2025 08:00", UIUtils.formatLocalDateTime(padded));

        // Null input should return empty string, not throw an exception
        assertEquals("", UIUtils.formatLocalDateTime(null));
    }

    // Test 15: User username is immutable after construction
    // Verifies user cannot change username and it always returns original username
    @Test
    void testUserUsernameIsImmutable() {
        User user = new User(
                "charlie", "pass", "c@email.com",
                "0400000003", 0, 0, false, UserType.REGULAR
        );

        assertEquals("charlie", user.getUsername());

        // Confirm no setUsername method exists on User
        boolean hasSetUsername = false;
        for (java.lang.reflect.Method m : User.class.getMethods()) {
            if (m.getName().equals("setUsername")) {
                hasSetUsername = true;
                break;
            }
        }
        assertFalse(hasSetUsername, "User should not have a setUsername() method");
    }

    /*
    FINN APP TESTING END
     */


}