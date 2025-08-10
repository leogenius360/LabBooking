package com.testlab.labbooking.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.testlab.labbooking.R;
import com.testlab.labbooking.models.Booking;
import com.testlab.labbooking.models.Lab;
import com.testlab.labbooking.models.User;
import com.testlab.labbooking.utils.AuthUtils;
import com.testlab.labbooking.utils.DatabaseUtils;
import com.testlab.labbooking.utils.DateTimeUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class BookingActivity extends AppCompatActivity {

    private Spinner spinnerLabs;
    private TextView tvSelectedDate, tvStartTime, tvEndTime;
    private EditText etPurpose;
    private Button btnSelectDate, btnSelectStartTime, btnSelectEndTime, btnSubmitBooking;
    private ProgressBar progressBar;

    private List<Lab> availableLabs;
    private ArrayAdapter<String> labsAdapter;

    private String selectedLabId;
    private String selectedLabName;
    private String selectedDate;
    private String selectedStartTime;
    private String selectedEndTime;

    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        initViews();
        setupToolbar();
        loadUserData();
        loadLabs();
        setupClickListeners();

        // Check if specific lab was passed
        String labId = getIntent().getStringExtra("lab_id");
        String labName = getIntent().getStringExtra("lab_name");
        if (labId != null && labName != null) {
            selectedLabId = labId;
            selectedLabName = labName;
        }
    }

    private void initViews() {
        spinnerLabs = findViewById(R.id.spinnerLabs);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvStartTime = findViewById(R.id.tvStartTime);
        tvEndTime = findViewById(R.id.tvEndTime);
        etPurpose = findViewById(R.id.etPurpose);
        btnSelectDate = findViewById(R.id.btnSelectDate);
        btnSelectStartTime = findViewById(R.id.btnSelectStartTime);
        btnSelectEndTime = findViewById(R.id.btnSelectEndTime);
        btnSubmitBooking = findViewById(R.id.btnSubmitBooking);
        progressBar = findViewById(R.id.progressBar);

        availableLabs = new ArrayList<>();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("New Booking");
        }
    }

    private void loadUserData() {
        AuthUtils.getCurrentUserData(new AuthUtils.UserDataCallback() {
            @Override
            public void onUserDataReceived(User user) {
                currentUser = user;
            }

            @Override
            public void onError(String error) {
                Toast.makeText(BookingActivity.this, "Error loading user data", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void loadLabs() {
        showProgress(true);

        DatabaseUtils.getInstance()
                .collection(DatabaseUtils.LABS_COLLECTION)
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    availableLabs.clear();
                    List<String> labNames = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Lab lab = document.toObject(Lab.class);
                        lab.setId(document.getId());
                        availableLabs.add(lab);
                        labNames.add(lab.getName());
                    }

                    labsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labNames);
                    labsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerLabs.setAdapter(labsAdapter);

                    // Select specific lab if passed
                    if (selectedLabId != null) {
                        for (int i = 0; i < availableLabs.size(); i++) {
                            if (availableLabs.get(i).getId().equals(selectedLabId)) {
                                spinnerLabs.setSelection(i);
                                break;
                            }
                        }
                    }

                    showProgress(false);
                })
                .addOnFailureListener(e -> {
                    showProgress(false);
                    Toast.makeText(this, "Error loading labs: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupClickListeners() {
        btnSelectDate.setOnClickListener(v -> showDatePicker());
        btnSelectStartTime.setOnClickListener(v -> showStartTimePicker());
        btnSelectEndTime.setOnClickListener(v -> showEndTimePicker());
        btnSubmitBooking.setOnClickListener(v -> submitBooking());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    tvSelectedDate.setText(selectedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        // Set minimum date to today
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void showStartTimePicker() {
        Calendar calendar = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    selectedStartTime = String.format("%02d:%02d", hourOfDay, minute);
                    tvStartTime.setText(selectedStartTime);

                    // Auto-set end time 1 hour later
                    Calendar endCal = Calendar.getInstance();
                    endCal.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    endCal.set(Calendar.MINUTE, minute);
                    endCal.add(Calendar.HOUR, 1);

                    selectedEndTime = String.format("%02d:%02d",
                            endCal.get(Calendar.HOUR_OF_DAY),
                            endCal.get(Calendar.MINUTE));
                    tvEndTime.setText(selectedEndTime);
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true);
        timePickerDialog.show();
    }

    private void showEndTimePicker() {
        Calendar calendar = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    selectedEndTime = String.format("%02d:%02d", hourOfDay, minute);
                    tvEndTime.setText(selectedEndTime);
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true);
        timePickerDialog.show();
    }

    private void submitBooking() {
        if (!validateInputs()) return;

        showProgress(true);

        // Get selected lab
        int selectedPosition = spinnerLabs.getSelectedItemPosition();
        Lab selectedLab = availableLabs.get(selectedPosition);

        // Check for conflicts first
        checkForConflicts(selectedLab, () -> {
            // No conflicts, proceed with booking
            createBooking(selectedLab);
        });
    }

    private boolean validateInputs() {
        // Check required fields
        if (spinnerLabs.getSelectedItemPosition() == -1) {
            showError("Please select a lab");
            return false;
        }

        if (selectedDate == null || selectedStartTime == null || selectedEndTime == null) {
            showError("Please select date and time");
            return false;
        }

        if (etPurpose.getText().toString().trim().isEmpty()) {
            etPurpose.setError("Purpose is required");
            return false;
        }

        // Validate time logic
        if (selectedStartTime.compareTo(selectedEndTime) >= 0) {
            showError("End time must be after start time");
            return false;
        }

        // Check duration (max 4 hours)
        int durationMinutes = DateTimeUtils.getTimeDifferenceInMinutes(selectedStartTime, selectedEndTime);
        if (durationMinutes > 240) { // 4 hours
            showError("Maximum booking duration is 4 hours");
            return false;
        }

        // Check if booking is in the past
        if (DateTimeUtils.isToday(selectedDate) &&
                selectedStartTime.compareTo(DateTimeUtils.getCurrentTime()) < 0) {
            showError("Cannot book in the past");
            return false;
        }

        return true;
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void checkForConflicts(Lab lab, Runnable onNoConflicts) {
        DatabaseUtils.getConflictingBookingsQuery(lab.getId(), selectedDate, selectedStartTime, selectedEndTime)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        onNoConflicts.run();
                    } else {
                        showProgress(false);

                        // Show specific conflict message
                        StringBuilder conflictMsg = new StringBuilder("Conflicts with:\n");
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Booking conflict = doc.toObject(Booking.class);
                            conflictMsg.append("- ")
                                    .append(conflict.getStartTime())
                                    .append(" to ")
                                    .append(conflict.getEndTime())
                                    .append("\n");
                        }

                        new AlertDialog.Builder(BookingActivity.this)
                                .setTitle("Time Slot Unavailable")
                                .setMessage(conflictMsg.toString())
                                .setPositiveButton("OK", null)
                                .show();
                    }
                })
                .addOnFailureListener(e -> {
                    showProgress(false);
                    Toast.makeText(this, "Error checking availability: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void createBooking(Lab lab) {
        String purpose = etPurpose.getText().toString().trim();

        Booking booking = new Booking(
                lab.getId(),
                currentUser.getId(),
                lab.getName(),
                currentUser.getName(),
                selectedDate,
                selectedStartTime,
                selectedEndTime,
                purpose
        );

        DatabaseUtils.getInstance()
                .collection(DatabaseUtils.BOOKINGS_COLLECTION)
                .add(booking)
                .addOnSuccessListener(documentReference -> {
                    showProgress(false);
                    Toast.makeText(this, "Booking submitted successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    showProgress(false);
                    Toast.makeText(this, "Error creating booking: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSubmitBooking.setEnabled(!show);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}