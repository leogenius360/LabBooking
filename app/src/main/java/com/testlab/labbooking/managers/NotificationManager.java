package com.testlab.labbooking.managers;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.testlab.labbooking.models.Booking;
import com.testlab.labbooking.models.Lab;
import com.testlab.labbooking.models.Notification;
import com.testlab.labbooking.models.User;
import com.testlab.labbooking.utils.DatabaseUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralized notification management for the lab booking system
 */
public class NotificationManager {
    private static final String TAG = "NotificationManager";

    // ======================= BOOKING NOTIFICATIONS =======================

    /**
     * Send notification when booking is created
     */
    public static void sendBookingCreatedNotification(Booking booking) {
        String title = "Booking Created";
        String message = String.format("Your booking for %s on %s has been submitted and is pending review.",
                booking.getLabName(), booking.getDateTimeDisplay());

        Notification notification = new Notification(
                booking.getUserId(),
                title,
                message,
                Notification.NotificationType.BOOKING_APPROVED,
                booking.getId()
        );

        createAndSendNotification(notification);
    }

    /**
     * Send notification when booking is approved
     */
    public static void sendBookingApprovedNotification(Booking booking) {
        String title = "Booking Approved ✅";
        String message = String.format("Great news! Your booking for %s on %s has been approved. " +
                        "Remember to check in when you arrive.",
                booking.getLabName(), booking.getDateTimeDisplay());

        Notification notification = new Notification(
                booking.getUserId(),
                title,
                message,
                Notification.NotificationType.BOOKING_APPROVED,
                booking.getId()
        );
        notification.setActionUrl("booking_details/" + booking.getId());
        notification.setPriority(1); // High priority

        createAndSendNotification(notification);
    }

    /**
     * Send notification when booking is rejected
     */
    public static void sendBookingRejectedNotification(Booking booking, String reason) {
        String title = "Booking Rejected";
        String message = String.format("Your booking for %s on %s has been rejected.",
                booking.getLabName(), booking.getDateTimeDisplay());

        if (reason != null && !reason.trim().isEmpty()) {
            message += "\n\nReason: " + reason;
        }

        Notification notification = new Notification(
                booking.getUserId(),
                title,
                message,
                Notification.NotificationType.BOOKING_REJECTED,
                booking.getId()
        );
        notification.setPriority(1); // High priority

        createAndSendNotification(notification);
    }

    /**
     * Send notification when booking is cancelled
     */
    public static void sendBookingCancelledNotification(Booking booking, String reason) {
        String title = "Booking Cancelled";
        String message = String.format("Your booking for %s on %s has been cancelled.",
                booking.getLabName(), booking.getDateTimeDisplay());

        if (reason != null && !reason.trim().isEmpty()) {
            message += "\n\nReason: " + reason;
        }

        Notification notification = new Notification(
                booking.getUserId(),
                title,
                message,
                Notification.NotificationType.BOOKING_CANCELLED,
                booking.getId()
        );

        createAndSendNotification(notification);
    }

