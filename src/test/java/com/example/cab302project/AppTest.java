package com.example.cab302project;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.time.LocalDate;

public class AppTest {
/*
    JACK APP TESTING START
     */

    // Test 1: Valid latitude is accepted by setLocation()
    // Checks that a valid in-range latitude updates the stored value on the crime record.
    // Protects the map from silently dropping valid location updates.
    @Test
    void testSetLocationAcceptsValidLatitude() {
        CrimeRecord crime = new CrimeRecord(1, CrimeCategory.VANDALISM, LocalDateTime.now(), -27.4709, 153.0235, "Test", "jack", false);
        crime.setLocation(-33.8688, 153.0235);
        assertEquals(-33.8688, crime.getLatitude(), 0.0001);
    }

    // Test 2: Out-of-range latitude is rejected by setLocation()
    // Checks that a latitude above 90 is ignored and the original value is preserved.
    // Prevents garbage coordinates from reaching the Leaflet map.
    @Test
    void testSetLocationRejectsLatitudeAbove90() {
        CrimeRecord crime = new CrimeRecord(2, CrimeCategory.VANDALISM, LocalDateTime.now(), -27.4709, 153.0235, "Test", "jack", false);
        crime.setLocation(91.0, 153.0235);
        assertEquals(-27.4709, crime.getLatitude(), 0.0001);
    }

    // Test 3: Out-of-range longitude is rejected by setLocation()
    // Checks that a longitude above 180 is ignored and the original value is preserved.
    // Prevents invalid map pin placement from bad form input.
    @Test
    void testSetLocationRejectsLongitudeAbove180() {
        CrimeRecord crime = new CrimeRecord(3, CrimeCategory.VANDALISM, LocalDateTime.now(), -27.4709, 153.0235, "Test", "jack", false);
        crime.setLocation(-27.4709, 181.0);
        assertEquals(153.0235, crime.getLongitude(), 0.0001);
    }

    // Test 4: LOW severity categories return the correct severity enum
    // Checks that petty offence categories like GRAFFITI and LOITERING are LOW.
    // Drives the yellow colour coding on the crime map and list view.
    @Test
    void testLowSeverityCategoriesAreCorrect() {
        assertEquals(CrimeCategory.Severity.LOW, CrimeCategory.GRAFFITI.getSeverity());
        assertEquals(CrimeCategory.Severity.LOW, CrimeCategory.LOITERING.getSeverity());
        assertEquals(CrimeCategory.Severity.LOW, CrimeCategory.VANDALISM.getSeverity());
    }

    // Test 5: MEDIUM severity categories return the correct severity enum
    // Checks that offences like TRESPASSING and WEAPONS map to MEDIUM severity.
    // Drives the orange colour coding on the crime map and list view.
    @Test
    void testMediumSeverityCategoriesAreCorrect() {
        assertEquals(CrimeCategory.Severity.MEDIUM, CrimeCategory.TRESPASSING.getSeverity());
        assertEquals(CrimeCategory.Severity.MEDIUM, CrimeCategory.WEAPONS.getSeverity());
        assertEquals(CrimeCategory.Severity.MEDIUM, CrimeCategory.STALKING.getSeverity());
    }

    // Test 6: CRITICAL severity categories return the correct severity enum
    // Checks that violent offences like SEXUALOFFENCE and ARMEDROBBERY are CRITICAL.
    // Drives the red colour coding on the crime map and list view.
    @Test
    void testCriticalSeverityCategoriesAreCorrect() {
        assertEquals(CrimeCategory.Severity.CRITICAL, CrimeCategory.SEXUALOFFENCE.getSeverity());
        assertEquals(CrimeCategory.Severity.CRITICAL, CrimeCategory.ARMEDROBBERY.getSeverity());
        assertEquals(CrimeCategory.Severity.CRITICAL, CrimeCategory.BREAKINGANDENTERING.getSeverity());
    }

    // Test 7: UserSession is null before any login
    // Checks that getInstance() returns null when no user has logged in.
    // Prevents NPE if the session is accessed before authentication.
    @Test
    void testUserSessionIsNullBeforeLogin() {
        UserSession.logout();
        assertNull(UserSession.getInstance());
    }

