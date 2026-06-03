package com.example.demo.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.TimeZone;

public final class ArtistlanDateTimeUtils {

    public static final String MEXICO_TIME_ZONE_ID = "America/Mexico_City";
    private static final ZoneId MEXICO_ZONE_ID = ZoneId.of(MEXICO_TIME_ZONE_ID);
    private static final TimeZone MEXICO_TIME_ZONE = TimeZone.getTimeZone(MEXICO_ZONE_ID);

    private ArtistlanDateTimeUtils() {
    }

    public static ZoneId mexicoZoneId() {
        return MEXICO_ZONE_ID;
    }

    public static LocalDateTime nowMexico() {
        return LocalDateTime.now(MEXICO_ZONE_ID);
    }

    public static LocalDate todayMexico() {
        return LocalDate.now(MEXICO_ZONE_ID);
    }

    public static void applyMexicoTimeZoneAsDefault() {
        TimeZone.setDefault(MEXICO_TIME_ZONE);
    }
}
