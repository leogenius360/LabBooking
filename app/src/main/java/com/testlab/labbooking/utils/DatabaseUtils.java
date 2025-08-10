package com.testlab.labbooking.utils;

import android.util.Log;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.*;
import com.testlab.labbooking.models.Booking;
import com.testlab.labbooking.models.BookingStatus;
import com.testlab.labbooking.models.Lab;
import com.testlab.labbooking.models.User;

import java.util.*;
import java.util.concurrent.Callable;

public class DatabaseUtils {
    private static final String TAG = "DatabaseUtils";
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Collection names
    public static final String USERS_COLLECTION = "users";
    public static final String LABS_COLLECTION = "labs";
    public static final String BOOKINGS_COLLECTION = "bookings";
    public static final String NOTIFICATIONS_COLLECTION = "notifications";
    public static final String SETTINGS_COLLECTION = "settings";
    public static final String ANALYTICS_COLLECTION = "analytics";

    // Booking statuses (keeping string constants for backward compatibility)
    public static final String STATUS_PENDING = BookingStatus.PENDING.getValue();
    public static final String STATUS_APPROVED = BookingStatus.APPROVED.getValue();
    public static final String STATUS_REJECTED = BookingStatus.REJECTED.getValue();
    public static final String STATUS_CANCELLED = BookingStatus.CANCELLED.getValue();
    public static final String STATUS_COMPLETED = BookingStatus.COMPLETED.getValue();

    // User roles
    public static final String ROLE_USER = "user";
    public static final String ROLE_STUDENT = "student";
    public static final String ROLE_FACULTY = "faculty";
    public static final String ROLE_ADMIN = "admin";

    // Common field names
    public static final String FIELD_CREATED_AT = "createdAt";
    public static final String FIELD_UPDATED_AT = "updatedAt";
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_USER_ID = "userId";
    public static final String FIELD_LAB_ID = "labId";
    public static final String FIELD_DATE = "date";
    public static final String FIELD_START_TIME = "startTime";
    public static final String FIELD_END_TIME = "endTime";
    public static final String FIELD_ACTIVE = "isActive";
    public static final String FIELD_PRIORITY = "priority";
    public static final String FIELD_CATEGORY = "category";

    public static FirebaseFirestore getInstance() {
        return db;
    }

    // ======================= ENHANCED BOOKING QUERIES =======================

    /**
     * Check for booking conflicts with improved logic
     */
    public static Query getConflictingBookingsQuery(String labId, String date,
                                                    String startTime, String endTime) {
        return db.collection(BOOKINGS_COLLECTION)
                .whereEqualTo(FIELD_LAB_ID, labId)
                .whereEqualTo(FIELD_DATE, date)
                .whereIn(FIELD_STATUS, Arrays.asList(STATUS_PENDING, STATUS_APPROVED))
                .orderBy(FIELD_START_TIME);
    }

    /**
     * Get user's bookings with pagination
     */
    public static Query getUserBookingsQuery(String userId, int limit) {
        return db.collection(BOOKINGS_COLLECTION)
                .whereEqualTo(FIELD_USER_ID, userId)
                .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
                .limit(limit);
    }

    /**
     * Get user's upcoming bookings
     */
    public static Query getUserUpcomingBookingsQuery(String userId) {
        String today = DateTimeUtils.getCurrentDate();
        return db.collection(BOOKINGS_COLLECTION)
                .whereEqualTo(FIELD_USER_ID, userId)
                .whereIn(FIELD_STATUS, Arrays.asList(STATUS_PENDING, STATUS_APPROVED))
                .whereGreaterThanOrEqualTo(FIELD_DATE, today)
                .orderBy(FIELD_DATE, Query.Direction.ASCENDING)
                .orderBy(FIELD_START_TIME, Query.Direction.ASCENDING);
    }

    /**
     * Get pending bookings with priority sorting
     */
    public static Query getPendingBookingsQuery() {
        return db.collection(BOOKINGS_COLLECTION)
                .whereEqualTo(FIELD_STATUS, STATUS_PENDING)
                .orderBy(FIELD_PRIORITY, Query.Direction.ASCENDING)
                .orderBy(FIELD_CREATED_AT, Query.Direction.ASCENDING);
    }