    // Test 8: UserSession stores the logged-in user's username
    // Checks that the username returned after login matches the user that was logged in.
    // Used to display the reporter name in the crime detail panel.
    @Test
    void testUserSessionStoresUsername() {
        User user = new User("jacktest", "abc", "j@test.com", "0411223344", 0, 0, false, UserType.REGULAR);
        UserSession.login(user);
        assertEquals("jacktest", UserSession.getInstance().getUser().getUsername());
        UserSession.logout();
    }

    // Test 9: UserSession isPolice() returns false for a regular user
    // Checks that a non-police user does not pass the police permission check.
    // Prevents regular users from seeing police-only features like Save.
    @Test
    void testUserSessionIsPoliceReturnsFalseForRegularUser() {
        User user = new User("citizen1", "pass", "c@test.com", "0422334455", 0, 0, false, UserType.REGULAR);
        UserSession.login(user);
        assertFalse(UserSession.isPolice());
        UserSession.logout();
    }

    // Test 10: UIUtils.isValidEmail() accepts a standard email address
    // Checks that a properly formatted email passes validation on the register screen.
    // Prevents empty or malformed emails from being saved to the database.
    @Test
    void testIsValidEmailAcceptsStandardEmail() {
        assertTrue(UIUtils.isValidEmail("jacksheppard@gmail.com"));
    }

    // Test 11: UIUtils.isValidEmail() rejects an email with no domain
    // Checks that an email missing the domain part after @ is rejected.
    // Prevents garbage data from being stored in the users table.
    @Test
    void testIsValidEmailRejectsNoDomain() {
        assertFalse(UIUtils.isValidEmail("jacksheppard@"));
    }

    // Test 12: UIUtils.isValidPhone() accepts a valid 10-digit number
    // Checks that a standard Australian mobile number passes validation.
    // Phone number is stored in the user profile and displayed on screen.
    @Test
    void testIsValidPhoneAcceptsTenDigitNumber() {
        assertTrue(UIUtils.isValidPhone("0433221100"));
    }

    // Test 13: UIUtils.isValidPhone() rejects a number that is too short
    // Checks that a phone number with fewer than 10 digits is rejected.
    // Prevents truncated numbers from being saved to the user profile.
    @Test
    void testIsValidPhoneRejectsTooShort() {
        assertFalse(UIUtils.isValidPhone("04332211"));
    }

    // Test 14: UIUtils.isValidCoordinate() accepts Brisbane CBD coordinates
    // Checks that the default map location passes the coordinate validator.
    // Used when placing crime pins and validating form input.
    @Test
    void testIsValidCoordinateAcceptsBrisbaneCBD() {
        assertTrue(UIUtils.isValidCoordinate(-27.4709, 153.0235));
    }

    // Test 15: UIUtils.isValidCoordinate() rejects both values out of range
    // Checks that wildly invalid lat/lon fails the validator.
    // Stops bad geocoding results from being saved as crime locations.
    @Test
    void testIsValidCoordinateRejectsBothOutOfRange() {
        assertFalse(UIUtils.isValidCoordinate(999.0, 999.0));
    }

    // Test 16: SuburbSearchService.isInBoundingBox() returns true when inside
    // Checks that Brisbane CBD coordinates are detected inside a Brisbane bounding box.
    // Used to pre-filter crimes before the JS point-in-polygon check on the map.
    @Test
    void testIsInBoundingBoxReturnsTrueWhenInside() {
        // Bounding box: [minLat, maxLat, minLon, maxLon] covering Brisbane CBD
        double[] box = new double[]{-27.5000, -27.4000, 153.0000, 153.1000};
        assertTrue(SuburbSearchService.isInBoundingBox(-27.4709, 153.0235, box));
    }

    // Test 17: SuburbSearchService.isInBoundingBox() returns false when outside
    // Checks that Sydney coordinates are not detected inside a Brisbane bounding box.
    // Ensures crimes from other cities don't appear when a Brisbane suburb is searched.
    @Test
    void testIsInBoundingBoxReturnsFalseWhenOutside() {
        // Bounding box covering Brisbane CBD
        double[] box = new double[]{-27.5000, -27.4000, 153.0000, 153.1000};
        // Sydney coordinates — clearly outside the Brisbane box
        assertFalse(SuburbSearchService.isInBoundingBox(-33.8688, 151.2093, box));
    }

