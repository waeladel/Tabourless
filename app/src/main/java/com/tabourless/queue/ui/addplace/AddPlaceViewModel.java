package com.tabourless.queue.ui.addplace;

import android.util.Log;

import androidx.lifecycle.ViewModel;

import com.tabourless.queue.data.AddPlaceRepository;
import com.tabourless.queue.models.Place;

public class AddPlaceViewModel extends ViewModel {

    private final static String TAG = AddPlaceViewModel.class.getSimpleName();

    private Place mPlace;
    private AddPlaceRepository mAddPlaceRepository;

    public AddPlaceViewModel() {

        mAddPlaceRepository = new AddPlaceRepository();
        mPlace = new Place();
   }

    public Place getPlace() {
        return mPlace;
    }

    public void setPlace(Place mPlace) {
        this.mPlace = mPlace;
    }

    // To add saved place to database
    public void addPlace(){
        mAddPlaceRepository.addPlace(mPlace);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "mama EditProfileViewModel onCleared:");
        // Remove all Listeners from relationRepository
        mAddPlaceRepository.removeListeners();
    }

}