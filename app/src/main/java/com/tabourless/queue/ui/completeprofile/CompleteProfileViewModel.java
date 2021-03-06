package com.tabourless.queue.ui.completeprofile;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.tabourless.queue.data.UserRepository;
import com.tabourless.queue.interfaces.FirebaseUserCallback;
import com.tabourless.queue.models.User;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

public class CompleteProfileViewModel extends ViewModel {

    private MutableLiveData<String> mText;
    private ArrayList<Integer> mBirthYears;
    private UserRepository mUserRepository;
    private User mUser;
    private boolean isSelectAvatarClicked;
    private final static String TAG = CompleteProfileViewModel.class.getSimpleName();


    public CompleteProfileViewModel() {
        Log.d(TAG, "CompleteProfileViewModel init");
        //mText = new MutableLiveData<>();
        mBirthYears = new ArrayList<Integer>();
        mUserRepository = new UserRepository();
    }

    public boolean isSelectAvatarClicked() {
        return isSelectAvatarClicked;
    }

    public void setSelectAvatarClicked(boolean avatar) {
        isSelectAvatarClicked = avatar;
    }

    public ArrayList<Integer> getYears() {
        // get this year
        int thisYear = Calendar.getInstance().get(Calendar.YEAR);

        // Add years from 1900 till now to the spinner adapter
        for (int i = 1900; i <= thisYear; i++) {
            mBirthYears.add(i);
        }
        Collections.reverse(mBirthYears); // reverse year to start from 2020 first
        return mBirthYears;
    }

    public User getUser() {
        return mUser;
    }

    public void setUser(User user) {
        this.mUser = user;
    }

    public void getUserOnce(String userId, FirebaseUserCallback callback) {
        if(mUser == null){
            mUserRepository.getUserOnce(userId, callback);
        }else{
            callback.onCallback(mUser);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "CompleteProfileViewModel onCleared:");
        // Remove all Listeners from relationRepository
        mUserRepository.removeListeners();
    }
}