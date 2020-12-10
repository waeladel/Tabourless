package com.tabourless.queue.ui.search;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipDrawable;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.tabourless.queue.R;
import com.tabourless.queue.databinding.ChipItemBinding;
import com.tabourless.queue.databinding.FragmentSearchBinding;
import com.tabourless.queue.interfaces.FirebaseOnCompleteCallback;
import com.tabourless.queue.interfaces.FirebaseUserCallback;
import com.tabourless.queue.interfaces.FirebaseUserQueueCallback;
import com.tabourless.queue.models.Counter;
import com.tabourless.queue.models.Customer;
import com.tabourless.queue.models.Place;
import com.tabourless.queue.models.PlaceMarker;
import com.tabourless.queue.models.Queue;
import com.tabourless.queue.models.User;
import com.tabourless.queue.models.UserQueue;
import com.tabourless.queue.ui.DeniedPermissionAlertFragment;
import com.tabourless.queue.ui.ExplainPermissionAlertFragment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;
import static com.tabourless.queue.App.CUSTOMER_STATUS_WAITING;


/**
 * A simple {@link Fragment} subclass.
 */
public class SearchFragment extends Fragment implements OnMapReadyCallback
        , GoogleMap.OnMapLongClickListener
        , GoogleMap.OnMapClickListener
        , GoogleMap.OnInfoWindowClickListener, GoogleMap.OnCameraIdleListener, GoogleMap.OnMarkerClickListener {
    
    private final static String TAG = SearchFragment.class.getSimpleName();
    private FirebaseUser mFirebaseCurrentUser;
    private String mCurrentUserId;

    private SearchViewModel mViewModel;
    private FragmentSearchBinding mBinding;
    private ChipItemBinding mChipBinding;
    private NavController navController;
    private Context mContext;
    private Activity mActivity;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationAvailability mLocationAvailability;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private boolean isLocationUpdatesRequested;
    private String mSelectedPlaceKey; // to save selected place when marker is clicked

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
        //Get current logged in user
        mFirebaseCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mCurrentUserId = mFirebaseCurrentUser!= null ? mFirebaseCurrentUser.getUid() : null;

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
                mViewModel.moveToLocation(mViewModel.getCurrentLocation(), true);
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mBinding = FragmentSearchBinding.inflate(inflater, container, false);
        //mChipBinding = ChipItemBinding.inflate(inflater, container, false);

        View view = mBinding.getRoot();

        // Enable book button again as user may rotate the screen while the button is disabled due to booking or unbooking
        mBinding.bookButton.setEnabled(true);
        mBinding.bookButton.setClickable(true);

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
                    goToAddPlace(mViewModel.getAddPlaceMarker().getPosition());
                    Log.d(TAG, "marker getPosition: "+mViewModel.getAddPlaceMarker().getPosition());
                }

            }
        });

        // Listener for book button
        mBinding.bookButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // make sure that there a selected service
                if(mBinding.servicesChipGroup.getCheckedChipId() == -1){
                    Toast.makeText(mContext, R.string.select_service_error, Toast.LENGTH_SHORT).show();
                    return;
                }

                // Disable book button till we finish booking or unbooking so that user don't click it twice
                mBinding.bookButton.setEnabled(false);
                mBinding.bookButton.setClickable(false);
                // get selected service/queue
                Log.d(TAG, "onClick: CheckedChipId = "+ mBinding.servicesChipGroup.getCheckedChipId());
                //Chip selectedChip = mBinding.servicesChipGroup.getCheckedChipId();
                final UserQueue selectedQueue = mViewModel.chipsQueuesMap.get(mBinding.servicesChipGroup.getCheckedChipId());
                if(selectedQueue != null){
                    Log.d(TAG, "onClick: selectedQueue key= "+ selectedQueue.getKey() + " name= "+ selectedQueue.getName()+ " Place Id " + selectedQueue.getPlaceId());
                    // double check that user is not in this queue before
                    mViewModel.getUserQueueOnce(mCurrentUserId, selectedQueue.getKey(), new FirebaseUserQueueCallback() {
                        @Override
                        public void onCallback(UserQueue userQueue) {
                            if(userQueue != null){
                                if(userQueue.getJoinedLong() != 0){
                                    Log.d(TAG, "onCallback: current user already in the queue, lets unbook");
                                    // Remove the user from the queue instead of adding him//her when clicking on unbook button
                                    mViewModel.removeCurrentCustomer(userQueue, new FirebaseOnCompleteCallback() {
                                        @Override
                                        public void onCallback(@NonNull Task<Void> task) {
                                            // enable book button again as we finished the process
                                            mBinding.bookButton.setEnabled(true);
                                            mBinding.bookButton.setClickable(true);

                                            if(null != task && task.isSuccessful()){
                                                // Go to customers recycler
                                                Log.d(TAG, "FirebaseOnCompleteCallback onCallback: "+task.isSuccessful());
                                                mBinding.bookButton.setText(R.string.book_button_title);
                                            }else{
                                                Toast.makeText(mContext, R.string.unbook_queue_error, Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    });
                                }else{
                                    Log.d(TAG, "onCallback: current user quited the queue");
                                    bookQueue(selectedQueue);
                                }
                            }else{
                                Log.d(TAG, "onCallback: current user didn't join the queue");
                                bookQueue(selectedQueue);
                            }
                        }
                    });

                }
                // join customer to queue key
            }
        });

        mBinding.editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToEditPlace(mSelectedPlaceKey);
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
        if (null != mBinding && null != mBinding.map) {
            mBinding.map.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (null != mBinding &&  null != mBinding.map) {
            mBinding.map.onLowMemory();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != mBinding &&  null != mBinding.map) {
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
                mViewModel.getMap().getUiSettings().setMapToolbarEnabled(false); // to disable acees to Google Maps mobile app when click on a marker
                //mMap.getUiSettings().setZoomControlsEnabled(true);

                //startLocationUpdates();
                // A listener to add marker on user's long click
                mViewModel.getMap().setOnMapLongClickListener(this);
                mViewModel.getMap().setOnMapClickListener(this);
                mViewModel.getMap().setOnInfoWindowClickListener(this);
                mViewModel.getMap().setOnCameraIdleListener(this);
                mViewModel.getMap().setOnMarkerClickListener(this);

                // add previous add place marker
                if(null != mViewModel.getAddPlaceMarker()){
                    addPlaceMarker(mViewModel.getAddPlaceMarker().getPosition());
                }

                // add previous markers
                if(mViewModel.placesMarkersMap.size() != 0){
                    showMarkers();
                }

                // Always user cameraPosition instead of currentLocation if not null
                if(null == mViewModel.getCameraPosition()){
                    // only get location if we don't have user's current location yet
                    if(null == mViewModel.getCurrentLocation()){
                        getLastLocation();
                    }else{
                        // Just move to location without request it
                        mViewModel.moveToLocation(mViewModel.getCurrentLocation(), false);
                    }
                }else{
                    // User camera position instead of user's current location
                    mViewModel.moveToLatLng(mViewModel.getCameraPosition().target, false);
                }


            }
        }
    }

    // Add new queue when long click
    @Override
    public void onMapLongClick(LatLng latLng) {
        mBinding.placeInfo.setVisibility(View.INVISIBLE); // Hide place info
        mBinding.addQueueButton.setVisibility(View.VISIBLE); // Show add place button
        addPlaceMarker(latLng);
    }

    // get queue id when click on a place
    @Override
    public void onMapClick(LatLng latLng) {
        mBinding.placeInfo.setVisibility(View.INVISIBLE); // Hide place info
        mBinding.addQueueButton.setVisibility(View.VISIBLE); // Show add place button
    }

    // listen to camera idle to update nearby places
    @Override
    public void onCameraIdle() {
        Log.d(TAG, "onCameraIdle: ");
        // Save camera position
        mViewModel.setCameraPosition(mViewModel.getMap().getCameraPosition());
        // Load nearby places
        //mViewModel.getNearbyPlaces(mViewModel.getMap().getCameraPosition().target);
        mViewModel.getNearbyPlaces(mViewModel.getMap().getCameraPosition().target).observe(getViewLifecycleOwner(), new Observer<Map<String, Place>>() {
            @Override
            public void onChanged(Map<String, Place> placeMap) {
                if(placeMap != null){
                    // show a marker for the place
                    Log.d(TAG, "getNearbyPlaces onChanged: "+ placeMap.size());
                    // Loop throw all places
                    for (Object o : placeMap.entrySet()) {
                        Map.Entry pair = (Map.Entry) o;
                        Log.d(TAG, "onCameraIdle placeMap map key/val = " + pair.getKey() + " = " + pair.getValue());
                        Place place = placeMap.get(String.valueOf(pair.getKey()));
                        if (place != null) {
                            place.setKey(String.valueOf(pair.getKey()));
                            Log.d(TAG, "place name=" + place.getName());
                            // Create new marker
                            MarkerOptions markerOptions = new MarkerOptions();
                            markerOptions.position(new LatLng(place.getLatitude(), place.getLongitude()))
                                    .title(place.getName())
                                    .icon(BitmapDescriptorFactory.defaultMarker(13.0F))
                                    //.icon(BitmapDescriptorFactory.fromResource(R.mipmap.account_circle_72dp))
                                    .draggable(false);
                                    //.snippet(place.getQueues().size());
                            // Only add place's marker if it wasn't added before
                            if(null == mViewModel.placesMarkersMap.get(place.getKey())){
                                Marker marker = mViewModel.getMap().addMarker(markerOptions);
                                //mViewModel.displayedMarkers.put(place.getKey(), marker);
                                //mViewModel.displayedPlaces.put(marker, place);
                                PlaceMarker placeMarker = new PlaceMarker(place.getKey(), place, marker);
                                mViewModel.placesMarkersMap.put(place.getKey(), placeMarker);
                                Log.d(TAG, "onChanged: marker id= "+ marker.getId());
                            }
                        }
                    }
                }
            }
        });

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Log.d(TAG, "onMarkerClick: marker id= "+marker.getId());
        // Don't show place info if maker is add place marker
        if(null == mViewModel.getAddPlaceMarker() || !TextUtils.equals(marker.getId(), mViewModel.getAddPlaceMarker().getId())){
            //Loop throw all places markers map to get selected place
            for (Object o : mViewModel.placesMarkersMap.entrySet()) {
                Map.Entry pair = (Map.Entry) o;
                Log.d(TAG, "onMarkerClick displayedPlaces Map key/val = " + pair.getKey() + " = " + pair.getValue());
                PlaceMarker placeMarker = mViewModel.placesMarkersMap.get(String.valueOf(pair.getKey()));
                if(placeMarker != null && marker.equals(placeMarker.getMarker())){
                    showPlaceInfo(placeMarker.getPlace()); // Display info window to select book or edit place
                }
            }
        }else{
            // Hide place info card
            mBinding.placeInfo.setVisibility(View.INVISIBLE); // Hide place info
            mBinding.addQueueButton.setVisibility(View.VISIBLE); // Show add place button
        }

        return false; // return false if you want map to keep handling click listeners
    }

    // Go to add queue when user click marker info
     @Override
    public void onInfoWindowClick(Marker marker) {
        if(null != mViewModel.getAddPlaceMarker() && TextUtils.equals(marker.getId(), mViewModel.getAddPlaceMarker().getId())){
            goToAddPlace(marker.getPosition());
        }
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
                    .icon(BitmapDescriptorFactory.defaultMarker(165.0F))
                    .draggable(true)
                    .snippet(getString(R.string.add_marker_snippet));

            mViewModel.setAddPlaceMarker(mViewModel.getMap().addMarker(markerOptions));
            mViewModel.getAddPlaceMarker().showInfoWindow(); // To display add new place window
            mViewModel.moveToLatLng(latLng, true);
        }
    }

    private void showMarkers() {

        for (Object o : mViewModel.placesMarkersMap.entrySet()) {
            Map.Entry pair = (Map.Entry) o;
            Log.d(TAG, "showMarkers PlacesMarkersMap map key/val = " + pair.getKey() + " = " + pair.getValue());
            PlaceMarker placeMarker =  mViewModel.placesMarkersMap.get(String.valueOf(pair.getKey()));
            if (placeMarker != null && placeMarker.getMarker() != null) {
                // Create new marker
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(placeMarker.getMarker().getPosition())
                        .title(placeMarker.getMarker().getTitle())
                        //.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                        .icon(BitmapDescriptorFactory.defaultMarker(13.0F))
                        .draggable(false);
                //.snippet(place.getQueues().size());

                //mViewModel.displayedPlaces.remove(marker); // remove previous place associated with the old marker

                Marker marker = mViewModel.getMap().addMarker(markerOptions);
                //mViewModel.displayedMarkers.put(String.valueOf(pair.getKey()), marker);
                //mViewModel.displayedPlaces.put(marker, place);
                placeMarker.setMarker(marker);
                mViewModel.placesMarkersMap.put(placeMarker.getKey(), placeMarker);
                Log.d(TAG, "showMarkers: maker name = "+placeMarker.getMarker().getTitle()+ " marker id= "+ placeMarker.getMarker().getId());

            }
        }
    }

    private void goToAddPlace(LatLng latLng) {
        NavDirections direction = SearchFragmentDirections.actionSearchToAddPlace(latLng, null);
        navController.navigate(direction);
    }

    private void goToEditPlace(String placeKey) {
        NavDirections direction = SearchFragmentDirections.actionSearchToAddPlace(null, placeKey);
        navController.navigate(direction);
    }

    private void showPlaceInfo(Place place) {
        mSelectedPlaceKey = place.getKey(); // will be used when click on edit place
        mBinding.placeInfo.setVisibility(View.VISIBLE); // Make info card view visible
        mBinding.addQueueButton.setVisibility(View.INVISIBLE); // Hide add place button
        mBinding.placeName.setText(place.getName()); // set selected place's name

        mBinding.servicesChipGroup.clearCheck();
        mBinding.servicesChipGroup.removeAllViews(); // remove all previous views to start fresh
        mViewModel.chipsQueuesMap.clear(); // To remove all chips associations with queues

        // Loop throw queues to display chips
        for (Object o : place.getQueues().entrySet()) {
            Map.Entry queuePair = (Map.Entry) o;
            Log.d(TAG, "showPlaceInfo place.getQueues() map key/val = " + queuePair.getKey() + " = " + queuePair.getValue());
            Queue queue = place.getQueues().get(String.valueOf(queuePair.getKey()));
            if(null != queue){
                queue.setKey(String.valueOf(queuePair.getKey()));
                // set PlaceId inside userQueue object, it helps to access queues inside place node later
                UserQueue userQueue = new UserQueue(queue.getKey(), queue.getName(), place.getKey(), place.getName());
                // Don't display more than 30 character
                String shortenString = queue.getName().substring(0, Math.min(queue.getName().length(), 30));
                //Chip mChipBinding = new Chip(mContext);
                //mChipBinding.setText(shortenString);
                ChipItemBinding mChipBinding = ChipItemBinding.inflate(getLayoutInflater(), mBinding.servicesChipGroup, false);
                mChipBinding.chipItem.setText(shortenString);
                mChipBinding.chipItem.setId(ViewCompat.generateViewId());

                // Disable checking by default to only enable it if there is at least one counter open
                mChipBinding.chipItem.setCheckable(false);
                // Clicking is enabled by default to toast a message when the user selects a service that has no counter open
                //mChipBinding.chipItem.setClickable(true);

                // Enable checking the chipItem if at least on of the queue's counter is open
                // Loop throw all counters to see if there is at least one of them is open
                for (Object C : queue.getCounters().entrySet()) {
                    Map.Entry CounterPair = (Map.Entry) C;
                    Log.d(TAG, "showPlaceInfo queue.getCounters() map key/val = " + CounterPair.getKey() + " = " + CounterPair.getValue());
                    Counter counter = queue.getCounters().get(String.valueOf(CounterPair.getKey()));
                    if (null != counter) {
                        counter.setKey(String.valueOf(CounterPair.getKey()));
                        if(counter.isOpen()){
                            Log.d(TAG, "Counter name= " + counter.getName() + " is Open= "+counter.isOpen());
                            mChipBinding.chipItem.setCheckable(true);
                        }

                    }
                } // End of looping throw all counters

                if(!mChipBinding.chipItem.isCheckable()) {
                    // ChipItem is not checkable because it has no open counter
                    mChipBinding.chipItem.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // Toast a message when the user selects a service that has no counter open
                            Toast.makeText(mContext, R.string.counters_closed_error, Toast.LENGTH_LONG).show();
                        }
                    });
                }

                //ChipDrawable chipDrawable = ChipDrawable.createFromAttributes(mContext, null, 0, R.style.Widget_MaterialComponents_Chip_Choice);
                //chip.setChipDrawable(chipDrawable);
                mBinding.servicesChipGroup.addView(mChipBinding.chipItem);
                mViewModel.chipsQueuesMap.put(mChipBinding.chipItem.getId(), userQueue); // a map to get selected service
            }
        }

        // Listen to chip selection
        mBinding.servicesChipGroup.setOnCheckedChangeListener(new ChipGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(ChipGroup group, int checkedId) {
                UserQueue selectedUserQueue = mViewModel.chipsQueuesMap.get(checkedId);
                if(selectedUserQueue != null){
                    Log.d(TAG, "onCheckedChanged: "+selectedUserQueue.getKey());
                    // Allow user to unbook if if already booked
                    mViewModel.getUserQueueOnce(mCurrentUserId, selectedUserQueue.getKey(), new FirebaseUserQueueCallback() {
                        @Override
                        public void onCallback(UserQueue userQueue) {
                            if(userQueue == null){
                                Log.d(TAG, "onCallback: current user didn't join the queue");
                                mBinding.bookButton.setEnabled(true);
                                mBinding.bookButton.setClickable(true);
                                mBinding.bookButton.setText(R.string.book_button_title);
                            }else{
                                if(userQueue.getJoinedLong() != 0){
                                    Log.d(TAG, "onCallback: current user already in the queue");
                                    mBinding.bookButton.setEnabled(true);
                                    mBinding.bookButton.setClickable(true);
                                    mBinding.bookButton.setText(R.string.unbook_button_title);

                                }else{
                                    Log.d(TAG, "onCallback: current user quited the queue");
                                    mBinding.bookButton.setEnabled(true);
                                    mBinding.bookButton.setClickable(true);
                                    mBinding.bookButton.setText(R.string.book_button_title);
                                }
                            }
                        }
                    });
                }
            }
        });

    }

    // To add customer to queue
    private void bookQueue(final UserQueue selectedQueue) {
        // Get current user once, to get currentUser's name and avatar for notifications
        mViewModel.getUserOnce(mCurrentUserId, new FirebaseUserCallback() {
            @Override
            public void onCallback(User user) {
                if(user != null){
                    Log.d(TAG,  "FirebaseUserCallback onCallback. name= " + user.getName() + " hashcode= "+ hashCode());
                    // Set customer object properties
                    // set user age
                    Calendar c = Calendar.getInstance();
                    int year = c.get(Calendar.YEAR);
                    int age = year- user.getBirthYear();

                    Customer customer = new Customer(user.getKey(), user.getAvatar(), user.getName(), user.getGender(), age, user.getDisabled(), CUSTOMER_STATUS_WAITING);
                    mViewModel.addCurrentCustomer(selectedQueue, customer, new FirebaseOnCompleteCallback() {
                        @Override
                        public void onCallback(@NonNull Task<Void> task) {
                            // enable book button again as we finished the process
                            mBinding.bookButton.setEnabled(true);
                            mBinding.bookButton.setClickable(true);

                            if(null != task && task.isSuccessful()){
                                // Go to customers recycler
                                Log.d(TAG, "FirebaseOnCompleteCallback onCallback: "+task.isSuccessful());
                                /*NavDirections direction = SearchFragmentDirections.actionSearchToCustomers(userQueue.getPlaceId(), userQueue.getKey());
                                navController.navigate(direction);*/
                                navController.navigate(R.id.queues);
                            }else{
                                Toast.makeText(mContext, R.string.book_queue_error, Toast.LENGTH_LONG).show();
                            }
                        }
                    });

                }else{
                    // enable book button again as we finished the process
                    mBinding.bookButton.setEnabled(true);
                    mBinding.bookButton.setClickable(true);

                    Toast.makeText(mContext, R.string.fetch_profile_error, Toast.LENGTH_LONG).show();
                }
            }
        });

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
                                            mViewModel.moveToLocation(mViewModel.getCurrentLocation(), true);
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
