package com.example.cab302project;

public class CrimeReport
{
    private String description;
    private String priority;
    private String time;

    // Crime report structure
    public CrimeReport(String description, String priority, String time) {
        this.description = description;
        this.priority = priority;
        this.time = time;
    }

    // access report data
    public String getDescription() { return description; }
    public String getPriority() { return priority; }
    public String getTime() { return time; }

    // Controls how it is displayed
    @Override
    public String toString() {
        return "[" + priority + "] " + description + " (" + time + ")";
    }
}
