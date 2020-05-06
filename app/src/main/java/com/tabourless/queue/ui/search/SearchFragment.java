package com.tabourless.queue.ui.search;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.tabourless.queue.R;
import com.tabourless.queue.databinding.FragmentSearchBinding;
import com.tabourless.queue.ui.DeniedPermissionAlertFragment;
import com.tabourless.queue.ui.ExplainPermissionAlertFragment;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;


/**
 * A simple {@link Fragment} subclass.
 */
public class SearchFragment extends Fragment implements OnMapReadyCallback
        , GoogleMap.OnMapLongClickListener
        , GoogleMap.OnMapClickListener
        , GoogleMap.OnInfoWindowClickListener {
    
    private final static String TAG = SearchFragment.class.getSimpleName();

    private SearchViewModel mViewModel;
    private FragmentSearchBinding mBinding;
    private NavController navController;
    private Context mContext;
    private Activity mActivity;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationAvailability mLocationAvailability;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private boolean isLocationUpdatesRequested;

    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 7000;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 7001;
    private static final int REQUEST_CHECK_SETTINGS = 51;
    public static final int EXPLAIN_READ_EXTERNAL_STORAGE = 7;
    public static final int PERMISSION_NOT_GRANTED = 8;
    private static final int EXPLAIN_LOCATION_PERMISSION = 9;
    private  static final String EXPLAIN_PERMISSION_ALERT_FRAGMENT = "ExplainPermissionFragment"; // Tag for confirm block alert fragment
    private  static final String DENIED_PERMISSION_ALERT_FRAGMENT = "DeniedPermissionFragment"; // Tag for confirm block alert fragment

    private static final int UPDATE_INTERVAL = 5 * 60 * 1000;  /* 10 secs */
    private static final int FASTEST_INTERVAL  = 2 * 60 * 1000;
    private static final int MAX_WAIT_TIME  = 20 * 60 * 1000;
    private static final long EXPIRATION_DURATION = 30 * 60 * 1000L;
    private static final int DISPLACEMENT = 10;


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;

        if (context instanceof Activity){// check if fragmentContext is an activity
            mActivity =(Activity) context;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: ");
        mViewModel = new ViewModelProvider(this).get(SearchViewModel.class);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                /*for (Location location : locationResult.getLocations()) {

                }*/
                mViewModel.setCurrentLocation(locationResult.getLastLocation());
                mViewModel.moveToLocation(mViewModel.getCurrentLocation());
            }
        };

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mBinding = FragmentSearchBinding.inflate(inflater, container, false);
        View view = mBinding.getRoot();

        // To get notified when the map is ready to be used.
        mBinding.map.onCreate(savedInstanceState);

        navController = NavHostFragment.findNavController(this);

        Log.d(TAG, "onCreateView: ");

        // change icon of add place FAB
        if(null != mViewModel.getAddPlaceMarker()){
            mBinding.addQueueButton.setImageResource(R.drawable.ic_select_this_location_24px);
        }else{
            mBinding.addQueueButton.setImageResource(R.drawable.ic_add_location_24px);
        }
        // Add new queue
        mBinding.addQueueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // change button icon to save place
                mBinding.addQueueButton.setImageResource(R.drawable.ic_select_this_location_24px);

                // if there are no markers add marker
                if(null == mViewModel.getAddPlaceMarker()){
                    LatLng currentLocationLatLng = new LatLng(mViewModel.getCurrentLocation().getLatitude(), mViewModel.getCurrentLocation().getLongitude());
                    addPlaceMarker(currentLocationLatLng);
                }else{
                    // if there is a place marker go to save place
                    goToAddQueue(mViewModel.getAddPlaceMarker().getPosition());
                }

            }
        });

        return view;
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume: mFusedLocationClient = "+mFusedLocationClient);
        super.onResume();
        mBinding.map.onResume();
        //isGooglePlayServicesAvailable();

        initMapDelayed();

        if (isLocationUpdatesRequested){
            // location requests again because we stopped it onPause.
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper());
        }
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause: ");
        super.onPause();
        mBinding.map.onPause();
        if(mFusedLocationClient != null){
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mBinding.map != null) {
            mBinding.map.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mBinding.map != null) {
            mBinding.map.onLowMemory();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBinding.map != null) {
            mBinding.map.onDestroy();
        }
    }

    /**
     * Manipulates the map when it's available.
     * The API invokes this callback when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user receives a prompt to install
     * Play services inside the SupportMapFragment. The API invokes this method after the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady: = "+ googleMap);
        if(googleMap != null){
            mViewModel.setMap(googleMap);
            if(checkPermissions() && isGooglePlayServicesAvailable()) {
                mViewModel.getMap().setMyLocationEnabled(true);
                mViewModel.getMap().getUiSettings().setMyLocationButtonEnabled(true);
                mViewModel.getMap().setIndoorEnabled(true);

                //mMap.getUiSettings().setMapToolbarEnabled(true);
                //mMap.getUiSettings().setZoomControlsEnabled(true);

                //startLocationUpdates();
                // A listener to add marker on user's long click
                mViewModel.getMap().setOnMapLongClickListener(this);
                mViewModel.getMap().setOnMapClickListener(this);
                mViewModel.getMap().setOnInfoWindowClickListener(this);

                // add previous place marker
                if(null != mViewModel.getAddPlaceMarker()){
                    addPlaceMarker(mViewModel.getAddPlaceMarker().getPosition());
                }

                // only move camera to current location if we don't have current location yet
                if(null == mViewModel.getCurrentLocation()){
                    getLastLocation();
                }

            }
        }
    }

    // Add new queue when long click
    @Override
    public void onMapLongClick(LatLng latLng) {
        addPlaceMarker(latLng);
    }

    // get queue id when click on a place
    @Override
    public void onMapClick(LatLng latLng) {

    }

    // Go to add queue when user click marker info
    @Override
    public void onInfoWindowClick(Marker marker) {
        goToAddQueue(marker.getPosition());
    }

    private void addPlaceMarker(LatLng latLng) {
        if(mViewModel.getMap() != null){

            if (null != mViewModel.getAddPlaceMarker()){
                mViewModel.getAddPlaceMarker().remove();// remove existing marker
            }

            // Create new marker
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng)
                    .title(getString(R.string.add_marker_title))
                    //.icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_front_car_black))
                    .draggable(true)
                    .snippet(getString(R.string.add_marker_snippet));

            mViewModel.setAddPlaceMarker(mViewModel.getMap().addMarker(markerOptions));

            mViewModel.moveToLatLng(latLng);
        }
    }

    private void goToAddQueue(LatLng latLng) {
    }

    private boolean checkPermissions() {
        if (ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_COARSE_LOCATION)== PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            requestPermissions();
            return false;
        }
    }

    private void requestPermissions() {

        // Permission has not been granted and must be requested.
        if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity,
                android.Manifest.permission.ACCESS_COARSE_LOCATION ) || ActivityCompat.shouldShowRequestPermissionRationale(mActivity,
                android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            showExplainDialog();

        } else {
            // No explanation needed; request the permission
            // Request the permission. The result will be received in onRequestPermissionResult().
            ActivityCompat.requestPermissions(mActivity,
                    new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION,
                            android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

    }

    private void showExplainDialog() {
        ExplainPermissionAlertFragment explainFragment = ExplainPermissionAlertFragment.newInstance(mActivity);
        if(getParentFragmentManager() != null) {
            explainFragment.show(getParentFragmentManager(), EXPLAIN_PERMISSION_ALERT_FRAGMENT);
        }
    }

    private void showDeniedDialog() {
        DeniedPermissionAlertFragment deniedFragment = DeniedPermissionAlertFragment.newInstance(mActivity);
        if(getParentFragmentManager() != null) {
            deniedFragment.show(getParentFragmentManager(), DENIED_PERMISSION_ALERT_FRAGMENT);
        }
    }

    private boolean isGooglePlayServicesAvailable() {
        // Check that Google Play services is available
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int resultCode = googleAPI.isGooglePlayServicesAvailable(mContext);
        // If Google Play services is not available
        if(resultCode != ConnectionResult.SUCCESS) {
            if(googleAPI.isUserResolvableError(resultCode)) {
                googleAPI.getErrorDialog(mActivity, resultCode,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            }
            return false;
        }
        Log.d(TAG, "Google Play services is available.");
        return true;
    }

    public void initMapDelayed() {
        //runner.postOnMainHandlerDelayed();
        final Handler handler = new Handler();
        final Runnable r = new Runnable() {
            public void run() {
                initMap();
            }
        };

        handler.postDelayed(r, 500);
    }

    private void initMap() {
        mBinding.map.getMapAsync(this);
    }

    // get last location once
    private void getLastLocation() {

        Log.d(TAG, "getLastLocation is on");
        if(checkPermissions() && isGooglePlayServicesAvailable()){
            mFusedLocationClient = getFusedLocationProviderClient(mActivity);

            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(mActivity, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(final Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                mFusedLocationClient.getLocationAvailability().addOnSuccessListener(new OnSuccessListener<LocationAvailability>() {
                                    @Override
                                    public void onSuccess(LocationAvailability locationAvailability) {
                                        mLocationAvailability = locationAvailability;
                                        // check if last location is up to date
                                        if(mLocationAvailability != null && mLocationAvailability.isLocationAvailable()){
                                            Log.d(TAG, "location = "+location+ "isLocationAvailable = "+mLocationAvailability.isLocationAvailable());

                                            // Logic to handle location object
                                            mViewModel.setCurrentLocation(location);
                                            // Move camera to location
                                            mViewModel.moveToLocation(mViewModel.getCurrentLocation());
                                        }else{
                                            getLocationUpdates();
                                        }
                                    }
                                });

                            }else{
                                getLocationUpdates();
                            }
                        }
                    });

        }
    }

    private void getLocationUpdates() {
        if(checkPermissions() && isGooglePlayServicesAvailable()) {
            // Create the location request to start receiving updates
            mLocationRequest = new LocationRequest();
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            mLocationRequest.setInterval(UPDATE_INTERVAL);
            mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
            mLocationRequest.setMaxWaitTime(MAX_WAIT_TIME);
            mLocationRequest.setExpirationDuration(EXPIRATION_DURATION);
            mLocationRequest.setSmallestDisplacement(DISPLACEMENT);

            // Create LocationSettingsRequest object using location request
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
            builder.addLocationRequest(mLocationRequest);

            SettingsClient client = LocationServices.getSettingsClient(mActivity);
            Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

            task.addOnSuccessListener(mActivity, new OnSuccessListener<LocationSettingsResponse>() {
                @Override
                public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                    // All location settings are satisfied. The client can initialize
                    // location requests here.
                    mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper());
                    isLocationUpdatesRequested = true;
                }
            }).addOnFailureListener(mActivity, new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    if (e instanceof ResolvableApiException) {
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            ResolvableApiException resolvable = (ResolvableApiException) e;
                            resolvable.startResolutionForResult(mActivity,
                                    REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException sendEx) {
                            // Ignore the error.
                        }
                    }
                }
            });


        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                Log.d(TAG, "onRequestPermissionsResult: grantResults.length= "+grantResults.length
                        +"grantResults= "+ grantResults[0]);
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // init task you need to do.
                    if(isGooglePlayServicesAvailable()) {
                        getLastLocation();
                        //startLocationUpdates();
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    showDeniedDialog();
                    //Toast.makeText(PostActivity.this, R.string.permission_denied, Toast.LENGTH_LONG).show();
                }
                break;
            }

        }
    }

    // listen if user enabled location or kept it desabled
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_SETTINGS){
            if (resultCode ==  Activity.RESULT_OK){
                // User enabled location, lets get his location again
                getLastLocation();
            }
        }
    }

}