    /**
     * Get today's bookings
     */
    public static Query getTodaysBookingsQuery() {
        String today = DateTimeUtils.getCurrentDate();
        return db.collection(BOOKINGS_COLLECTION)
                .whereEqualTo(FIELD_DATE, today)
                .whereIn(FIELD_STATUS, Arrays.asList(STATUS_APPROVED))
                .orderBy(FIELD_START_TIME, Query.Direction.ASCENDING);
    }

    /**
     * Get overdue bookings (approved but not checked out)
     */
    public static Query getOverdueBookingsQuery() {
        String today = DateTimeUtils.getCurrentDate();
        String currentTime = DateTimeUtils.getCurrentTime();

        return db.collection(BOOKINGS_COLLECTION)
                .whereEqualTo(FIELD_STATUS, STATUS_APPROVED)
                .whereEqualTo("checkedIn", true)
                .whereEqualTo("checkedOut", false)
                .whereLessThan(FIELD_DATE, today);
    }

    /**
     * Get lab usage analytics for date range
     */
    public static Query getLabUsageAnalyticsQuery(String labId, String startDate, String endDate) {
        return db.collection(BOOKINGS_COLLECTION)
                .whereEqualTo(FIELD_LAB_ID, labId)
                .whereGreaterThanOrEqualTo(FIELD_DATE, startDate)
                .whereLessThanOrEqualTo(FIELD_DATE, endDate)
                .whereIn(FIELD_STATUS, Arrays.asList(STATUS_APPROVED, STATUS_COMPLETED));
    }

    // ======================= ENHANCED LAB QUERIES =======================

    /**
     * Get available labs for a specific user type
     */
    public static Query getAvailableLabsForUserQuery(String userType) {
        return db.collection(LABS_COLLECTION)
                .whereEqualTo(FIELD_ACTIVE, true)
                .whereEqualTo("maintenanceMode", false)
                .whereArrayContains("allowedUserTypes", userType.toLowerCase())
                .orderBy(FIELD_PRIORITY, Query.Direction.ASCENDING)
                .orderBy("name", Query.Direction.ASCENDING);
    }

    /**
     * Get labs by category
     */
    public static Query getLabsByCategoryQuery(String category) {
        return db.collection(LABS_COLLECTION)
                .whereEqualTo(FIELD_ACTIVE, true)
                .whereEqualTo(FIELD_CATEGORY, category)
                .orderBy(FIELD_PRIORITY, Query.Direction.ASCENDING);
    }

    /**
     * Search labs by name or description
     */
    public static Query searchLabsQuery(String searchTerm) {
        // Note: Firestore doesn't support full-text search natively
        // This is a basic implementation - consider using Algolia for better search
        return db.collection(LABS_COLLECTION)
                .whereEqualTo(FIELD_ACTIVE, true)
                .whereGreaterThanOrEqualTo("name", searchTerm)
                .whereLessThanOrEqualTo("name", searchTerm + "\uf8ff")
                .orderBy("name");
    }

    /**
     * Get labs requiring maintenance
     */
    public static Query getMaintenanceLabsQuery() {
        return db.collection(LABS_COLLECTION)
                .whereEqualTo("maintenanceMode", true)
                .orderBy("name", Query.Direction.ASCENDING);
    }

    // ======================= ENHANCED BOOKING OPERATIONS =======================

    /**
     * Create booking with validation
     */
    public static Task<DocumentReference> createBookingWithValidation(Booking booking) {
        if (!booking.isValid()) {
            return Tasks.forException(new IllegalArgumentException(
                    "Invalid booking data: " + String.join(", ", booking.getValidationErrors())));
        }

        // Check for conflicts first
        return isTimeSlotAvailable(booking.getLabId(), booking.getDate(),
                booking.getStartTime(), booking.getEndTime())
                .continueWithTask(task -> {
                    if (task.isSuccessful() && task.getResult()) {
                        return createBooking(booking);
                    } else {
                        return Tasks.forException(new IllegalStateException("Time slot is not available"));
                    }
                });
    }

    /**
     * Create booking (internal method)
     */
    private static Task<DocumentReference> createBooking(Booking booking) {
        return db.collection(BOOKINGS_COLLECTION).add(booking);
    }

