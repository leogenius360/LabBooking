package com.testlab.labbooking.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateTimeUtils {

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String TIME_FORMAT = "HH:mm";
    private static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm";

    public static String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        return sdf.format(new Date());
    }

    public static String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT, Locale.getDefault());
        return sdf.format(new Date());
    }

    public static boolean isTimeAfter(String time1, String time2) {
        return time1.compareTo(time2) > 0;
    }

    public static boolean isDateInFuture(String date) {
        String currentDate = getCurrentDate();
        return date.compareTo(currentDate) >= 0;
    }

    public static boolean isTimesOverlapping(String start1, String end1, String start2, String end2) {
        return !(end1.compareTo(start2) <= 0 || start1.compareTo(end2) >= 0);
    }

    public static long getTimestamp(String date, String time) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(DATETIME_FORMAT, Locale.getDefault());
            Date datetime = sdf.parse(date + " " + time);
            return datetime != null ? datetime.getTime() : 0;
        } catch (ParseException e) {
            return 0;
        }
    }

    public static String formatDateTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATETIME_FORMAT, Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public static boolean isValidTimeRange(String startTime, String endTime) {
        return startTime.compareTo(endTime) < 0;
    }

    public static int getTimeDifferenceInMinutes(String startTime, String endTime) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT, Locale.getDefault());
            Date start = sdf.parse(startTime);
            Date end = sdf.parse(endTime);

            if (start != null && end != null) {
                long diffInMillis = end.getTime() - start.getTime();
                return (int) (diffInMillis / (1000 * 60));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static boolean isDateTodayOrFuture(String date) {
        String currentDate = getCurrentDate();
        return date.compareTo(currentDate) >= 0;
    }

    public static boolean isToday(String date) {
        return date.equals(getCurrentDate());
    }

    public static boolean isTimeInFuture(String time) {
        String currentTime = getCurrentTime();
        return time.compareTo(currentTime) > 0;
    }

    public static boolean isValidBookingTime(String date, String startTime) {
        if (isToday(date)) {
            return isTimeInFuture(startTime);
        }
        return true;
    }
}