package com.tabourless.queue.data;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryDataEventListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.tabourless.queue.interfaces.FirebaseOnCompleteCallback;
import com.tabourless.queue.interfaces.FirebasePlaceCallback;
import com.tabourless.queue.interfaces.FirebaseUserCallback;
import com.tabourless.queue.interfaces.FirebaseUserQueueCallback;
import com.tabourless.queue.models.Customer;
import com.tabourless.queue.models.FirebaseListeners;
import com.tabourless.queue.models.Place;
import com.tabourless.queue.models.Queue;
import com.tabourless.queue.models.User;
import com.tabourless.queue.models.UserQueue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.tabourless.queue.App.DATABASE_REF_CUSTOMERS;
import static com.tabourless.queue.App.DATABASE_REF_CUSTOMER_USER_ID;
import static com.tabourless.queue.App.DATABASE_REF_PLACES;
import static com.tabourless.queue.App.DATABASE_REF_QUEUE_JOINED;
import static com.tabourless.queue.App.DATABASE_REF_USERS;
import static com.tabourless.queue.App.DATABASE_REF_USER_QUEUES;

public class SearchRepository {

    private final static String TAG = SearchRepository.class.getSimpleName();

    // [START declare_database_ref]
    private DatabaseReference mDatabaseRef;
    private DatabaseReference mPlacesRef , mCustomersRef, mUsersRef, mUserQueuesRef; // to get place, to add customer to queue, to get current user
    private  GeoFire mGeoFire; // GeoFire database reference
    private GeoQuery mGeoQuery; // GeoFire Query

    private MutableLiveData<Place> mPlace; // return place object
    private MutableLiveData<Map<String, Place>> mNearbyPlaces; // to observe nearby places
    //private List<Place> mPlacesArrayList;// an array list to add all matched places
    private Map<String, Place> mPlacesMap;
    // HashMap to keep track of Firebase Listeners
    //private HashMap< DatabaseReference , ValueEventListener> mListenersMap;
    // Change mListenersList to static so that it's the same for all instance
    private  List<FirebaseListeners> mListenersList; // = new ArrayList<>();

    private FirebaseUser mFirebaseCurrentUser;
    private String mCurrentUserId;


