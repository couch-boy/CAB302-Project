package com.example.cab302project;

public enum CrimeCategory {
    THEFT("Theft"),
    VANDALISM("Vandalism"),
    ASSAULT("Assault"),
    OTHER("Other");

    private final String label;
    CrimeCategory(String label) { this.label = label; }
    @Override
    public String toString() { return label; }
}
