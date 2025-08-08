package com.testlab.labbooking.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.testlab.labbooking.R;
import com.testlab.labbooking.adapters.LabsAdapter;
import com.testlab.labbooking.viewmodels.LabViewModel;

public class LabsFragment extends Fragment {

    private RecyclerView recyclerLabs;
    private LabsAdapter labsAdapter;
    private LabViewModel labViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        labViewModel = new ViewModelProvider(this).get(LabViewModel.class);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerLabs = view.findViewById(R.id.recyclerLabs);
        setupRecyclerView();
        setupObservers();
        labViewModel.loadLabs();
    }

    private void setupRecyclerView() {
        labsAdapter = new LabsAdapter(requireContext());
        recyclerLabs.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerLabs.setAdapter(labsAdapter);
    }

    private void setupObservers() {
        labViewModel.getLabs().observe(getViewLifecycleOwner(), labs -> {
            labsAdapter.updateLabs(labs);
        });

        labViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
        });
    }
}