    // A listener for places changes
    private ValueEventListener placeListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // Get Post object and use the values to update the UI
            if (dataSnapshot.exists()) {
                // Get place value
                Log.d(TAG, "dataSnapshot key: " + dataSnapshot.getKey()+" Listener = "+placeListener);
                //mCurrentUser = dataSnapshot.getValue(User.class);
                mPlace.postValue(dataSnapshot.getValue(Place.class));
            } else {
                // Place is null, error out
                mPlace.postValue(null); // return null to disable buttons when unsaved new user opened his profile
                Log.w(TAG, "place is null, no such user");
            }
        }
        @Override
        public void onCancelled(DatabaseError databaseError) {
            // Getting Post failed, log a message
            Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
        }
    };

    private GeoQueryDataEventListener mGeoQueryListener = new GeoQueryDataEventListener() {
        @Override
        public void onDataEntered(DataSnapshot dataSnapshot, GeoLocation location) {
            // The location of a dataSnapshot now matches the query criteria.
            Log.d(TAG, "GeoFire onDataEntered: dataSnapshot key = "+dataSnapshot.getKey());
            final Place place = dataSnapshot.getValue(Place.class);
            if(place != null){
                place.setKey(dataSnapshot.getKey());
                mPlacesMap.put(place.getKey(), place);
                mNearbyPlaces.postValue(mPlacesMap);
            }
        }

        @Override
        public void onDataExited(DataSnapshot dataSnapshot) {
            // The location of a dataSnapshot no longer matches the query criteria.
            Place place = dataSnapshot.getValue(Place.class);
            if(place != null){
                place.setKey(dataSnapshot.getKey());
                mPlacesMap.remove(place.getKey());
                mNearbyPlaces.postValue(mPlacesMap);
                Log.d(TAG, "GeoFire onDataExited: dataSnapshot key = "+dataSnapshot.getKey()+ " mPlacesArrayList size= "+place.getName());
            }
        }

        @Override
        public void onDataMoved(DataSnapshot dataSnapshot, GeoLocation location) {
            // The location of a dataSnapshot changed but the location still matches the query criteria
        }

        @Override
        public void onDataChanged(DataSnapshot dataSnapshot, GeoLocation location) {
            // The dataSnapshot is changed
            Place place = dataSnapshot.getValue(Place.class);
            if(place != null){
                place.setKey(dataSnapshot.getKey());
                mPlacesMap.put(place.getKey(), place);
                mNearbyPlaces.postValue(mPlacesMap);
                Log.d(TAG, "GeoFire onDataChanged: dataSnapshot key = "+dataSnapshot.getKey()+ " changed place name= "+ place.getName() + " queues size= "+place.getQueues().size());
            }
        }

        @Override
        public void onGeoQueryReady() {
            // All current data has been loaded from the server and all initial events have been fired
            Log.d(TAG, "onGeoQueryReady:" );
        }

        @Override
        public void onGeoQueryError(DatabaseError error) {
            // There was an error while performing this query, e.g. a violation of security rules
            Log.e(TAG, "onGeoQueryError: "+ error.getMessage());
        }

    };


    public SearchRepository(){
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        mPlacesRef = mDatabaseRef.child(DATABASE_REF_PLACES);
        mCustomersRef = mDatabaseRef.child(DATABASE_REF_CUSTOMERS);;
        mUsersRef = mDatabaseRef.child(DATABASE_REF_USERS);
        mUserQueuesRef = mDatabaseRef.child(DATABASE_REF_USER_QUEUES);
        mGeoFire = new GeoFire(mPlacesRef);

        //Get current logged in user
        mFirebaseCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mCurrentUserId = mFirebaseCurrentUser != null ? mFirebaseCurrentUser.getUid() : null;

        mPlace = new MutableLiveData<>(); // to get a specific place
        mNearbyPlaces = new MutableLiveData<Map<String, Place>>(); // to get nearby markers
        //mPlacesArrayList = new ArrayList<>(); // An array list to add all matched places
        mNearbyPlaces = new MutableLiveData<>();  // a MutableLiveData place to update the found place when entered
        mPlacesMap = new HashMap<>(); // A map to add found places

        if(mListenersList == null){
            mListenersList = new ArrayList<>();
            Log.d(TAG, "mListenersList is null. new ArrayList is created= " + mListenersList.size());
        }else{
            Log.d(TAG, "mListenersList is not null. Size= " + mListenersList.size());
            if(mListenersList.size() >0){
                Log.d(TAG, "mListenersList is not null and not empty. Size= " + mListenersList.size()+" Remove previous listeners");
                // No need to remove old Listeners, we are gonna reuse them
                removeListeners();
                //mListenersList = new ArrayList<>();
            }
        }

    }

    public MutableLiveData<Place> getPlace(String placeId){

        DatabaseReference placeRef = mPlacesRef.child(placeId);
        Log.d(TAG, "getPlace initiated: " + placeId);
        Log.d(TAG, "getPlace Listeners size= "+ mListenersList.size());
        if(mListenersList.size()== 0){
            // Need to add a new Listener
            Log.d(TAG, "getPlace adding new Listener= "+ mListenersList);
            placeRef.addValueEventListener(placeListener);
            mListenersList.add(new FirebaseListeners(placeRef, placeListener));
        }else{
            Log.d(TAG, "postSnapshot Listeners size is not 0= "+ mListenersList.size());
            //there is an old Listener, need to check if it's on this ref
            for (int i = 0; i < mListenersList.size(); i++) {
                if(mListenersList.get(i).getListener().equals(placeListener) &&
                        !mListenersList.get(i).getQueryOrRef().equals(placeRef)){
                    // We used this listener before, but on another Ref
                    Log.d(TAG, "We used this listener before, is it on the same ref?");
                    Log.d(TAG, "getPlace adding new Listener= "+ placeListener);
                    placeRef.addValueEventListener(placeListener);
                    mListenersList.add(new FirebaseListeners(placeRef, placeListener));
                }else if((mListenersList.get(i).getListener().equals(placeListener) &&
                        mListenersList.get(i).getQueryOrRef().equals(placeRef))){
                    //there is old Listener on the ref
                    Log.d(TAG, "getPlace Listeners= there is old Listener on the ref= "+mListenersList.get(i).getQueryOrRef()+ " Listener= " + mListenersList.get(i).getListener());
                }else{
                    //placeListener is never used
                    Log.d(TAG, "getPlace Listener is never created");
                    placeRef.addValueEventListener(placeListener);
                    mListenersList.add(new FirebaseListeners(placeRef, placeListener));
                }
            }
        }

        for (int i = 0; i < mListenersList.size(); i++) {
            Log.d(TAG, "getPlace loop throw Listeners ref= "+ mListenersList.get(i).getQueryOrRef()+ " Listener= "+ mListenersList.get(i).getListener());
        }
        return mPlace;
    }

    public void getPlaceOnce(String placeId, final FirebasePlaceCallback callback){

        DatabaseReference placeRef = mPlacesRef.child(placeId);
        //final MutableLiveData<User> mCurrentUser = new MutableLiveData<>();
        Log.d(TAG, "getUser initiated: " + placeId);

        placeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Get user value
                    Log.d(TAG, "getPlaceOnce dataSnapshot key: "
                            + dataSnapshot.getKey()+" Listener = "+placeListener);
                    //mSingleValueUser = dataSnapshot.getValue(User.class);
                    //mSingleValueUser.postValue(dataSnapshot.getValue(User.class));
                    //int sID = dataSnapshot.child("soundId").getValue(Integer.class);
                    //Log.d(TAG, "getUserOnce User sound id= "+ sID);
                    Place place = dataSnapshot.getValue(Place.class);
                    if(place != null){
                        place.setKey(dataSnapshot.getKey());
                    }
                    callback.onCallback(place);
                } else {
                    // Return a null user to view model to know when user doesn't exist,
                    // So we don't create or update tokens and online presence
                    callback.onCallback(null);
                    Log.w(TAG, "getPlaceOnce place is null, no such user");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "getPlaceOnce place onCancelled" +databaseError);
            }
        });
    }

    public MutableLiveData<Map<String, Place>> getNearbyPlaces(LatLng point){

        // creates a new query around the selected place with a radius of 0.6 kilometers
        if(null == mGeoQuery){
            mGeoQuery = mGeoFire.queryAtLocation(new GeoLocation(point.latitude, point.longitude), 2);
            mGeoQuery.addGeoQueryDataEventListener(mGeoQueryListener);
        }else{
            mGeoQuery.setCenter(new GeoLocation(point.latitude, point.longitude));
        }

        //mGeoQuery.removeAllListeners();
        //mPlacesArrayList.clear();
        return mNearbyPlaces;
    }

    public void addPlace(final Place place){

        mPlacesRef.child(place.getKey()).setValue(place).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // place is added successfully, we need to add GeoFire lat and lng
                Log.d(TAG, "onSuccess: data added");
                GeoFire geoFire = new GeoFire(mPlacesRef.child(place.getKey()));
                geoFire.setLocation("location", new GeoLocation(place.getLatitude(), place.getLongitude()));

            }
        });
    }

    // To add the user who booked the queue to customers column
    public void addCurrentCustomer(UserQueue selectedQueue, Customer customer, final FirebaseOnCompleteCallback callback) {

        // Remove counters from the UserQueue, we don't need counters in userQueue nod
        // We only added counters to selectedQueue to check suitable counters for user (customer) before agree to add him
        selectedQueue.getCounters().clear();

        String customerPushKey = mCustomersRef.child(selectedQueue.getPlaceId()).child(selectedQueue.getKey()).push().getKey();
        Map<String, Object> childUpdates = new HashMap<>();// Map to update all
        Map<String, Object> customerValues = customer.toMap();
        Map<String, Object> queueValues = selectedQueue.toMap();

        childUpdates.put(DATABASE_REF_CUSTOMERS +"/"+ selectedQueue.getPlaceId() +"/"+ selectedQueue.getKey() + "/" + customerPushKey, customerValues);
        childUpdates.put( DATABASE_REF_USER_QUEUES +"/"+ mCurrentUserId+ "/" + selectedQueue.getKey() ,queueValues);

        mDatabaseRef.updateChildren(childUpdates).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                callback.onCallback(task);
            }
        });

    }

    // To remove the user who from the queue he booked
    public void removeCurrentCustomer(final UserQueue selectedQueue, final FirebaseOnCompleteCallback callback) {
        // Get the current customer key from customers nod to remove it
        // Must call a query because the customer key is a pushed id not the user id
        Log.d(TAG, "removeCurrentCustomer placeId= "+ selectedQueue.getPlaceId()+ " QueueId= "+ selectedQueue.getKey() + " CurrentUserId= "+mCurrentUserId);
        DatabaseReference currentCustomerRef = mCustomersRef.child(selectedQueue.getPlaceId()).child(selectedQueue.getKey());
        Log.d(TAG, "removeCurrentCustomer currentCustomerRef= "+ currentCustomerRef.getRef());

        Query query = currentCustomerRef.orderByChild(DATABASE_REF_CUSTOMER_USER_ID)
                .equalTo(mCurrentUserId);
                //.limitToFirst(1);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "dataSnapshot ChildrenCount= "+ dataSnapshot.getChildrenCount());
                if (dataSnapshot.exists()) {
                    Map<String, Object> childUpdates = new HashMap<>(); // A HashMap to update the database
                    // loop throw all found results. the result is suppose to be only one customer anyway.
                    // But in case there are more than one booking we will delete them all
                    for (DataSnapshot snapshot: dataSnapshot.getChildren()){
                        String customerKey = snapshot.getKey();
                        Log.d(TAG, "current customer to be removed: "+ customerKey);

                        if(!TextUtils.isEmpty(customerKey)){
                            // Update current customer to null to remove it from the customers node
                            childUpdates.put(DATABASE_REF_CUSTOMERS +"/" + selectedQueue.getPlaceId() + "/" + selectedQueue.getKey() + "/" + customerKey, null);
                        }
                    }

                    childUpdates.put(DATABASE_REF_USER_QUEUES +"/" + mCurrentUserId + "/" + selectedQueue.getKey()+ "/"+ DATABASE_REF_QUEUE_JOINED , 0);

                    // update Data base
                    mDatabaseRef.updateChildren(childUpdates).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            callback.onCallback(task);
                        }
                    });
                } else {
                    // Return a null customer to view model to know when customer doesn't exist in the customer nod,
                    // So we don't create or update tokens and online presence
                    callback.onCallback(null);
                    Log.w(TAG, "removeCurrentCustomer is null, this user didn't join the queue before");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "getUserOnce User onCancelled" +databaseError);
            }
        });
    }

    public void getUserOnce(String userId, final FirebaseUserCallback callback){

        DatabaseReference UserRef = mUsersRef.child(userId);
        //final MutableLiveData<User> mCurrentUser = new MutableLiveData<>();
        Log.d(TAG, "getUser initiated: " + userId);

        UserRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Get user value
                    User user = dataSnapshot.getValue(User.class);
                    if(user != null){
                        user.setKey(dataSnapshot.getKey());
                    }
                    callback.onCallback(user);
                } else {
                    // Return a null user to view model to know when user doesn't exist,
                    // So we don't create or update tokens and online presence
                    callback.onCallback(null);
                    Log.w(TAG, "getUserOnce User is null, no such user");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "getUserOnce User onCancelled" +databaseError);
            }
        });
    }

    public void getUserQueueOnce(String userId, String queueId, final FirebaseUserQueueCallback callback){

        DatabaseReference userQueueRef = mUserQueuesRef.child(userId).child(queueId);
        //final MutableLiveData<User> mCurrentUser = new MutableLiveData<>();
        Log.d(TAG, "getUserQueueOnce initiated: " + userId);

        userQueueRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Get user value
                    UserQueue userQueue = dataSnapshot.getValue(UserQueue.class);
                    if(userQueue != null){
                        userQueue.setKey(dataSnapshot.getKey());
                    }
                    callback.onCallback(userQueue);
                } else {
                    // Return a null user to view model to know when user doesn't exist,
                    // So we don't create or update tokens and online presence
                    callback.onCallback(null);
                    Log.w(TAG, "getUserQueue is null, this user didn't join the queue before");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "getUserOnce User onCancelled" +databaseError);
            }
        });
    }




    public void removeListeners(){
        if(null != mListenersList){
            for (int i = 0; i < mListenersList.size(); i++) {
                //Log.d(TAG, "remove Listeners ref= "+ mListenersList.get(i).getReference()+ " Listener= "+ mListenersList.get(i).getListener());
                //Log.d(TAG, "remove Listeners Query= "+ mListenersList.get(i).getQuery()+ " Listener= "+ mListenersList.get(i).getListener());
                Log.d(TAG, "remove Listeners Query or Ref= "+ mListenersList.get(i).getQueryOrRef()+ " Listener= "+ mListenersList.get(i).getListener());

                if(null != mListenersList.get(i).getListener()){
                    mListenersList.get(i).getQueryOrRef().removeEventListener(mListenersList.get(i).getListener());
                }
            }
            mListenersList.clear();

            // Clear geo fire Listeners and places arrayList
            if(mGeoQuery != null){
                mGeoQuery.removeAllListeners();
            }
            //mPlacesArrayList.clear();
            //mPlacesMap.clear();
        }
    }

}

