package com.example.cab302project;

/**
 * Represents the various categories of crimes handled by the system.
 * Each category is associated with a specific display name and a {@link Severity} level.
 */
public enum CrimeCategory {

    /** Represents miscellaneous crimes not specifically categorized. */
    OTHER("Other", Severity.LOW),

    /** Crimes involving unauthorized marking or painting on property. */
    GRAFFITI("Graffiti", Severity.LOW),

    /** Crimes involving the improper disposal of waste. */
    LITTERING("Littering", Severity.LOW),

    /** Crimes involving excessive or disruptive noise levels. */
    NOISE("Noise", Severity.LOW),

    /** Crimes involving remaining in a public place without apparent purpose. */
    LOITERING("Loitering", Severity.LOW),

    /** Crimes involving willful destruction or defacement of property. */
    VANDALISM("Vandalism", Severity.LOW),

    /** Crimes involving disruptive behavior in public spaces. */
    PUBLICDISTURBANCE("Public Disturbance", Severity.LOW),

    /** General crimes resulting in damage to physical property. */
    PROPERTYDAMAGE("Property Damage", Severity.LOW),

    /** Theft of low-value property without the use of force. */
    PETTYTHEFT("Petty Theft", Severity.LOW),

    /** Unauthorized entry onto another person's property. */
    TRESPASSING("Trespassing", Severity.MEDIUM),

    /** Crimes involving the illegal consumption or possession of controlled substances. */
    DRUGUSE("Drug Use", Severity.MEDIUM),

    /** Theft involving the use of force or intimidation. */
    ROBBERY("Robbery", Severity.MEDIUM),

    /** Physical attack or threat of attack on another person. */
    ASSAULT("Assault", Severity.MEDIUM),

    /** Crimes involving physical or emotional abuse within a domestic setting. */
    DOMESTICABUSE("Domestic Abuse", Severity.MEDIUM),

    /** Crimes involving illegal possession or use of restricted weapons. */
    WEAPONS("Weapons", Severity.MEDIUM),

    /** The act of following or harassing a person in a threatening manner. */
    STALKING("Stalking", Severity.MEDIUM),

    /** Illegal entry into a building with the intent to commit a crime. */
    BREAKINGANDENTERING("Breaking and Entering", Severity.CRITICAL),

    /** Crimes involving non-consensual sexual acts. */
    SEXUALOFFENCE("Sexual Offence", Severity.CRITICAL),

    /** Robbery committed with the use of a lethal weapon. */
    ARMEDROBBERY("Armed Robbery", Severity.CRITICAL),

    /** Willful and malicious burning of property. */
    ARSON("Arson", Severity.CRITICAL),

    /** The unlawful killing of one human being by another. */
    HOMICIDE("Homicide", Severity.CRITICAL);

    /**
     * Defines the severity levels for crime categories.
     */
    public enum Severity {
        /** Low priority/severity. */
        LOW("Low"),
        /** Moderate priority/severity. */
        MEDIUM("Medium"),
        /** High priority/urgent severity. */
        CRITICAL("Critical");

        // Severity value is immutable after creation
        private final String label;

        /**
         * Constructs a Severity level with a display label.
         * @param label The string representation of the severity.
         */
        Severity(String label) {
            this.label = label;
        }

        /**
         * Returns the display label of the severity.
         * @return A string representing the severity level.
         */
        @Override
        public String toString() {return label;}
    }

    // Name and severity are immutable after creation
    private final String name;
    private final Severity severity;

    /**
     * Constructs a CrimeCategory with a name and associated severity.
     * @param name The display name of the crime.
     * @param severity The {@link Severity} level of the crime.
     */
    CrimeCategory(String name, Severity severity) {
        this.name = name;
        this.severity = severity;
    }

    /**
     * Returns the display name of the crime category.
     * @return The category name.
     */
    @Override
    public String toString() { return name; }

    // Getters

    /**
     * Gets the human-readable name of the crime category.
     * @return The category name.
     */
    public String getName() { return name; }

    /**
     * Gets the severity level associated with this crime category.
     * @return The {@link Severity} level.
     */
    public Severity getSeverity() { return severity; }
}
