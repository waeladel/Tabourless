package com.tabourless.queue.ui.search;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tabourless.queue.R;
import com.tabourless.queue.databinding.FragmentSearchBinding;


/**
 * A simple {@link Fragment} subclass.
 */
public class SearchFragment extends Fragment {

    private SearchViewModel mViewModel;
    private FragmentSearchBinding mBinding;
    private NavController navController;

    public SearchFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mViewModel = new ViewModelProvider(this).get(SearchViewModel.class);

        mBinding = FragmentSearchBinding.inflate(inflater, container, false);
        View view = mBinding.getRoot();

        navController = NavHostFragment.findNavController(this);

        return view;
    }
}
