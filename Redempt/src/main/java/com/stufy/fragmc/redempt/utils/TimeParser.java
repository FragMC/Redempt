package com.stufy.fragmc.redempt.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeParser {

    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)([ydhms]+)");

    public static long parseTime(String timeString) throws IllegalArgumentException {
        if (timeString.equalsIgnoreCase("inf") || timeString.equalsIgnoreCase("infinite")) {
            return -1;
        }

        long totalMilliseconds = 0;
        Matcher matcher = TIME_PATTERN.matcher(timeString.toLowerCase());

        while (matcher.find()) {
            int value = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);

            switch (unit) {
                case "y":
                    totalMilliseconds += value * 365L * 24 * 60 * 60 * 1000;
                    break;
                case "d":
                    totalMilliseconds += value * 24L * 60 * 60 * 1000;
                    break;
                case "h":
                    totalMilliseconds += value * 60L * 60 * 1000;
                    break;
                case "m":
                    totalMilliseconds += value * 60L * 1000;
                    break;
                case "s":
                    totalMilliseconds += value * 1000L;
                    break;
                case "ms":
                    totalMilliseconds += value;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid time unit: " + unit);
            }
        }

        if (totalMilliseconds == 0) {
            throw new IllegalArgumentException("Invalid time format: " + timeString);
        }

        return System.currentTimeMillis() + totalMilliseconds;
    }

    public static String formatTime(long expiryTime) {
        if (expiryTime == -1) {
            return "Never";
        }

        long currentTime = System.currentTimeMillis();
        long timeLeft = expiryTime - currentTime;

        if (timeLeft <= 0) {
            return "Expired";
        }

        long seconds = timeLeft / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long years = days / 365;

        StringBuilder sb = new StringBuilder();

        if (years > 0) {
            sb.append(years).append("y ");
            days %= 365;
        }
        if (days > 0) {
            sb.append(days).append("d ");
            hours %= 24;
        }
        if (hours > 0) {
            sb.append(hours).append("h ");
            minutes %= 60;
        }
        if (minutes > 0) {
            sb.append(minutes).append("m ");
            seconds %= 60;
        }
        if (seconds > 0 && years == 0 && days == 0) {
            sb.append(seconds).append("s");
        }

        return sb.toString().trim();
    }
}