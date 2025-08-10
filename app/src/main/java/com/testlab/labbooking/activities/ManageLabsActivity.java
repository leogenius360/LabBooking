package com.testlab.labbooking.activities;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TimePicker;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.testlab.labbooking.R;
import com.testlab.labbooking.adapters.LabsAdapter;
import com.testlab.labbooking.models.Lab;
import com.testlab.labbooking.utils.DatabaseUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ManageLabsActivity extends AppCompatActivity implements LabsAdapter.LabActionListener {

    private RecyclerView recyclerLabs;
    private LabsAdapter labsAdapter;
    private TextView tvEmptyState;
    private TextView tvEmptyStateSubtext;
    private TextView tvResultsCount;
    private MaterialButton btnAddFirstLab;
    private FloatingActionButton fabAddLab;
    private View layoutEmptyState;
    private SearchView searchView;

    private List<Lab> allLabs = new ArrayList<>();
    private List<Lab> filteredLabs = new ArrayList<>();

    // Time picker variables
    private EditText currentTimeField;
    private final Calendar calendar = Calendar.getInstance();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_labs);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupClickListeners();
        loadLabs();
    }

    private void initViews() {
        recyclerLabs = findViewById(R.id.recyclerLabs);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvEmptyStateSubtext = findViewById(R.id.tvEmptyStateSubtext);
        tvResultsCount = findViewById(R.id.tvResultsCount);
        btnAddFirstLab = findViewById(R.id.btnAddFirstLab);
        fabAddLab = findViewById(R.id.fabAddLab);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.manage_labs);
        }
    }

    private void setupRecyclerView() {
        labsAdapter = new LabsAdapter(this, true);
        labsAdapter.setLabActionListener(this);
        recyclerLabs.setLayoutManager(new LinearLayoutManager(this));
        recyclerLabs.setAdapter(labsAdapter);
    }

    private void setupClickListeners() {
        if (btnAddFirstLab != null) {
            btnAddFirstLab.setOnClickListener(v -> showAddLabDialog());
        }

        if (fabAddLab != null) {
            fabAddLab.setOnClickListener(v -> showAddLabDialog());
        }
    }

    private void loadLabs() {
        DatabaseUtils.getAllLabsQuery()
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null) {
                        DatabaseUtils.logOperation("LOAD_LABS_ERROR",
                                DatabaseUtils.LABS_COLLECTION, null, null);
                        Toast.makeText(this, getString(R.string.error_loading_labs,
                                        DatabaseUtils.getFormattedErrorMessage(error)),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        allLabs.clear();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Lab lab = document.toObject(Lab.class);
                            lab.setId(document.getId());
                            allLabs.add(lab);
                        }

                        // Apply current search filter if any
                        if (searchView != null && !searchView.getQuery().toString().isEmpty()) {
                            filterLabs(searchView.getQuery().toString());
                        } else {
                            filteredLabs.clear();
                            filteredLabs.addAll(allLabs);
                            updateUI();
                        }
                    } else {
                        allLabs.clear();
                        filteredLabs.clear();
                        updateUI();
                    }
                });
    }

    private void updateUI() {
        labsAdapter.updateLabs(filteredLabs);

        // Update results counter
        if (tvResultsCount != null) {
            if (!filteredLabs.isEmpty()) {
                tvResultsCount.setText(getString(R.string.labs_count, filteredLabs.size()));
                tvResultsCount.setVisibility(View.VISIBLE);
            } else {
                tvResultsCount.setVisibility(View.GONE);
            }
        }

        // Handle empty state
        if (filteredLabs.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            recyclerLabs.setVisibility(View.GONE);

            if (allLabs.isEmpty()) {
                tvEmptyState.setText(R.string.no_labs_found);
                tvEmptyStateSubtext.setText(R.string.tap_plus_to_add_lab);
                tvEmptyStateSubtext.setVisibility(View.VISIBLE);
                if (btnAddFirstLab != null) {
                    btnAddFirstLab.setVisibility(View.VISIBLE);
                }
            } else {
                tvEmptyState.setText(R.string.no_labs_match_search);
                tvEmptyStateSubtext.setVisibility(View.GONE);
                if (btnAddFirstLab != null) {
                    btnAddFirstLab.setVisibility(View.GONE);
                }
            }
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            recyclerLabs.setVisibility(View.VISIBLE);
        }
    }

    private void filterLabs(String query) {
        filteredLabs.clear();

        if (TextUtils.isEmpty(query)) {
            filteredLabs.addAll(allLabs);
        } else {
            String lowerCaseQuery = query.toLowerCase().trim();
            for (Lab lab : allLabs) {
                if (matchesSearchQuery(lab, lowerCaseQuery)) {
                    filteredLabs.add(lab);
                }
            }
        }

        updateUI();
    }

    private boolean matchesSearchQuery(Lab lab, String query) {
        return (lab.getName() != null && lab.getName().toLowerCase().contains(query)) ||
                (lab.getDescription() != null && lab.getDescription().toLowerCase().contains(query)) ||
                (lab.getLocation() != null && lab.getLocation().toLowerCase().contains(query)) ||
                (lab.getCategory() != null && lab.getCategory().toLowerCase().contains(query)) ||
                (lab.getResources() != null &&
                        lab.getResources().stream().anyMatch(resource ->
                                resource.toLowerCase().contains(query)));
    }

    @Override
    public void onEdit(Lab lab) {
        showEditLabDialog(lab);
    }

    @Override
    public void onDelete(Lab lab) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_lab)
                .setMessage(getString(R.string.delete_lab_confirmation, lab.getName()))
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteLab(lab))
                .setNegativeButton(R.string.cancel, null)
                .setIcon(R.drawable.ic_warning)
                .show();
    }

    @Override
    public void onToggleStatus(Lab lab) {
        DatabaseUtils.updateLabStatus(lab.getId(), !lab.isActive())
                .addOnSuccessListener(aVoid -> {
                    String message = lab.isActive() ?
                            getString(R.string.lab_disabled, lab.getName()) :
                            getString(R.string.lab_enabled, lab.getName());
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

                    DatabaseUtils.logOperation("TOGGLE_LAB_STATUS",
                            DatabaseUtils.LABS_COLLECTION, lab.getId(), null);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, getString(R.string.error_updating_lab,
                                    DatabaseUtils.getFormattedErrorMessage(e)),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteLab(Lab lab) {
        DatabaseUtils.deleteLab(lab.getId())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, getString(R.string.lab_deleted_successfully),
                            Toast.LENGTH_SHORT).show();
                    DatabaseUtils.logOperation("DELETE_LAB",
                            DatabaseUtils.LABS_COLLECTION, lab.getId(), null);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, getString(R.string.error_deleting_lab,
                                    DatabaseUtils.getFormattedErrorMessage(e)),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showEditLabDialog(Lab lab) {
        showLabDialog(lab, false);
    }

    private void showAddLabDialog() {
        showLabDialog(null, true);
    }

    private void showLabDialog(Lab lab, boolean isNew) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isNew ? R.string.add_new_lab : R.string.edit_lab);

        View view = getLayoutInflater().inflate(R.layout.dialog_add_lab, null);
        builder.setView(view);

        setupDialogFields(view, lab, isNew);

        builder.setPositiveButton(isNew ? R.string.add_lab : R.string.save,
                (dialog, which) -> {
                    if (validateLabInput(view)) {
                        if (isNew) {
                            Lab newLab = createLabFromDialog(view);
                            createLab(newLab);
                        } else {
                            updateLabFromDialog(lab, view);
                            updateLab(lab);
                        }
                    }
                });

        builder.setNegativeButton(R.string.cancel, null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void setupDialogFields(View view, Lab lab, boolean isNew) {
        EditText etLabName = view.findViewById(R.id.etLabName);
        EditText etDescription = view.findViewById(R.id.etDescription);
        EditText etCapacity = view.findViewById(R.id.etCapacity);
        EditText etLocation = view.findViewById(R.id.etLocation);
        EditText etResources = view.findViewById(R.id.etResources);
        EditText etCategory = view.findViewById(R.id.etCategory);
        EditText etOpenTime = view.findViewById(R.id.etOpenTime);
        EditText etCloseTime = view.findViewById(R.id.etCloseTime);
        EditText etMinBookingMinutes = view.findViewById(R.id.etMinBookingMinutes);
        EditText etMaxBookingHours = view.findViewById(R.id.etMaxBookingHours);
        EditText etPriority = view.findViewById(R.id.etPriority);
        EditText etHourlyRate = view.findViewById(R.id.etHourlyRate);
        CheckBox cbActive = view.findViewById(R.id.cbActive);
        CheckBox cbMaintenanceMode = view.findViewById(R.id.cbMaintenanceMode);

        if (isNew) {
            // Set default values for new lab
            cbActive.setChecked(true);
            cbMaintenanceMode.setChecked(false);
            etOpenTime.setText(getString(R.string.default_open_time));
            etCloseTime.setText(getString(R.string.default_close_time));
            etMinBookingMinutes.setText(getString(R.string.default_min_duration));
            etMaxBookingHours.setText(getString(R.string.default_max_duration));
            etPriority.setText(getString(R.string.default_priority));
            etHourlyRate.setText(getString(R.string.default_cost));
        } else {
            // Pre-fill with existing data
            etLabName.setText(lab.getName());
            etDescription.setText(lab.getDescription());
            etCapacity.setText(String.valueOf(lab.getCapacity()));
            etLocation.setText(lab.getLocation());

            if (lab.getResources() != null && !lab.getResources().isEmpty()) {
                etResources.setText(TextUtils.join(", ", lab.getResources()));
            }

            if (lab.getCategory() != null) {
                etCategory.setText(lab.getCategory());
            }

            etOpenTime.setText(lab.getOpenTime() != null ? lab.getOpenTime() : getString(R.string.default_open_time));
            etCloseTime.setText(lab.getCloseTime() != null ? lab.getCloseTime() : getString(R.string.default_close_time));
            etMinBookingMinutes.setText(String.valueOf(lab.getMinBookingMinutes() > 0 ? lab.getMinBookingMinutes() : 30));
            etMaxBookingHours.setText(String.valueOf(lab.getMaxBookingHours() > 0 ? lab.getMaxBookingHours() : 4));
            etPriority.setText(String.valueOf(lab.getPriority() > 0 ? lab.getPriority() : 5));
            etHourlyRate.setText(String.valueOf(lab.getHourlyRate()));

            cbActive.setChecked(lab.isActive());
            cbMaintenanceMode.setChecked(lab.isMaintenanceMode());
        }

        // Setup time pickers
        setupTimePicker(etOpenTime);
        setupTimePicker(etCloseTime);
    }

    private void setupTimePicker(EditText timeField) {
        timeField.setOnClickListener(v -> {
            currentTimeField = timeField;
            showTimePickerDialog();
        });
    }

    private void showTimePickerDialog() {
        // Parse current time from field
        String currentTimeStr = currentTimeField.getText().toString();
        int hour = 8; // default hour
        int minute = 0; // default minute

        try {
            Date time = timeFormat.parse(currentTimeStr);
            if (time != null) {
                calendar.setTime(time);
                hour = calendar.get(Calendar.HOUR_OF_DAY);
                minute = calendar.get(Calendar.MINUTE);
            }
        } catch (Exception e) {
            // Use defaults
        }

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (TimePicker view, int selectedHour, int selectedMinute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, selectedHour);
                    calendar.set(Calendar.MINUTE, selectedMinute);
                    currentTimeField.setText(timeFormat.format(calendar.getTime()));
                },
                hour,
                minute,
                false
        );

        timePickerDialog.show();
    }

    private boolean validateLabInput(View view) {
        EditText etLabName = view.findViewById(R.id.etLabName);
        EditText etCapacity = view.findViewById(R.id.etCapacity);
        EditText etLocation = view.findViewById(R.id.etLocation);
        EditText etMinBookingMinutes = view.findViewById(R.id.etMinBookingMinutes);
        EditText etMaxBookingHours = view.findViewById(R.id.etMaxBookingHours);
        EditText etPriority = view.findViewById(R.id.etPriority);

        String name = etLabName.getText().toString().trim();
        String capacityStr = etCapacity.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String minDurationStr = etMinBookingMinutes.getText().toString().trim();
        String maxDurationStr = etMaxBookingHours.getText().toString().trim();
        String priorityStr = etPriority.getText().toString().trim();

        // Validate required fields
        if (TextUtils.isEmpty(name)) {
            etLabName.setError(getString(R.string.lab_name_required));
            etLabName.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(capacityStr)) {
            etCapacity.setError(getString(R.string.capacity_required));
            etCapacity.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(location)) {
            etLocation.setError(getString(R.string.location_required));
            etLocation.requestFocus();
            return false;
        }

        // Validate numeric fields
        try {
            int capacity = Integer.parseInt(capacityStr);
            if (capacity <= 0) {
                etCapacity.setError(getString(R.string.capacity_must_be_positive));
                etCapacity.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            etCapacity.setError(getString(R.string.invalid_capacity_number));
            etCapacity.requestFocus();
            return false;
        }

        // Validate duration fields
        try {
            int minDuration = Integer.parseInt(minDurationStr);
            int maxDuration = Integer.parseInt(maxDurationStr);

            if (minDuration <= 0) {
                etMinBookingMinutes.setError("Minimum duration must be greater than 0");
                etMinBookingMinutes.requestFocus();
                return false;
            }

            if (maxDuration <= 0) {
                etMaxBookingHours.setError("Maximum duration must be greater than 0");
                etMaxBookingHours.requestFocus();
                return false;
            }

            if (minDuration >= maxDuration * 60) {
                etMinBookingMinutes.setError("Minimum duration must be less than maximum");
                etMinBookingMinutes.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            etMinBookingMinutes.setError("Please enter valid numbers");
            etMinBookingMinutes.requestFocus();
            return false;
        }

        // Validate priority
        try {
            int priority = Integer.parseInt(priorityStr);
            if (priority < 1 || priority > 10) {
                etPriority.setError("Priority must be between 1 and 10");
                etPriority.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            etPriority.setError("Please enter a valid priority");
            etPriority.requestFocus();
            return false;
        }

        return true;
    }

    private Lab createLabFromDialog(View view) {
        Lab lab = new Lab();
        updateLabFromDialog(lab, view);
        lab.setCreatedAt(new Date());
        return lab;
    }

    private void updateLabFromDialog(Lab lab, View view) {
        EditText etLabName = view.findViewById(R.id.etLabName);
        EditText etDescription = view.findViewById(R.id.etDescription);
        EditText etCapacity = view.findViewById(R.id.etCapacity);
        EditText etLocation = view.findViewById(R.id.etLocation);
        EditText etResources = view.findViewById(R.id.etResources);
        EditText etCategory = view.findViewById(R.id.etCategory);
        EditText etOpenTime = view.findViewById(R.id.etOpenTime);
        EditText etCloseTime = view.findViewById(R.id.etCloseTime);
        EditText etMinBookingMinutes = view.findViewById(R.id.etMinBookingMinutes);
        EditText etMaxBookingHours = view.findViewById(R.id.etMaxBookingHours);
        EditText etPriority = view.findViewById(R.id.etPriority);
        EditText etHourlyRate = view.findViewById(R.id.etHourlyRate);
        CheckBox cbActive = view.findViewById(R.id.cbActive);
        CheckBox cbMaintenanceMode = view.findViewById(R.id.cbMaintenanceMode);

        lab.setName(etLabName.getText().toString().trim());
        lab.setDescription(etDescription.getText().toString().trim());
        lab.setCapacity(Integer.parseInt(etCapacity.getText().toString().trim()));
        lab.setLocation(etLocation.getText().toString().trim());
        lab.setActive(cbActive.isChecked());
        lab.setMaintenanceMode(cbMaintenanceMode.isChecked());
        lab.setUpdatedAt(new Date());

        String resourcesStr = etResources.getText().toString().trim();
        if (!TextUtils.isEmpty(resourcesStr)) {
            lab.setResources(Arrays.asList(resourcesStr.split("\\s*,\\s*")));
        } else {
            lab.setResources(new ArrayList<>());
        }

        String category = etCategory.getText().toString().trim();
        lab.setCategory(TextUtils.isEmpty(category) ? null : category);

        lab.setOpenTime(etOpenTime.getText().toString().trim());
        lab.setCloseTime(etCloseTime.getText().toString().trim());
        lab.setMinBookingMinutes(Integer.parseInt(etMinBookingMinutes.getText().toString().trim()));
        lab.setMaxBookingHours(Integer.parseInt(etMaxBookingHours.getText().toString().trim()));
        lab.setPriority(Integer.parseInt(etPriority.getText().toString().trim()));

        try {
            lab.setHourlyRate(Double.parseDouble(etHourlyRate.getText().toString().trim()));
        } catch (NumberFormatException e) {
            lab.setHourlyRate(0.0);
        }
    }

    private void createLab(Lab lab) {
        DatabaseUtils.createLab(lab)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, getString(R.string.lab_created_successfully),
                            Toast.LENGTH_SHORT).show();
                    DatabaseUtils.logOperation("CREATE_LAB",
                            DatabaseUtils.LABS_COLLECTION, documentReference.getId(), null);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, getString(R.string.error_creating_lab,
                                    DatabaseUtils.getFormattedErrorMessage(e)),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void updateLab(Lab lab) {
        DatabaseUtils.updateLab(lab.getId(), lab)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, getString(R.string.lab_updated_successfully),
                            Toast.LENGTH_SHORT).show();
                    DatabaseUtils.logOperation("UPDATE_LAB",
                            DatabaseUtils.LABS_COLLECTION, lab.getId(), null);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, getString(R.string.error_updating_lab,
                                    DatabaseUtils.getFormattedErrorMessage(e)),
                            Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.manage_labs_menu, menu);

        // Setup search functionality
        MenuItem searchItem = menu.findItem(R.id.action_search);
        if (searchItem != null) {
            searchView = (SearchView) searchItem.getActionView();
            if (searchView != null) {
                searchView.setQueryHint(getString(R.string.search_labs_hint));
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        filterLabs(query);
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        filterLabs(newText);
                        return true;
                    }
                });

                searchView.setOnCloseListener(() -> {
                    filteredLabs.clear();
                    filteredLabs.addAll(allLabs);
                    updateUI();
                    return false;
                });
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.action_add_lab) {
            showAddLabDialog();
            return true;
        } else if (itemId == R.id.action_refresh) {
            loadLabs();
            Toast.makeText(this, getString(R.string.refreshing_labs), Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == R.id.action_filter) {
            showFilterDialog();
            return true;
        } else if (itemId == R.id.action_sort) {
            showSortDialog();
            return true;
        } else if (itemId == R.id.action_export) {
            exportLabsData();
            return true;
        } else if (itemId == R.id.action_settings) {
            // Navigate to settings if needed
            Toast.makeText(this, "Settings not implemented yet", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showFilterDialog() {
        String[] filterOptions = {
                "All Labs",
                "Active Only",
                "Inactive Only",
                "Maintenance Mode",
                "Available for Booking"
        };

        new AlertDialog.Builder(this)
                .setTitle("Filter Labs")
                .setItems(filterOptions, (dialog, which) -> {
                    applyFilter(which);
                })
                .show();
    }

    private void applyFilter(int filterType) {
        List<Lab> filtered = new ArrayList<>();

        switch (filterType) {
            case 0: // All Labs
                filtered.addAll(allLabs);
                break;
            case 1: // Active Only
                for (Lab lab : allLabs) {
                    if (lab.isActive()) {
                        filtered.add(lab);
                    }
                }
                break;
            case 2: // Inactive Only
                for (Lab lab : allLabs) {
                    if (!lab.isActive()) {
                        filtered.add(lab);
                    }
                }
                break;
            case 3: // Maintenance Mode
                for (Lab lab : allLabs) {
                    if (lab.isMaintenanceMode()) {
                        filtered.add(lab);
                    }
                }
                break;
            case 4: // Available for Booking
                for (Lab lab : allLabs) {
                    if (lab.isActive() && !lab.isMaintenanceMode()) {
                        filtered.add(lab);
                    }
                }
                break;
        }

        filteredLabs.clear();
        filteredLabs.addAll(filtered);
        updateUI();
    }

    private void showSortDialog() {
        String[] sortOptions = {
                "Name (A-Z)",
                "Name (Z-A)",
                "Priority (Low to High)",
                "Priority (High to Low)",
                "Capacity (Low to High)",
                "Capacity (High to Low)",
                "Location",
                "Recently Added"
        };

        new AlertDialog.Builder(this)
                .setTitle("Sort Labs")
                .setItems(sortOptions, (dialog, which) -> {
                    applySorting(which);
                })
                .show();
    }

    private void applySorting(int sortType) {
        switch (sortType) {
            case 0: // Name A-Z
                filteredLabs.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                break;
            case 1: // Name Z-A
                filteredLabs.sort((a, b) -> b.getName().compareToIgnoreCase(a.getName()));
                break;
            case 2: // Priority Low to High
                filteredLabs.sort((a, b) -> Integer.compare(a.getPriority(), b.getPriority()));
                break;
            case 3: // Priority High to Low
                filteredLabs.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
                break;
            case 4: // Capacity Low to High
                filteredLabs.sort((a, b) -> Integer.compare(a.getCapacity(), b.getCapacity()));
                break;
            case 5: // Capacity High to Low
                filteredLabs.sort((a, b) -> Integer.compare(b.getCapacity(), a.getCapacity()));
                break;
            case 6: // Location
                filteredLabs.sort((a, b) -> {
                    String locA = a.getLocation() != null ? a.getLocation() : "";
                    String locB = b.getLocation() != null ? b.getLocation() : "";
                    return locA.compareToIgnoreCase(locB);
                });
                break;
            case 7: // Recently Added
                filteredLabs.sort((a, b) -> {
                    Date dateA = a.getCreatedAt() != null ? a.getCreatedAt() : new Date(0);
                    Date dateB = b.getCreatedAt() != null ? b.getCreatedAt() : new Date(0);
                    return dateB.compareTo(dateA);
                });
                break;
        }

        updateUI();
    }

    private void exportLabsData() {
        // This is a placeholder for export functionality
        // You could implement CSV export, JSON export, etc.
        Toast.makeText(this, "Export functionality would be implemented here",
                Toast.LENGTH_SHORT).show();

        // Example implementation outline:
        /*
        StringBuilder csvData = new StringBuilder();
        csvData.append("Name,Description,Capacity,Location,Category,Active,Maintenance\n");

        for (Lab lab : filteredLabs) {
            csvData.append(lab.getName()).append(",")
                   .append(lab.getDescription() != null ? lab.getDescription() : "").append(",")
                   .append(lab.getCapacity()).append(",")
                   .append(lab.getLocation() != null ? lab.getLocation() : "").append(",")
                   .append(lab.getCategory() != null ? lab.getCategory() : "").append(",")
                   .append(lab.isActive()).append(",")
                   .append(lab.isMaintenanceMode()).append("\n");
        }

        // Save to file or share
        */
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up any listeners or resources if needed
    }
}