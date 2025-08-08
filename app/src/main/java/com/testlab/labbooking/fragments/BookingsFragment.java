package com.testlab.labbooking.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.testlab.labbooking.R;
import com.testlab.labbooking.adapters.BookingsAdapter;
import com.testlab.labbooking.models.Booking;
import com.testlab.labbooking.models.User;
import com.testlab.labbooking.utils.DatabaseUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BookingsFragment extends Fragment {

    private RecyclerView recyclerBookings;
    private BookingsAdapter bookingsAdapter;
    private User currentUser;

    public void setUser(User user) {
        this.currentUser = user;
        if (isAdded() && getView() != null) {
            loadUserBookings(user);
        }
    }

    public static BookingsFragment newInstance(User user) {
        BookingsFragment fragment = new BookingsFragment();
        Bundle args = new Bundle();
        args.putSerializable("currentUser", (Serializable) user);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bookings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        if (getArguments() != null) {
            currentUser = (User) getArguments().getSerializable("currentUser");
        }
        
        recyclerBookings = view.findViewById(R.id.recyclerBookings);
        setupRecyclerView();
        loadUserBookings(currentUser);
    }

    private void setupRecyclerView() {
        bookingsAdapter = new BookingsAdapter(requireContext(), false);
        recyclerBookings.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerBookings.setAdapter(bookingsAdapter);
    }

    public void loadUserBookings(User user) {
        if (user == null || user.getId() == null) {
            Toast.makeText(getContext(), "User data not available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Load bookings using user ID
        DatabaseUtils.getUserBookingsQuery(user.getId())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Booking> bookings = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Booking booking = document.toObject(Booking.class);
                        booking.setId(document.getId());
                        bookings.add(booking);
                    }
                    bookingsAdapter.updateBookings(bookings);
                })
                .addOnFailureListener(e -> Toast.makeText(
                        requireContext(), 
                        "Error loading bookings: " + e.getMessage(), 
                        Toast.LENGTH_SHORT
                ).show());
    }

}