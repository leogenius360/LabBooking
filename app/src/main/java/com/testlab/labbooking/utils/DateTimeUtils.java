package com.testlab.labbooking.utils;

import android.util.Log;

import com.testlab.labbooking.models.Booking;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class DateTimeUtils {
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String TIME_FORMAT = "HH:mm";
    private static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm";
    private static final String DISPLAY_DATE_FORMAT = "MMM dd, yyyy";
    private static final String DISPLAY_TIME_FORMAT = "h:mm a";

    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
    private static final SimpleDateFormat timeFormatter = new SimpleDateFormat(TIME_FORMAT, Locale.getDefault());
    private static final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat(DATETIME_FORMAT, Locale.getDefault());
    private static final SimpleDateFormat displayDateFormatter = new SimpleDateFormat(DISPLAY_DATE_FORMAT, Locale.getDefault());
    private static final SimpleDateFormat displayTimeFormatter = new SimpleDateFormat(DISPLAY_TIME_FORMAT, Locale.getDefault());

    static {
        // Set timezone to system default
        TimeZone defaultTimeZone = TimeZone.getDefault();
        dateFormatter.setTimeZone(defaultTimeZone);
        timeFormatter.setTimeZone(defaultTimeZone);
        dateTimeFormatter.setTimeZone(defaultTimeZone);
        displayDateFormatter.setTimeZone(defaultTimeZone);
        displayTimeFormatter.setTimeZone(defaultTimeZone);
    }

    // ======================= CURRENT DATE/TIME =======================

    /**
     * Get current date in YYYY-MM-DD format
     */
    public static String getCurrentDate() {
        return dateFormatter.format(new Date());
    }

    /**
     * Get current time in HH:MM format
     */
    public static String getCurrentTime() {
        return timeFormatter.format(new Date());
    }

    /**
     * Get current datetime in YYYY-MM-DD HH:MM format
     */
    public static String getCurrentDateTime() {
        return dateTimeFormatter.format(new Date());
    }

    // ======================= DATE FORMATTING =======================

    /**
     * Format date for display (e.g., "Jan 15, 2024")
     */
    public static String formatDateForDisplay(String date) {
        try {
            Date dateObj = dateFormatter.parse(date);
            return displayDateFormatter.format(dateObj);
        } catch (ParseException e) {
            return date; // Return original if parsing fails
        }
    }

    /**
     * Format time for display (e.g., "2:30 PM")
     */
    public static String formatTimeForDisplay(String time) {
        try {
            Date timeObj = timeFormatter.parse(time);
            return displayTimeFormatter.format(timeObj);
        } catch (ParseException e) {
            return time; // Return original if parsing fails
        }
    }

    /**
     * Format duration in minutes to readable format
     */
    public static String formatDuration(int minutes) {
        int hours = minutes / 60;
        int remainingMinutes = minutes % 60;

        if (hours > 0 && remainingMinutes > 0) {
            return hours + "h " + remainingMinutes + "m";
        } else if (hours > 0) {
            return hours + "h";
        } else {
            return remainingMinutes + "m";
        }
    }

    // ======================= DATE CALCULATIONS =======================

    /**
     * Get time difference in minutes between two times
     */
    public static int getTimeDifferenceInMinutes(String startTime, String endTime) {
        try {
            Date start = timeFormatter.parse(startTime);
            Date end = timeFormatter.parse(endTime);

            if (start != null && end != null) {
                long diffInMillis = end.getTime() - start.getTime();
                return (int) TimeUnit.MILLISECONDS.toMinutes(diffInMillis);
            }
        } catch (ParseException e) {
            Log.e("DateTimeUtils", "Error parsing time: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Check if one time is after another
     */
    public static boolean isTimeAfter(String time1, String time2) {
        try {
            Date t1 = timeFormatter.parse(time1);
            Date t2 = timeFormatter.parse(time2);
            return t1 != null && t2 != null && t1.after(t2);
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * Check if time is within a range
     */
    public static boolean isTimeWithinRange(String time, String startRange, String endRange) {
        try {
            Date timeObj = timeFormatter.parse(time);
            Date startObj = timeFormatter.parse(startRange);
            Date endObj = timeFormatter.parse(endRange);

            if (timeObj != null && startObj != null && endObj != null) {
                return !timeObj.before(startObj) && !timeObj.after(endObj);
            }
        } catch (ParseException e) {
            Log.e("DateTimeUtils", "Error parsing time range: " + e.getMessage());
        }
        return false;
    }

    /**
     * Check if two time slots overlap
     */
    public static boolean doTimeSlotsOverlap(String start1, String end1, String start2, String end2) {
        try {
            Date s1 = timeFormatter.parse(start1);
            Date e1 = timeFormatter.parse(end1);
            Date s2 = timeFormatter.parse(start2);
            Date e2 = timeFormatter.parse(end2);

            if (s1 != null && e1 != null && s2 != null && e2 != null) {
                // Two time slots overlap if: start1 < end2 AND start2 < end1
                return s1.before(e2) && s2.before(e1);
            }
        } catch (ParseException e) {
            Log.e("DateTimeUtils", "Error parsing time slots: " + e.getMessage());
        }
        return false;
    }

    // ======================= DATE VALIDATION =======================

    /**
     * Check if date is in the past
     */
    public static boolean isPastDate(String date) {
        try {
            Date dateObj = dateFormatter.parse(date);
            Date today = dateFormatter.parse(getCurrentDate());
            return dateObj != null && today != null && dateObj.before(today);
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * Check if date is today
     */
    public static boolean isToday(String date) {
        return getCurrentDate().equals(date);
    }

    /**
     * Check if date is in the future
     */
    public static boolean isFutureDate(String date) {
        try {
            Date dateObj = dateFormatter.parse(date);
            Date today = dateFormatter.parse(getCurrentDate());
            return dateObj != null && today != null && dateObj.after(today);
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * Check if date and time is in the past
     */
    public static boolean isDateTimePast(String date, String time) {
        try {
            String dateTimeString = date + " " + time;
            Date dateTime = dateTimeFormatter.parse(dateTimeString);
            return dateTime != null && dateTime.before(new Date());
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * Check if date is within a range
     */
    public static boolean isDateInRange(String date, String startDate, String endDate) {
        try {
            Date dateObj = dateFormatter.parse(date);
            Date startObj = dateFormatter.parse(startDate);
            Date endObj = dateFormatter.parse(endDate);

            if (dateObj != null && startObj != null && endObj != null) {
                return !dateObj.before(startObj) && !dateObj.after(endObj);
            }
        } catch (ParseException e) {
            Log.e("DateTimeUtils", "Error parsing date range: " + e.getMessage());
        }
        return false;
    }

    // ======================= WEEK CALCULATIONS =======================

    /**
     * Get start of week for a given date
     */
    public static String getWeekStartDate(String date) {
        try {
            Date dateObj = dateFormatter.parse(date);
            if (dateObj != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(dateObj);
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                return dateFormatter.format(cal.getTime());
            }
        } catch (ParseException e) {
            Log.e("DateTimeUtils", "Error getting week start: " + e.getMessage());
        }
        return getCurrentDate();
    }

    /**
     * Get start of current week
     */
    public static String getWeekStartDate() {
        return getWeekStartDate(getCurrentDate());
    }

    /**
     * Get end of week for a given date
     */
    public static String getWeekEndDate(String date) {
        try {
            Date dateObj = dateFormatter.parse(date);
            if (dateObj != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(dateObj);
                cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
                return dateFormatter.format(cal.getTime());
            }
        } catch (ParseException e) {
            Log.e("DateTimeUtils", "Error getting week end: " + e.getMessage());
        }
        return getCurrentDate();
    }

    /**
     * Get end of current week
     */
    public static String getWeekEndDate() {
        return getWeekEndDate(getCurrentDate());
    }

    // ======================= DATE ARITHMETIC =======================

    /**
     * Add days to a date
     */
    public static String addDaysToDate(String date, int days) {
        try {
            Date dateObj = dateFormatter.parse(date);
            if (dateObj != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(dateObj);
                cal.add(Calendar.DAY_OF_MONTH, days);
                return dateFormatter.format(cal.getTime());
            }
        } catch (ParseException e) {
            Log.e("DateTimeUtils", "Error adding days to date: " + e.getMessage());
        }
        return date;
    }

    /**
     * Get date N days from today
     */
    public static String getDateFromToday(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, days);
        return dateFormatter.format(cal.getTime());
    }

    /**
     * Get hours until a specific date and time
     */
    public static long getHoursUntilDateTime(String date, String time) {
        try {
            String dateTimeString = date + " " + time;
            Date targetDateTime = dateTimeFormatter.parse(dateTimeString);

            if (targetDateTime != null) {
                long diffInMillis = targetDateTime.getTime() - System.currentTimeMillis();
                return TimeUnit.MILLISECONDS.toHours(diffInMillis);
            }
        } catch (ParseException e) {
            Log.e("DateTimeUtils", "Error calculating hours until: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Get days between two dates
     */
    public static int getDaysBetween(String startDate, String endDate) {
        try {
            Date start = dateFormatter.parse(startDate);
            Date end = dateFormatter.parse(endDate);

            if (start != null && end != null) {
                long diffInMillis = end.getTime() - start.getTime();
                return (int) TimeUnit.MILLISECONDS.toDays(diffInMillis);
            }
        } catch (ParseException e) {
            Log.e("DateTimeUtils", "Error calculating days between: " + e.getMessage());
        }
        return 0;
    }

    // ======================= TIME SLOT GENERATION =======================

    /**
     * Generate available time slots for a day
     */
    public static List<String> generateTimeSlots(String openTime, String closeTime, int intervalMinutes) {
        List<String> timeSlots = new ArrayList<>();

        try {
            Date start = timeFormatter.parse(openTime);
            Date end = timeFormatter.parse(closeTime);

            if (start != null && end != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(start);

                while (cal.getTime().before(end)) {
                    timeSlots.add(timeFormatter.format(cal.getTime()));
                    cal.add(Calendar.MINUTE, intervalMinutes);
                }
            }
        } catch (ParseException e) {
            Log.e("DateTimeUtils", "Error generating time slots: " + e.getMessage());
        }

        return timeSlots;
    }

    /**
     * Get available booking slots for a specific duration
     */
    public static List<TimeSlot> getAvailableSlots(String openTime, String closeTime,
                                                   int durationMinutes, int intervalMinutes) {
        List<TimeSlot> slots = new ArrayList<>();

        try {
            Date start = timeFormatter.parse(openTime);
            Date end = timeFormatter.parse(closeTime);

            if (start != null && end != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(start);

                while (true) {
                    Calendar endCal = (Calendar) cal.clone();
                    endCal.add(Calendar.MINUTE, durationMinutes);

                    if (endCal.getTime().after(end)) {
                        break;
                    }

                    String slotStart = timeFormatter.format(cal.getTime());
                    String slotEnd = timeFormatter.format(endCal.getTime());

                    slots.add(new TimeSlot(slotStart, slotEnd));
                    cal.add(Calendar.MINUTE, intervalMinutes);
                }
            }
        } catch (ParseException e) {
            Log.e("DateTimeUtils", "Error generating available slots: " + e.getMessage());
        }

        return slots;
    }

    // ======================= DAY OF WEEK UTILITIES =======================

    /**
     * Get day of week for a date
     */
    public static String getDayOfWeek(String date) {
        try {
            Date dateObj = dateFormatter.parse(date);
            if (dateObj != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(dateObj);

                String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
                return days[cal.get(Calendar.DAY_OF_WEEK) - 1];
            }
        } catch (ParseException e) {
            Log.e("DateTimeUtils", "Error getting day of week: " + e.getMessage());
        }
        return "";
    }

    /**
     * Check if date falls on weekend
     */
    public static boolean isWeekend(String date) {
        String dayOfWeek = getDayOfWeek(date);
        return "Saturday".equals(dayOfWeek) || "Sunday".equals(dayOfWeek);
    }

    /**
     * Get next occurrence of a specific day
     */
    public static String getNextDayOccurrence(String dayName) {
        String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        int targetDay = -1;

        for (int i = 0; i < days.length; i++) {
            if (days[i].equalsIgnoreCase(dayName)) {
                targetDay = i + 1; // Calendar.DAY_OF_WEEK is 1-based
                break;
            }
        }

        if (targetDay == -1) {
            return getCurrentDate();
        }

        Calendar cal = Calendar.getInstance();
        int currentDay = cal.get(Calendar.DAY_OF_WEEK);
        int daysToAdd = (targetDay - currentDay + 7) % 7;

        if (daysToAdd == 0) {
            daysToAdd = 7; // Next week if it's the same day
        }

        cal.add(Calendar.DAY_OF_MONTH, daysToAdd);
        return dateFormatter.format(cal.getTime());
    }

    // ======================= VALIDATION UTILITIES =======================

    /**
     * Validate date format
     */
    public static boolean isValidDate(String date) {
        try {
            Date dateObj = dateFormatter.parse(date);
            return dateObj != null;
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * Validate time format
     */
    public static boolean isValidTime(String time) {
        try {
            Date timeObj = timeFormatter.parse(time);
            return timeObj != null;
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * Validate that date is within booking advance limit
     */
    public static boolean isWithinAdvanceLimit(String date, int advanceDays) {
        try {
            Date targetDate = dateFormatter.parse(date);
            Date maxAdvanceDate = dateFormatter.parse(getDateFromToday(advanceDays));

            return targetDate != null && maxAdvanceDate != null &&
                    !targetDate.after(maxAdvanceDate);
        } catch (ParseException e) {
            return false;
        }
    }

    // ======================= BUSINESS LOGIC HELPERS =======================

    /**
     * Get business hours for a day (excluding weekends by default)
     */
    public static boolean isBusinessDay(String date) {
        return !isWeekend(date);
    }

    /**
     * Get next business day
     */
    public static String getNextBusinessDay() {
        String nextDay = getDateFromToday(1);
        while (isWeekend(nextDay)) {
            nextDay = addDaysToDate(nextDay, 1);
        }
        return nextDay;
    }

    /**
     * Get working days between two dates
     */
    public static int getWorkingDaysBetween(String startDate, String endDate) {
        int totalDays = getDaysBetween(startDate, endDate);
        int workingDays = 0;

        for (int i = 0; i <= totalDays; i++) {
            String currentDate = addDaysToDate(startDate, i);
            if (isBusinessDay(currentDate)) {
                workingDays++;
            }
        }

        return workingDays;
    }

    // ======================= TIME SLOT CLASS =======================

    public static class TimeSlot {
        private String startTime;
        private String endTime;
        private boolean isAvailable;

        public TimeSlot(String startTime, String endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.isAvailable = true;
        }

        public TimeSlot(String startTime, String endTime, boolean isAvailable) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.isAvailable = isAvailable;
        }

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }

        public boolean isAvailable() {
            return isAvailable;
        }

        public void setAvailable(boolean available) {
            isAvailable = available;
        }

        public String getDisplayTime() {
            return formatTimeForDisplay(startTime) + " - " + formatTimeForDisplay(endTime);
        }

        public int getDurationMinutes() {
            return getTimeDifferenceInMinutes(startTime, endTime);
        }

        @Override
        public String toString() {
            return getDisplayTime() + (isAvailable ? " (Available)" : " (Booked)");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TimeSlot timeSlot = (TimeSlot) o;
            return Objects.equals(startTime, timeSlot.startTime) &&
                    Objects.equals(endTime, timeSlot.endTime);
        }

        @Override
        public int hashCode() {
            return Objects.hash(startTime, endTime);
        }
    }

    // ======================= CALENDAR UTILITIES =======================

    /**
     * Get calendar events for a date range
     */
    public static List<CalendarEvent> getCalendarEvents(List<Booking> bookings) {
        List<CalendarEvent> events = new ArrayList<>();

        for (Booking booking : bookings) {
            CalendarEvent event = new CalendarEvent();
            event.title = booking.getLabName();
            event.description = booking.getPurpose();
            event.date = booking.getDate();
            event.startTime = booking.getStartTime();
            event.endTime = booking.getEndTime();
            event.status = booking.getStatus().getValue();
            event.bookingId = booking.getId();

            events.add(event);
        }

        return events;
    }

    public static class CalendarEvent {
        public String title;
        public String description;
        public String date;
        public String startTime;
        public String endTime;
        public String status;
        public String bookingId;

        public String getDisplayDateTime() {
            return formatDateForDisplay(date) + " at " +
                    formatTimeForDisplay(startTime) + " - " + formatTimeForDisplay(endTime);
        }
    }

    // ======================= REMINDER UTILITIES =======================

    /**
     * Check if booking needs reminder
     */
    public static boolean needsReminder(Booking booking, int reminderHours) {
        if (booking.isReminderSent() || !booking.isApproved()) {
            return false;
        }

        long hoursUntil = getHoursUntilDateTime(booking.getDate(), booking.getStartTime());
        return hoursUntil <= reminderHours && hoursUntil > 0;
    }

    /**
     * Get relative time description (e.g., "in 2 hours", "yesterday")
     */
    public static String getRelativeTimeDescription(String date, String time) {
        try {
            String dateTimeString = date + " " + time;
            Date targetDateTime = dateTimeFormatter.parse(dateTimeString);

            if (targetDateTime != null) {
                long diffInMillis = targetDateTime.getTime() - System.currentTimeMillis();
                long diffInHours = TimeUnit.MILLISECONDS.toHours(Math.abs(diffInMillis));
                long diffInDays = TimeUnit.MILLISECONDS.toDays(Math.abs(diffInMillis));

                if (diffInMillis > 0) { // Future
                    if (diffInHours < 1) {
                        long minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis);
                        return "in " + minutes + " minute" + (minutes != 1 ? "s" : "");
                    } else if (diffInHours < 24) {
                        return "in " + diffInHours + " hour" + (diffInHours != 1 ? "s" : "");
                    } else if (diffInDays == 1) {
                        return "tomorrow";
                    } else {
                        return "in " + diffInDays + " day" + (diffInDays != 1 ? "s" : "");
                    }
                } else { // Past
                    if (diffInHours < 1) {
                        long minutes = TimeUnit.MILLISECONDS.toMinutes(Math.abs(diffInMillis));
                        return minutes + " minute" + (minutes != 1 ? "s" : "") + " ago";
                    } else if (diffInHours < 24) {
                        return diffInHours + " hour" + (diffInHours != 1 ? "s" : "") + " ago";
                    } else if (diffInDays == 1) {
                        return "yesterday";
                    } else {
                        return diffInDays + " day" + (diffInDays != 1 ? "s" : "") + " ago";
                    }
                }
            }
        } catch (ParseException e) {
            Log.e("DateTimeUtils", "Error getting relative time: " + e.getMessage());
        }

        return "unknown";
    }

    // ======================= RECURRENCE UTILITIES =======================

    /**
     * Generate recurring dates based on pattern
     */
    public static List<String> generateRecurringDates(String startDate, String endDate, String pattern) {
        List<String> dates = new ArrayList<>();

        try {
            Date start = dateFormatter.parse(startDate);
            Date end = dateFormatter.parse(endDate);

            if (start != null && end != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(start);

                while (!cal.getTime().after(end)) {
                    dates.add(dateFormatter.format(cal.getTime()));

                    switch (pattern.toLowerCase()) {
                        case "daily":
                            cal.add(Calendar.DAY_OF_MONTH, 1);
                            break;
                        case "weekly":
                            cal.add(Calendar.WEEK_OF_YEAR, 1);
                            break;
                        case "monthly":
                            cal.add(Calendar.MONTH, 1);
                            break;
                        default:
                            cal.add(Calendar.WEEK_OF_YEAR, 1); // Default to weekly
                            break;
                    }
                }
            }
        } catch (ParseException e) {
            Log.e("DateTimeUtils", "Error generating recurring dates: " + e.getMessage());
        }

        return dates;
    }
}