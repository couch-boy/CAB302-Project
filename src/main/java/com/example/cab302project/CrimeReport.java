package com.example.cab302project;

public class CrimeReport
{
    private String description;
    private String priority;
    private String time;

    public CrimeReport(String description, String priority, String time) {
        this.description = description;
        this.priority = priority;
        this.time = time;
    }

    public String getDescription() { return description; }
    public String getPriority() { return priority; }
    public String getTime() { return time; }

    @Override
    public String toString() {
        return "[" + priority + "] " + description + " (" + time + ")";
    }
}
