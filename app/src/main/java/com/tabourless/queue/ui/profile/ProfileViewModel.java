package com.tabourless.queue.ui.profile;

import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.tabourless.queue.data.RelationRepository;
import com.tabourless.queue.data.UserRepository;
import com.tabourless.queue.interfaces.FirebaseUserCallback;
import com.tabourless.queue.models.Relation;
import com.tabourless.queue.models.User;

public class ProfileViewModel extends ViewModel {

    private final static String TAG = ProfileViewModel.class.getSimpleName();

    private RelationRepository relationRepository;
    private UserRepository mUserRepository;
    private LiveData<Relation> mRelation;
    private LiveData<User> mUser;

    public ProfileViewModel() {
        Log.d(TAG, "ProfileViewModel init");
        relationRepository = new RelationRepository();
        mUserRepository = new UserRepository();
    }

    public LiveData<Relation> getRelation(String currentUserId , String userId){
        if(mRelation == null){
            Log.d(TAG, "mRelation is null, get relation from database");
            mRelation = relationRepository.getRelation(currentUserId, userId);
        }else{
            Log.d(TAG, "mRelation already exist");
        }
        return mRelation;
    }

    public void blockUser(String currentUserId, String userId, String relation, boolean isDeleteChat) {
        relationRepository.blockUser(currentUserId, userId, relation, isDeleteChat);
    }

    public void unblockUser(String currentUserId, String userId, String relationStatus) {
        relationRepository.unblockUser(currentUserId, userId, relationStatus);
    }

    public LiveData<User> getUser(String currentUserId){
        if(mUser == null){
            Log.d(TAG, "mUser is null, get relation from database");
            mUser =  mUserRepository.getCurrentUser(currentUserId);
        }
        return mUser;
    }

    public void getUserOnce(String userId, FirebaseUserCallback callback) {
        mUserRepository.getUserOnce(userId, callback);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "mama ProfileViewModel onCleared:");
        // Remove all Listeners from relationRepository
        relationRepository.removeListeners();
        mUserRepository.removeListeners();
    }

}