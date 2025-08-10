package com.testlab.labbooking.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.testlab.labbooking.R;
import com.testlab.labbooking.activities.BookingActivity;
import com.testlab.labbooking.adapters.LabsAdapter;
import com.testlab.labbooking.models.Lab;
import com.testlab.labbooking.models.User;
import com.testlab.labbooking.repositories.LabRepository;
import com.testlab.labbooking.viewmodels.LabViewModel;

import java.util.List;

public class LabsFragment extends Fragment implements LabsAdapter.OnLabBookClickListener {

    private RecyclerView recyclerLabs;
    private LabsAdapter labsAdapter;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout emptyStateLayout;
    private TextView emptyStateMessage;

    private ProgressBar progressBar;
    private LabViewModel labViewModel;

    private SearchView searchView;
    private MenuItem searchMenuItem;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        labViewModel = new ViewModelProvider(this).get(LabViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_labs, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        setupSwipeRefresh();
        setupObservers();

        // Load labs data
        labViewModel.loadLabs();
    }

    private void initViews(View view) {
        recyclerLabs = view.findViewById(R.id.recyclerLabs);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        emptyStateMessage = view.findViewById(R.id.emptyStateMessage);
        progressBar = view.findViewById(R.id.progressBar);
    }

    private void setupRecyclerView() {
        labsAdapter = new LabsAdapter(requireContext());
        labsAdapter.setOnLabBookClickListener(this);

        recyclerLabs.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerLabs.setAdapter(labsAdapter);
    }

    private void setupSwipeRefresh() {
        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(() -> {
                labViewModel.refreshLabs();
            });
        }
    }

    private void setupObservers() {
        // Observe labs data
        labViewModel.getLabs().observe(getViewLifecycleOwner(), this::handleLabsUpdate);

        // Observe loading state
        labViewModel.getIsLoading().observe(getViewLifecycleOwner(), this::handleLoadingState);

        // Observe errors
        labViewModel.getError().observe(getViewLifecycleOwner(), this::handleError);

        // Observe search query changes
        labViewModel.getSearchQuery().observe(getViewLifecycleOwner(), query -> {
            if (searchView != null && !searchView.getQuery().toString().equals(query)) {
                searchView.setQuery(query, false);
            }
        });
    }

    private void handleLabsUpdate(List<Lab> labs) {
        if (labs != null) {
            labsAdapter.updateLabs(labs);
            updateEmptyState(labs.isEmpty());
        }
        hideLoading();
    }

    private void handleLoadingState(Boolean isLoading) {
        if (isLoading != null && isLoading) {
            showLoading();
        } else {
            hideLoading();
        }
    }

    private void handleError(String error) {
        if (error != null && !error.isEmpty()) {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            showEmptyMessage("Failed to load labs. Please try again.");
        }
        hideLoading();
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

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            String message = getEmptyStateMessage();
            showEmptyMessage(message);
        } else {
            showEmptyMessage(null);
        }
    }

    private String getEmptyStateMessage() {
        // Customize message based on current state
        if (labViewModel.hasActiveFilters()) {
            return "No labs found matching your criteria";
        } else {
            return "No active labs available at the moment";
        }
    }

    private void showEmptyMessage(String message) {
        if (emptyStateLayout != null) {
            if (message != null && !message.isEmpty()) {
                emptyStateMessage.setText(message);
                emptyStateLayout.setVisibility(View.VISIBLE);
                recyclerLabs.setVisibility(View.GONE);
            } else {
                emptyStateLayout.setVisibility(View.GONE);
                recyclerLabs.setVisibility(View.VISIBLE);
            }
        }
    }

    // Public method for refreshing data (called from Dashboard)
    public void refreshData() {
        labViewModel.refreshLabs();
    }

    // Set current user for role-based lab filtering
    public void setCurrentUser(User user) {
        labViewModel.setCurrentUser(user);
        // Reload labs with user context
        labViewModel.loadLabs();
    }

    // LabsAdapter.OnLabBookClickListener implementation
    @Override
    public void onBookLab(Lab lab) {
        if (lab == null) {
            Toast.makeText(requireContext(), "Error: Lab information not available", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!lab.isBookingAllowed()) {
            Toast.makeText(requireContext(), lab.getStatusText(), Toast.LENGTH_SHORT).show();
            return;
        }

        // Start booking activity with lab details
        Intent intent = new Intent(requireContext(), BookingActivity.class);
        intent.putExtra("lab_id", lab.getId());
        intent.putExtra("lab_name", lab.getName());
        intent.putExtra("lab_capacity", lab.getCapacity());
        intent.putExtra("lab_location", lab.getLocation());
        startActivity(intent);
    }

    // Menu handling for search
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_labs_menu, menu);

        searchMenuItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) searchMenuItem.getActionView();

        if (searchView != null) {
            setupSearchView();
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    private void setupSearchView() {
        searchView.setQueryHint("Search labs...");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                labViewModel.searchLabs(query);
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Search as user types (with small delay to avoid too many queries)
                if (newText.length() == 0) {
                    labViewModel.clearFilters();
                } else if (newText.length() >= 2) {
                    labViewModel.searchLabs(newText);
                }
                return true;
            }
        });

        // Handle search view close
        searchView.setOnCloseListener(() -> {
            labViewModel.clearFilters();
            return false;
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            refreshData();
            return true;
        } else if (id == R.id.action_clear_filters) {
            labViewModel.clearFilters();
            if (searchView != null) {
                searchView.setQuery("", false);
                searchView.clearFocus();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up references
        recyclerLabs = null;
        labsAdapter = null;
        swipeRefresh = null;
        emptyStateLayout = null;
        emptyStateMessage = null;
        progressBar = null;
        searchView = null;
        searchMenuItem = null;
    }
}