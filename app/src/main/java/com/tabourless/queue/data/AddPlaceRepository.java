package com.tabourless.queue.data;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.tabourless.queue.interfaces.FirebasePlaceCallback;
import com.tabourless.queue.interfaces.FirebaseUserCallback;
import com.tabourless.queue.models.FirebaseListeners;
import com.tabourless.queue.models.Place;
import com.tabourless.queue.models.User;

import java.util.ArrayList;
import java.util.List;

public class AddPlaceRepository {

    private final static String TAG = AddPlaceRepository.class.getSimpleName();

    // [START declare_database_ref]
    private DatabaseReference mDatabaseRef;
    private DatabaseReference mPlacesRef; // to get place

    private MutableLiveData<Place> mPlace; // return place object

    // HashMap to keep track of Firebase Listeners
    //private HashMap< DatabaseReference , ValueEventListener> mListenersMap;
    // Change mListenersList to static so that it's the same for all instance
    private  List<FirebaseListeners> mListenersList;// = new ArrayList<>();

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



    public AddPlaceRepository(){
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        mPlacesRef = mDatabaseRef.child("places");

        mPlace = new MutableLiveData<>();

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
                    Place palce = dataSnapshot.getValue(Place.class);
                    if(palce != null){
                        palce.setKey(dataSnapshot.getKey());
                    }
                    callback.onCallback(palce);
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

    public void addPlace(Place place){

        mPlacesRef.child(place.getKey()).setValue(place).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // place is added successfully, we need to add GeoFire lat and lng
                Log.d(TAG, "onSuccess: data added");
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
        }
    }
 }

