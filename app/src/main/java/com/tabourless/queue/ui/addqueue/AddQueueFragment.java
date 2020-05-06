package com.tabourless.queue.ui.addqueue;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.tabourless.queue.R;
import com.tabourless.queue.databinding.FragmentAddQueueBinding;
import com.tabourless.queue.ui.completeprofile.CompleteProfileFragmentArgs;


/**
 * A simple {@link Fragment} subclass.
 */
public class AddQueueFragment extends Fragment {
    private final static String TAG = AddQueueFragment.class.getSimpleName();
    private FirebaseUser mFirebaseCurrentUser;
    private AddQueueViewModel mViewModel;
    private FragmentAddQueueBinding mBinding;
    private NavController navController;
    private String currentUserId;
    private LatLng point;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Get current logged in user
        mFirebaseCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = mFirebaseCurrentUser!= null ? mFirebaseCurrentUser.getUid() : null;

        if(getArguments() != null && getArguments().containsKey("point")) {
            // Check if should display Edit profile
            point = AddQueueFragmentArgs.fromBundle(getArguments()).getPoint();
            Log.d(TAG, "point: "+point);
        }

        mViewModel = new ViewModelProvider(this).get(AddQueueViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mBinding = FragmentAddQueueBinding.inflate(inflater, container, false);
        View view = mBinding.getRoot();

        navController = NavHostFragment.findNavController(this);

        return view;
    }
}
