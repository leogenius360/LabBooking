package com.testlab.labbooking.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.testlab.labbooking.R;
import com.testlab.labbooking.utils.AuthUtils;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword, etName;
    private Button btnLogin, btnRegister, btnToggleMode;
    private ProgressBar progressBar;
    private TextView tvTitle;
    private boolean isLoginMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user is already logged in
        if (AuthUtils.isUserLoggedIn()) {
            goToDashboard();
        }

        setContentView(R.layout.activity_login);
        initViews();
        setupClickListeners();
    }

    private void goToDashboard() {
        startActivity(new Intent(this, DashboardActivity.class));
        finish(); // Finish LoginActivity to prevent going back
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etName = findViewById(R.id.etName);
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

    private void toggleMode() {
        isLoginMode = !isLoginMode;
        updateUIForMode();
    }

    private void updateUIForMode() {
        if (isLoginMode) {
            tvTitle.setText("Login");
            etName.setVisibility(View.GONE);
            btnLogin.setVisibility(View.VISIBLE);
            btnRegister.setVisibility(View.GONE);
            btnToggleMode.setText("Need an account? Register");
        } else {
            tvTitle.setText("Register");
            etName.setVisibility(View.VISIBLE);
            btnLogin.setVisibility(View.GONE);
            btnRegister.setVisibility(View.VISIBLE);
            btnToggleMode.setText("Have an account? Login");
        }
    }

    private void performLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!validateInput(email, password, null)) return;

        showProgress(true);

        AuthUtils.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    showProgress(false);
                    if (task.isSuccessful()) {
                        startActivity(new Intent(this, DashboardActivity.class));
                        finish();
                    } else {
                        String error = task.getException() != null ?
                                task.getException().getMessage() : "Login failed";
                        Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void performRegistration() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String name = etName.getText().toString().trim();

        if (!validateInput(email, password, name)) return;

        showProgress(true);

        AuthUtils.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    showProgress(false);
                    if (task.isSuccessful()) {
                        String userId = task.getResult().getUser().getUid();
                        String role = "student";

                        // Create user document and wait for completion
                        AuthUtils.createUserDocument(userId, name, email, role, new AuthUtils.AuthCallback() {
                            @Override
                            public void onSuccess() {
                                goToDashboard();
                            }

                            @Override
                            public void onFailure(String error) {
                                Toast.makeText(LoginActivity.this,
                                        "User creation failed: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        String error = task.getException() != null ?
                                task.getException().getMessage() : "Registration failed";
                        Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean validateInput(String email, String password, String name) {
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            return false;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            return false;
        }

        if (!isLoginMode && TextUtils.isEmpty(name)) {
            etName.setError("Name is required");
            return false;
        }

        return true;
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!show);
        btnRegister.setEnabled(!show);
    }
}