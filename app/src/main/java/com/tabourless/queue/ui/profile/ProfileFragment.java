package com.tabourless.queue.ui.profile;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tabourless.queue.databinding.FragmentPlacesBinding;
import com.tabourless.queue.databinding.FragmentProfileBinding;

public class ProfileFragment extends Fragment {
    private ProfileViewModel mViewModel;
    private FragmentProfileBinding mBinding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        mViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        mBinding = FragmentProfileBinding.inflate(inflater, container, false);
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
