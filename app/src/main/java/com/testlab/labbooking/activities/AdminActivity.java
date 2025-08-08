package com.testlab.labbooking.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.testlab.labbooking.R;
import com.testlab.labbooking.adapters.BookingsAdapter;
import com.testlab.labbooking.models.Booking;
import com.testlab.labbooking.models.Lab;
import com.testlab.labbooking.models.User;
import com.testlab.labbooking.utils.AuthUtils;
import com.testlab.labbooking.utils.DatabaseUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdminActivity extends AppCompatActivity implements BookingsAdapter.BookingActionListener {

    private RecyclerView recyclerPendingBookings;
    private BookingsAdapter bookingsAdapter;
    private FloatingActionButton fabAddLab;
    private TextView tvEmptyState;

    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // Check if user is admin
        checkAdminAccess();

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupClickListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPendingBookings();
    }

    private void checkAdminAccess() {
        AuthUtils.getCurrentUserData(new AuthUtils.UserDataCallback() {
            @Override
            public void onUserDataReceived(User user) {
                currentUser = user;
                if (!user.isAdmin()) {
                    Toast.makeText(AdminActivity.this, "Access denied. Admin only.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(AdminActivity.this, "Error verifying access", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void initViews() {
        recyclerPendingBookings = findViewById(R.id.recyclerPendingBookings);
        fabAddLab = findViewById(R.id.fabAddLab);
        tvEmptyState = findViewById(R.id.tvEmptyState);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Admin Panel");
        }
    }

    private void setupRecyclerView() {
        bookingsAdapter = new BookingsAdapter(this, true);
        bookingsAdapter.setBookingActionListener(this);
        recyclerPendingBookings.setLayoutManager(new LinearLayoutManager(this));
        recyclerPendingBookings.setAdapter(bookingsAdapter);
    }

    private void setupClickListeners() {
        fabAddLab.setOnClickListener(v -> showAddLabDialog());
    }

    private void loadPendingBookings() {
        DatabaseUtils.getPendingBookingsQuery()
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error loading bookings: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        List<Booking> bookings = new ArrayList<>();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Booking booking = document.toObject(Booking.class);
                            booking.setId(document.getId());
                            bookings.add(booking);
                        }
                        bookingsAdapter.updateBookings(bookings);
                        tvEmptyState.setVisibility(View.GONE);
                    } else {
                        bookingsAdapter.updateBookings(new ArrayList<>());
                        tvEmptyState.setVisibility(View.VISIBLE);
                    }
                });
    }

    @Override
    public void onApprove(Booking booking) {
        showActionDialog(booking, DatabaseUtils.STATUS_APPROVED, "Approve Booking");
    }

    @Override
    public void onReject(Booking booking) {
        showActionDialog(booking, DatabaseUtils.STATUS_REJECTED, "Reject Booking");
    }

    private void showActionDialog(Booking booking, String newStatus, String title) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage("Lab: " + booking.getLabName() + "\nDate: " + booking.getDate() +
                "\nTime: " + booking.getStartTime() + " - " + booking.getEndTime() +
                "\nUser: " + booking.getUserName());

        // Add notes input
        EditText etNotes = new EditText(this);
        etNotes.setHint("Admin notes (optional)");
        builder.setView(etNotes);

        builder.setPositiveButton("Confirm", (dialog, which) -> {
            String notes = etNotes.getText().toString().trim();
            updateBookingStatus(booking, newStatus, notes);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updateBookingStatus(Booking booking, String status, String notes) {
        DatabaseUtils.getInstance().collection(DatabaseUtils.BOOKINGS_COLLECTION)
                .document(booking.getId())
                .update("status", status, "adminNotes", notes)
                .addOnSuccessListener(aVoid -> {
                    String message = DatabaseUtils.STATUS_APPROVED.equals(status) ?
                            "Booking approved" : "Booking rejected";
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error updating booking: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showAddLabDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Lab");

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_lab, null);
        builder.setView(view);

        EditText etLabName = view.findViewById(R.id.etLabName);
        EditText etDescription = view.findViewById(R.id.etDescription);
        EditText etCapacity = view.findViewById(R.id.etCapacity);
        EditText etLocation = view.findViewById(R.id.etLocation);
        EditText etResources = view.findViewById(R.id.etResources);
        CheckBox cbActive = view.findViewById(R.id.cbActive);

        builder.setPositiveButton("Save", (dialog, which) -> {
            // Validate inputs
            String name = etLabName.getText().toString().trim();
            String description = etDescription.getText().toString().trim();
            String capacityStr = etCapacity.getText().toString().trim();
            String location = etLocation.getText().toString().trim();
            String resourcesStr = etResources.getText().toString().trim();
            boolean isActive = cbActive.isChecked();

            if (name.isEmpty()) {
                Toast.makeText(this, "Lab name is required", Toast.LENGTH_SHORT).show();
                return;
            }

            int capacity = 10; // default
            try {
                capacity = Integer.parseInt(capacityStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid capacity. Using default (10)", Toast.LENGTH_SHORT).show();
            }

            List<String> resources = new ArrayList<>();
            if (!resourcesStr.isEmpty()) {
                resources = Arrays.asList(resourcesStr.split("\\s*,\\s*"));
            }

            // Create lab object
            Lab lab = new Lab();
            lab.setName(name);
            lab.setDescription(description);
            lab.setCapacity(capacity);
            lab.setLocation(location);
            lab.setResources(resources);
            lab.setActive(isActive);

            // Save to Firestore
            saveLabToFirestore(lab);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void saveLabToFirestore(Lab lab) {
        DatabaseUtils.getInstance().collection(DatabaseUtils.LABS_COLLECTION)
                .add(lab)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Lab added successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error adding lab: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.admin_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_all_bookings) {
//            startActivity(new Intent(this, AllBookingsActivity.class));
            return true;
        } else if (id == R.id.action_manage_labs) {
//            startActivity(new Intent(this, ManageLabsActivity.class));
            return true;
        } else if (id == R.id.action_reports) {
            // TODO: Implement reports
            Toast.makeText(this, "Reports feature coming soon", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}