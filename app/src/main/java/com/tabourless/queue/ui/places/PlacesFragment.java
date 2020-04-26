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
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;

import com.tabourless.queue.R;
import com.tabourless.queue.databinding.FragmentPlacesBinding;

public class PlacesFragment extends Fragment {

    private PlacesViewModel mViewModel;
    private FragmentPlacesBinding mBinding;
    private NavController navController;

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

        navController = NavHostFragment.findNavController(this);
        // just to test profiles
        mBinding.textHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                NavDirections direction = PlacesFragmentDirections.actionPlacesToProfile("uZUsaqEbfpTbFuO3mzcIeuiVqcx1");
                //check if we are on Main Fragment not on complete Profile already
                if (null != navController.getCurrentDestination() && R.id.places == navController.getCurrentDestination().getId()) {
                    //navController.navigate(R.id.complete_profile_fragment);
                    // Must use direction to get the benefits of pop stack
                    navController.navigate(direction);
                }
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
