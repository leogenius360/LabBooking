package com.testlab.labbooking.activities;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.testlab.labbooking.R;
import com.testlab.labbooking.adapters.BookingsAdapter;
import com.testlab.labbooking.models.Booking;
import com.testlab.labbooking.models.BookingStatus;
import com.testlab.labbooking.utils.DatabaseUtils;

import java.util.ArrayList;
import java.util.List;

public class AllBookingsActivity extends AppCompatActivity {

    private RecyclerView recyclerBookings;
    private BookingsAdapter bookingsAdapter;
    private TextView tvEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_bookings);

        initViews();
        setupToolbar();
        setupRecyclerView();
        loadAllBookings();
    }

    private void initViews() {
        recyclerBookings = findViewById(R.id.recyclerBookings);
        tvEmptyState = findViewById(R.id.tvEmptyState);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("All Bookings");
        }
    }

    private void setupRecyclerView() {
        bookingsAdapter = new BookingsAdapter(this, true);
        recyclerBookings.setLayoutManager(new LinearLayoutManager(this));
        recyclerBookings.setAdapter(bookingsAdapter);
    }

    private void loadAllBookings() {
        DatabaseUtils.getInstance().collection(DatabaseUtils.BOOKINGS_COLLECTION)
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bookings_filter_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.filter_pending) {
            filterBookings(BookingStatus.PENDING);
            return true;
        } else if (id == R.id.filter_approved) {
            filterBookings(BookingStatus.APPROVED);
            return true;
        } else if (id == R.id.filter_rejected) {
            filterBookings(BookingStatus.REJECTED);
            return true;
        } else if (id == R.id.filter_all) {
            loadAllBookings();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void filterBookings(BookingStatus status) {
        DatabaseUtils.getInstance().collection(DatabaseUtils.BOOKINGS_COLLECTION)
                .whereEqualTo("status", status.toString())
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
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}