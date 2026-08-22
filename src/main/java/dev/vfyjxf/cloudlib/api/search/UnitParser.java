package dev.vfyjxf.cloudlib.api.search;

public final class UnitParser {
    private UnitParser() {
    }

    public static long parseTime(String raw) {
        String input = raw.trim().toLowerCase();
        if (input.endsWith("tick")) {
            return Long.parseLong(input.substring(0, input.length() - 4));
        }
        if (input.endsWith("t")) {
            return Long.parseLong(input.substring(0, input.length() - 1));
        }
        if (input.endsWith("s")) {
            return Long.parseLong(input.substring(0, input.length() - 1)) * 20L;
        }
        if (input.endsWith("m")) {
            return Long.parseLong(input.substring(0, input.length() - 1)) * 1200L;
        }
        if (input.endsWith("h")) {
            return Long.parseLong(input.substring(0, input.length() - 1)) * 72000L;
        }
        return Long.parseLong(input);
    }

    public static long parseEnergy(String raw) {
        String input = raw.trim().toLowerCase();
        if (input.endsWith("fe") || input.endsWith("rf")) {
            input = input.substring(0, input.length() - 2);
        }
        long multiplier = 1;
        if (input.endsWith("k")) {
            multiplier = 1_000L;
            input = input.substring(0, input.length() - 1);
        } else if (input.endsWith("m")) {
            multiplier = 1_000_000L;
            input = input.substring(0, input.length() - 1);
        } else if (input.endsWith("g")) {
            multiplier = 1_000_000_000L;
            input = input.substring(0, input.length() - 1);
        }
        return Math.round(Double.parseDouble(input) * multiplier);
    }

    public static double parseChance(String raw) {
        String input = raw.trim().toLowerCase();
        if (input.endsWith("%")) {
            return Double.parseDouble(input.substring(0, input.length() - 1)) / 100D;
        }
        return Double.parseDouble(input);
    }
}
