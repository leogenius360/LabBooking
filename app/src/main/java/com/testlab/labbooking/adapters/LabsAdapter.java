package com.testlab.labbooking.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.testlab.labbooking.R;
import com.testlab.labbooking.models.Lab;

import java.util.List;

public class LabsAdapter extends RecyclerView.Adapter<LabsAdapter.LabViewHolder> {

    public interface LabActionListener {
        void onEdit(Lab lab);
        void onDelete(Lab lab);
        void onToggleStatus(Lab lab);
    }

    private Context context;
    private List<Lab> labs;
    private LabActionListener labActionListener;

    public LabsAdapter(Context context) {
        this.context = context;
    }

    public void setLabActionListener(LabActionListener listener) {
        this.labActionListener = listener;
    }

    public void updateLabs(List<Lab> labs) {
        this.labs = labs;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LabViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lab_admin, parent, false);
        return new LabViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LabViewHolder holder, int position) {
        Lab lab = labs.get(position);

        holder.tvLabName.setText(lab.getName());
        holder.tvCapacity.setText("Capacity: " + lab.getCapacity());
        holder.tvLocation.setText(lab.getLocation());
        holder.tvStatus.setText(lab.isActive() ? "Active" : "Inactive");
        holder.tvStatus.setTextColor(context.getResources().getColor(
                lab.isActive() ? R.color.green : R.color.red));

        holder.btnEdit.setOnClickListener(v -> {
            if (labActionListener != null) labActionListener.onEdit(lab);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (labActionListener != null) labActionListener.onDelete(lab);
        });

        holder.btnToggleStatus.setOnClickListener(v -> {
            if (labActionListener != null) labActionListener.onToggleStatus(lab);
        });
    }

    @Override
    public int getItemCount() {
        return labs != null ? labs.size() : 0;
    }

    static class LabViewHolder extends RecyclerView.ViewHolder {
        TextView tvLabName, tvCapacity, tvLocation, tvStatus;
        Button btnEdit, btnDelete, btnToggleStatus;

        public LabViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLabName = itemView.findViewById(R.id.tvLabName);
            tvCapacity = itemView.findViewById(R.id.tvCapacity);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnToggleStatus = itemView.findViewById(R.id.btnToggleStatus);
        }
    }
}