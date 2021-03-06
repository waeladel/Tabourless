package com.tabourless.queue.ui.search;

import android.location.Location;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.material.chip.Chip;
import com.tabourless.queue.data.SearchRepository;
import com.tabourless.queue.interfaces.FirebaseOnCompleteCallback;
import com.tabourless.queue.interfaces.FirebaseUserCallback;
import com.tabourless.queue.interfaces.FirebaseUserQueueCallback;
import com.tabourless.queue.models.Customer;
import com.tabourless.queue.models.Place;
import com.tabourless.queue.models.PlaceMarker;
import com.tabourless.queue.models.Queue;
import com.tabourless.queue.models.UserQueue;

import java.util.HashMap;
import java.util.Map;

public class SearchViewModel extends ViewModel {

    private final static String TAG = SearchViewModel.class.getSimpleName();
    private GoogleMap mMap;
    private Location mCurrentLocation;
    private Marker mAddPlaceMarker;
    private SearchRepository mSearchRepository;
    private static final long ZOOM_LEVEL = 14;
    private LiveData<Map<String, Place>> mNearbyPlace;
    //public Map<String, Marker> displayedMarkers;
    //public Map<Marker, Place> displayedPlaces;
    public Map<String, PlaceMarker> placesMarkersMap;
    public Map<Integer, UserQueue> chipsQueuesMap;
    private CameraPosition mCameraPosition; // To save camera position to reuse it when clicking back arrow or rotate device

    public SearchViewModel() {
        mSearchRepository = new SearchRepository();
        //displayedMarkers = new HashMap<>();
        //displayedPlaces = new HashMap<>();
        placesMarkersMap = new HashMap<>();
        chipsQueuesMap  = new HashMap<>();
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

    public Marker getAddPlaceMarker() {
        return mAddPlaceMarker;
    }

    public void setAddPlaceMarker(Marker marker) {
        this.mAddPlaceMarker = marker;
    }

    // Add a user to the queue when clicking the book button
    public void addCurrentCustomer(UserQueue selectedQueue, Customer customer, FirebaseOnCompleteCallback callback) {
        mSearchRepository.addCurrentCustomer(selectedQueue, customer, callback);
    }
    // Remove the user from the queue instead of adding him//her when clicking on unbook button
    public void removeCurrentCustomer(UserQueue selectedQueue, FirebaseOnCompleteCallback callback) {
        mSearchRepository.removeCurrentCustomer(selectedQueue, callback);
    }

    public void moveToLocation(Location location, boolean animate) {
        LatLng latLang = new LatLng(location.getLatitude(), location.getLongitude());
        //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLang,14.0f));
        if(animate){
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLang, ZOOM_LEVEL));
        }else{
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLang, ZOOM_LEVEL));
        }
    }

    public void moveToLatLng(LatLng latLng, boolean animate) {
        if(animate){
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, ZOOM_LEVEL));
        }else{
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, ZOOM_LEVEL));
        }
    }

    public CameraPosition getCameraPosition(){
        return mCameraPosition;
    }

    public void setCameraPosition(CameraPosition cameraPosition) {
        this.mCameraPosition = cameraPosition;
    }

    public LiveData<Map<String, Place>> getNearbyPlaces (LatLng point) {
        mNearbyPlace = mSearchRepository.getNearbyPlaces(point);
        return mNearbyPlace;
    }

    public void getUserOnce(String userId, FirebaseUserCallback callback) {
        mSearchRepository.getUserOnce(userId, callback);
    }

    public void getUserQueueOnce(String userId, String queueId, FirebaseUserQueueCallback callback) {
        mSearchRepository.getUserQueueOnce(userId, queueId, callback);
    }


    @Override
    protected void onCleared() {
        Log.d(TAG, "SearchViewModel onCleared:");
        mSearchRepository.removeListeners();
        super.onCleared();
    }

}