    // Test 18: CrimeRecord description is stored and returned correctly
    // Checks that the description passed to the constructor comes back unchanged.
    // Description is displayed in the crime detail panel and map popup.
    @Test
    void testCrimeRecordDescriptionIsStoredCorrectly() {
        String desc = "Witnessed graffiti on the bridge wall";
        CrimeRecord crime = new CrimeRecord(10, CrimeCategory.GRAFFITI, LocalDateTime.now(),
                -27.4709, 153.0235, desc, "jack", false);
        assertEquals(desc, crime.getDescription());
    }

    // Test 19: User email is stored and returned correctly
    // Checks that the email passed to the User constructor is retrievable.
    // Email is displayed on the profile screen and used for account lookup.
    @Test
    void testUserEmailIsStoredCorrectly() {
        User user = new User("jacktest2", "pass", "jack@radius.com", "0400112233", 0, 0, false, UserType.REGULAR);
        assertEquals("jack@radius.com", user.getEmail());
    }

    // Test 20: User dark mode preference is stored and returned correctly
    // Checks that the dark mode flag set at construction is readable via isDarkMode().
    // Controls the app theme and is persisted to the user profile in the database.
    @Test
    void testUserDarkModePreferenceIsStoredCorrectly() {
        User darkUser = new User("nightowl", "pass", "n@test.com", "0455667788", 0, 0, true, UserType.REGULAR);
        assertTrue(darkUser.isDarkMode());

        User lightUser = new User("daybird", "pass", "d@test.com", "0455667799", 0, 0, false, UserType.REGULAR);
        assertFalse(lightUser.isDarkMode());
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

        User user = new User("jordan", "pass", "j@email.com", "0400000000", 0, 0, false, UserType.REGULAR);
        UserSession.login(user);

        String currentUser = UserSession.getInstance().getUser().getUsername();

        var filtered = dao.getAllCrimes().stream().filter(c -> currentUser.equals(c.getReporter())).toList();

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

        CrimeRecord crime = new CrimeRecord(10, CrimeCategory.ROBBERY, now, -27.4, 153.0, "desc", "user", false);

        assertEquals(now, crime.getTimestamp());
    }

