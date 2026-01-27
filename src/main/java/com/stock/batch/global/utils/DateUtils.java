package com.stock.batch.global.utils;

import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

public class DateUtils {

    private static final List<DateTimeFormatter> DATE_TIME_FORMATTERS = Arrays.asList(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
    );

    private static final Map<Pattern, DateTimeFormatter> DATE_PATTERNS = new LinkedHashMap<>();

    static {
        DATE_PATTERNS.put(
                Pattern.compile("^\\d{4}-(0[1-9]|1[012])-(0[1-9]|[12]\\d|3[01])$"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd")
        );
        DATE_PATTERNS.put(
                Pattern.compile("^\\d{4}\\.(0[1-9]|1[012])\\.(0[1-9]|[12]\\d|3[01])$"),
                DateTimeFormatter.ofPattern("yyyy.MM.dd")
        );
        DATE_PATTERNS.put(
                Pattern.compile("^\\d{4}(0[1-9]|1[012])(0[1-9]|[12]\\d|3[01])$"),
                DateTimeFormatter.ofPattern("yyyyMMdd")
        );
    }

    public static String getStringNowDateFormat(String pattern) {
        return getStringDateFormat(LocalDateTime.now(), pattern);
    }

    public static String getStringDateFormat(LocalDateTime ldt, String pattern) {
        return ldt.format(DateTimeFormatter.ofPattern(pattern).withLocale(Locale.KOREAN));
    }

    public static LocalDate toStringLocalDate(String str) {
        if (!StringUtils.hasText(str)) {
            throw new IllegalArgumentException("Date string cannot be null or empty");
        }

        return DATE_PATTERNS.entrySet().stream()
                .filter(entry -> entry.getKey().matcher(str).matches())
                .findFirst()
                .map(entry -> LocalDate.parse(str, entry.getValue()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid date format: " + str));
    }

    public static String toLocalDatetimeString(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "";
    }

    public static String toLocalDateString(LocalDate date) {
        return date != null ? date.format(DateTimeFormatter.ofPattern("yyyyMMdd")) : "";
    }

    public static LocalDateTime toStringLocalDateTime(String str) {
        if (str == null || str.trim().isEmpty()) {
            return null;
        }

        return DATE_TIME_FORMATTERS.stream()
                .map(formatter -> tryParse(str, formatter))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unable to parse date: " + str));
    }

    private static LocalDateTime tryParse(String str, DateTimeFormatter formatter) {
        try {
            return LocalDateTime.parse(str, formatter);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}

