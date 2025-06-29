package id.rnggagib.taskforge.utils;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for parsing and formatting time durations
 * Supports formats like: 1d, 1h, 30m, 45s, 2h30m, etc.
 */
public class TimeUtils {
    
    // Pattern to match time formats: 1d, 1h, 30m, 45s, etc.
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)([dhms])");
    
    /**
     * Parse a time string into milliseconds
     * Examples: "1h" -> 3600000ms, "30m" -> 1800000ms, "2h30m" -> 9000000ms
     * 
     * @param timeString Time string to parse (e.g., "1h", "30m", "2d")
     * @return Time in milliseconds, or 0 if invalid
     */
    public static long parseTimeToMillis(String timeString) {
        if (timeString == null || timeString.trim().isEmpty()) {
            return 0;
        }
        
        timeString = timeString.toLowerCase().trim();
        Matcher matcher = TIME_PATTERN.matcher(timeString);
        
        long totalMillis = 0;
        
        while (matcher.find()) {
            int amount = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);
            
            switch (unit) {
                case "d": // days
                    totalMillis += TimeUnit.DAYS.toMillis(amount);
                    break;
                case "h": // hours
                    totalMillis += TimeUnit.HOURS.toMillis(amount);
                    break;
                case "m": // minutes
                    totalMillis += TimeUnit.MINUTES.toMillis(amount);
                    break;
                case "s": // seconds
                    totalMillis += TimeUnit.SECONDS.toMillis(amount);
                    break;
            }
        }
        
        return totalMillis;
    }
    
    /**
     * Format milliseconds into a readable time string
     * Examples: 3600000ms -> "1h", 1800000ms -> "30m"
     * 
     * @param millis Time in milliseconds
     * @return Formatted time string
     */
    public static String formatTime(long millis) {
        if (millis <= 0) {
            return "0s";
        }
        
        StringBuilder result = new StringBuilder();
        
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        
        if (days > 0) {
            result.append(days).append("d");
        }
        if (hours > 0) {
            result.append(hours).append("h");
        }
        if (minutes > 0) {
            result.append(minutes).append("m");
        }
        if (seconds > 0 || result.length() == 0) {
            result.append(seconds).append("s");
        }
        
        return result.toString();
    }
    
    /**
     * Check if a cooldown time has expired
     * 
     * @param startTime When the cooldown started (System.currentTimeMillis())
     * @param cooldownDuration Duration of cooldown in milliseconds
     * @return true if cooldown has expired, false if still active
     */
    public static boolean isCooldownExpired(long startTime, long cooldownDuration) {
        return System.currentTimeMillis() >= (startTime + cooldownDuration);
    }
    
    /**
     * Get remaining cooldown time in milliseconds
     * 
     * @param startTime When the cooldown started
     * @param cooldownDuration Duration of cooldown in milliseconds
     * @return Remaining time in milliseconds, or 0 if expired
     */
    public static long getRemainingCooldown(long startTime, long cooldownDuration) {
        long endTime = startTime + cooldownDuration;
        long remaining = endTime - System.currentTimeMillis();
        return Math.max(0, remaining);
    }
    
    /**
     * Validate if a time string is in correct format
     * 
     * @param timeString Time string to validate
     * @return true if valid format, false otherwise
     */
    public static boolean isValidTimeFormat(String timeString) {
        if (timeString == null || timeString.trim().isEmpty()) {
            return false;
        }
        
        return TIME_PATTERN.matcher(timeString.toLowerCase().trim()).find();
    }
}
