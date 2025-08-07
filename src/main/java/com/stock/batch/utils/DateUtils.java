package com.stock.batch.utils;

import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class DateUtils {

    private static final List<DateTimeFormatter> FORMATTERS = Arrays.asList(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
    );


    public static String getStringNowDateFormat(String pattern) {
        return getStringDateFormat(LocalDateTime.now(),pattern);
    }

    public static String getStringDateFormat(LocalDateTime ldt , String pattern){
        return ldt.format(DateTimeFormatter.ofPattern(pattern).withLocale(Locale.KOREAN));
    }

    public static LocalDate toStringLocalDate(String str){
        if(StringUtils.hasText(str)){
            if(str.matches("^[\\d]{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$") ){
                return LocalDate.parse(str, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            }else if(str.matches("^[\\d]{4}\\.(0[1-9]|1[012])\\.(0[1-9]|[12][0-9]|3[01])$") ){
                return LocalDate.parse(str, DateTimeFormatter.ofPattern("yyyy.MM.dd"));
            }else if(str.matches("^[\\d]{4}(0[1-9]|1[012])(0[1-9]|[12][0-9]|3[01])$")){
                return LocalDate.parse(str, DateTimeFormatter.ofPattern("yyyyMMdd"));
            }
        }

        throw new IllegalArgumentException();
    }

    public static String toLocalDatetimeString(LocalDateTime dateTime){
        String formatDate = "";
        if(dateTime != null){
            formatDate = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        return formatDate;
    }

    public static String toLocalDateString(LocalDate date){
        String formatDate = "";
        if(date != null){
            formatDate = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        }
        return formatDate;
    }

    public static LocalDateTime toStringLocalDateTime(String str) {
        if (str == null || str.trim().isEmpty()) {
            return null;
        }

        for (DateTimeFormatter formatter : FORMATTERS) {
                return LocalDateTime.parse(str, formatter);
        }

        throw new IllegalArgumentException();
    }

}

