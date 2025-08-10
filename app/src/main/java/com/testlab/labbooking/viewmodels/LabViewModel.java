package com.testlab.labbooking.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.testlab.labbooking.models.Lab;
import com.testlab.labbooking.models.User;
import com.testlab.labbooking.repositories.LabRepository;

import java.util.List;

public class LabViewModel extends ViewModel {

    private LabRepository repository;

    // LiveData from repository
    private LiveData<List<Lab>> labs;
    private LiveData<String> error;
    private LiveData<Boolean> isLoading;

    // Additional state for filtering
    private MutableLiveData<String> searchQuery;
    private MutableLiveData<String> selectedCategory;
    private User currentUser;

    public LabViewModel() {
        repository = LabRepository.getInstance();

        // Get LiveData from repository
        labs = repository.getLabsLiveData();
        error = repository.getErrorLiveData();
        isLoading = repository.getLoadingLiveData();

        // Initialize search and filter state
        searchQuery = new MutableLiveData<>("");
        selectedCategory = new MutableLiveData<>(null);
    }

    // Getters for LiveData
    public LiveData<List<Lab>> getLabs() {
        return labs;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getSearchQuery() {
        return searchQuery;
    }

    public LiveData<String> getSelectedCategory() {
        return selectedCategory;
    }

    // Set current user for role-based lab filtering
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    // Load operations
    public void loadLabs() {
        repository.loadActiveLabs(currentUser);
    }

    public void refreshLabs() {
        repository.refreshLabs();
    }

    // Search functionality
    public void searchLabs(String query) {
        searchQuery.setValue(query);

        if (query == null || query.trim().isEmpty()) {
            loadLabs(); // Load all labs if search is empty
        } else {
            repository.searchLabs(query);
        }
    }

    // Category filtering
    public void filterByCategory(String category) {
        selectedCategory.setValue(category);

        if (category == null || category.trim().isEmpty()) {
            loadLabs(); // Load all labs if no category selected
        } else {
            repository.loadLabsByCategory(category);
        }
    }

    // Clear filters and search
    public void clearFilters() {
        searchQuery.setValue("");
        selectedCategory.setValue(null);
        loadLabs();
    }

    // Get available categories for filtering UI
    public void getLabCategories(LabRepository.CategoryCallback callback) {
        repository.getLabCategories(callback);
    }

    // Get single lab details
    public void getLabById(String labId, LabRepository.SingleLabCallback callback) {
        repository.getLabById(labId, callback);
    }

    // Check if any filters are active
    public boolean hasActiveFilters() {
        String query = searchQuery.getValue();
        String category = selectedCategory.getValue();

        return (query != null && !query.trim().isEmpty()) ||
                (category != null && !category.trim().isEmpty());
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Clean up repository resources
        repository.cleanup();
    }
}