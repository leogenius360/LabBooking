package com.testlab.labbooking.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.testlab.labbooking.R;
import com.testlab.labbooking.adapters.LabsAdapter;
import com.testlab.labbooking.models.Lab;
import com.testlab.labbooking.utils.DatabaseUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ManageLabsActivity extends AppCompatActivity implements LabsAdapter.LabActionListener {

    private RecyclerView recyclerLabs;
    private LabsAdapter labsAdapter;
    private TextView tvEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_labs);

        initViews();
        setupToolbar();
        setupRecyclerView();
        loadLabs();
    }

    private void initViews() {
        recyclerLabs = findViewById(R.id.recyclerLabs);
        tvEmptyState = findViewById(R.id.tvEmptyState);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Manage Labs");
        }
    }

    private void setupRecyclerView() {
        labsAdapter = new LabsAdapter(this);
        labsAdapter.setLabActionListener(this);
        recyclerLabs.setLayoutManager(new LinearLayoutManager(this));
        recyclerLabs.setAdapter(labsAdapter);
    }

    private void loadLabs() {
        DatabaseUtils.getInstance().collection(DatabaseUtils.LABS_COLLECTION)
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error loading labs: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        List<Lab> labs = new ArrayList<>();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Lab lab = document.toObject(Lab.class);
                            lab.setId(document.getId());
                            labs.add(lab);
                        }
                        labsAdapter.updateLabs(labs);
                        tvEmptyState.setVisibility(View.GONE);
                    } else {
                        labsAdapter.updateLabs(new ArrayList<>());
                        tvEmptyState.setVisibility(View.VISIBLE);
                    }
                });
    }

    @Override
    public void onEdit(Lab lab) {
        showEditLabDialog(lab);
    }

    @Override
    public void onDelete(Lab lab) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Lab")
                .setMessage("Are you sure you want to delete " + lab.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> deleteLab(lab))
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onToggleStatus(Lab lab) {
        DatabaseUtils.getInstance().collection(DatabaseUtils.LABS_COLLECTION)
                .document(lab.getId())
                .update("isActive", !lab.isActive())
                .addOnSuccessListener(aVoid -> {
                    String status = lab.isActive() ? "disabled" : "enabled";
                    Toast.makeText(this, lab.getName() + " has been " + status, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error updating lab: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteLab(Lab lab) {
        DatabaseUtils.getInstance().collection(DatabaseUtils.LABS_COLLECTION)
                .document(lab.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, lab.getName() + " deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error deleting lab: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showEditLabDialog(Lab lab) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Lab");
        
        View view = getLayoutInflater().inflate(R.layout.dialog_add_lab, null);
        builder.setView(view);
        
        EditText etLabName = view.findViewById(R.id.etLabName);
        EditText etDescription = view.findViewById(R.id.etDescription);
        EditText etCapacity = view.findViewById(R.id.etCapacity);
        EditText etLocation = view.findViewById(R.id.etLocation);
        EditText etResources = view.findViewById(R.id.etResources);
        CheckBox cbActive = view.findViewById(R.id.cbActive);
        
        // Pre-fill with existing data
        etLabName.setText(lab.getName());
        etDescription.setText(lab.getDescription());
        etCapacity.setText(String.valueOf(lab.getCapacity()));
        etLocation.setText(lab.getLocation());
        
        if (lab.getResources() != null) {
            etResources.setText(TextUtils.join(", ", lab.getResources()));
        }
        
        cbActive.setChecked(lab.isActive());
        
        builder.setPositiveButton("Save", (dialog, which) -> {
            // Get updated values
            String name = etLabName.getText().toString().trim();
            String description = etDescription.getText().toString().trim();
            int capacity = Integer.parseInt(etCapacity.getText().toString().trim());
            String location = etLocation.getText().toString().trim();
            String resourcesStr = etResources.getText().toString().trim();
            boolean isActive = cbActive.isChecked();
            
            // Update lab object
            lab.setName(name);
            lab.setDescription(description);
            lab.setCapacity(capacity);
            lab.setLocation(location);
            
            if (!resourcesStr.isEmpty()) {
                lab.setResources(Arrays.asList(resourcesStr.split("\\s*,\\s*")));
            }
            
            lab.setActive(isActive);
            
            // Save to Firestore
            updateLab(lab);
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void updateLab(Lab lab) {
        DatabaseUtils.getInstance().collection(DatabaseUtils.LABS_COLLECTION)
                .document(lab.getId())
                .set(lab)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Lab updated successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error updating lab: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.manage_labs_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_add_lab) {
            showAddLabDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAddLabDialog() {
        // Same as in AdminActivity, but without FAB
        // You can move this to a shared method if needed
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}