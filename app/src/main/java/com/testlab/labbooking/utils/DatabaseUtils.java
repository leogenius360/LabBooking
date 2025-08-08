package com.testlab.labbooking.utils;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.testlab.labbooking.models.Booking;

import java.util.HashMap;
import java.util.Map;

public class DatabaseUtils {
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Collection names
    public static final String USERS_COLLECTION = "users";
    public static final String LABS_COLLECTION = "labs";
    public static final String BOOKINGS_COLLECTION = "bookings";

    // Booking statuses
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_REJECTED = "rejected";
    public static final String STATUS_CANCELLED = "cancelled";

    public static FirebaseFirestore getInstance() {
        return db;
    }

    // Check if a time slot is available for a lab
    public static Query getConflictingBookingsQuery(String labId, String date,
                                                    String startTime, String endTime) {
        return db.collection(BOOKINGS_COLLECTION)
                .whereEqualTo("labId", labId)
                .whereEqualTo("date", date)
                .whereIn("status", java.util.Arrays.asList(STATUS_PENDING, STATUS_APPROVED))
                .whereGreaterThanOrEqualTo("startTime", startTime)
                .whereLessThan("endTime", endTime);
    }

    // Get user's bookings
    public static Query getUserBookingsQuery(String userId) {
        return db.collection(BOOKINGS_COLLECTION)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING);
    }

    // Get pending bookings for admin
    public static Query getPendingBookingsQuery() {
        return db.collection(BOOKINGS_COLLECTION)
                .whereEqualTo("status", STATUS_PENDING)
                .orderBy("createdAt", Query.Direction.ASCENDING);
    }

    // Get all bookings for a specific lab
    public static Query getLabBookingsQuery(String labId) {
        return db.collection(BOOKINGS_COLLECTION)
                .whereEqualTo("labId", labId)
                .orderBy("date", Query.Direction.DESCENDING)
                .orderBy("startTime", Query.Direction.ASCENDING);
    }

    // Update booking status
    public static void updateBookingStatus(String bookingId, String status, String adminNotes) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("updatedAt", System.currentTimeMillis());
        if (adminNotes != null && !adminNotes.isEmpty()) {
            updates.put("adminNotes", adminNotes);
        }

        db.collection(BOOKINGS_COLLECTION)
                .document(bookingId)
                .update(updates);
    }
}