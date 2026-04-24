package com.example.cab302project;

public enum UserType {
    REGULAR("Regular User"),
    POLICE("Police Officer");

    // Immutable display name
    private final String displayName;

    // Constructor
    UserType(String displayName) {
        this.displayName = displayName;
    }

    // Override for default toString functionality
    @Override
    public String toString() { return displayName; }

    // Getter
    public String getDisplayName() {
        return displayName;
    }
}