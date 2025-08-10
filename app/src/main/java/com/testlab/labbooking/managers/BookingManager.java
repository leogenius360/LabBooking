package com.testlab.labbooking.managers;

import android.util.Log;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.testlab.labbooking.models.Booking;
import com.testlab.labbooking.models.BookingStatus;
import com.testlab.labbooking.models.Lab;
import com.testlab.labbooking.models.User;
import com.testlab.labbooking.utils.AuthUtils;
import com.testlab.labbooking.utils.DatabaseUtils;
import com.testlab.labbooking.utils.DateTimeUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * Simplified booking management class that handles the complete booking workflow
 */
public class BookingManager {
    private static final String TAG = "BookingManager";

    // ======================= BOOKING CREATION WORKFLOW =======================

    /**
     * Complete booking creation workflow with all validations
     */
    public static Task<BookingResult> createBooking(String labId, String date, String startTime,
                                                    String endTime, String purpose,
                                                    int numberOfParticipants,
                                                    List<String> requiredResources) {

        return AuthUtils.getCurrentUserData(new AuthUtils.UserDataCallback() {
            @Override
            public void onUserDataReceived(User user) {
                // Create booking object
                Booking booking = new Booking();
                booking.setLabId(labId);
                booking.setUserId(user.getId());
                booking.setUserName(user.getName());
                booking.setUserEmail(user.getEmail());
                booking.setUserPhone(user.getPhoneNumber());
                booking.setDate(date);
                booking.setStartTime(startTime);
                booking.setEndTime(endTime);
                booking.setPurpose(purpose);
                booking.setNumberOfParticipants(numberOfParticipants);
                booking.setRequiredResources(requiredResources);

                // Step 1: Check user permissions and limits
                checkUserBookingPermissions(user, labId, date)
                        .continueWithTask(permissionTask -> {
                            if (!permissionTask.isSuccessful() || !permissionTask.getResult().canBook) {
                                return Tasks.forException(new IllegalStateException(
                                        permissionTask.getResult().message));
                            }

                            // Step 2: Get lab data and set booking details
                            return DatabaseUtils.getLabById(labId);
                        })
                        .continueWithTask(labTask -> {
                            if (labTask.isSuccessful() && labTask.getResult().exists()) {
                                Lab lab = labTask.getResult().toObject(Lab.class);
                                if (lab != null) {
                                    booking.setLabName(lab.getName());

                                    // Calculate cost
                                    double hours = booking.getDurationHours();
                                    booking.setTotalCost(hours * lab.getHourlyRate());

                                    // Set approval requirement
                                    if (!lab.isRequiresApproval() || user.isCanBookWithoutApproval()) {
                                        booking.setStatus(BookingStatus.APPROVED);
                                    }

                                    // Step 3: Validate booking details
                                    return DatabaseUtils.validateBookingRequest(booking);
                                }
                            }
                            return Tasks.forException(new IllegalStateException("Lab not found"));
                        })
                        .continueWithTask(validationTask -> {
                            if (validationTask.isSuccessful()) {
                                DatabaseUtils.ValidationResult validation = validationTask.getResult();
                                if (!validation.isValid) {
                                    return Tasks.forException(new IllegalArgumentException(
                                            String.join(", ", validation.errors)));
                                }

                                // Step 4: Create the booking
                                return DatabaseUtils.createBookingWithValidation(booking);
                            }
                            return Tasks.forException(Objects.requireNonNull(validationTask.getException()));
                        })
                        .continueWith(createTask -> {
                            BookingResult result = new BookingResult();

                            if (createTask.isSuccessful()) {
                                DocumentReference docRef = createTask.getResult();
                                booking.setId(docRef.getId());

                                result.success = true;
                                result.booking = booking;
                                result.message = booking.getStatus() == BookingStatus.APPROVED ?
                                        "Booking created and approved!" :
                                        "Booking created and pending approval";

                                // Send notification
                                NotificationManager.sendBookingCreatedNotification(booking);

                                Log.d(TAG, "Booking created successfully: " + docRef.getId());
                            } else {
                                result.success = false;
                                result.message = DatabaseUtils.getFormattedErrorMessage(createTask.getException());
                                Log.e(TAG, "Error creating booking", createTask.getException());
                            }

                            return result;
                        });
            }

            @Override
            public void onError(String error) {
                // Handle user data error
            }
        });
    }

    /**
     * Quick booking creation for simple use cases
     */
    public static Task<BookingResult> quickBooking(String labId, String date, String startTime,
                                                   String endTime, String purpose) {
        return createBooking(labId, date, startTime, endTime, purpose, 1, null);
    }

