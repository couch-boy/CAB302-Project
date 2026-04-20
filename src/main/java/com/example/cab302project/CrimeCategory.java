package com.example.cab302project;

public enum CrimeCategory {
    //enum label, category name, category severity
    OTHER("Other", Severity.LOW),
    GRAFFITI("Graffiti", Severity.LOW),
    LITTERING("Littering", Severity.LOW),
    NOISE("Noise", Severity.LOW),
    LOITERING("Loitering", Severity.LOW),
    VANDALISM("Vandalism", Severity.LOW),
    PUBLICDISTURBANCE("Public Disturbance", Severity.LOW),
    PROPERTYDAMAGE("Property Damage", Severity.LOW),
    PETTYTHEFT("Petty Theft", Severity.LOW),
    TRESPASSING("Trespassing", Severity.MEDIUM),
    DRUGUSE("Drug Use", Severity.MEDIUM),
    ROBBERY("Robbery", Severity.MEDIUM),
    ASSAULT("Assault", Severity.MEDIUM),
    DOMESTICABUSE("Domestic Abuse", Severity.MEDIUM),
    WEAPONS("Weapons", Severity.MEDIUM),
    STALKING("Stalking", Severity.MEDIUM),
    BREAKINGANDENTERING("Breaking and Entering", Severity.CRITICAL),
    SEXUALOFFENCE("Sexual Offence", Severity.CRITICAL),
    ARMEDROBBERY("Armed Robbery", Severity.CRITICAL),
    ARSON("Arson", Severity.CRITICAL),
    HOMICIDE("Homicide", Severity.CRITICAL);

    // Enum containing severity information
    public enum Severity {
        LOW("Low"),
        MEDIUM("Medium"),
        CRITICAL("Critical");

        // Severity value is immutable after creation
        private final String label;

        // Constructor
        Severity(String label) {
            this.label = label;
        }

        // Override for default toString functionality
        @Override
        public String toString() {return label;}
    }

    // Name and severity are immutable after creation
    private final String name;
    private final Severity severity;

    // Constructor
    CrimeCategory(String name, Severity severity) {
        this.name = name;
        this.severity = severity;
    }

    // Override for default toString functionality
    @Override
    public String toString() { return name; }

    // Getters
    public String getName() { return name; }
    public Severity getSeverity() { return severity; }
}
