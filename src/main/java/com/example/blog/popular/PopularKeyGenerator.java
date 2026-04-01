package com.example.blog.popular;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Locale;

public final class PopularKeyGenerator {

    private PopularKeyGenerator() {}

    public static String dailyKey(LocalDate date) {
        return "popular:daily:" + date;
    }

    public static String weeklyKey(LocalDate date) {
        WeekFields weekFields = WeekFields.of(Locale.KOREA);
        int weekOfYear = date.get(weekFields.weekOfWeekBasedYear());
        int weekYear = date.get(weekFields.weekBasedYear());
        return "popular:weekly:" + weekYear + "-W" + weekOfYear;
    }
}