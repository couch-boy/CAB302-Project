package com.example.cab302project;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

public class AppTest {
    /*
    JACK APP TESTING START
     */
    // Test 1: CrimeRecord coordinate validation
    // Checks that setLocation() rejects out-of-range coordinates and keeps
    // the original valid values. Protects map rendering from bad data.
    @Test
    void testCrimeRecordCoordinateValidation() {
        CrimeRecord crime = new CrimeRecord(1, CrimeCategory.ASSAULT, LocalDateTime.now(), -27.4709, 153.0235, "Test", "user1", false);

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
        User publicUser = new User("testuser", "pass123", "test@email.com", "0400000000", -27.4709, 153.0235, false, UserType.REGULAR);
        UserSession.login(publicUser);

        assertNotNull(UserSession.getInstance());
        assertEquals("testuser", UserSession.getInstance().getUser().getUsername());
        assertFalse(UserSession.isPolice());

        // Log out and verify session is cleared
        UserSession.logout();
        assertNull(UserSession.getInstance());

        // Log in as police and verify isPolice() returns true
        User policeUser = new User("officer1", "secure", "police@law.com", "0411111111", -27.4709, 153.0235, false, UserType.POLICE);
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
        CrimeRecord nullReporter = new CrimeRecord(1, CrimeCategory.NOISE, LocalDateTime.now(), -27.4709, 153.0235, "desc", null, false);
        assertEquals("Anonymous", nullReporter.getReporterDisplayName());

        // Empty string reporter should display as Anonymous
        CrimeRecord emptyReporter = new CrimeRecord(2, CrimeCategory.NOISE, LocalDateTime.now(), -27.4709, 153.0235, "desc", "", false);
        assertEquals("Anonymous", emptyReporter.getReporterDisplayName());

        // Named reporter should display their username
        CrimeRecord namedReporter = new CrimeRecord(3, CrimeCategory.NOISE, LocalDateTime.now(), -27.4709, 153.0235, "desc", "jack123", false);
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

    // Test 23: Relative time helper returns "Today" for same-day crimes
    @Test
    void testCrimeRelativeTimeTodayIsToday() throws Exception {
        LocalDateTime today = java.time.LocalDate.now().atTime(12, 0);
        assertEquals("Today", UIUtils.getRelativeTime(today));
    }

    // Test 24: Relative time helper returns "1 day ago" for yesterday's crimes
    @Test
    void testCrimeRelativeTimeYesterdayIsOneDayAgo() {
        LocalDateTime yesterday = java.time.LocalDate.now().minusDays(1).atTime(12, 0);
        assertEquals("1 day ago", UIUtils.getRelativeTime(yesterday));
    }

    // Test 25: Relative time helper returns days for older crimes
    @Test
    void testCrimeRelativeTimeThreeDaysAgoIsThreeDaysAgo() {
        LocalDateTime threeDaysAgo = java.time.LocalDate.now().minusDays(3).atTime(12, 0);
        assertEquals("3 days ago", UIUtils.getRelativeTime(threeDaysAgo));
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
