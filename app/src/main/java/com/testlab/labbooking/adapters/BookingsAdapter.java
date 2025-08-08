package com.testlab.labbooking.adapters;

import android.content.Context;
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
        Booking booking = bookings.get(position);
        holder.bind(booking);
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    public void updateBookings(List<Booking> newBookings) {
        this.bookings.clear();
        this.bookings.addAll(newBookings);
        notifyDataSetChanged();
    }

    class BookingViewHolder extends RecyclerView.ViewHolder {
        private TextView tvLabName, tvDateTime, tvPurpose, tvStatus, tvAdminNotes;
        private LinearLayout layoutActions;
        private Button btnApprove, btnReject;

        public BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLabName = itemView.findViewById(R.id.tvLabName);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvPurpose = itemView.findViewById(R.id.tvPurpose);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvAdminNotes = itemView.findViewById(R.id.tvAdminNotes);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
        }

        public void bind(Booking booking) {
            tvLabName.setText(booking.getLabName());
            tvDateTime.setText(booking.getDate() + " | " + booking.getStartTime() + " - " + booking.getEndTime());
            tvPurpose.setText("Purpose: " + booking.getPurpose());

            // Set status with appropriate color
            tvStatus.setText(booking.getStatus().toUpperCase());
            int statusColor = getStatusColor(booking.getStatus());
            tvStatus.setBackgroundColor(statusColor);

            // Show admin notes if available
            if (booking.getAdminNotes() != null && !booking.getAdminNotes().isEmpty()) {
                tvAdminNotes.setText("Admin Notes: " + booking.getAdminNotes());
                tvAdminNotes.setVisibility(View.VISIBLE);
            } else {
                tvAdminNotes.setVisibility(View.GONE);
            }

            // Show admin actions if in admin view and booking is pending
            if (isAdminView && DatabaseUtils.STATUS_PENDING.equals(booking.getStatus())) {
                layoutActions.setVisibility(View.VISIBLE);
                btnApprove.setOnClickListener(v -> {
                    if (listener != null) listener.onApprove(booking);
                });
                btnReject.setOnClickListener(v -> {
                    if (listener != null) listener.onReject(booking);
                });
            } else {
                layoutActions.setVisibility(View.GONE);
            }
        }

        private int getStatusColor(String status) {
            switch (status) {
                case DatabaseUtils.STATUS_APPROVED:
                    return ContextCompat.getColor(context, android.R.color.holo_green_dark);
                case DatabaseUtils.STATUS_REJECTED:
                case DatabaseUtils.STATUS_CANCELLED:
                    return ContextCompat.getColor(context, android.R.color.holo_red_dark);
                case DatabaseUtils.STATUS_PENDING:
                default:
                    return ContextCompat.getColor(context, android.R.color.holo_orange_dark);
            }
        }

//        public void updateBookings(List<Booking> bookings) {
//            this.bookings = bookings;
//            notifyDataSetChanged();
//
//            // Add this to your adapter
//            if (bookings.isEmpty()) {
//                if (emptyView != null) {
//                    emptyView.setVisibility(View.VISIBLE);
//                }
//            } else {
//                if (emptyView != null) {
//                    emptyView.setVisibility(View.GONE);
//                }
//            }
//        }
    }
}