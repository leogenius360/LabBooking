package com.testlab.labbooking.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.testlab.labbooking.R;
import com.testlab.labbooking.models.User;
import com.testlab.labbooking.utils.AuthUtils;
import com.testlab.labbooking.utils.DatabaseUtils;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    // Input fields
    private EditText etEmail, etPassword, etName, etIdNumber;
    private AutoCompleteTextView etDepartment;
    private TextInputLayout tilName, tilIdNumber, tilDepartment;

    // Buttons and UI
    private Button btnLogin, btnRegister, btnToggleMode, btnForgotPassword;
    private ProgressBar progressBar;
    private TextView tvTitle;

    private boolean isLoginMode = true;

    // Department options
    private static final String[] DEPARTMENTS = {
            "Computer Science",
            "Information Technology",
            "Engineering",
            "Mathematics",
            "Physics",
            "Chemistry",
            "Biology",
            "Business Administration",
            "Economics",
            "Psychology",
            "English",
            "History",
            "Political Science",
            "Sociology",
            "Art & Design",
            "Music",
            "Medicine",
            "Nursing",
            "Pharmacy",
            "Law",
            "Education",
            "Architecture",
            "Civil Engineering",
            "Mechanical Engineering",
            "Electrical Engineering",
            "Chemical Engineering",
            "Other"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user is already logged in and initialize session
        if (AuthUtils.isUserLoggedIn()) {
            initializeUserSession();
            return;
        }

        setContentView(R.layout.activity_login);
        initViews();
        setupClickListeners();
        setupDepartmentDropdown();
    }

    private void initializeUserSession() {
        AuthUtils.initializeUserSession(new AuthUtils.SessionCallback() {
            @Override
            public void onSessionResult(boolean success, String message, User user) {
                if (success) {
                    goToDashboard();
                } else {
                    // Session failed, show login screen
                    setContentView(R.layout.activity_login);
                    initViews();
                    setupClickListeners();
                    setupDepartmentDropdown();

                    if (message != null && !message.contains("not logged in")) {
                        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private void goToDashboard() {
        startActivity(new Intent(this, DashboardActivity.class));
        finish();
    }

    private void initViews() {
        // Input fields
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etName = findViewById(R.id.etName);
        etIdNumber = findViewById(R.id.etIdNumber);
        etDepartment = findViewById(R.id.etDepartment);

        // Input layouts
        tilName = findViewById(R.id.tilName);
        tilIdNumber = findViewById(R.id.tilIdNumber);
        tilDepartment = findViewById(R.id.tilDepartment);

        // Buttons and UI
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnToggleMode = findViewById(R.id.btnToggleMode);
        progressBar = findViewById(R.id.progressBar);
        tvTitle = findViewById(R.id.tvTitle);

        updateUIForMode();
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> performLogin());
        btnRegister.setOnClickListener(v -> performRegistration());
        btnToggleMode.setOnClickListener(v -> toggleMode());
    }

    private void setupDepartmentDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, DEPARTMENTS);
        etDepartment.setAdapter(adapter);
        etDepartment.setThreshold(1);
    }

    private void toggleMode() {
        isLoginMode = !isLoginMode;
        updateUIForMode();
        clearInputErrors();
    }

    private void updateUIForMode() {
        if (isLoginMode) {
            // Login mode
            tvTitle.setText("Login");
            tilName.setVisibility(View.GONE);
            tilIdNumber.setVisibility(View.GONE);
            tilDepartment.setVisibility(View.GONE);
            btnLogin.setVisibility(View.VISIBLE);
            btnRegister.setVisibility(View.GONE);
            btnToggleMode.setText("Need an account? Register");
        } else {
            // Registration mode
            tvTitle.setText("Register");
            tilName.setVisibility(View.VISIBLE);
            tilIdNumber.setVisibility(View.VISIBLE);
            tilDepartment.setVisibility(View.VISIBLE);
            btnLogin.setVisibility(View.GONE);
            btnRegister.setVisibility(View.VISIBLE);
            btnToggleMode.setText("Have an account? Login");
        }
    }

    private void performLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!validateLoginInput(email, password)) {
            return;
        }

        showProgress(true);

        AuthUtils.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Update last login and initialize session
                        String userId = AuthUtils.getCurrentUserId();
                        if (userId != null) {
                            AuthUtils.updateLastLogin(userId);

                            // Verify user profile exists and is complete
                            verifyUserProfileAndProceed();
                        } else {
                            showProgress(false);
                            showError("Login successful but user ID not found");
                        }
                    } else {
                        showProgress(false);
                        handleAuthError(task.getException());
                    }
                });
    }

    private void verifyUserProfileAndProceed() {
        AuthUtils.getCurrentUserData(new AuthUtils.UserDataCallback() {
            @Override
            public void onUserDataReceived(User user) {
                showProgress(false);

                if (!user.isActive()) {
                    AuthUtils.signOut();
                    showError("Your account has been deactivated. Please contact an administrator.");
                    return;
                }

                // Check if profile is complete
                if (!isProfileComplete(user)) {
                    Toast.makeText(LoginActivity.this,
                            "Please complete your profile in Settings", Toast.LENGTH_LONG).show();
                }

                // Check if email verification is required
                if (!user.isVerified() && !AuthUtils.isCurrentUserEmailVerified()) {
                    Toast.makeText(LoginActivity.this,
                            "Please verify your email for full access", Toast.LENGTH_LONG).show();
                }

                goToDashboard();
            }

            @Override
            public void onError(String error) {
                showProgress(false);
                // If user document doesn't exist, create it with basic info
                if (error.contains("not found")) {
                    createMissingUserProfile();
                } else {
                    showError("Error loading profile: " + error);
                }
            }
        });
    }

    private boolean isProfileComplete(User user) {
        boolean hasBasicInfo = user.getName() != null && !user.getName().trim().isEmpty() &&
                user.getDepartment() != null && !user.getDepartment().trim().isEmpty();

        boolean hasRoleSpecificInfo = true;
        if (user.isStudent()) {
            hasRoleSpecificInfo = user.getStudentId() != null && !user.getStudentId().trim().isEmpty();
        } else if (user.isFaculty()) {
            hasRoleSpecificInfo = user.getEmployeeId() != null && !user.getEmployeeId().trim().isEmpty();
        }

        return hasBasicInfo && hasRoleSpecificInfo;
    }

    private void createMissingUserProfile() {
        String userId = AuthUtils.getCurrentUserId();
        String email = AuthUtils.getCurrentUserEmail();
        String name = AuthUtils.getCurrentUserDisplayName();

        if (userId == null || email == null) {
            showError("Unable to create user profile");
            return;
        }

        // Determine role from email
        String role = AuthUtils.getDefaultRoleFromEmail(email);

        AuthUtils.createEnhancedUserDocument(userId,
                name != null ? name : email.split("@")[0],
                email, role, "", "",
                new AuthUtils.AuthCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(LoginActivity.this,
                                "Please complete your profile in Settings", Toast.LENGTH_LONG).show();
                        goToDashboard();
                    }

                    @Override
                    public void onFailure(String error) {
                        showError("Failed to create user profile: " + error);
                    }
                });
    }

    private void performRegistration() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String idNumber = etIdNumber.getText().toString().trim();
        String department = etDepartment.getText().toString().trim();

        if (!validateRegistrationInput(email, password, name, idNumber, department)) {
            return;
        }

        showProgress(true);

        AuthUtils.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = task.getResult().getUser().getUid();

                        // Determine role based on email domain
                        String role = AuthUtils.getDefaultRoleFromEmail(email);

                        // Create enhanced user document with all required fields
                        AuthUtils.createEnhancedUserDocument(userId, name, email, role,
                                department, idNumber, new AuthUtils.AuthCallback() {
                                    @Override
                                    public void onSuccess() {
                                        // Send email verification
                                        AuthUtils.sendEmailVerification(new AuthUtils.AuthCallback() {
                                            @Override
                                            public void onSuccess() {
                                                showProgress(false);
                                                showSuccessMessage("Registration successful! Please check your email for verification.");
                                                goToDashboard();
                                            }

                                            @Override
                                            public void onFailure(String error) {
                                                showProgress(false);
                                                // Still proceed to dashboard even if email fails
                                                Toast.makeText(LoginActivity.this,
                                                        "Registration successful! Email verification failed: " + error,
                                                        Toast.LENGTH_LONG).show();
                                                goToDashboard();
                                            }
                                        });
                                    }

                                    @Override
                                    public void onFailure(String error) {
                                        showProgress(false);
                                        showError("Registration failed: " + error);
                                        // Delete the Firebase Auth user if profile creation failed
                                        AuthUtils.getCurrentUser().delete();
                                    }
                                });
                    } else {
                        showProgress(false);
                        handleAuthError(task.getException());
                    }
                });
    }

    private boolean validateLoginInput(String email, String password) {
        clearInputErrors();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return false;
        }

        if (!AuthUtils.isValidEmail(email)) {
            etEmail.setError("Please enter a valid email address");
            etEmail.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return false;
        }

        return true;
    }

    private boolean validateRegistrationInput(String email, String password, String name,
                                              String idNumber, String department) {
        clearInputErrors();

        if (TextUtils.isEmpty(name)) {
            etName.setError("Full name is required");
            etName.requestFocus();
            return false;
        }

        if (name.trim().length() < 2) {
            etName.setError("Please enter your full name");
            etName.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return false;
        }

        if (!AuthUtils.isValidEmail(email)) {
            etEmail.setError("Please enter a valid email address");
            etEmail.requestFocus();
            return false;
        }

        // Suggest university email if not using one
        if (!AuthUtils.isUniversityEmail(email)) {
            Toast.makeText(this, "Consider using your university email for better access",
                    Toast.LENGTH_LONG).show();
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return false;
        }

        // Password strength validation
//        if (!isPasswordStrong(password)) {
//            etPassword.setError("Password should contain letters and numbers");
//            etPassword.requestFocus();
//            return false;
//        }

        if (TextUtils.isEmpty(idNumber)) {
            String role = AuthUtils.getDefaultRoleFromEmail(email);
            String fieldName = DatabaseUtils.ROLE_STUDENT.equals(role) ? "Student ID" : "Employee ID";
            etIdNumber.setError(fieldName + " is required");
            etIdNumber.requestFocus();
            return false;
        }

        if (idNumber.length() < 3) {
            etIdNumber.setError("ID number must be at least 3 characters");
            etIdNumber.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(department)) {
            etDepartment.setError("Department is required");
            etDepartment.requestFocus();
            return false;
        }

        return true;
    }

    private boolean isPasswordStrong(String password) {
        // Check for at least one letter and one number
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasNumber = password.matches(".*\\d.*");
        return hasLetter && hasNumber;
    }

    private void clearInputErrors() {
        etEmail.setError(null);
        etPassword.setError(null);
        etName.setError(null);
        etIdNumber.setError(null);
        etDepartment.setError(null);
    }

    private void handleAuthError(Exception exception) {
        String errorMessage;

        if (exception != null) {
            String message = exception.getMessage();
            if (message != null) {
                if (message.contains("email address is already in use")) {
                    errorMessage = "An account with this email already exists. Try logging in instead.";
                } else if (message.contains("password is invalid") || message.contains("wrong-password")) {
                    errorMessage = "Incorrect password. Please try again.";
                } else if (message.contains("no user record") || message.contains("user-not-found")) {
                    errorMessage = "No account found with this email. Please register first.";
                } else if (message.contains("too-many-requests")) {
                    errorMessage = "Too many failed attempts. Please try again later.";
                } else if (message.contains("network error")) {
                    errorMessage = "Network error. Please check your connection.";
                } else {
                    errorMessage = DatabaseUtils.getFormattedErrorMessage(exception);
                }
            } else {
                errorMessage = "Authentication failed. Please try again.";
            }
        } else {
            errorMessage = "An unexpected error occurred.";
        }

        showError(errorMessage);
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!show);
        btnRegister.setEnabled(!show);
        btnToggleMode.setEnabled(!show);

        // Disable input fields during progress
        etEmail.setEnabled(!show);
        etPassword.setEnabled(!show);
        etName.setEnabled(!show);
        etIdNumber.setEnabled(!show);
        etDepartment.setEnabled(!show);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        AuthUtils.logAuthEvent("ERROR", AuthUtils.getCurrentUserId(), message);
    }

    private void showSuccessMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        AuthUtils.logAuthEvent("SUCCESS", AuthUtils.getCurrentUserId(), message);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clear any cached data if activity is destroyed
        if (isFinishing()) {
            AuthUtils.clearUserCache();
        }
    }
}