    // ======================= BOOKING MANAGEMENT =======================

    /**
     * Approve booking with notification
     */
    public static Task<BookingResult> approveBooking(String bookingId, String adminNotes) {
        return updateBookingWithWorkflow(bookingId, BookingStatus.APPROVED, adminNotes,
                "Booking approved successfully");
    }

    /**
     * Reject booking with notification
     */
    public static Task<BookingResult> rejectBooking(String bookingId, String reason) {
        return updateBookingWithWorkflow(bookingId, BookingStatus.REJECTED, reason,
                "Booking rejected");
    }

    /**
     * Cancel booking with workflow
     */
    public static Task<BookingResult> cancelBooking(String bookingId, String reason) {
        return DatabaseUtils.getInstance().collection(DatabaseUtils.BOOKINGS_COLLECTION)
                .document(bookingId)
                .get()
                .continueWithTask(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        Booking booking = task.getResult().toObject(Booking.class);
                        if (booking != null && booking.canBeCancelled()) {
                            return DatabaseUtils.cancelBookingWithRefund(bookingId, reason, booking.getUserId())
                                    .continueWith(cancelTask -> {
                                        BookingResult result = new BookingResult();
                                        if (cancelTask.isSuccessful()) {
                                            booking.cancel(reason);
                                            result.success = true;
                                            result.booking = booking;
                                            result.message = "Booking cancelled successfully";

                                            // Send cancellation notification
                                            NotificationManager.sendBookingCancelledNotification(booking, reason);
                                        } else {
                                            result.success = false;
                                            result.message = DatabaseUtils.getFormattedErrorMessage(cancelTask.getException());
                                        }
                                        return result;
                                    });
                        }
                    }
                    return Tasks.forResult(new BookingResult(false, "Cannot cancel this booking"));
                });
    }

    /**
     * Check-in to booking
     */
    public static Task<BookingResult> checkInBooking(String bookingId) {
        return updateBookingCheckInOut(bookingId, true, false, "Checked in successfully");
    }

    /**
     * Check-out from booking
     */
    public static Task<BookingResult> checkOutBooking(String bookingId) {
        return updateBookingCheckInOut(bookingId, false, true, "Checked out successfully");
    }

    // ======================= BOOKING QUERIES SIMPLIFIED =======================

    /**
     * Get user's dashboard data
     */
    public static Task<UserDashboardData> getUserDashboard(String userId) {
        return Tasks.call(new Callable<UserDashboardData>() {
            @Override
            public UserDashboardData call() throws Exception {
                UserDashboardData dashboard = new UserDashboardData();

                // Get user's upcoming bookings
                DatabaseUtils.getUserUpcomingBookingsQuery(userId).get()
                        .addOnSuccessListener(upcomingSnapshot -> {
                            if (upcomingSnapshot != null) {
                                dashboard.upcomingBookings = upcomingSnapshot.toObjects(Booking.class);
                            }
                        });

                // Get user's recent bookings
                DatabaseUtils.getUserBookingsQuery(userId, 10).get()
                        .addOnSuccessListener(recentSnapshot -> {
                            if (recentSnapshot != null) {
                                dashboard.recentBookings = recentSnapshot.toObjects(Booking.class);
                            }
                        });

                return dashboard;
            }
        });
    }

    /**
     * Get available time slots for a lab on a specific date
     */
    public static Task<List<DateTimeUtils.TimeSlot>> getAvailableTimeSlots(String labId, String date,
                                                                           int durationMinutes) {
        return DatabaseUtils.getLabById(labId)
                .continueWithTask(labTask -> {
                    if (labTask.isSuccessful() && labTask.getResult().exists()) {
                        Lab lab = labTask.getResult().toObject(Lab.class);
                        if (lab != null) {
                            // Generate all possible slots
                            List<DateTimeUtils.TimeSlot> allSlots = DateTimeUtils.getAvailableSlots(
                                    lab.getOpenTime(), lab.getCloseTime(), durationMinutes, 30);

                            // Check which slots are available
                            return DatabaseUtils.getLabBookingsQuery(labId).get()
                                    .continueWith(bookingsTask -> {
                                        if (bookingsTask.isSuccessful()) {
                                            List<Booking> existingBookings = bookingsTask.getResult().toObjects(Booking.class);

                                            // Mark unavailable slots
                                            for (DateTimeUtils.TimeSlot slot : allSlots) {
                                                for (Booking booking : existingBookings) {
                                                    if (booking.getDate().equals(date) &&
                                                            booking.isActive() &&
                                                            DateTimeUtils.doTimeSlotsOverlap(
                                                                    slot.getStartTime(), slot.getEndTime(),
                                                                    booking.getStartTime(), booking.getEndTime())) {
                                                        slot.setAvailable(false);
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                        return allSlots;
                                    });
                        }
                    }
                    return Tasks.forException(new IllegalStateException("Lab not found"));
                });
    }

    // ======================= VALIDATION HELPERS =======================

    private static Task<DatabaseUtils.BookingLimitResult> checkUserBookingPermissions(User user, String labId, String date) {
        return DatabaseUtils.checkUserBookingLimits(user.getId(), date)
                .continueWithTask(limitsTask -> {
                    DatabaseUtils.BookingLimitResult result = limitsTask.getResult();

                    if (result.canBook) {
                        // Additional checks
                        if (user.isLabRestricted(labId)) {
                            result.canBook = false;
                            result.message = "You are restricted from booking this lab";
                        } else if (!user.isActive()) {
                            result.canBook = false;
                            result.message = "Your account is inactive";
                        } else if (!user.isVerified()) {
                            result.canBook = false;
                            result.message = "Please verify your email before booking";
                        }
                    } else {
                        if (result.activeBookings >= result.maxBookings) {
                            result.message = "You have reached your maximum concurrent bookings (" +
                                    result.maxBookings + ")";
                        } else if (result.weeklyHours >= result.maxWeeklyHours) {
                            result.message = "You have reached your weekly booking limit (" +
                                    result.maxWeeklyHours + " hours)";
                        }
                    }

                    return Tasks.forResult(result);
                });
    }

    private static Task<BookingResult> updateBookingWithWorkflow(String bookingId, BookingStatus newStatus,
                                                                 String notes, String successMessage) {
        return DatabaseUtils.getInstance().collection(DatabaseUtils.BOOKINGS_COLLECTION)
                .document(bookingId)
                .get()
                .continueWithTask(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        Booking booking = task.getResult().toObject(Booking.class);
                        if (booking != null) {
                            String currentUserId = AuthUtils.getCurrentUserId();

                            return DatabaseUtils.updateBookingStatus(bookingId, newStatus.getValue(), notes)
                                    .continueWith(updateTask -> {
                                        BookingResult result = new BookingResult();

                                        if (updateTask.isSuccessful()) {
                                            booking.setStatus(newStatus);
                                            booking.setAdminNotes(notes);

                                            result.success = true;
                                            result.booking = booking;
                                            result.message = successMessage;

                                            // Send appropriate notification
                                            if (newStatus == BookingStatus.APPROVED) {
                                                NotificationManager.sendBookingApprovedNotification(booking);
                                            } else if (newStatus == BookingStatus.REJECTED) {
                                                NotificationManager.sendBookingRejectedNotification(booking, notes);
                                            }

                                            Log.d(TAG, "Booking " + bookingId + " updated to " + newStatus);
                                        } else {
                                            result.success = false;
                                            result.message = DatabaseUtils.getFormattedErrorMessage(updateTask.getException());
                                        }

                                        return result;
                                    });
                        }
                    }
                    return Tasks.forResult(new BookingResult(false, "Booking not found"));
                });
    }

    private static Task<BookingResult> updateBookingCheckInOut(String bookingId, boolean checkIn,
                                                               boolean checkOut, String successMessage) {
        return DatabaseUtils.getInstance().collection(DatabaseUtils.BOOKINGS_COLLECTION)
                .document(bookingId)
                .get()
                .continueWithTask(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        Booking booking = task.getResult().toObject(Booking.class);
                        if (booking != null) {
                            if (checkIn) {
                                booking.checkIn();
                                booking.setStatus(BookingStatus.IN_PROGRESS);
                            }
                            if (checkOut) {
                                booking.checkOut();
                                // Status is set to COMPLETED in checkOut() method
                            }

                            return DatabaseUtils.getInstance().collection(DatabaseUtils.BOOKINGS_COLLECTION)
                                    .document(bookingId)
                                    .set(booking)
                                    .continueWith(updateTask -> {
                                        BookingResult result = new BookingResult();

                                        if (updateTask.isSuccessful()) {
                                            result.success = true;
                                            result.booking = booking;
                                            result.message = successMessage;

                                            if (checkOut) {
                                                NotificationManager.sendBookingCompletedNotification(booking);
                                            }
                                        } else {
                                            result.success = false;
                                            result.message = DatabaseUtils.getFormattedErrorMessage(updateTask.getException());
                                        }

                                        return result;
                                    });
                        }
                    }
                    return Tasks.forResult(new BookingResult(false, "Booking not found"));
                });
    }

    // ======================= QUICK ACCESS METHODS =======================

    /**
     * Get today's user bookings
     */
    public static Task<List<Booking>> getTodaysBookings(String userId) {
        String today = DateTimeUtils.getCurrentDate();
        return DatabaseUtils.getInstance().collection(DatabaseUtils.BOOKINGS_COLLECTION)
                .whereEqualTo(DatabaseUtils.FIELD_USER_ID, userId)
                .whereEqualTo(DatabaseUtils.FIELD_DATE, today)
                .whereIn(DatabaseUtils.FIELD_STATUS,
                        java.util.Arrays.asList(BookingStatus.APPROVED.getValue(),
                                BookingStatus.IN_PROGRESS.getValue()))
                .orderBy(DatabaseUtils.FIELD_START_TIME)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        return task.getResult().toObjects(Booking.class);
                    }
                    return new java.util.ArrayList<>();
                });
    }

    /**
     * Get next upcoming booking
     */
    public static Task<Booking> getNextUpcomingBooking(String userId) {
        return DatabaseUtils.getUserUpcomingBookingsQuery(userId)
                .limit(1)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        return task.getResult().toObjects(Booking.class).get(0);
                    }
                    return null;
                });
    }

    /**
     * Check if user can book at this time
     */
    public static Task<BookingResult> checkBookingEligibility(String userId, String labId,
                                                              String date, String startTime,
                                                              String endTime) {
        return AuthUtils.getCurrentUserData(new AuthUtils.UserDataCallback() {
            @Override
            public void onUserDataReceived(User user) {
                BookingEligibility eligibility = new BookingEligibility();

                // Basic user checks
                if (!user.isActive()) {
                    eligibility.eligible = false;
                    eligibility.reason = "Account is inactive";
                    return;
                }

                if (!user.isVerified()) {
                    eligibility.eligible = false;
                    eligibility.reason = "Email verification required";
                    return;
                }

                if (user.isLabRestricted(labId)) {
                    eligibility.eligible = false;
                    eligibility.reason = "You are restricted from this lab";
                    return;
                }

                // Check time slot availability
                DatabaseUtils.isTimeSlotAvailable(labId, date, startTime, endTime)
                        .addOnSuccessListener(available -> {
                            if (!available) {
                                eligibility.eligible = false;
                                eligibility.reason = "Time slot is not available";
                            } else {
                                // Check user limits
                                DatabaseUtils.checkUserBookingLimits(userId, date)
                                        .addOnSuccessListener(limitResult -> {
                                            eligibility.eligible = limitResult.canBook;
                                            eligibility.reason = limitResult.canBook ?
                                                    "Eligible to book" : limitResult.message;
                                            eligibility.currentBookings = limitResult.activeBookings;
                                            eligibility.maxBookings = limitResult.maxBookings;
                                            eligibility.weeklyHours = limitResult.weeklyHours;
                                            eligibility.maxWeeklyHours = limitResult.maxWeeklyHours;
                                        });
                            }
                        });
            }

            @Override
            public void onError(String error) {
                // Handle error
            }
        });
    }

    // ======================= ADMIN WORKFLOWS =======================

    /**
     * Batch approve bookings
     */
    public static Task<BatchResult> batchApproveBookings(List<String> bookingIds, String adminNotes) {
        String adminId = AuthUtils.getCurrentUserId();
        if (adminId == null) {
            return Tasks.forResult(new BatchResult(false, "Admin not logged in", 0, 0));
        }

        return DatabaseUtils.batchApproveBookings(bookingIds, adminId, adminNotes)
                .continueWith(task -> {
                    BatchResult result = new BatchResult();

                    if (task.isSuccessful()) {
                        result.success = true;
                        result.message = "Bookings approved successfully";
                        result.processedCount = bookingIds.size();
                        result.successCount = bookingIds.size();

                        // Send notifications for each approved booking
                        for (String bookingId : bookingIds) {
                            NotificationManager.sendBookingApprovedNotificationById(bookingId);
                        }
                    } else {
                        result.success = false;
                        result.message = DatabaseUtils.getFormattedErrorMessage(task.getException());
                        result.processedCount = bookingIds.size();
                        result.successCount = 0;
                    }

                    return result;
                });
    }

    /**
     * Get admin dashboard data
     */
    public static Task<AdminDashboardData> getAdminDashboard() {
        return Tasks.call(new Callable<AdminDashboardData>() {
            @Override
            public AdminDashboardData call() throws Exception {
                AdminDashboardData dashboard = new AdminDashboardData();

                // Get enhanced statistics
                DatabaseUtils.getEnhancedBookingStatistics(new DatabaseUtils.EnhancedStatisticsCallback() {
                    @Override
                    public void onStatisticsReceived(Map<String, Object> statistics) {
                        dashboard.statistics = statistics;
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Error loading admin statistics: " + error);
                    }
                });

                // Get pending bookings
                DatabaseUtils.getPendingBookingsQuery().get()
                        .addOnSuccessListener(pendingSnapshot -> {
                            if (pendingSnapshot != null) {
                                dashboard.pendingBookings = pendingSnapshot.toObjects(Booking.class);
                            }
                        });

                // Get today's bookings
                DatabaseUtils.getTodaysBookingsQuery().get()
                        .addOnSuccessListener(todaySnapshot -> {
                            if (todaySnapshot != null) {
                                dashboard.todaysBookings = todaySnapshot.toObjects(Booking.class);
                            }
                        });

                return dashboard;
            }
        });
    }

    // ======================= RESULT CLASSES =======================

    public static class BookingResult {
        public boolean success;
        public String message;
        public Booking booking;
        public double refundAmount;

        public BookingResult() {}

        public BookingResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public BookingResult(boolean success, String message, Booking booking) {
            this.success = success;
            this.message = message;
            this.booking = booking;
        }
    }

    public static class BatchResult {
        public boolean success;
        public String message;
        public int processedCount;
        public int successCount;

        public BatchResult() {}

        public BatchResult(boolean success, String message, int processed, int successful) {
            this.success = success;
            this.message = message;
            this.processedCount = processed;
            this.successCount = successful;
        }
    }

    public static class BookingEligibility {
        public boolean eligible = true;
        public String reason = "";
        public int currentBookings = 0;
        public int maxBookings = 0;
        public int weeklyHours = 0;
        public int maxWeeklyHours = 0;
    }

    public static class UserDashboardData {
        public List<Booking> upcomingBookings;
        public List<Booking> recentBookings;
        public Booking nextBooking;
        public int pendingCount;
        public int thisWeekCount;
    }

    public static class AdminDashboardData {
        public Map<String, Object> statistics;
        public List<Booking> pendingBookings;
        public List<Booking> todaysBookings;
        public List<Booking> overdueBookings;
    }

    // ======================= UTILITY METHODS =======================

    /**
     * Check if booking can be modified
     */
    public static boolean canModifyBooking(Booking booking, String userId) {
        return booking != null &&
                booking.getUserId().equals(userId) &&
                booking.canBeModified();
    }

    /**
     * Check if booking can be cancelled
     */
    public static boolean canCancelBooking(Booking booking, String userId) {
        return booking != null &&
                booking.getUserId().equals(userId) &&
                booking.canBeCancelled();
    }

    /**
     * Get booking status color for UI
     */
    public static String getBookingStatusColor(BookingStatus status) {
        return status != null ? status.getColorCode() : BookingStatus.PENDING.getColorCode();
    }

    /**
     * Format booking for display
     */
    public static String formatBookingDisplay(Booking booking) {
        if (booking == null) return "Invalid booking";

        return String.format("%s at %s on %s (%s)",
                booking.getLabName(),
                booking.getTimeSlot(),
                DateTimeUtils.formatDateForDisplay(booking.getDate()),
                booking.getStatus().getDisplayName());
    }

    /**
     * Calculate total weekly usage for user
     */
    public static Task<Integer> getUserWeeklyUsage(String userId, String date) {
        String weekStart = DateTimeUtils.getWeekStartDate(date);
        String weekEnd = DateTimeUtils.getWeekEndDate(date);

        return DatabaseUtils.getInstance().collection(DatabaseUtils.BOOKINGS_COLLECTION)
                .whereEqualTo(DatabaseUtils.FIELD_USER_ID, userId)
                .whereGreaterThanOrEqualTo(DatabaseUtils.FIELD_DATE, weekStart)
                .whereLessThanOrEqualTo(DatabaseUtils.FIELD_DATE, weekEnd)
                .whereIn(DatabaseUtils.FIELD_STATUS,
                        java.util.Arrays.asList(BookingStatus.APPROVED.getValue(),
                                BookingStatus.COMPLETED.getValue(),
                                BookingStatus.IN_PROGRESS.getValue()))
                .get()
                .continueWith(task -> {
                    int totalMinutes = 0;
                    if (task.isSuccessful()) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : task.getResult().getDocuments()) {
                            Booking booking = doc.toObject(Booking.class);
                            if (booking != null) {
                                totalMinutes += booking.getDurationMinutes();
                            }
                        }
                    }
                    return totalMinutes / 60; // Return hours
                });
    }
}