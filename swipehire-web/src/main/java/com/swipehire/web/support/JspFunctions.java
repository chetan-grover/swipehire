package com.swipehire.web.support;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

/**
 * Static methods exposed to JSPs as custom EL functions (declared in WEB-INF/swipehire.tld).
 * Needed because JSTL's fmt:formatDate only understands java.util.Date, not java.time.
 * Usage: ${f:date(job.deadline)}, ${f:ago(n.createdAt)}, ${f:money(job.stipendMax)}.
 */
public final class JspFunctions {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter ISO_LOCAL = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private JspFunctions() {
    }

    public static String date(Object value) {
        return value instanceof TemporalAccessor t ? DATE.format(t) : "";
    }

    public static String dateTime(Object value) {
        if (value instanceof LocalDate d) {
            return DATE.format(d);
        }
        return value instanceof TemporalAccessor t ? DATE_TIME.format(t) : "";
    }

    /** Value for an HTML datetime-local input. */
    public static String isoLocal(Object value) {
        return value instanceof LocalDateTime t ? ISO_LOCAL.format(t) : "";
    }

    /** "5 min ago", "3 h ago", "2 days ago". */
    public static String ago(Object value) {
        if (!(value instanceof LocalDateTime t)) {
            return "";
        }
        Duration d = Duration.between(t, LocalDateTime.now());
        if (d.toMinutes() < 1) {
            return "just now";
        }
        if (d.toHours() < 1) {
            return d.toMinutes() + " min ago";
        }
        if (d.toDays() < 1) {
            return d.toHours() + " h ago";
        }
        return d.toDays() + (d.toDays() == 1 ? " day ago" : " days ago");
    }

    /** Days until a date (negative if past): used for "closes in 5 days". */
    public static long daysUntil(Object value) {
        return value instanceof LocalDate d ? Duration.between(LocalDate.now().atStartOfDay(), d.atStartOfDay()).toDays() : 0;
    }

    /** Indian-style rupee amount: 25000 -> "₹25,000". */
    public static String money(Integer amount) {
        if (amount == null) {
            return "";
        }
        return "₹" + String.format(Locale.ENGLISH, "%,d", amount);
    }

    /** 0.734 -> 73 */
    public static long pct(Double value) {
        return value == null ? 0 : Math.round(value * 100);
    }

    /** First letter of each of the first two words, for avatar placeholders. */
    public static String initials(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        return (parts[0].substring(0, 1) + (parts.length > 1 ? parts[1].substring(0, 1) : "")).toUpperCase(Locale.ROOT);
    }

    public static String title(Object enumValue) {
        if (enumValue == null) {
            return "";
        }
        String s = enumValue.toString().replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
