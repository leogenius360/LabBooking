package com.testlab.labbooking.repositories;

import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.testlab.labbooking.models.Lab;
import com.testlab.labbooking.utils.DatabaseUtils;

import java.util.ArrayList;
import java.util.List;

public class LabRepository {
    private static LabRepository instance;
    private MutableLiveData<List<Lab>> labsLiveData;
    private MutableLiveData<String> errorLiveData;

    private LabRepository() {
        labsLiveData = new MutableLiveData<>();
        errorLiveData = new MutableLiveData<>();
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

    public void loadActiveLabs() {
        DatabaseUtils.getInstance()
                .collection(DatabaseUtils.LABS_COLLECTION)
                .whereEqualTo("isActive", true)
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null) {
                        errorLiveData.setValue("Error loading labs: " + error.getMessage());
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        List<Lab> labs = new ArrayList<>();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Lab lab = document.toObject(Lab.class);
                            lab.setId(document.getId());
                            labs.add(lab);
                        }
                        labsLiveData.setValue(labs);
                    }
                });
    }
}