    /**
     * Send booking reminder notification
     */
    public static void sendBookingReminderNotification(Booking booking) {
        String title = "Booking Reminder 🔔";
        String message = String.format("Don't forget! You have a booking for %s starting in 1 hour at %s. " +
                        "Please arrive on time and check in.",
                booking.getLabName(),
                com.testlab.labbooking.utils.DateTimeUtils.formatTimeForDisplay(booking.getStartTime()));

        Notification notification = new Notification(
                booking.getUserId(),
                title,
                message,
                Notification.NotificationType.BOOKING_REMINDER,
                booking.getId()
        );
        notification.setActionUrl("check_in/" + booking.getId());
        notification.setPriority(1); // High priority
        notification.setPersistent(true); // Don't auto-dismiss

        createAndSendNotification(notification);

        // Update booking reminder status
        Map<String, Object> updates = new HashMap<>();
        updates.put("reminderSent", true);
        updates.put("reminderSentAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        DatabaseUtils.getInstance().collection(DatabaseUtils.BOOKINGS_COLLECTION)
                .document(booking.getId())
                .update(updates);
    }

    /**
     * Send notification when booking is completed
     */
    public static void sendBookingCompletedNotification(Booking booking) {
        String title = "Booking Completed ✅";
        String message = String.format("Thank you for using %s. Your booking has been completed. " +
                        "Duration: %s",
                booking.getLabName(), booking.getFormattedDuration());

        Notification notification = new Notification(
                booking.getUserId(),
                title,
                message,
                Notification.NotificationType.BOOKING_COMPLETED,
                booking.getId()
        );
        notification.setActionUrl("booking_summary/" + booking.getId());

        createAndSendNotification(notification);
    }

    /**
     * Send overdue notification
     */
    public static void sendBookingOverdueNotification(Booking booking) {
        String title = "Booking Overdue ⚠️";
        String message = String.format("Your booking for %s was scheduled to end at %s but you haven't checked out yet. " +
                        "Please check out as soon as possible.",
                booking.getLabName(),
                com.testlab.labbooking.utils.DateTimeUtils.formatTimeForDisplay(booking.getEndTime()));

        Notification notification = new Notification(
                booking.getUserId(),
                title,
                message,
                Notification.NotificationType.BOOKING_OVERDUE,
                booking.getId()
        );
        notification.setActionUrl("check_out/" + booking.getId());
        notification.setPriority(1); // High priority
        notification.setPersistent(true);

        createAndSendNotification(notification);
    }

    // ======================= SYSTEM NOTIFICATIONS =======================

    /**
     * Send lab maintenance notification to affected users
     */
    public static void sendLabMaintenanceNotification(Lab lab, String maintenanceMessage,
                                                      java.util.List<String> affectedUserIds) {
        String title = "Lab Maintenance Notice";
        String message = String.format("The %s will be under maintenance. %s",
                lab.getName(), maintenanceMessage);

        for (String userId : affectedUserIds) {
            Notification notification = new Notification(
                    userId,
                    title,
                    message,
                    Notification.NotificationType.LAB_MAINTENANCE,
                    lab.getId()
            );
            notification.setPriority(2);
            notification.setPersistent(true);

            createAndSendNotification(notification);
        }
    }

    /**
     * Send system update notification to all users
     */
    public static void sendSystemUpdateNotification(String updateMessage, java.util.List<String> userIds) {
        String title = "System Update";

        for (String userId : userIds) {
            Notification notification = new Notification(
                    userId,
                    title,
                    updateMessage,
                    Notification.NotificationType.SYSTEM_UPDATE
            );
            notification.setPriority(2);

            createAndSendNotification(notification);
        }
    }

    /**
     * Send admin message to specific users
     */
    public static void sendAdminMessage(String adminMessage, java.util.List<String> userIds,
                                        String adminName) {
        String title = "Message from " + (adminName != null ? adminName : "Administrator");

        for (String userId : userIds) {
            Notification notification = new Notification(
                    userId,
                    title,
                    adminMessage,
                    Notification.NotificationType.ADMIN_MESSAGE
            );
            notification.setPriority(1);
            notification.setPersistent(true);

            createAndSendNotification(notification);
        }
    }

    // ======================= PAYMENT NOTIFICATIONS =======================

    /**
     * Send payment required notification
     */
    public static void sendPaymentRequiredNotification(Booking booking) {
        String title = "Payment Required 💳";
        String message = String.format("Payment of $%.2f is required for your booking of %s on %s.",
                booking.getTotalCost(), booking.getLabName(), booking.getDateTimeDisplay());

        Notification notification = new Notification(
                booking.getUserId(),
                title,
                message,
                Notification.NotificationType.PAYMENT_REQUIRED,
                booking.getId()
        );
        notification.setActionUrl("payment/" + booking.getId());
        notification.setPriority(1);
        notification.setPersistent(true);

        createAndSendNotification(notification);
    }

    /**
     * Send payment confirmation notification
     */
    public static void sendPaymentConfirmedNotification(Booking booking, String paymentReference) {
        String title = "Payment Confirmed ✅";
        String message = String.format("Payment of $%.2f for %s has been confirmed. " +
                        "Reference: %s",
                booking.getTotalCost(), booking.getLabName(), paymentReference);

        Notification notification = new Notification(
                booking.getUserId(),
                title,
                message,
                Notification.NotificationType.PAYMENT_CONFIRMED,
                booking.getId()
        );
        notification.setActionUrl("receipt/" + paymentReference);

        createAndSendNotification(notification);
    }

    // ======================= NOTIFICATION OPERATIONS =======================

    /**
     * Create and send notification
     */
    private static void createAndSendNotification(Notification notification) {
        DatabaseUtils.createNotification(
                notification.getUserId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getTypeString(),
                notification.getRelatedId()
        ).addOnSuccessListener(docRef -> {
            notification.setId(docRef.getId());
            Log.d(TAG, "Notification created: " + notification.getTitle());

            // Send push notification if user has enabled it
            sendPushNotificationIfEnabled(notification);

            // Send email notification if user has enabled it
            sendEmailNotificationIfEnabled(notification);

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error creating notification", e);
        });
    }

    /**
     * Send notification by booking ID (for batch operations)
     */
    public static void sendBookingApprovedNotificationById(String bookingId) {
        DatabaseUtils.getInstance().collection(DatabaseUtils.BOOKINGS_COLLECTION)
                .document(bookingId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Booking booking = doc.toObject(Booking.class);
                        if (booking != null) {
                            sendBookingApprovedNotification(booking);
                        }
                    }
                });
    }

    /**
     * Mark notification as read
     */
    public static Task<Void> markAsRead(String notificationId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("read", true);
        updates.put("readAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        return DatabaseUtils.getInstance().collection(DatabaseUtils.NOTIFICATIONS_COLLECTION)
                .document(notificationId)
                .update(updates);
    }

    /**
     * Dismiss notification
     */
    public static Task<Void> dismissNotification(String notificationId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("dismissed", true);
        updates.put("dismissedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        return DatabaseUtils.getInstance().collection(DatabaseUtils.NOTIFICATIONS_COLLECTION)
                .document(notificationId)
                .update(updates);
    }

    /**
     * Get user's unread notifications
     */
    public static com.google.firebase.firestore.Query getUnreadNotificationsQuery(String userId) {
        return DatabaseUtils.getInstance().collection(DatabaseUtils.NOTIFICATIONS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("read", false)
                .whereEqualTo("dismissed", false)
                .orderBy("priority", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING);
    }

    /**
     * Get all user notifications with pagination
     */
    public static com.google.firebase.firestore.Query getUserNotificationsQuery(String userId, int limit) {
        return DatabaseUtils.getInstance().collection(DatabaseUtils.NOTIFICATIONS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("dismissed", false)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit);
    }

//    /**
//     * Clear old notifications (cleanup job)
//     */
//    public static Task<Void> clearOldNotifications(int daysOld) {
//        long cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L);
//
//        return DatabaseUtils.getInstance().collection(DatabaseUtils.NOTIFICATIONS_COLLECTION)
//                .whereEqualTo("read", true)
//                .whereLessThan("createdAt", new java.util.Date(cutoffTime))
//                .get()
//                .continueWithTask(task -> {
//                    if (task.isSuccessful()) {
//                        com.google.firebase.firestore.WriteBatch batch = DatabaseUtils.getInstance().batch();
//
//                        for (com.google.firebase.firestore.DocumentSnapshot doc : task.getResult().getDocuments()) {
//                            batch.delete(doc.getReference());
//                        }
//
//                        return batch.commit();
//                    }
//                    return task;
//                });
//    }

    // ======================= EXTERNAL NOTIFICATION SERVICES =======================

    /**
     * Send push notification if user has enabled it
     */
    private static void sendPushNotificationIfEnabled(Notification notification) {
        // Get user preferences first
        DatabaseUtils.getUserById(notification.getUserId())
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        User user = userDoc.toObject(User.class);
                        if (user != null && user.isPushNotifications()) {
                            // TODO: Integrate with Firebase Cloud Messaging
                            sendFCMNotification(notification, user);
                        }
                    }
                });
    }

    /**
     * Send email notification if user has enabled it
     */
    private static void sendEmailNotificationIfEnabled(Notification notification) {
        DatabaseUtils.getUserById(notification.getUserId())
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        User user = userDoc.toObject(User.class);
                        if (user != null && user.isEmailNotifications()) {
                            // TODO: Integrate with email service (SendGrid, etc.)
                            sendEmailNotification(notification, user);
                        }
                    }
                });
    }

    /**
     * Send FCM push notification
     */
    private static void sendFCMNotification(Notification notification, User user) {
        // TODO: Implement Firebase Cloud Messaging
        Log.d(TAG, "Would send FCM notification to: " + user.getEmail());

        /*
        Example FCM implementation:

        Map<String, String> data = new HashMap<>();
        data.put("title", notification.getTitle());
        data.put("message", notification.getMessage());
        data.put("type", notification.getTypeString());
        data.put("relatedId", notification.getRelatedId());
        data.put("actionUrl", notification.getActionUrl());

        Message message = Message.builder()
                .setToken(userFCMToken) // You'll need to store FCM tokens
                .putAllData(data)
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(notification.getTitle())
                        .setBody(notification.getMessage())
                        .build())
                .build();

        FirebaseMessaging.getInstance().send(message);
        */
    }

    /**
     * Send email notification
     */
    private static void sendEmailNotification(Notification notification, User user) {
        // TODO: Implement email service integration
        Log.d(TAG, "Would send email notification to: " + user.getEmail());

        /*
        Example email implementation with SendGrid or similar:

        Email email = new Email();
        email.setFrom("noreply@yourlab.edu");
        email.setTo(user.getEmail());
        email.setSubject(notification.getTitle());
        email.setHtmlContent(buildEmailHtml(notification, user));

        SendGridService.send(email);
        */
    }

    // ======================= AUTOMATED NOTIFICATIONS =======================

    /**
     * Send automated reminders (call this from a scheduled job)
     */
    public static void sendAutomatedReminders() {
        String tomorrow = com.testlab.labbooking.utils.DateTimeUtils.getDateFromToday(1);

        // Get tomorrow's approved bookings that haven't had reminders sent
        DatabaseUtils.getInstance().collection(DatabaseUtils.BOOKINGS_COLLECTION)
                .whereEqualTo(DatabaseUtils.FIELD_DATE, tomorrow)
                .whereEqualTo(DatabaseUtils.FIELD_STATUS, DatabaseUtils.STATUS_APPROVED)
                .whereEqualTo("reminderSent", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Booking booking = doc.toObject(Booking.class);
                        if (booking != null) {
                            // Check if reminder should be sent (24 hours before)
                            long hoursUntil = com.testlab.labbooking.utils.DateTimeUtils.getHoursUntilDateTime(
                                    booking.getDate(), booking.getStartTime());

                            if (hoursUntil <= 24 && hoursUntil > 0) {
                                sendBookingReminderNotification(booking);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error sending automated reminders", e));
    }

    /**
     * Check for overdue bookings and send notifications
     */
    public static void checkOverdueBookings() {
        DatabaseUtils.getOverdueBookingsQuery().get()
                .addOnSuccessListener(querySnapshot -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Booking booking = doc.toObject(Booking.class);
                        if (booking != null && booking.isOverdue()) {
                            // Update status to overdue
                            Map<String, Object> updates = new HashMap<>();
                            updates.put(DatabaseUtils.FIELD_STATUS, DatabaseUtils.STATUS_PENDING); // You might want a separate OVERDUE status
                            updates.put(DatabaseUtils.FIELD_UPDATED_AT, com.google.firebase.firestore.FieldValue.serverTimestamp());

                            DatabaseUtils.getInstance().collection(DatabaseUtils.BOOKINGS_COLLECTION)
                                    .document(booking.getId())
                                    .update(updates);

                            sendBookingOverdueNotification(booking);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error checking overdue bookings", e));
    }

    // ======================= BULK NOTIFICATION OPERATIONS =======================

    /**
     * Send notification to multiple users
     */
    public static Task<Void> sendBulkNotification(java.util.List<String> userIds, String title,
                                                  String message, Notification.NotificationType type) {
        com.google.firebase.firestore.WriteBatch batch = DatabaseUtils.getInstance().batch();

        for (String userId : userIds) {
            Notification notification = new Notification(userId, title, message, type);
            DocumentReference notifRef = DatabaseUtils.getInstance().collection(DatabaseUtils.NOTIFICATIONS_COLLECTION).document();
            batch.set(notifRef, notification);
        }

        return batch.commit();
    }

    /**
     * Send notification to users with specific role
     */
    public static void sendRoleBasedNotification(String role, String title, String message,
                                                 Notification.NotificationType type) {
        DatabaseUtils.getUsersByRoleQuery(role, 100).get()
                .addOnSuccessListener(querySnapshot -> {
                    java.util.List<String> userIds = new java.util.ArrayList<>();

                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        User user = doc.toObject(User.class);
                        if (user != null && user.isActive()) {
                            userIds.add(user.getId());
                        }
                    }

                    if (!userIds.isEmpty()) {
                        sendBulkNotification(userIds, title, message, type);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error sending role-based notification", e));
    }

    // ======================= NOTIFICATION PREFERENCES =======================

    /**
     * Update user notification preferences
     */
    public static Task<Void> updateNotificationPreferences(String userId, boolean email,
                                                           boolean sms, boolean push) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("emailNotifications", email);
        updates.put("smsNotifications", sms);
        updates.put("pushNotifications", push);
        updates.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        return DatabaseUtils.getInstance().collection(DatabaseUtils.USERS_COLLECTION)
                .document(userId)
                .update(updates);
    }

    // ======================= UTILITY METHODS =======================

    /**
     * Get notification count for user
     */
    public static Task<Integer> getUnreadNotificationCount(String userId) {
        return getUnreadNotificationsQuery(userId).get()
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        return task.getResult().size();
                    }
                    return 0;
                });
    }

//    /**
//     * Clear all notifications for user
//     */
//    public static Task<Void> clearAllUserNotifications(String userId) {
//        return DatabaseUtils.getInstance().collection(DatabaseUtils.NOTIFICATIONS_COLLECTION)
//                .whereEqualTo("userId", userId)
//                .get()
//                .continueWithTask(task -> {
//                    if (task.isSuccessful()) {
//                        com.google.firebase.firestore.WriteBatch batch = DatabaseUtils.getInstance().batch();
//
//                        for (com.google.firebase.firestore.DocumentSnapshot doc : task.getResult().getDocuments()) {
//                            batch.update(doc.getReference(), "dismissed", true,
//                                    "dismissedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
//                        }
//
//                        return batch.commit();
//                    }
//                    return task;
//                });
//    }

    /**
     * Build email HTML content for notifications
     */
    private static String buildEmailHtml(Notification notification, User user) {
        // TODO: Create proper HTML email template
        return String.format(
                "<html><body>" +
                        "<h2>%s</h2>" +
                        "<p>Dear %s,</p>" +
                        "<p>%s</p>" +
                        "<br>" +
                        "<p>Best regards,<br>Lab Booking System</p>" +
                        "</body></html>",
                notification.getTitle(),
                user.getName(),
                notification.getMessage().replace("\n", "<br>"));
    }

    // ======================= CALLBACK INTERFACES =======================

    public interface NotificationCallback {
        void onNotificationSent(boolean success, String message);
    }

    public interface NotificationCountCallback {
        void onCountReceived(int count);

        void onError(String error);
    }
}