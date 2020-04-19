package com.tabourless.queue.ui.places;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.tabourless.queue.databinding.FragmentPlacesBinding;

public class PlacesFragment extends Fragment {

    private PlacesViewModel mViewModel;
    private FragmentPlacesBinding mBinding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        mViewModel = new ViewModelProvider(this).get(PlacesViewModel.class);

        mBinding = FragmentPlacesBinding.inflate(inflater, container, false);
        View view = mBinding.getRoot();

        mViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                mBinding.textHome.setText(s);
            }
        });
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }
}