    /**
     * Batch approve multiple bookings
     */
    public static Task<Void> batchApproveBookings(List<String> bookingIds, String adminId, String notes) {
        WriteBatch batch = db.batch();

        for (String bookingId : bookingIds) {
            DocumentReference bookingRef = db.collection(BOOKINGS_COLLECTION).document(bookingId);
            Map<String, Object> updates = new HashMap<>();
            updates.put(FIELD_STATUS, STATUS_APPROVED);
            updates.put("reviewedBy", adminId);
            updates.put("reviewedAt", FieldValue.serverTimestamp());
            updates.put("adminNotes", notes);
            updates.put(FIELD_UPDATED_AT, FieldValue.serverTimestamp());

            batch.update(bookingRef, updates);
        }

        return batch.commit();
    }

    /**
     * Cancel booking with automatic refund calculation
     */
    public static Task<Void> cancelBookingWithRefund(String bookingId, String reason, String userId) {
        return db.collection(BOOKINGS_COLLECTION).document(bookingId).get()
                .continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot doc = task.getResult();
                        if (doc.exists()) {
                            Booking booking = doc.toObject(Booking.class);

                            Map<String, Object> updates = new HashMap<>();
                            updates.put(FIELD_STATUS, STATUS_CANCELLED);
                            updates.put("cancellationReason", reason);
                            updates.put(FIELD_UPDATED_AT, FieldValue.serverTimestamp());

                            // Calculate refund if applicable
                            if (booking != null && booking.getTotalCost() > 0) {
                                double refundAmount = calculateRefundAmount(booking);
                                updates.put("refundAmount", refundAmount);
                                updates.put("refundProcessed", false);
                            }

                            return db.collection(BOOKINGS_COLLECTION).document(bookingId).update(updates);
                        }
                    }
                    return Tasks.forException(new IllegalStateException("Booking not found"));
                });
    }

    // ======================= USER MANAGEMENT =======================

    /**
     * Create user with role-based setup
     */
    public static Task<Void> createUserWithDefaults(String userId, User user) {
        return db.collection(USERS_COLLECTION)
                .document(userId)
                .set(user)
                .continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        // Create user analytics document
                        return createUserAnalytics(userId);
                    }
                    return task;
                });
    }

    /**
     * Update user last login
     */
    public static Task<Void> updateUserLastLogin(String userId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastLoginAt", FieldValue.serverTimestamp());

        return db.collection(USERS_COLLECTION)
                .document(userId)
                .update(updates);
    }

    /**
     * Get users by role with pagination
     */
    public static Query getUsersByRoleQuery(String role, int limit) {
        return db.collection(USERS_COLLECTION)
                .whereEqualTo("role", role)
                .whereEqualTo(FIELD_ACTIVE, true)
                .orderBy("name", Query.Direction.ASCENDING)
                .limit(limit);
    }

    // ======================= ANALYTICS AND STATISTICS =======================

    /**
     * Get comprehensive booking statistics
     */
    public static void getEnhancedBookingStatistics(EnhancedStatisticsCallback callback) {
        Map<String, Object> stats = new HashMap<>();

        // Get all-time statistics
        getAllBookingsQuery().get().addOnSuccessListener(allSnapshot -> {
            if (allSnapshot != null) {
                stats.put("totalBookings", allSnapshot.size());

                int pending = 0, approved = 0, rejected = 0, cancelled = 0, completed = 0;
                double totalRevenue = 0;

                for (DocumentSnapshot doc : allSnapshot.getDocuments()) {
                    Booking booking = doc.toObject(Booking.class);
                    if (booking != null) {
                        switch (booking.getStatus()) {
                            case PENDING:
                                pending++;
                                break;
                            case APPROVED:
                                approved++;
                                break;
                            case REJECTED:
                                rejected++;
                                break;
                            case CANCELLED:
                                cancelled++;
                                break;
                            case COMPLETED:
                                completed++;
                                totalRevenue += booking.getTotalCost();
                                break;
                        }
                    }
                }

                stats.put("pending", pending);
                stats.put("approved", approved);
                stats.put("rejected", rejected);
                stats.put("cancelled", cancelled);
                stats.put("completed", completed);
                stats.put("totalRevenue", totalRevenue);

                // Get today's statistics
                getTodaysBookingsQuery().get().addOnSuccessListener(todaySnapshot -> {
                    stats.put("todayBookings", todaySnapshot != null ? todaySnapshot.size() : 0);

                    // Get this week's statistics
                    String weekStart = DateTimeUtils.getWeekStartDate();
                    String weekEnd = DateTimeUtils.getWeekEndDate();

                    getBookingsByDateRangeQuery(weekStart, weekEnd).get()
                            .addOnSuccessListener(weekSnapshot -> {
                                stats.put("weekBookings", weekSnapshot != null ? weekSnapshot.size() : 0);
                                callback.onStatisticsReceived(stats);
                            })
                            .addOnFailureListener(e -> callback.onError("Error loading week statistics"));

                }).addOnFailureListener(e -> callback.onError("Error loading today statistics"));
            }
        }).addOnFailureListener(e -> callback.onError("Error loading statistics"));
    }

    /**
     * Get lab utilization statistics
     */
    public static void getLabUtilizationStats(String labId, String startDate, String endDate,
                                              UtilizationCallback callback) {
        getLabUsageAnalyticsQuery(labId, startDate, endDate).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot != null) {
                        int totalBookings = querySnapshot.size();
                        int totalHoursBooked = 0;
                        Map<String, Integer> dailyUsage = new HashMap<>();

                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Booking booking = doc.toObject(Booking.class);
                            if (booking != null) {
                                totalHoursBooked += booking.getDurationMinutes();

                                String date = booking.getDate();
                                dailyUsage.put(date, dailyUsage.getOrDefault(date, 0) + 1);
                            }
                        }

                        Map<String, Object> utilizationData = new HashMap<>();
                        utilizationData.put("totalBookings", totalBookings);
                        utilizationData.put("totalHoursBooked", totalHoursBooked / 60.0);
                        utilizationData.put("dailyUsage", dailyUsage);

                        callback.onUtilizationDataReceived(utilizationData);
                    }
                })
                .addOnFailureListener(e -> callback.onError("Error calculating utilization"));
    }

    /**
     * Check user booking limits
     */
    public static Task<BookingLimitResult> checkUserBookingLimits(String userId, String date) {
        return getUserActiveBookingsQuery(userId).get()
                .continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot snapshot = task.getResult();

                        // Get user data to check limits
                        return getUserById(userId).continueWith(userTask -> {
                            if (userTask.isSuccessful()) {
                                User user = userTask.getResult().toObject(User.class);

                                if (user != null && snapshot != null) {
                                    int activeBookings = snapshot.size();
                                    int weeklyHours = calculateWeeklyHours(snapshot.getDocuments(), date);

                                    BookingLimitResult result = new BookingLimitResult();
                                    result.canBook = activeBookings < user.getMaxSimultaneousBookings() &&
                                            weeklyHours < user.getMaxWeeklyHours();
                                    result.activeBookings = activeBookings;
                                    result.maxBookings = user.getMaxSimultaneousBookings();
                                    result.weeklyHours = weeklyHours;
                                    result.maxWeeklyHours = user.getMaxWeeklyHours();

                                    return result;
                                }
                            }
                            return new BookingLimitResult(); // Default allow
                        });
                    }
                    return Tasks.forException(task.getException());
                });
    }

    // ======================= ENHANCED VALIDATION =======================

    /**
     * Comprehensive time slot validation
     */
    public static Task<ValidationResult> validateBookingRequest(Booking booking) {
        return Tasks.call(new Callable<ValidationResult>() {
            @Override
            public ValidationResult call() throws Exception {
                ValidationResult result = new ValidationResult();

                // Basic validation
                if (!booking.isValid()) {
                    result.isValid = false;
                    result.errors = booking.getValidationErrors();
                    return result;
                }

                // Check if lab exists and is available
                return getLabById(booking.getLabId()).continueWith(labTask -> {
                    if (labTask.isSuccessful()) {
                        Lab lab = labTask.getResult().toObject(Lab.class);

                        if (lab == null) {
                            result.isValid = false;
                            result.errors.add("Lab not found");
                            return result;
                        }

                        if (!lab.isBookingAllowed()) {
                            result.isValid = false;
                            result.errors.add("Lab is not available for booking");
                            return result;
                        }

                        // Check capacity
                        if (booking.getNumberOfParticipants() > lab.getCapacity()) {
                            result.isValid = false;
                            result.errors.add("Number of participants exceeds lab capacity");
                            return result;
                        }

                        // Check if booking is within lab hours
                        if (!DateTimeUtils.isTimeWithinRange(booking.getStartTime(),
                                lab.getOpenTime(), lab.getCloseTime()) ||
                                !DateTimeUtils.isTimeWithinRange(booking.getEndTime(),
                                        lab.getOpenTime(), lab.getCloseTime())) {
                            result.isValid = false;
                            result.errors.add("Booking time is outside lab operating hours");
                            return result;
                        }

                        // Check duration limits
                        int durationMinutes = booking.getDurationMinutes();
                        if (durationMinutes < lab.getMinBookingMinutes()) {
                            result.isValid = false;
                            result.errors.add("Booking duration is less than minimum required");
                            return result;
                        }

                        if (durationMinutes > lab.getMaxBookingHours() * 60) {
                            result.isValid = false;
                            result.errors.add("Booking duration exceeds maximum allowed");
                            return result;
                        }

                        result.isValid = true;
                        return result;
                    }

                    result.isValid = false;
                    result.errors.add("Error validating lab information");
                    return result;
                }).getResult();
            }
        });
    }

    // ======================= NOTIFICATION MANAGEMENT =======================

    /**
     * Create notification record
     */
    public static Task<DocumentReference> createNotification(String userId, String title,
                                                             String message, String type,
                                                             String relatedId) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("title", title);
        notification.put("message", message);
        notification.put("type", type); // "booking", "reminder", "admin", "system"
        notification.put("relatedId", relatedId);
        notification.put("read", false);
        notification.put("createdAt", FieldValue.serverTimestamp());

        return db.collection(NOTIFICATIONS_COLLECTION).add(notification);
    }

    /**
     * Mark notifications as read
     */
    public static Task<Void> markNotificationsAsRead(String userId, List<String> notificationIds) {
        WriteBatch batch = db.batch();

        for (String notificationId : notificationIds) {
            DocumentReference notifRef = db.collection(NOTIFICATIONS_COLLECTION).document(notificationId);
            batch.update(notifRef, "read", true, "readAt", FieldValue.serverTimestamp());
        }

        return batch.commit();
    }

    // ======================= HELPER CLASSES =======================

    public static class BookingLimitResult {
        public boolean canBook = true;
        public int activeBookings = 0;
        public int maxBookings = 0;
        public int weeklyHours = 0;
        public int maxWeeklyHours = 0;
        public String message = "";
    }

    public static class ValidationResult {
        public boolean isValid = true;
        public List<String> errors = new ArrayList<>();
        public List<String> warnings = new ArrayList<>();
    }

    // ======================= CALLBACK INTERFACES =======================

    public interface EnhancedStatisticsCallback {
        void onStatisticsReceived(Map<String, Object> statistics);
        void onError(String error);
    }

    public interface UtilizationCallback {
        void onUtilizationDataReceived(Map<String, Object> utilizationData);
        void onError(String error);
    }

    public interface ValidationCallback {
        void onValidationComplete(ValidationResult result);
        void onError(String error);
    }

    public interface BookingLimitCallback {
        void onLimitCheckComplete(BookingLimitResult result);
        void onError(String error);
    }

    // ======================= UTILITY METHODS =======================

    private static int calculateWeeklyHours(List<DocumentSnapshot> bookings, String targetDate) {
        String weekStart = DateTimeUtils.getWeekStartDate(targetDate);
        String weekEnd = DateTimeUtils.getWeekEndDate(targetDate);

        int totalMinutes = 0;
        for (DocumentSnapshot doc : bookings) {
            Booking booking = doc.toObject(Booking.class);
            if (booking != null &&
                    DateTimeUtils.isDateInRange(booking.getDate(), weekStart, weekEnd)) {
                totalMinutes += booking.getDurationMinutes();
            }
        }

        return totalMinutes / 60;
    }

    private static double calculateRefundAmount(Booking booking) {
        // Implement refund policy logic
        // For example: full refund if cancelled 24h before, 50% if same day, etc.
        long hoursUntilStart = DateTimeUtils.getHoursUntilDateTime(booking.getDate(), booking.getStartTime());

        if (hoursUntilStart >= 24) {
            return booking.getTotalCost(); // Full refund
        } else if (hoursUntilStart >= 4) {
            return booking.getTotalCost() * 0.5; // 50% refund
        } else {
            return 0; // No refund
        }
    }

    private static Task<Void> createUserAnalytics(String userId) {
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("userId", userId);
        analytics.put("totalBookings", 0);
        analytics.put("totalHoursBooked", 0);
        analytics.put("favoriteLabId", null);
        analytics.put("lastBookingDate", null);
        analytics.put("createdAt", FieldValue.serverTimestamp());

        return db.collection(ANALYTICS_COLLECTION)
                .document(userId)
                .set(analytics);
    }

    /**
     * Enhanced error message formatting
     */
    public static String getFormattedErrorMessage(Exception e) {
        if (e == null) return "Unknown error occurred";

        String message = e.getMessage();
        if (message == null || message.isEmpty()) {
            return "An error occurred while processing your request";
        }

        // Handle specific Firebase errors
        if (message.contains("PERMISSION_DENIED")) {
            return "You don't have permission to perform this action";
        } else if (message.contains("UNAVAILABLE")) {
            return "Service is temporarily unavailable. Please try again later";
        } else if (message.contains("DEADLINE_EXCEEDED")) {
            return "Request timed out. Please check your connection and try again";
        } else if (message.contains("ALREADY_EXISTS")) {
            return "This record already exists";
        } else if (message.contains("NOT_FOUND")) {
            return "The requested item was not found";
        } else if (message.contains("INVALID_ARGUMENT")) {
            return "Invalid data provided. Please check your input";
        }

        return message;
    }

    /**
     * Log database operations with more context
     */
    public static void logOperation(String operation, String collection, String documentId,
                                    String userId) {
        Log.d(TAG, String.format("Database Operation: %s on %s/%s by user %s",
                operation, collection, documentId != null ? documentId : "new",
                userId != null ? userId : "system"));
    }

    // ======================= LEGACY METHOD COMPATIBILITY =======================

    // Keep existing methods for backward compatibility
    public static Query getUserBookingsQuery(String userId) {
        return getUserBookingsQuery(userId, 50); // Default limit
    }

    public static Query getUserActiveBookingsQuery(String userId) {
        return getUserUpcomingBookingsQuery(userId);
    }

    public static Query getActiveLabsQuery() {
        return db.collection(LABS_COLLECTION)
                .whereEqualTo(FIELD_ACTIVE, true)
                .whereEqualTo("maintenanceMode", false)
                .orderBy(FIELD_PRIORITY, Query.Direction.ASCENDING)
                .orderBy("name", Query.Direction.ASCENDING);
    }

    public static Query getAllLabsQuery() {
        return db.collection(LABS_COLLECTION)
                .orderBy(FIELD_PRIORITY, Query.Direction.ASCENDING)
                .orderBy("name", Query.Direction.ASCENDING);
    }

    public static Task<DocumentSnapshot> getLabById(String labId) {
        return db.collection(LABS_COLLECTION).document(labId).get();
    }

    public static Task<DocumentSnapshot> getUserById(String userId) {
        return db.collection(USERS_COLLECTION).document(userId).get();
    }

    public static Query getAllUsersQuery() {
        return db.collection(USERS_COLLECTION)
                .orderBy("name", Query.Direction.ASCENDING);
    }

    public static Query getAdminUsersQuery() {
        return db.collection(USERS_COLLECTION)
                .whereEqualTo("role", ROLE_ADMIN);
    }

    public static Query getAllBookingsQuery() {
        return db.collection(BOOKINGS_COLLECTION)
                .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING);
    }

    public static Query getLabBookingsQuery(String labId) {
        return db.collection(BOOKINGS_COLLECTION)
                .whereEqualTo(FIELD_LAB_ID, labId)
                .orderBy(FIELD_DATE, Query.Direction.DESCENDING)
                .orderBy(FIELD_START_TIME, Query.Direction.ASCENDING);
    }

    public static Query getBookingsByDateRangeQuery(String startDate, String endDate) {
        return db.collection(BOOKINGS_COLLECTION)
                .whereGreaterThanOrEqualTo(FIELD_DATE, startDate)
                .whereLessThanOrEqualTo(FIELD_DATE, endDate)
                .orderBy(FIELD_DATE, Query.Direction.ASCENDING);
    }

    public static Task<Void> updateBookingStatus(String bookingId, String status, String adminNotes) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(FIELD_STATUS, status);
        updates.put(FIELD_UPDATED_AT, FieldValue.serverTimestamp());
        updates.put("reviewedAt", FieldValue.serverTimestamp());

        if (adminNotes != null && !adminNotes.trim().isEmpty()) {
            updates.put("adminNotes", adminNotes);
        }

        return db.collection(BOOKINGS_COLLECTION).document(bookingId).update(updates);
    }

    public static Task<Void> cancelBooking(String bookingId, String reason) {
        return cancelBookingWithRefund(bookingId, reason, null);
    }

    public static Task<Void> deleteBooking(String bookingId) {
        return db.collection(BOOKINGS_COLLECTION).document(bookingId).delete();
    }

    public static Task<DocumentReference> createLab(Lab lab) {
        return db.collection(LABS_COLLECTION).add(lab);
    }

    public static Task<Void> updateLab(String labId, Lab lab) {
        return db.collection(LABS_COLLECTION).document(labId).set(lab);
    }

    public static Task<Void> updateLabStatus(String labId, boolean isActive) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(FIELD_ACTIVE, isActive);
        updates.put(FIELD_UPDATED_AT, FieldValue.serverTimestamp());

        return db.collection(LABS_COLLECTION).document(labId).update(updates);
    }

    public static Task<Void> deleteLab(String labId) {
        return db.collection(LABS_COLLECTION).document(labId).delete();
    }

    public static Task<Void> saveUserProfile(String userId, User user) {
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(new Date());
        }

        return db.collection(USERS_COLLECTION).document(userId).set(user);
    }

    public static Task<Void> updateUserRole(String userId, boolean isAdmin) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isAdmin", isAdmin);
        updates.put("role", isAdmin ? ROLE_ADMIN : ROLE_USER);
        updates.put(FIELD_UPDATED_AT, FieldValue.serverTimestamp());

        return db.collection(USERS_COLLECTION).document(userId).update(updates);
    }

    public static Task<Boolean> isTimeSlotAvailable(String labId, String date, String startTime, String endTime) {
        return getConflictingBookingsQuery(labId, date, startTime, endTime)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            // Check for actual conflicts
                            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                Booking existingBooking = doc.toObject(Booking.class);
                                if (existingBooking != null &&
                                        DateTimeUtils.doTimeSlotsOverlap(startTime, endTime,
                                                existingBooking.getStartTime(), existingBooking.getEndTime())) {
                                    return false;
                                }
                            }
                        }
                        return true;
                    }
                    return false;
                });
    }

    public static Task<Void> batchUpdateBookings(Map<String, Map<String, Object>> bookingUpdates) {
        WriteBatch batch = db.batch();

        for (Map.Entry<String, Map<String, Object>> entry : bookingUpdates.entrySet()) {
            DocumentReference bookingRef = db.collection(BOOKINGS_COLLECTION).document(entry.getKey());
            Map<String, Object> updates = entry.getValue();
            updates.put(FIELD_UPDATED_AT, FieldValue.serverTimestamp());

            batch.update(bookingRef, updates);
        }

        return batch.commit();
    }

    // Legacy callback interfaces for backward compatibility
    public interface StatisticsCallback {
        void onStatisticsReceived(Map<String, Integer> statistics);
        void onError(String error);
    }

    public interface BookingValidationCallback {
        void onValidationComplete(boolean isValid, String message);
    }

    public interface OperationCallback {
        void onSuccess();
        void onFailure(String error);
    }

    // Legacy statistics method for backward compatibility
    public static void getBookingStatistics(StatisticsCallback callback) {
        getEnhancedBookingStatistics(new EnhancedStatisticsCallback() {
            @Override
            public void onStatisticsReceived(Map<String, Object> statistics) {
                Map<String, Integer> legacyStats = new HashMap<>();
                legacyStats.put("pending", (Integer) statistics.getOrDefault("pending", 0));
                legacyStats.put("approved", (Integer) statistics.getOrDefault("approved", 0));
                legacyStats.put("rejected", (Integer) statistics.getOrDefault("rejected", 0));
                legacyStats.put("total", (Integer) statistics.getOrDefault("totalBookings", 0));
                callback.onStatisticsReceived(legacyStats);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
}