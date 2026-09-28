package com.orderflow.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtil {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");

    private DateUtil() { }

    public static String formatForDisplay(String isoDateTime) {
        if (isoDateTime == null || isoDateTime.isBlank()) return "";
        try {
            return LocalDateTime.parse(isoDateTime).format(DISPLAY_FORMAT);
        } catch (Exception e) {
            return isoDateTime;
        }
    }
}
