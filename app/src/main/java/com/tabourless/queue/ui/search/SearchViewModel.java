package com.tabourless.queue.ui.search;

import android.location.Location;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.tabourless.queue.databinding.FragmentSearchBinding;

public class SearchViewModel extends ViewModel {

    private final static String TAG = SearchViewModel.class.getSimpleName();
    private GoogleMap mMap;
    private Location mCurrentLocation;
    private static final long ZOOM_LEVEL = 14;

    public SearchViewModel() {
    }

    public void setMap (GoogleMap map) {
        Log.d(TAG, "mMap: "+mMap);
        mMap = map;
    }

    public void setCurrentLocation (Location location) {
        mCurrentLocation = location;
    }

    public Location getCurrentLocation () {
        return mCurrentLocation;
    }

    public GoogleMap getMap () {
       return mMap;
    }

    public void moveToLocation (Location location) {
        LatLng latLang = new LatLng(location.getLatitude(), location.getLongitude());
        //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLang,14.0f));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLang, ZOOM_LEVEL));
    }
}