package com.testlab.labbooking.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.testlab.labbooking.R;
import com.testlab.labbooking.models.Booking;
import com.testlab.labbooking.models.BookingStatus;
import com.testlab.labbooking.utils.DatabaseUtils;

import java.util.ArrayList;
import java.util.List;

public class BookingsAdapter extends RecyclerView.Adapter<BookingsAdapter.BookingViewHolder> {

    private List<Booking> bookings;
    private Context context;
    private boolean isAdminView;
    private BookingActionListener listener;

    public interface BookingActionListener {
        void onApprove(Booking booking);
        void onReject(Booking booking);
        void onCancel(Booking booking);
    }

    public BookingsAdapter(Context context, boolean isAdminView) {
        this.context = context;
        this.isAdminView = isAdminView;
        this.bookings = new ArrayList<>();
    }

    public void setBookingActionListener(BookingActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_booking, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        if (position >= 0 && position < bookings.size()) {
            Booking booking = bookings.get(position);
            holder.bind(booking);
        }
    }

    @Override
    public int getItemCount() {
        return bookings != null ? bookings.size() : 0;
    }

    public void updateBookings(List<Booking> newBookings) {
        if (newBookings == null) {
            this.bookings.clear();
        } else {
            this.bookings.clear();
            this.bookings.addAll(newBookings);
        }
        notifyDataSetChanged();
    }

    public void addBooking(Booking booking) {
        if (booking != null) {
            this.bookings.add(0, booking); // Add at beginning
            notifyItemInserted(0);
        }
    }

    public void removeBooking(String bookingId) {
        if (bookingId == null || bookingId.isEmpty()) return;

        for (int i = 0; i < bookings.size(); i++) {
            if (bookingId.equals(bookings.get(i).getId())) {
                bookings.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    public void updateBooking(Booking updatedBooking) {
        if (updatedBooking == null || updatedBooking.getId() == null) return;

        for (int i = 0; i < bookings.size(); i++) {
            if (updatedBooking.getId().equals(bookings.get(i).getId())) {
                bookings.set(i, updatedBooking);
                notifyItemChanged(i);
                break;
            }
        }
    }

    class BookingViewHolder extends RecyclerView.ViewHolder {
        private TextView tvLabName, tvDateTime, tvPurpose, tvStatus, tvAdminNotes, tvUserName;
        private LinearLayout layoutActions;
        private Button btnApprove, btnReject, btnCancel;

        public BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            initViews();
        }

        private void initViews() {
            tvLabName = itemView.findViewById(R.id.tvLabName);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvPurpose = itemView.findViewById(R.id.tvPurpose);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvAdminNotes = itemView.findViewById(R.id.tvAdminNotes);
            tvUserName = itemView.findViewById(R.id.tvUserName); // For admin view
            layoutActions = itemView.findViewById(R.id.layoutActions);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
            btnCancel = itemView.findViewById(R.id.btnCancel);
        }

        public void bind(Booking booking) {
            if (booking == null) return;

            // Basic info
            tvLabName.setText(booking.getLabName() != null ? booking.getLabName() : "Unknown Lab");
            tvDateTime.setText(booking.getDateTimeDisplay());
            tvPurpose.setText("Purpose: " + booking.getPurpose());

            // Status with color
            BookingStatus status = booking.getStatus();
            tvStatus.setText(status.getDisplayName());
            tvStatus.setBackgroundColor(Color.parseColor(status.getColorCode()));

            // Admin notes if present
            tvAdminNotes.setVisibility(
                    booking.getAdminNotes() != null ? View.VISIBLE : View.GONE);
            if (booking.getAdminNotes() != null) {
                tvAdminNotes.setText("Notes: " + booking.getAdminNotes());
            }

            // Show user name in admin view
            if (isAdminView && tvUserName != null) {
                tvUserName.setText("Booked by: " + booking.getUserName());
                tvUserName.setVisibility(View.VISIBLE);
            }

            // Action buttons based on status and view type
            setupActionButtons(booking, status);
        }

        private void setupActionButtons(Booking booking, BookingStatus status) {
            boolean showActions = (isAdminView && status.canAdminApprove()) ||
                    (!isAdminView && status.canUserCancel());

            layoutActions.setVisibility(showActions ? View.VISIBLE : View.GONE);

            if (isAdminView && status == BookingStatus.PENDING) {
                btnApprove.setVisibility(View.VISIBLE);
                btnReject.setVisibility(View.VISIBLE);
                btnCancel.setVisibility(View.GONE);
            } else if (!isAdminView && status == BookingStatus.PENDING) {
                btnApprove.setVisibility(View.GONE);
                btnReject.setVisibility(View.GONE);
                btnCancel.setVisibility(View.VISIBLE);
            }
        }
    }
}