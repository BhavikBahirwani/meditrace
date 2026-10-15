package com.meditrace.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Indian-style date formatting for the UI ("15 Oct 2026", "15 Oct 2026, 08:30 PM").
 * <p>
 * JSTL's {@code <fmt:formatDate>} only understands the old {@code java.util.Date}, so model
 * classes expose pre-formatted text through these helpers instead.
 */
public final class DateUtil {

    /** All times are shown in Indian Standard Time. */
    public static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH_YEAR = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

    private DateUtil() {
        // no instances
    }

    public static String formatDate(LocalDate date) {
        return date == null ? "" : DATE.format(date);
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : DATE_TIME.format(dateTime);
    }

    /** Medicine packs print expiry as month + year, e.g. "Mar 2028". */
    public static String formatMonthYear(LocalDate date) {
        return date == null ? "" : MONTH_YEAR.format(date);
    }

    /** Converts a block timestamp (epoch millis) to IST date-time text. */
    public static String formatEpochMillis(long epochMillis) {
        return formatDateTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), IST));
    }
}
