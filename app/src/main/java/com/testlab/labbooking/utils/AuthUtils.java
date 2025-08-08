package com.testlab.labbooking.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.testlab.labbooking.models.User;

public class AuthUtils {
    private static final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static FirebaseAuth getInstance() {
        return mAuth;
    }

    public static FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    public static boolean isUserLoggedIn() {
        return getCurrentUser() != null;
    }

    public static String getCurrentUserId() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public static void signOut() {
        mAuth.signOut();
    }

    // Create user document in Firestore after successful registration
    public static void createUserDocument(String userId, String name, String email,
                                          String role, AuthCallback callback) {
        User user = new User(userId, name, email, role);
        db.collection(DatabaseUtils.USERS_COLLECTION)
                .document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // Interface for auth callbacks
    public interface AuthCallback {
        void onSuccess();

        void onFailure(String error);
    }

    // Interface for user data callbacks
    public interface UserDataCallback {
        void onUserDataReceived(User user);

        void onError(String error);
    }

    // Get current user data from Firestore
    public static void getCurrentUserData(UserDataCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }

        db.collection(DatabaseUtils.USERS_COLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        callback.onUserDataReceived(user);
                    } else {
                        callback.onError("User data not found");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}