package com.zelg.zelgrank.util;

import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and prints the short duration format used everywhere in the plugin,
 * like 30s, 5m, 1h, 7d, 2w, 1M and 1y. Units can be chained: 1d12h.
 */
public final class Durations {

    private static final Pattern PART = Pattern.compile("(\\d+)([smhdwMy])");

    private static final long MINUTE = 60;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;
    private static final long WEEK = 7 * DAY;
    private static final long MONTH = 30 * DAY;
    private static final long YEAR = 365 * DAY;

    private Durations() {
    }

    /**
     * @return the parsed duration, or null when the input isn't a valid duration
     */
    public static @Nullable Duration parse(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }

        Matcher matcher = PART.matcher(input);
        long seconds = 0;
        int end = 0;
        while (matcher.find()) {
            if (matcher.start() != end) {
                return null;
            }
            end = matcher.end();

            try {
                long amount = Long.parseLong(matcher.group(1));
                seconds = Math.addExact(seconds, Math.multiplyExact(amount, unitSeconds(matcher.group(2).charAt(0))));
            } catch (ArithmeticException | NumberFormatException e) {
                return null; // someone typed 99999999999y
            }
        }

        if (end != input.length() || seconds <= 0) {
            return null;
        }
        return Duration.ofSeconds(seconds);
    }

    /**
     * Short form for command feedback, e.g. 30d or 1y. Days win over weeks and months
     * because that's how people type them (30d, not 1M).
     */
    public static String toKey(Duration duration) {
        long seconds = duration.getSeconds();
        if (seconds % YEAR == 0) return seconds / YEAR + "y";
        if (seconds % DAY == 0) return seconds / DAY + "d";
        if (seconds % HOUR == 0) return seconds / HOUR + "h";
        if (seconds % MINUTE == 0) return seconds / MINUTE + "m";
        return seconds + "s";
    }

    /** Human readable time left, two units at most: "3d 4h", "2h 15m", "45s". */
    public static String pretty(Duration duration) {
        long seconds = Math.max(0, duration.getSeconds());
        long days = seconds / DAY;
        long hours = (seconds % DAY) / HOUR;
        long minutes = (seconds % HOUR) / MINUTE;
        long secs = seconds % MINUTE;

        if (days > 0) return hours > 0 ? days + "d " + hours + "h" : days + "d";
        if (hours > 0) return minutes > 0 ? hours + "h " + minutes + "m" : hours + "h";
        if (minutes > 0) return minutes + "m";
        return secs + "s";
    }

    private static long unitSeconds(char unit) {
        return switch (unit) {
            case 's' -> 1;
            case 'm' -> MINUTE;
            case 'h' -> HOUR;
            case 'd' -> DAY;
            case 'w' -> WEEK;
            case 'M' -> MONTH;
            case 'y' -> YEAR;
            default -> throw new IllegalArgumentException("unknown unit " + unit);
        };
    }
}
