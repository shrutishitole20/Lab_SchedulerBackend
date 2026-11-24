package com.lab.enums;

public enum Priority {
    EMERGENCY(1, 15),
    HIGH(2, 30),
    NORMAL(3, 120),
    LOW(4, 240);

    private final int rank;
    private final int defaultTargetMinutes;

    Priority(int rank, int defaultTargetMinutes) {
        this.rank = rank;
        this.defaultTargetMinutes = defaultTargetMinutes;
    }

    public int getRank() {
        return rank;
    }

    public int getDefaultTargetMinutes() {
        return defaultTargetMinutes;
    }

    public static Priority fromString(String val) {
        if (val == null || val.trim().isEmpty()) {
            return NORMAL;
        }
        String clean = val.trim().toUpperCase();
        for (Priority p : values()) {
            if (p.name().equals(clean)) {
                return p;
            }
        }
        return NORMAL;
    }

    public static Priority fromRank(int rank) {
        for (Priority p : values()) {
            if (p.getRank() == rank) {
                return p;
            }
        }
        return NORMAL;
    }

    @Override
    public String toString() {
        return name() + " (Rank " + rank + ", " + defaultTargetMinutes + " min)";
    }
}
