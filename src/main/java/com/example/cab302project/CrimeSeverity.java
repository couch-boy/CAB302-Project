package com.example.cab302project;

public enum CrimeSeverity {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High");

    private final String label;
    CrimeSeverity(String label) { this.label = label; }
    @Override
    public String toString() { return label; }
}
