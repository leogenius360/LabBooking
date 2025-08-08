package com.testlab.labbooking.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.testlab.labbooking.models.Lab;
import com.testlab.labbooking.repositories.LabRepository;

import java.util.List;

public class LabViewModel extends ViewModel {
    private LabRepository labRepository;
    
    public LabViewModel() {
        labRepository = LabRepository.getInstance();
    }
    
    public LiveData<List<Lab>> getLabs() {
        return labRepository.getLabsLiveData();
    }
    
    public LiveData<String> getError() {
        return labRepository.getErrorLiveData();
    }
    
    public void loadLabs() {
        labRepository.loadActiveLabs();
    }
}