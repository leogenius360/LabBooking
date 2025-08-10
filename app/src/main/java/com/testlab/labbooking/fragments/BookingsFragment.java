package com.testlab.labbooking.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.testlab.labbooking.R;
import com.testlab.labbooking.adapters.BookingsAdapter;
import com.testlab.labbooking.models.Booking;
import com.testlab.labbooking.models.User;
import com.testlab.labbooking.utils.DatabaseUtils;

import java.util.ArrayList;
import java.util.List;

public class BookingsFragment extends Fragment implements BookingsAdapter.BookingActionListener {

    private static final String TAG = "BookingsFragment";
    private static final String ARG_USER = "user";

    private RecyclerView recyclerBookings;
    private BookingsAdapter bookingsAdapter;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvEmptyMessage;
    private ProgressBar progressBar;
    private User currentUser;

    public static BookingsFragment newInstance(User user) {
        BookingsFragment fragment = new BookingsFragment();
        Bundle args = new Bundle();
        if (user != null) {
            args.putSerializable(ARG_USER, user);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentUser = (User) getArguments().getSerializable(ARG_USER);
        }
        Log.d(TAG, "BookingsFragment created with user: " + (currentUser != null ? currentUser.getName() : "null"));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bookings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        setupSwipeRefresh();

        // Load bookings if user is available
        if (currentUser != null) {
            loadUserBookings(currentUser);
        } else {
            showEmptyState("Please log in to view your bookings");
        }
    }

    private void initViews(View view) {
        recyclerBookings = view.findViewById(R.id.recyclerBookings);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);
        progressBar = view.findViewById(R.id.progressBar);
    }

    private void setupRecyclerView() {
        bookingsAdapter = new BookingsAdapter(requireContext(), false); // false = user view
        bookingsAdapter.setBookingActionListener(this);
        recyclerBookings.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerBookings.setAdapter(bookingsAdapter);
    }

    private void setupSwipeRefresh() {
        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(() -> {
                if (currentUser != null) {
                    loadUserBookings(currentUser);
                } else {
                    hideLoading();
                    Toast.makeText(requireContext(), "Cannot refresh: User not available", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public void loadUserBookings(User user) {
        if (user == null || user.getId() == null) {
            showEmptyState("User information not available");
            return;
        }

        showLoading();

        DatabaseUtils.getUserBookingsQuery(user.getId())
                .addSnapshotListener(this::handleBookingsSnapshot);
    }

    private void handleBookingsSnapshot(QuerySnapshot snapshot, FirebaseFirestoreException error) {
        hideLoading();

        if (error != null) {
            Log.e(TAG, "Error loading bookings", error);
            showEmptyState("Failed to load bookings");
            Toast.makeText(requireContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        if (snapshot == null || snapshot.isEmpty()) {
            showEmptyState("No bookings found");
            return;
        }

        List<Booking> bookings = snapshot.toObjects(Booking.class);
        // Set IDs from document references
        for (int i = 0; i < bookings.size(); i++) {
            bookings.get(i).setId(snapshot.getDocuments().get(i).getId());
        }

        bookingsAdapter.updateBookings(bookings);
        showEmptyState(null); // Hide empty state
        Log.d(TAG, "Loaded " + bookings.size() + " bookings");
    }

    private void showLoading() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    private void hideLoading() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        if (swipeRefresh != null) {
            swipeRefresh.setRefreshing(false);
        }
    }

    private void showEmptyState(String message) {
        if (tvEmptyMessage != null && recyclerBookings != null) {
            if (message != null && !message.isEmpty()) {
                tvEmptyMessage.setText(message);
                tvEmptyMessage.setVisibility(View.VISIBLE);
                recyclerBookings.setVisibility(View.GONE);
            } else {
                tvEmptyMessage.setVisibility(View.GONE);
                recyclerBookings.setVisibility(View.VISIBLE);
            }
        }
    }

    // BookingActionListener implementation
    @Override
    public void onApprove(Booking booking) {
        // Not used in user view
    }

    @Override
    public void onReject(Booking booking) {
        // Not used in user view
    }

    @Override
    public void onCancel(Booking booking) {
        if (booking == null || booking.getId() == null) {
            Toast.makeText(requireContext(), "Invalid booking", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show confirmation dialog and cancel booking
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Cancel Booking")
                .setMessage("Are you sure you want to cancel this booking?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    DatabaseUtils.cancelBooking(booking.getId(), "Cancelled by user")
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(requireContext(), "Booking cancelled", Toast.LENGTH_SHORT).show();
                                // The snapshot listener will automatically update the UI
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error cancelling booking", e);
                                Toast.makeText(requireContext(), "Failed to cancel booking", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up references
        recyclerBookings = null;
        bookingsAdapter = null;
        swipeRefresh = null;
        tvEmptyMessage = null;
        progressBar = null;
    }
}