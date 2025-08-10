package com.testlab.labbooking.utils;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.testlab.labbooking.managers.BookingManager;
import com.testlab.labbooking.models.User;

import java.util.HashMap;
import java.util.Map;

public class AuthUtils {
    private static final String TAG = "AuthUtils";
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

    public static String getCurrentUserEmail() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getEmail() : null;
    }

    public static String getCurrentUserDisplayName() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getDisplayName() : null;
    }

    public static boolean isCurrentUserEmailVerified() {
        FirebaseUser user = getCurrentUser();
        return user != null && user.isEmailVerified();
    }

    public static void signOut() {
        mAuth.signOut();
    }

    // ======================= USER REGISTRATION =======================

    /**
     * Create user document with enhanced profile setup
     */
    public static void createEnhancedUserDocument(String userId, String name, String email,
                                                  String role, String department, String idNumber,
                                                  AuthCallback callback) {
        User user = new User(userId, name, email, role);
        user.setDepartment(department);
        user.setVerified(isCurrentUserEmailVerified());

        // Set ID based on role
        if ("student".equalsIgnoreCase(role)) {
            user.setStudentId(idNumber);
            user.setProgram(""); // To be filled later
        } else if ("faculty".equalsIgnoreCase(role)) {
            user.setEmployeeId(idNumber);
        }

        DatabaseUtils.createUserWithDefaults(userId, user)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User document created successfully");
                    // Update last login
                    updateLastLogin(userId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating user document", e);
                    callback.onFailure(DatabaseUtils.getFormattedErrorMessage(e));
                });
    }

    /**
     * Legacy method for backward compatibility
     */
    public static void createUserDocument(String userId, String name, String email,
                                          String role, AuthCallback callback) {
        createEnhancedUserDocument(userId, name, email, role, "", "", callback);
    }

    // ======================= USER PROFILE MANAGEMENT =======================

    /**
     * Update user profile with validation
     */
    public static void updateUserProfile(User user, AuthCallback callback) {
        if (!user.isValid()) {
            callback.onFailure("Invalid user data: " + String.join(", ", user.getValidationErrors()));
            return;
        }

        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onFailure("User not logged in");
            return;
        }

        DatabaseUtils.saveUserProfile(userId, user)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User profile updated successfully");
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating user profile", e);
                    callback.onFailure(DatabaseUtils.getFormattedErrorMessage(e));
                });
    }

    /**
     * Update notification preferences
     */
    public static void updateNotificationPreferences(boolean email, boolean sms, boolean push,
                                                     AuthCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onFailure("User not logged in");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("emailNotifications", email);
        updates.put("smsNotifications", sms);
        updates.put("pushNotifications", push);
        updates.put("updatedAt", System.currentTimeMillis());

        db.collection(DatabaseUtils.USERS_COLLECTION)
                .document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(DatabaseUtils.getFormattedErrorMessage(e)));
    }

    // ======================= USER DATA RETRIEVAL =======================

    /**
     * Get current user data with caching
     */
    private static User cachedUser = null;
    private static long lastUserFetch = 0;
    private static final long CACHE_DURATION = 5 * 60 * 1000; // 5 minutes

    public static Task<BookingManager.BookingResult> getCurrentUserData(UserDataCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return null;
        }

        // Check cache first
        if (cachedUser != null &&
                System.currentTimeMillis() - lastUserFetch < CACHE_DURATION &&
                userId.equals(cachedUser.getId())) {
            callback.onUserDataReceived(cachedUser);
            return null;
        }

        // Fetch from database
        db.collection(DatabaseUtils.USERS_COLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            // Cache the user data
                            cachedUser = user;
                            lastUserFetch = System.currentTimeMillis();

                            callback.onUserDataReceived(user);
                        } else {
                            callback.onError("Failed to parse user data");
                        }
                    } else {
                        callback.onError("User profile not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user data", e);
                    callback.onError(DatabaseUtils.getFormattedErrorMessage(e));
                });
        return null;
    }

    /**
     * Refresh cached user data
     */
    public static void refreshUserData(UserDataCallback callback) {
        cachedUser = null;
        lastUserFetch = 0;
        getCurrentUserData(callback);
    }

    /**
     * Check if current user has specific role
     */
    public static void checkUserRole(String requiredRole, RoleCheckCallback callback) {
        getCurrentUserData(new UserDataCallback() {
            @Override
            public void onUserDataReceived(User user) {
                boolean hasRole = requiredRole.equalsIgnoreCase(user.getRole()) ||
                        user.isAdmin(); // Admins have access to everything
                callback.onRoleCheckComplete(hasRole, user.getRole());
            }

            @Override
            public void onError(String error) {
                callback.onRoleCheckComplete(false, "unknown");
            }
        });
    }

    /**
     * Check if current user can perform admin actions
     */
    public static void checkAdminPermission(PermissionCallback callback) {
        getCurrentUserData(new UserDataCallback() {
            @Override
            public void onUserDataReceived(User user) {
                callback.onPermissionResult(user.isAdmin(), "Admin access required");
            }

            @Override
            public void onError(String error) {
                callback.onPermissionResult(false, error);
            }
        });
    }

    /**
     * Check if user can book a specific lab
     */
    public static void checkLabBookingPermission(String labId, LabPermissionCallback callback) {
        getCurrentUserData(new UserDataCallback() {
            @Override
            public void onUserDataReceived(User user) {
                // Check if user is restricted from this lab
                if (user.isLabRestricted(labId)) {
                    callback.onPermissionResult(false, "You are restricted from booking this lab");
                    return;
                }

                // Check if user is active
                if (!user.isActive()) {
                    callback.onPermissionResult(false, "Your account is inactive");
                    return;
                }

                // Get lab data to check allowed user types
                DatabaseUtils.getLabById(labId)
                        .addOnSuccessListener(labDoc -> {
                            if (labDoc.exists()) {
                                com.testlab.labbooking.models.Lab lab = labDoc.toObject(com.testlab.labbooking.models.Lab.class);
                                if (lab != null) {
                                    if (!lab.isBookingAllowed()) {
                                        callback.onPermissionResult(false, "Lab is not available for booking");
                                    } else if (!lab.isUserTypeAllowed(user.getRole())) {
                                        callback.onPermissionResult(false, "Your user type is not allowed to book this lab");
                                    } else {
                                        callback.onPermissionResult(true, "Permission granted");
                                    }
                                } else {
                                    callback.onPermissionResult(false, "Lab data not found");
                                }
                            } else {
                                callback.onPermissionResult(false, "Lab not found");
                            }
                        })
                        .addOnFailureListener(e -> callback.onPermissionResult(false, "Error checking lab permissions"));
            }

            @Override
            public void onError(String error) {
                callback.onPermissionResult(false, error);
            }
        });
    }

    // ======================= SESSION MANAGEMENT =======================

    /**
     * Update last login time
     */
    public static void updateLastLogin(String userId) {
        if (userId != null) {
            DatabaseUtils.updateUserLastLogin(userId)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Last login updated"))
                    .addOnFailureListener(e -> Log.e(TAG, "Error updating last login", e));
        }
    }

    /**
     * Initialize user session
     */
    public static void initializeUserSession(SessionCallback callback) {
        if (!isUserLoggedIn()) {
            callback.onSessionResult(false, "User not logged in", null);
            return;
        }

        String userId = getCurrentUserId();
        updateLastLogin(userId);

        getCurrentUserData(new UserDataCallback() {
            @Override
            public void onUserDataReceived(User user) {
                if (!user.isActive()) {
                    signOut();
                    callback.onSessionResult(false, "Account is inactive", null);
                } else {
                    callback.onSessionResult(true, "Session initialized", user);
                }
            }

            @Override
            public void onError(String error) {
                callback.onSessionResult(false, error, null);
            }
        });
    }

    // ======================= EMAIL VERIFICATION =======================

    /**
     * Send email verification
     */
    public static void sendEmailVerification(AuthCallback callback) {
        FirebaseUser user = getCurrentUser();
        if (user != null) {
            user.sendEmailVerification()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Verification email sent");
                        callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error sending verification email", e);
                        callback.onFailure(DatabaseUtils.getFormattedErrorMessage(e));
                    });
        } else {
            callback.onFailure("User not logged in");
        }
    }

    /**
     * Check and update email verification status
     */
    public static void checkEmailVerification(AuthCallback callback) {
        FirebaseUser firebaseUser = getCurrentUser();
        if (firebaseUser != null) {
            firebaseUser.reload().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    boolean isVerified = firebaseUser.isEmailVerified();

                    // Update user document with verification status
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("isVerified", isVerified);
                    updates.put("updatedAt", System.currentTimeMillis());

                    db.collection(DatabaseUtils.USERS_COLLECTION)
                            .document(firebaseUser.getUid())
                            .update(updates)
                            .addOnSuccessListener(aVoid -> {
                                if (isVerified) {
                                    callback.onSuccess();
                                } else {
                                    callback.onFailure("Email not verified");
                                }
                            })
                            .addOnFailureListener(e -> callback.onFailure(DatabaseUtils.getFormattedErrorMessage(e)));
                } else {
                    callback.onFailure("Error checking verification status");
                }
            });
        } else {
            callback.onFailure("User not logged in");
        }
    }

    // ======================= PASSWORD MANAGEMENT =======================

    /**
     * Send password reset email
     */
    public static void sendPasswordResetEmail(String email, AuthCallback callback) {
        mAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Password reset email sent to: " + email);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending password reset email", e);
                    callback.onFailure(DatabaseUtils.getFormattedErrorMessage(e));
                });
    }

    // ======================= CALLBACK INTERFACES =======================

    public interface AuthCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public interface UserDataCallback {
        void onUserDataReceived(User user);
        void onError(String error);
    }

    public interface RoleCheckCallback {
        void onRoleCheckComplete(boolean hasRole, String actualRole);
    }

    public interface PermissionCallback {
        void onPermissionResult(boolean hasPermission, String message);
    }

    public interface LabPermissionCallback {
        void onPermissionResult(boolean canBook, String message);
    }

    public interface SessionCallback {
        void onSessionResult(boolean success, String message, User user);
    }

    // ======================= UTILITY METHODS =======================

    /**
     * Clear cached user data (call when user data might have changed)
     */
    public static void clearUserCache() {
        cachedUser = null;
        lastUserFetch = 0;
    }

    /**
     * Get user display name with fallback
     */
    public static String getUserDisplayName() {
        FirebaseUser user = getCurrentUser();
        if (user != null) {
            String displayName = user.getDisplayName();
            if (displayName != null && !displayName.trim().isEmpty()) {
                return displayName;
            }

            String email = user.getEmail();
            if (email != null) {
                return email.split("@")[0]; // Use email prefix
            }
        }
        return "Unknown User";
    }

    /**
     * Validate email format
     */
    public static boolean isValidEmail(String email) {
        return email != null &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * Extract domain from email
     */
    public static String getEmailDomain(String email) {
        if (email != null && email.contains("@")) {
            return email.substring(email.indexOf("@") + 1);
        }
        return "";
    }

    /**
     * Check if email belongs to university domain
     */
    public static boolean isUniversityEmail(String email) {
        String domain = getEmailDomain(email);
        // Add your university domains here
        return domain.endsWith(".edu") ||
                domain.endsWith(".ac.uk") ||
                domain.endsWith(".edu.gh") ||
                domain.contains("university") ||
                domain.contains("college");
    }

    /**
     * Determine default role based on email
     */
    public static String getDefaultRoleFromEmail(String email) {
        if (!isUniversityEmail(email)) {
            return DatabaseUtils.ROLE_USER;
        }

        String localPart = email.split("@")[0].toLowerCase();

        // Common patterns for faculty emails
        if (localPart.contains("prof") ||
                localPart.contains("dr") ||
                localPart.contains("faculty") ||
                localPart.contains("staff") ||
                localPart.contains("admin")) {
            return DatabaseUtils.ROLE_FACULTY;
        }

        // Default to student for university emails
        return DatabaseUtils.ROLE_STUDENT;
    }

    /**
     * Log authentication events
     */
    public static void logAuthEvent(String event, String userId, String details) {
        Log.d(TAG, String.format("Auth Event: %s for user %s - %s",
                event, userId != null ? userId : "unknown", details));
    }

    // ======================= SECURITY UTILITIES =======================

    /**
     * Check if user account needs security review
     */
    public static void checkAccountSecurity(SecurityCallback callback) {
        getCurrentUserData(new UserDataCallback() {
            @Override
            public void onUserDataReceived(User user) {
                SecurityStatus status = new SecurityStatus();

                // Check email verification
                status.emailVerified = user.isVerified();

                // Check if profile is complete
                status.profileComplete = isProfileComplete(user);

                // Check recent login activity
                if (user.getLastLoginAt() != null) {
                    long daysSinceLogin = (System.currentTimeMillis() - user.getLastLoginAt().getTime())
                            / (1000 * 60 * 60 * 24);
                    status.recentActivity = daysSinceLogin <= 30;
                }

                status.overallSecure = status.emailVerified && status.profileComplete;
                callback.onSecurityCheck(status);
            }

            @Override
            public void onError(String error) {
                SecurityStatus status = new SecurityStatus();
                status.overallSecure = false;
                callback.onSecurityCheck(status);
            }
        });
    }

    private static boolean isProfileComplete(User user) {
        return user.getName() != null && !user.getName().trim().isEmpty() &&
                user.getDepartment() != null && !user.getDepartment().trim().isEmpty() &&
                ((user.isStudent() && user.getStudentId() != null && !user.getStudentId().trim().isEmpty()) ||
                        (!user.isStudent() && user.getEmployeeId() != null && !user.getEmployeeId().trim().isEmpty()));
    }

    public static class SecurityStatus {
        public boolean emailVerified = false;
        public boolean profileComplete = false;
        public boolean recentActivity = false;
        public boolean overallSecure = false;

        public String getSecurityMessage() {
            if (overallSecure) {
                return "Account security is good";
            }

            StringBuilder message = new StringBuilder("Security issues: ");
            if (!emailVerified) message.append("Email not verified. ");
            if (!profileComplete) message.append("Profile incomplete. ");
            if (!recentActivity) message.append("No recent activity. ");

            return message.toString();
        }
    }

    public interface SecurityCallback {
        void onSecurityCheck(SecurityStatus status);
    }
}