    // Test 9: Actioned status toggling
    // Ensures the crime status flag behaves correctly.
    @Test
    void testCrimeActionedStatus() {
        CrimeRecord crime = new CrimeRecord(5, CrimeCategory.ASSAULT, LocalDateTime.now(), -27.4, 153.0, "desc", "user", false);

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

    // Test 11: CrimeRecord constructor stores the core values correctly
    @Test
    void testCrimeRecordConstructorStoresCoreValues() {
        LocalDateTime timestamp = LocalDateTime.of(2025, 1, 15, 10, 30);

        CrimeRecord crime = new CrimeRecord(
                42,
                CrimeCategory.ASSAULT,
                timestamp,
                -27.4709,
                153.0235,
                "Test description",
                "jordan",
                false
        );

        assertEquals(42, crime.getId());
        assertEquals(CrimeCategory.ASSAULT, crime.getCategory());
        assertEquals(timestamp, crime.getTimestamp());
        assertEquals(-27.4709, crime.getLatitude(), 0.0001);
        assertEquals(153.0235, crime.getLongitude(), 0.0001);
        assertEquals("Test description", crime.getDescription());
        assertEquals("jordan", crime.getReporter());
        assertFalse(crime.isActioned());
    }

    // Test 12: CrimeRecord setCategory() updates the stored category
    @Test
    void testCrimeRecordSetCategoryUpdatesValue() {
        CrimeRecord crime = new CrimeRecord(
                1, CrimeCategory.NOISE, LocalDateTime.now(),
                0, 0, "desc", "user", false
        );

        crime.setCategory(CrimeCategory.ROBBERY);

        assertEquals(CrimeCategory.ROBBERY, crime.getCategory());
    }

    // Test 13: CrimeRecord setTimestamp() updates the stored time
    @Test
    void testCrimeRecordSetTimestampUpdatesValue() {
        CrimeRecord crime = new CrimeRecord(
                1, CrimeCategory.NOISE, LocalDateTime.now(),
                0, 0, "desc", "user", false
        );

        LocalDateTime newTime = LocalDateTime.of(2025, 2, 20, 14, 45);
        crime.setTimestamp(newTime);

        assertEquals(newTime, crime.getTimestamp());
    }

    // Test 14: CrimeRecord setDescription() updates the stored description
    @Test
    void testCrimeRecordSetDescriptionUpdatesValue() {
        CrimeRecord crime = new CrimeRecord(
                1, CrimeCategory.NOISE, LocalDateTime.now(),
                0, 0, "old", "user", false
        );

        crime.setDescription("new description");

        assertEquals("new description", crime.getDescription());
    }

    // Test 15: CrimeRecord setReporter() updates the stored reporter
    @Test
    void testCrimeRecordSetReporterUpdatesValue() {
        CrimeRecord crime = new CrimeRecord(
                1, CrimeCategory.NOISE, LocalDateTime.now(),
                0, 0, "desc", "oldUser", false
        );

        crime.setReporter("newUser");

        assertEquals("newUser", crime.getReporter());
    }

    // Test 16: CrimeRecord getTimestampForDb() returns SQLite-friendly formatting
    @Test
    void testCrimeRecordTimestampForDbFormatting() {
        LocalDateTime timestamp = LocalDateTime.of(2025, 3, 7, 8, 5);

        CrimeRecord crime = new CrimeRecord(
                1, CrimeCategory.ROBBERY, timestamp,
                0, 0, "desc", "user", false
        );

        assertEquals("2025-03-07 08:05:00", crime.getTimestampForDb());
    }

    // Test 17: CrimeCategory getName() returns the readable category label
    @Test
    void testCrimeCategoryGetNameReturnsReadableLabels() {
        assertEquals("Assault", CrimeCategory.ASSAULT.getName());
        assertEquals("Robbery", CrimeCategory.ROBBERY.getName());
        assertEquals("Homicide", CrimeCategory.HOMICIDE.getName());
    }

    // Test 18: CrimeCategory toString() returns the readable display label
    @Test
    void testCrimeCategoryToStringReturnsReadableLabels() {
        assertEquals("Assault", CrimeCategory.ASSAULT.toString());
        assertEquals("Domestic Abuse", CrimeCategory.DOMESTICABUSE.toString());
        assertEquals("Arson", CrimeCategory.ARSON.toString());
    }

    // Test 19: CrimeCategory severity labels are human-readable
    @Test
    void testCrimeCategorySeverityToStringReturnsReadableLabels() {
        assertEquals("Low", CrimeCategory.Severity.LOW.toString());
        assertEquals("Medium", CrimeCategory.Severity.MEDIUM.toString());
        assertEquals("Critical", CrimeCategory.Severity.CRITICAL.toString());
    }

    // Test 20: Hotspot stores latitude, longitude and count correctly
    @Test
    void testHotspotConstructorStoresLatitudeLongitudeAndCount() {
        Hotspot hotspot = new Hotspot(-27.4709, 153.0235, 4);

        assertEquals(-27.4709, hotspot.getLatitude(), 0.0001);
        assertEquals(153.0235, hotspot.getLongitude(), 0.0001);
        assertEquals(4, hotspot.getCount());
    }

    // Test 21: My Reports filtering keeps only the logged-in user's reports
    @Test
    void testMyReportsFilteringReturnsOnlyCurrentUsersReports() {
        List<CrimeRecord> crimes = List.of(
                new CrimeRecord(1, CrimeCategory.ASSAULT, LocalDateTime.now(), 0, 0, "A", "jordan", false),
                new CrimeRecord(2, CrimeCategory.ROBBERY, LocalDateTime.now(), 0, 0, "B", "jack", false),
                new CrimeRecord(3, CrimeCategory.NOISE, LocalDateTime.now(), 0, 0, "C", "jordan", true)
        );

        String currentUser = "jordan";

        List<CrimeRecord> filtered = crimes.stream()
                .filter(c -> currentUser.equals(c.getReporter()))
                .toList();

        assertEquals(2, filtered.size());
        for (CrimeRecord crime : filtered) {
            assertEquals("jordan", crime.getReporter());
        }
    }

    // Test 22: My Reports filtering returns an empty list when there are no matches
    @Test
    void testMyReportsFilteringReturnsEmptyForNonMatchingUser() {
        List<CrimeRecord> crimes = List.of(
                new CrimeRecord(1, CrimeCategory.ASSAULT, LocalDateTime.now(), 0, 0, "A", "jack", false),
                new CrimeRecord(2, CrimeCategory.ROBBERY, LocalDateTime.now(), 0, 0, "B", "finn", false)
        );

        String currentUser = "jordan";

        List<CrimeRecord> filtered = crimes.stream()
                .filter(c -> currentUser.equals(c.getReporter()))
                .toList();

        assertTrue(filtered.isEmpty());
    }

    // Test 23: Severity filter returns only CRITICAL crimes
    @Test
    void testSeverityFilteringCriticalOnly() {
        List<CrimeRecord> crimes = List.of(
                new CrimeRecord(1, CrimeCategory.HOMICIDE, LocalDateTime.now(), 0, 0, "A", "user", false),
                new CrimeRecord(2, CrimeCategory.ASSAULT, LocalDateTime.now(), 0, 0, "B", "user", false),
                new CrimeRecord(3, CrimeCategory.NOISE, LocalDateTime.now(), 0, 0, "C", "user", false)
        );

        List<CrimeRecord> filtered = crimes.stream()
                .filter(c -> c.getCategory().getSeverity() == CrimeCategory.Severity.CRITICAL)
                .toList();

        assertEquals(1, filtered.size());
        assertEquals(CrimeCategory.HOMICIDE, filtered.get(0).getCategory());
    }

    // Test 24: Status filter returns only actioned crimes
    @Test
    void testStatusFilteringActionedOnly() {
        List<CrimeRecord> crimes = List.of(
                new CrimeRecord(1, CrimeCategory.ASSAULT, LocalDateTime.now(), 0, 0, "A", "user", true),
                new CrimeRecord(2, CrimeCategory.ROBBERY, LocalDateTime.now(), 0, 0, "B", "user", false),
                new CrimeRecord(3, CrimeCategory.NOISE, LocalDateTime.now(), 0, 0, "C", "user", true)
        );

        List<CrimeRecord> filtered = crimes.stream()
                .filter(CrimeRecord::isActioned)
                .toList();

        assertEquals(2, filtered.size());

        for (CrimeRecord crime : filtered) {
            assertTrue(crime.isActioned());
        }
    }

    // Test 25: Date filter returns only crimes from the last 7 days
    @Test
    void testDateFilteringLastSevenDays() {
        List<CrimeRecord> crimes = List.of(
                new CrimeRecord(1, CrimeCategory.ASSAULT,
                        LocalDateTime.now().minusDays(2),
                        0, 0, "Recent", "user", false),

                new CrimeRecord(2, CrimeCategory.ROBBERY,
                        LocalDateTime.now().minusDays(10),
                        0, 0, "Old", "user", false)
        );

        LocalDate today = LocalDate.now();

        List<CrimeRecord> filtered = crimes.stream()
                .filter(c -> {
                    LocalDate crimeDate = c.getTimestamp().toLocalDate();
                    return !crimeDate.isBefore(today.minusDays(6))
                            && !crimeDate.isAfter(today);
                })
                .toList();

        assertEquals(1, filtered.size());
        assertEquals("Recent", filtered.get(0).getDescription());
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
        User user = new User("alice", "pass", "alice@email.com", "0400000000", -27.4709, 153.0235, false, UserType.REGULAR);

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
        User regularUser = new User("bob", "pass", "bob@email.com", "0400000001", 0, 0, false, UserType.REGULAR);
        User policeUser = new User("officer", "pass", "cop@law.com", "0400000002", 0, 0, false, UserType.POLICE);

        assertFalse(regularUser.isPolice());
        assertTrue(policeUser.isPolice());

        // UserType.toString() should return display name, not enum name
        assertEquals("Regular User", UserType.REGULAR.toString());
        assertEquals("Police Officer", UserType.POLICE.toString());
        assertEquals("Regular User", UserType.REGULAR.getDisplayName());
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
        User user = new User("charlie", "pass", "c@email.com", "0400000003", 0, 0, false, UserType.REGULAR);

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

    /*
    MITCHELL APP TESTING START
     */

    // Test 16: Test UserSession isDarkMode() helper method safety
    // Verifies that UI will not crash when checking theme when no user is logged in (null session)
    @Test
    void testUserSessionDarkModeSafety() {
        // Ensure logged out first
        UserSession.logout();

        // Test 1: Should return false (default) when no session exists, not throw NullPointerException
        assertFalse(UserSession.isDarkMode(), "Should default to false if no session exists");

        // Test 2: Should return true when user has dark mode enabled
        User user = new User("user", "pass", "m@email.com", "0400000000", 0, 0, true, UserType.REGULAR);
        UserSession.login(user);
        assertTrue(UserSession.isDarkMode());

        UserSession.logout();
    }

    // Test 17: Test 12hr -> 24hr time conversion logic
    // Verifies that boundary hours (midnight and midday) are correctly converted
    // Uses identical logic to createRecordFromForm() from CrimesController
    @Test
    void testTimeConversionLogic() {
        // Logic: 12 AM should be 00:00
        int hourAM = 12;
        String ampmAM = "AM";
        int convertedAM = (ampmAM.equals("AM") && hourAM == 12) ? 0 : hourAM;
        assertEquals(0, convertedAM);

        // Logic: 12 PM should be 12:00
        int hourPM = 12;
        String ampmPM = "PM";
        int convertedPM = (ampmPM.equals("PM") && hourPM == 12) ? 12 : hourPM + 12;
        // CrimesController Logic: if (ampm.equals("PM") && hour < 12) hour += 12;
        // Verification:
        int h = 12;
        if ("PM".equals("PM") && h < 12) h += 12;
        assertEquals(12, h); // 12 PM stays 12

        h = 1;
        if ("PM".equals("PM") && h < 12) h += 12;
        assertEquals(13, h); // 1 PM becomes 13
    }

    // Test 18: Test stored data integrity of anonymous CrimeRecord
    // Verifies the preservation of a null value in database for anonymous CrimeRecords
    // while still providing a clean value of "Anonymous" for UI elements
    @Test
    void testReporterDataIntegrity() {
        CrimeRecord crime = new CrimeRecord(1, CrimeCategory.OTHER, LocalDateTime.now(), 0, 0, "Desc", null, false);

        // Raw value for DB should remain null
        assertNull(crime.getReporter());

        // UI value should be "Anonymous"
        assertEquals("Anonymous", crime.getReporterDisplayName());
    }

    // Test 19: Test regex parsing latitude and longitude
    // Verifies "lat,lon" (no space) and "lat,   lon" (multiple spaces) are correctly parsed
    @Test
    void testCoordinateRegexParsing() {
        String inputNoSpace = "-27.47,153.02";
        String inputWithSpaces = "-27.47,   153.02";

        String[] parts1 = inputNoSpace.split(",\\s*");
        String[] parts2 = inputWithSpaces.split(",\\s*");

        assertEquals(2, parts1.length);
        assertEquals("-27.47", parts1[0]);
        assertEquals("153.02", parts1[1]);

        assertEquals(2, parts2.length);
        assertEquals("-27.47", parts2[0]);
        assertEquals("153.02", parts2[1]);
    }

    // Test 20: Test user permissions are correctly identified via UserType enum, UserSession, and isPolice() helper
    // Verifies that a regular user is not given police permissions, and that a police user is correctly given police permissions
    @Test
    void testRolePermissions() {
        User regUser = new User("u1", "p", "e", "0", 0, 0, false, UserType.REGULAR);
        UserSession.login(regUser);
        assertFalse(UserSession.isPolice(), "Regular users should not have police permissions");

        User policeUser = new User("p1", "p", "e", "0", 0, 0, false, UserType.POLICE);
        UserSession.login(policeUser);
        assertTrue(UserSession.isPolice(), "Police users must return true for isPolice()");

        UserSession.logout();
    }

    /*
    MITCHELL APP TESTING END
     */


    /*
    MAAN APP TESTING START
     */
    private CrimeRecord maanCrime(int id, CrimeCategory category, double latitude, double longitude) {
        return new CrimeRecord(id, category, LocalDateTime.now(), latitude, longitude, "Maan test crime", "maan", false);
    }

    private double invokeDistanceKm(double lat1, double lon1, double lat2, double lon2) throws Exception {
        HotspotsController controller = new HotspotsController();
        var method = HotspotsController.class.getDeclaredMethod("distanceKm", double.class, double.class, double.class, double.class);
        method.setAccessible(true);
        return (double) method.invoke(controller, lat1, lon1, lat2, lon2);
    }

    private List<Hotspot> invokeBuildHotspots(List<CrimeRecord> crimes, double radiusKm) throws Exception {
        HotspotsController controller = new HotspotsController();
        var method = HotspotsController.class.getDeclaredMethod("buildHotspots", List.class, double.class);
        method.setAccessible(true);
        return (List<Hotspot>) method.invoke(controller, crimes, radiusKm);
    }

    private String invokeBuildHotspotJson(List<Hotspot> hotspots) throws Exception {
        HotspotsController controller = new HotspotsController();
        var method = HotspotsController.class.getDeclaredMethod("buildHotspotJson", List.class);
        method.setAccessible(true);
        return (String) method.invoke(controller, hotspots);
    }

    // Test 1: distanceKm returns 0 for identical coordinates.
    @Test
    void testHotspotDistanceKmSameLocationIsZero() throws Exception {
        double distance = invokeDistanceKm(-27.4709, 153.0235, -27.4709, 153.0235);

        assertEquals(0.0, distance, 0.0001);
    }

    // Test 2: distanceKm returns a positive value for different coordinates.
    @Test
    void testHotspotDistanceKmDifferentLocationsIsPositive() throws Exception {
        double distance = invokeDistanceKm(-27.4709, 153.0235, -27.4710, 153.0236);

        assertTrue(distance > 0);
    }

    // Test 3: distanceKm gives the same result when locations are reversed.
    @Test
    void testHotspotDistanceKmIsSymmetric() throws Exception {
        double forward = invokeDistanceKm(-27.4709, 153.0235, -33.8688, 151.2093);
        double backward = invokeDistanceKm(-33.8688, 151.2093, -27.4709, 153.0235);

        assertEquals(forward, backward, 0.0001);
    }

    // Test 4: distanceKm returns the expected approximate Brisbane-to-Sydney distance.
    @Test
    void testHotspotDistanceKmKnownCityDistance() throws Exception {
        double distance = invokeDistanceKm(-27.4705, 153.0260, -33.8688, 151.2093);

        assertEquals(731.0, distance, 5.0);
    }

    // Test 5: an empty crime list produces no hotspots.
    @Test
    void testBuildHotspotsEmptyCrimeListReturnsNoHotspots() throws Exception {
        List<Hotspot> hotspots = invokeBuildHotspots(List.of(), 2.0);

        assertTrue(hotspots.isEmpty());
    }

    // Test 6: a single crime creates one hotspot.
    @Test
    void testBuildHotspotsSingleCrimeCreatesOneHotspot() throws Exception {
        List<Hotspot> hotspots = invokeBuildHotspots(List.of(maanCrime(1, CrimeCategory.ASSAULT, -27.4709, 153.0235)), 2.0);

        assertEquals(1, hotspots.size());
    }

    // Test 7: a single-crime hotspot has a count of one.
    @Test
    void testBuildHotspotsSingleCrimeCountIsOne() throws Exception {
        List<Hotspot> hotspots = invokeBuildHotspots(List.of(maanCrime(1, CrimeCategory.ASSAULT, -27.4709, 153.0235)), 2.0);

        assertEquals(1, hotspots.get(0).getCount());
    }

    // Test 8: a single-crime hotspot keeps the original latitude.
    @Test
    void testBuildHotspotsSingleCrimeLatitudeMatchesCrime() throws Exception {
        List<Hotspot> hotspots = invokeBuildHotspots(List.of(maanCrime(1, CrimeCategory.ASSAULT, -27.4709, 153.0235)), 2.0);

        assertEquals(-27.4709, hotspots.get(0).getLatitude(), 0.0001);
    }

    // Test 9: a single-crime hotspot keeps the original longitude.
    @Test
    void testBuildHotspotsSingleCrimeLongitudeMatchesCrime() throws Exception {
        List<Hotspot> hotspots = invokeBuildHotspots(List.of(maanCrime(1, CrimeCategory.ASSAULT, -27.4709, 153.0235)), 2.0);

        assertEquals(153.0235, hotspots.get(0).getLongitude(), 0.0001);
    }

    // Test 10: nearby crimes are grouped into one hotspot.
    @Test
    void testBuildHotspotsGroupsNearbyCrimes() throws Exception {
        List<CrimeRecord> crimes = List.of(
                maanCrime(1, CrimeCategory.ASSAULT, -27.4709, 153.0235),
                maanCrime(2, CrimeCategory.ROBBERY, -27.4710, 153.0236)
        );

        List<Hotspot> hotspots = invokeBuildHotspots(crimes, 2.0);

        assertEquals(1, hotspots.size());
    }

    // Test 11: grouped nearby crimes increase the hotspot count.
    @Test
    void testBuildHotspotsNearbyCrimeCountIsTwo() throws Exception {
        List<CrimeRecord> crimes = List.of(
                maanCrime(1, CrimeCategory.ASSAULT, -27.4709, 153.0235),
                maanCrime(2, CrimeCategory.ROBBERY, -27.4710, 153.0236)
        );

        List<Hotspot> hotspots = invokeBuildHotspots(crimes, 2.0);

        assertEquals(2, hotspots.get(0).getCount());
    }

    // Test 12: distant crimes remain separate hotspots.
    @Test
    void testBuildHotspotsKeepsFarCrimesSeparate() throws Exception {
        List<CrimeRecord> crimes = List.of(
                maanCrime(1, CrimeCategory.ASSAULT, -27.4709, 153.0235),
                maanCrime(2, CrimeCategory.ROBBERY, -28.0000, 153.5000)
        );

        List<Hotspot> hotspots = invokeBuildHotspots(crimes, 2.0);

        assertEquals(2, hotspots.size());
    }

    // Test 13: crimes on the radius boundary are included.
    @Test
    void testHotspotRadiusBoundaryIsIncluded() throws Exception {
        List<CrimeRecord> crimes = List.of(
                maanCrime(1, CrimeCategory.ASSAULT, -27.4709, 153.0235),
                maanCrime(2, CrimeCategory.ROBBERY, -27.4880, 153.0235)
        );

        List<Hotspot> hotspots = invokeBuildHotspots(crimes, 2.0);

        assertEquals(1, hotspots.size());
    }

    // Test 14: a zero radius groups crimes at exactly the same location.
    @Test
    void testBuildHotspotsZeroRadiusGroupsIdenticalLocations() throws Exception {
        List<CrimeRecord> crimes = List.of(
                maanCrime(1, CrimeCategory.ASSAULT, -27.4709, 153.0235),
                maanCrime(2, CrimeCategory.ROBBERY, -27.4709, 153.0235)
        );

        List<Hotspot> hotspots = invokeBuildHotspots(crimes, 0.0);

        assertEquals(1, hotspots.size());
    }

    // Test 15: a zero radius does not group different locations.
    @Test
    void testBuildHotspotsZeroRadiusKeepsDifferentLocationsSeparate() throws Exception {
        List<CrimeRecord> crimes = List.of(
                maanCrime(1, CrimeCategory.ASSAULT, -27.4709, 153.0235),
                maanCrime(2, CrimeCategory.ROBBERY, -27.4710, 153.0236)
        );

        List<Hotspot> hotspots = invokeBuildHotspots(crimes, 0.0);

        assertEquals(2, hotspots.size());
    }

    // Test 16: grouped hotspot latitude is averaged.
    @Test
    void testBuildHotspotsAveragesGroupedLatitude() throws Exception {
        List<CrimeRecord> crimes = List.of(
                maanCrime(1, CrimeCategory.ASSAULT, -27.4700, 153.0235),
                maanCrime(2, CrimeCategory.ROBBERY, -27.4720, 153.0235)
        );

        List<Hotspot> hotspots = invokeBuildHotspots(crimes, 2.0);

        assertEquals(-27.4710, hotspots.get(0).getLatitude(), 0.0001);
    }

    // Test 17: grouped hotspot longitude is averaged.
    @Test
    void testBuildHotspotsAveragesGroupedLongitude() throws Exception {
        List<CrimeRecord> crimes = List.of(
                maanCrime(1, CrimeCategory.ASSAULT, -27.4709, 153.0200),
                maanCrime(2, CrimeCategory.ROBBERY, -27.4709, 153.0260)
        );

        List<Hotspot> hotspots = invokeBuildHotspots(crimes, 2.0);

        assertEquals(153.0230, hotspots.get(0).getLongitude(), 0.0001);
    }

    // Test 18: empty hotspot JSON is an empty array.
    @Test
    void testBuildHotspotJsonEmptyListReturnsEmptyArray() throws Exception {
        String json = invokeBuildHotspotJson(List.of());

        assertEquals("[]", json);
    }

    // Test 19: hotspot JSON includes the latitude field.
    @Test
    void testBuildHotspotJsonIncludesLatitude() throws Exception {
        String json = invokeBuildHotspotJson(List.of(new Hotspot(-27.4709, 153.0235, 3)));

        assertTrue(json.contains("\"lat\":-27.4709"));
    }

    // Test 20: hotspot JSON includes the count field.
    @Test
    void testBuildHotspotJsonIncludesCount() throws Exception {
        String json = invokeBuildHotspotJson(List.of(new Hotspot(-27.4709, 153.0235, 3)));

        assertTrue(json.contains("\"count\":3"));
    }
     /*
    MAAN APP TESTING END
     */

    /*
    END APP TESTING END
     */


}
