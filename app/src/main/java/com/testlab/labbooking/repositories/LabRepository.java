package com.testlab.labbooking.repositories;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.testlab.labbooking.models.Lab;
import com.testlab.labbooking.models.User;
import com.testlab.labbooking.utils.DatabaseUtils;

import java.util.ArrayList;
import java.util.List;

public class LabRepository {
    private static final String TAG = "LabRepository";
    private static LabRepository instance;

    private MutableLiveData<List<Lab>> labsLiveData;
    private MutableLiveData<String> errorLiveData;
    private MutableLiveData<Boolean> loadingLiveData;

    private ListenerRegistration labsListenerRegistration;

    private LabRepository() {
        labsLiveData = new MutableLiveData<>();
        errorLiveData = new MutableLiveData<>();
        loadingLiveData = new MutableLiveData<>();
    }

    public static LabRepository getInstance() {
        if (instance == null) {
            instance = new LabRepository();
        }
        return instance;
    }

    public MutableLiveData<List<Lab>> getLabsLiveData() {
        return labsLiveData;
    }

    public MutableLiveData<String> getErrorLiveData() {
        return errorLiveData;
    }

    public MutableLiveData<Boolean> getLoadingLiveData() {
        return loadingLiveData;
    }

    /**
     * Load active labs using DatabaseUtils query
     */
    public void loadActiveLabs() {
        loadActiveLabs(null);
    }

    /**
     * Load active labs for specific user type
     */
    public void loadActiveLabs(User currentUser) {
        Log.d(TAG, "Loading active labs" + (currentUser != null ? " for user type: " + currentUser.getRole() : ""));

        setLoading(true);

        // Detach previous listener if exists
        detachListener();

        // Use appropriate query based on user
        if (currentUser != null && currentUser.getRole() != null) {
            // Load labs available for user's role
            labsListenerRegistration = DatabaseUtils.getAvailableLabsForUserQuery(currentUser.getRole())
                    .addSnapshotListener((queryDocumentSnapshots, error) -> {
                        handleLabsSnapshot(queryDocumentSnapshots, error);
                    });
        } else {
            // Load all active labs
            labsListenerRegistration = DatabaseUtils.getActiveLabsQuery()
                    .addSnapshotListener((queryDocumentSnapshots, error) -> {
                        handleLabsSnapshot(queryDocumentSnapshots, error);
                    });
        }
    }

    /**
     * Load labs by category
     */
    public void loadLabsByCategory(String category) {
        Log.d(TAG, "Loading labs by category: " + category);

        setLoading(true);
        detachListener();

        labsListenerRegistration = DatabaseUtils.getLabsByCategoryQuery(category)
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    handleLabsSnapshot(queryDocumentSnapshots, error);
                });
    }

    /**
     * Search labs by name or description
     */
    public void searchLabs(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            loadActiveLabs();
            return;
        }

        Log.d(TAG, "Searching labs with term: " + searchTerm);

        setLoading(true);
        detachListener();

        labsListenerRegistration = DatabaseUtils.searchLabsQuery(searchTerm.trim())
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    handleLabsSnapshot(queryDocumentSnapshots, error);
                });
    }

    /**
     * Refresh labs data
     */
    public void refreshLabs() {
        Log.d(TAG, "Refreshing labs data");

        // Clear error state
        errorLiveData.setValue(null);

        // Reload active labs
        loadActiveLabs();
    }

    /**
     * Handle Firestore snapshot results
     */
    private void handleLabsSnapshot(com.google.firebase.firestore.QuerySnapshot queryDocumentSnapshots,
                                    com.google.firebase.firestore.FirebaseFirestoreException error) {
        setLoading(false);

        if (error != null) {
            String errorMessage = DatabaseUtils.getFormattedErrorMessage(error);
            Log.e(TAG, "Error loading labs: " + errorMessage, error);
            errorLiveData.setValue(errorMessage);
            labsLiveData.setValue(new ArrayList<>()); // Set empty list on error
            return;
        }

        if (queryDocumentSnapshots != null) {
            List<Lab> labs = new ArrayList<>();

            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                try {
                    Lab lab = document.toObject(Lab.class);
                    lab.setId(document.getId());

                    // Only add valid labs that are booking allowed
                    if (lab.isValid() && lab.isBookingAllowed()) {
                        labs.add(lab);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing lab document: " + document.getId(), e);
                }
            }

            Log.d(TAG, "Loaded " + labs.size() + " active labs");
            labsLiveData.setValue(labs);

            // Clear any previous errors
            errorLiveData.setValue(null);
        } else {
            Log.d(TAG, "No labs found");
            labsLiveData.setValue(new ArrayList<>());
        }
    }

    /**
     * Set loading state
     */
    private void setLoading(boolean isLoading) {
        loadingLiveData.setValue(isLoading);
    }

    /**
     * Detach Firestore listener to prevent memory leaks
     */
    private void detachListener() {
        if (labsListenerRegistration != null) {
            labsListenerRegistration.remove();
            labsListenerRegistration = null;
        }
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        Log.d(TAG, "Cleaning up LabRepository");
        detachListener();
    }

    /**
     * Get available categories for filtering
     */
    public void getLabCategories(CategoryCallback callback) {
        DatabaseUtils.getAllLabsQuery()
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> categories = new ArrayList<>();

                    if (queryDocumentSnapshots != null) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Lab lab = document.toObject(Lab.class);
                            String category = lab.getCategory();
                            if (category != null && !categories.contains(category)) {
                                categories.add(category);
                            }
                        }
                    }

                    callback.onCategoriesReceived(categories);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading categories", e);
                    callback.onError(DatabaseUtils.getFormattedErrorMessage(e));
                });
    }

    /**
     * Get single lab by ID
     */
    public void getLabById(String labId, SingleLabCallback callback) {
        if (labId == null || labId.trim().isEmpty()) {
            callback.onError("Invalid lab ID");
            return;
        }

        DatabaseUtils.getLabById(labId)
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Lab lab = documentSnapshot.toObject(Lab.class);
                        if (lab != null) {
                            lab.setId(documentSnapshot.getId());
                            callback.onLabReceived(lab);
                        } else {
                            callback.onError("Error parsing lab data");
                        }
                    } else {
                        callback.onError("Lab not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading lab: " + labId, e);
                    callback.onError(DatabaseUtils.getFormattedErrorMessage(e));
                });
    }

    // Callback interfaces
    public interface CategoryCallback {
        void onCategoriesReceived(List<String> categories);
        void onError(String error);
    }

    public interface SingleLabCallback {
        void onLabReceived(Lab lab);
        void onError(String error);
    }
}