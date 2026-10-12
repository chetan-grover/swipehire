package com.swipehire.web.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** One place for "what time is it": quotas reset at midnight India time, not server time. */
public final class AppTime {

    public static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private AppTime() {
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }
}
