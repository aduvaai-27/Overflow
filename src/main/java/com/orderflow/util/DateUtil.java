package com.orderflow.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * The database stores timestamps as raw ISO-8601 strings (e.g.
 * "2026-09-26T07:44:19.235643") because that's what LocalDateTime.now()
 * produces and what SQLite's date functions expect. This class turns that
 * into something a person should actually read: a space instead of "T",
 * and no millisecond noise.
 */
public class DateUtil {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");

    private DateUtil() { }

    /** e.g. "2026-09-26T07:44:19.235643" -> "2026-09-26 07:44 AM" */
    public static String formatForDisplay(String isoDateTime) {
        if (isoDateTime == null || isoDateTime.isBlank()) return "";
        try {
            return LocalDateTime.parse(isoDateTime).format(DISPLAY_FORMAT);
        } catch (Exception e) {
            return isoDateTime; // if it's an unexpected format, show it as-is rather than crash
        }
    }
}
