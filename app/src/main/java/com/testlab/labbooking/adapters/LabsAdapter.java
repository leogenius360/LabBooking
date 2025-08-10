package com.testlab.labbooking.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.testlab.labbooking.R;
import com.testlab.labbooking.activities.BookingActivity;
import com.testlab.labbooking.models.Lab;

import java.util.ArrayList;
import java.util.List;

public class LabsAdapter extends RecyclerView.Adapter<LabsAdapter.LabViewHolder> {

    // Interface for booking actions (used in main lab listing)
    public interface OnLabBookClickListener {
        void onBookLab(Lab lab);
    }

    // Interface for management actions (used in manage labs activity)
    public interface LabActionListener {
        void onEdit(Lab lab);
        void onDelete(Lab lab);
        void onToggleStatus(Lab lab);
    }

    private Context context;
    private List<Lab> labs;
    private OnLabBookClickListener bookClickListener;
    private LabActionListener labActionListener;
    private boolean isManagementMode = false;

    public LabsAdapter(Context context) {
        this.context = context;
        this.labs = new ArrayList<>();
    }

    public LabsAdapter(Context context, boolean isManagementMode) {
        this.context = context;
        this.labs = new ArrayList<>();
        this.isManagementMode = isManagementMode;
    }

    public void setOnLabBookClickListener(OnLabBookClickListener listener) {
        this.bookClickListener = listener;
    }

    public void setLabActionListener(LabActionListener listener) {
        this.labActionListener = listener;
    }

    public void updateLabs(List<Lab> labs) {
        this.labs = labs != null ? labs : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LabViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = isManagementMode ? R.layout.item_lab_admin : R.layout.item_lab;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new LabViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LabViewHolder holder, int position) {
        Lab lab = labs.get(position);
        holder.bind(lab);
    }

    @Override
    public int getItemCount() {
        return labs.size();
    }

    class LabViewHolder extends RecyclerView.ViewHolder {
        private TextView tvLabName;
        private TextView tvCapacity;
        private TextView tvLocation;
        private TextView tvDescription;
        private TextView tvResources;

        // For booking mode
        private Button btnBookLab;

        // For management mode
        private TextView tvStatus;
        private Button btnEdit;
        private Button btnDelete;
        private Button btnToggleStatus;

        public LabViewHolder(@NonNull View itemView) {
            super(itemView);
            initViews();
        }

        private void initViews() {
            tvLabName = itemView.findViewById(R.id.tvLabName);
            tvCapacity = itemView.findViewById(R.id.tvCapacity);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvResources = itemView.findViewById(R.id.tvResources);

            if (isManagementMode) {
                // Management mode views
                tvStatus = itemView.findViewById(R.id.tvStatus);
                btnEdit = itemView.findViewById(R.id.btnEdit);
                btnDelete = itemView.findViewById(R.id.btnDelete);
                btnToggleStatus = itemView.findViewById(R.id.btnToggleStatus);
            } else {
                // Booking mode views
                btnBookLab = itemView.findViewById(R.id.btnBookLab);
            }
        }

        public void bind(Lab lab) {
            // Set common data
            tvLabName.setText(lab.getName());
            tvCapacity.setText("Capacity: " + lab.getCapacity());
            tvLocation.setText(lab.getLocation());

            // Handle description
            if (tvDescription != null) {
                String description = lab.getDescription();
                if (description != null && !description.trim().isEmpty()) {
                    tvDescription.setText(description);
                    tvDescription.setVisibility(View.VISIBLE);
                } else {
                    tvDescription.setVisibility(View.GONE);
                }
            }

            // Handle resources
            if (tvResources != null) {
                String resources = getResourcesAsString(lab);
                tvResources.setText("Resources: " + resources);
            }

            if (isManagementMode) {
                bindManagementMode(lab);
            } else {
                bindBookingMode(lab);
            }
        }

        private void bindManagementMode(Lab lab) {
            // Set status
            if (tvStatus != null) {
                if (lab.isActive()) {
                    if (lab.isMaintenanceMode()) {
                        tvStatus.setText("Maintenance");
                        tvStatus.setBackgroundColor(context.getResources().getColor(android.R.color.holo_orange_dark));
                    } else {
                        tvStatus.setText("Active");
                        tvStatus.setBackgroundColor(context.getResources().getColor(android.R.color.holo_green_dark));
                    }
                } else {
                    tvStatus.setText("Inactive");
                    tvStatus.setBackgroundColor(context.getResources().getColor(android.R.color.holo_red_dark));
                }
            }

            // Set toggle button text
            if (btnToggleStatus != null) {
                btnToggleStatus.setText(lab.isActive() ? "Disable" : "Enable");
            }

            // Set click listeners
            if (btnEdit != null) {
                btnEdit.setOnClickListener(v -> {
                    if (labActionListener != null) {
                        labActionListener.onEdit(lab);
                    }
                });
            }

            if (btnDelete != null) {
                btnDelete.setOnClickListener(v -> {
                    if (labActionListener != null) {
                        labActionListener.onDelete(lab);
                    }
                });
            }

            if (btnToggleStatus != null) {
                btnToggleStatus.setOnClickListener(v -> {
                    if (labActionListener != null) {
                        labActionListener.onToggleStatus(lab);
                    }
                });
            }
        }

        private void bindBookingMode(Lab lab) {
            if (btnBookLab != null) {
                // Update button state based on lab availability
                if (lab.isActive() && !lab.isMaintenanceMode()) {
                    btnBookLab.setText("Book This Lab");
                    btnBookLab.setEnabled(true);
                    btnBookLab.setAlpha(1.0f);
                } else {
                    if (lab.isMaintenanceMode()) {
                        btnBookLab.setText("Under Maintenance");
                    } else {
                        btnBookLab.setText("Unavailable");
                    }
                    btnBookLab.setEnabled(false);
                    btnBookLab.setAlpha(0.6f);
                }

                // Set click listener
                btnBookLab.setOnClickListener(v -> {
                    if (lab.isActive() && !lab.isMaintenanceMode()) {
                        if (bookClickListener != null) {
                            bookClickListener.onBookLab(lab);
                        } else {
                            // Start booking activity directly
                            Intent intent = new Intent(context, BookingActivity.class);
                            intent.putExtra("lab_id", lab.getId());
                            intent.putExtra("lab_name", lab.getName());
                            context.startActivity(intent);
                        }
                    }
                });
            }
        }

        private String getResourcesAsString(Lab lab) {
            if (lab.getResources() != null && !lab.getResources().isEmpty()) {
                return String.join(", ", lab.getResources());
            }
            return "None";
        }
    }
}