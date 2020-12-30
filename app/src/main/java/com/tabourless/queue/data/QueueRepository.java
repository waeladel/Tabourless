package com.tabourless.queue.data;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.tabourless.queue.models.Customer;
import com.tabourless.queue.models.FirebaseListeners;
import com.tabourless.queue.models.Queue;
import com.tabourless.queue.models.User;

import java.util.ArrayList;
import java.util.List;

import static com.tabourless.queue.App.DATABASE_REF_CUSTOMERS;
import static com.tabourless.queue.App.DATABASE_REF_PLACES;
import static com.tabourless.queue.App.DATABASE_REF_QUEUES;
import static com.tabourless.queue.App.DATABASE_REF_USERS;

public class QueueRepository {

    private final static String TAG = MessagesRepository.class.getSimpleName();

    // [START declare_database_ref]
    private DatabaseReference mDatabaseRef , mQueueRef, mCurrentCustomerRef , mCurrentUserRef;
    private DatabaseReference mUsersRef , mMessagesRef, mChatRef;
    private Boolean isFirstLoaded = true;

    // Not static to only remove listeners of this repository instance
    // Start destination fragment is never destroyed , so when clicking on it's bottom navigation icon again it got destroyed to be recreated
    // When that happens clearing listeners is triggered on viewmodel Cleared, which removes that new listeners for the just added query
    // When new listener is removed we got 0 results and have no listeners for updates.
    private List<FirebaseListeners> mListenersList;

    private MutableLiveData<Queue> mQueue;
    private MutableLiveData<Customer> mCurrentCustomer;
    private MutableLiveData<User> mCurrentUser;

    // a listener for Queue changes
    private ValueEventListener QueueListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // Get Post object and use the values to update the UI
            if (dataSnapshot.exists()) {
                // Get user value
                Log.d(TAG, "chat dataSnapshot key: "
                        + dataSnapshot.getKey()+" Listener = "+QueueListener);
                mQueue.postValue(dataSnapshot.getValue(Queue.class));
            } else {
                // Chat is null, error out
                Log.w(TAG, "chat is null, no such chat");
                // Return null because we need to know when chat is deleted due to unblocking user
                // We delete the chat node when unblocking a user to start fresh.
                mQueue.postValue(null);
            }
        }
        @Override
        public void onCancelled(DatabaseError databaseError) {
            // Getting Post failed, log a message
            Log.w(TAG, "load chat:onCancelled", databaseError.toException());

        }
    };

    // a listener for current customer  changes
    private ValueEventListener mCustomerListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // Get Post object and use the values to update the UI
            if (dataSnapshot.exists()) {
                // Get user value
                Log.d(TAG, "chat dataSnapshot key: " + dataSnapshot.getKey()+" Listener = "+mCustomerListener);
                Customer customer = dataSnapshot.getValue(Customer.class);
                if (customer != null) {
                    customer.setKey(dataSnapshot.getKey());
                    mCurrentCustomer.postValue(customer);
                }else{
                    // To hide "Your token" and update customers ahead when user unbook
                    mCurrentCustomer.postValue(null);
                }


            } else {
                // Chat is null, error out
                Log.w(TAG, "chat is null, no such chat");
                // Return null because we need to know when chat is deleted due to unblocking user
                // We delete the chat node when unblocking a user to start fresh.
                mCurrentCustomer.postValue(null);
            }
        }
        @Override
        public void onCancelled(DatabaseError databaseError) {
            // Getting Post failed, log a message
            Log.w(TAG, "load chat:onCancelled", databaseError.toException());

        }
    };

    // a listener for current user  changes
    private ValueEventListener mUserListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // Get Post object and use the values to update the UI
            if (dataSnapshot.exists()) {
                // Get user value
                Log.d(TAG, "chat dataSnapshot key: " + dataSnapshot.getKey()+" Listener = "+ mUserListener);
                User user = dataSnapshot.getValue(User.class);
                if (user != null) {
                    user.setKey(dataSnapshot.getKey());
                    mCurrentUser.postValue(user);
                }else{
                    // To hide "Your token" and update customers ahead when user unbook
                    mCurrentUser.postValue(null);
                }


            } else {
                // Chat is null, error out
                Log.w(TAG, "chat is null, no such chat");
                // Return null because we need to know when chat is deleted due to unblocking user
                // We delete the chat node when unblocking a user to start fresh.
                mCurrentUser.postValue(null);
            }
        }
        @Override
        public void onCancelled(DatabaseError databaseError) {
            // Getting Post failed, log a message
            Log.w(TAG, "load chat:onCancelled", databaseError.toException());

        }
    };

    public QueueRepository(){
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        mQueue = new MutableLiveData<>();
        mCurrentCustomer = new MutableLiveData<>();
        mCurrentUser = new MutableLiveData<>();

        if(mListenersList == null){
            mListenersList = new ArrayList<>();
            Log.d(TAG, "mListenersList is null. new ArrayList is created= " + mListenersList.size());
        }else{
            Log.d(TAG, "mListenersList is not null. Size= " + mListenersList.size());
            if(mListenersList.size() >0){
                Log.d(TAG, "mListenersList is not null and not empty. Size= " + mListenersList.size()+" Remove previous listeners");
                removeListeners();
                //mListenersList = new ArrayList<>();
            }
        }
    }

    public LiveData<Customer> getCurrentCustomer(String placeKey, String queueKey, String currentUserId) {
        Log.d(TAG, "QueueRepository: queueId= "+ queueKey);
        // use received placeKey and queueKey to create a database ref
        mCurrentCustomerRef = mDatabaseRef.child(DATABASE_REF_CUSTOMERS).child(placeKey).child(queueKey).child(currentUserId);
        /*Query query = mCurrentCustomerRef.orderByChild(DATABASE_REF_CUSTOMER_USER_ID)
                .equalTo(currentUserId)
                .limitToFirst(1);*/

        Log.d(TAG, "getQueue mListenersList size= "+ mListenersList.size());
        if(mListenersList.size()== 0){
            // Need to add a new Listener
            Log.d(TAG, "getQueue adding new Listener= "+ mListenersList);
            //mListenersMap.put(postSnapshot.getRef(), mPickUpCounterListener);
            mCurrentCustomerRef.addValueEventListener(mCustomerListener);
            mListenersList.add(new FirebaseListeners(mCurrentCustomerRef, mCustomerListener));
        }else{
            Log.d(TAG, "postSnapshot Listeners size is not 0= "+ mListenersList.size());
            //there is an old Listener, need to check if it's on this ref
            for (int i = 0; i < mListenersList.size(); i++) {
                //Log.d(TAG, "getUser Listeners ref= "+ mListenersList.get(i).getQueryOrRef()+ " Listener= "+ mListenersList.get(i).getListener());
                if(mListenersList.get(i).getListener().equals(mCustomerListener) &&
                        !mListenersList.get(i).getQueryOrRef().equals(mCurrentCustomerRef)){
                    // We used this listener before, but on another Ref
                    Log.d(TAG, "We used this listener before, is it on the same ref?");
                    Log.d(TAG, "getQueue adding new Listener= "+ mCustomerListener);
                    mCurrentCustomerRef.addValueEventListener(mCustomerListener);
                    mListenersList.add(new FirebaseListeners(mCurrentCustomerRef, mCustomerListener));
                }else if((mListenersList.get(i).getListener().equals(mCustomerListener) &&
                        mListenersList.get(i).getQueryOrRef().equals(mCurrentCustomerRef))){
                    //there is old Listener on the ref
                    Log.d(TAG, "getQueue Listeners= there is old Listener on the ref= "+mListenersList.get(i).getQueryOrRef()+ " Listener= " + mListenersList.get(i).getListener());
                }else{
                    //CustomerListener is never used
                    Log.d(TAG, "Listener is never created");
                    mCurrentCustomerRef.addValueEventListener(mCustomerListener);
                    mListenersList.add(new FirebaseListeners(mCurrentCustomerRef, mCustomerListener));
                }
            }
        }
        return mCurrentCustomer;
    }

    public LiveData<User> getCurrentUser(String currentUserId) {
        // use received placeKey and queueKey to create a database ref
        mCurrentUserRef = mDatabaseRef.child(DATABASE_REF_USERS).child(currentUserId);

        Log.d(TAG, "getQueue mListenersList size= "+ mListenersList.size());
        if(mListenersList.size()== 0){
            // Need to add a new Listener
            Log.d(TAG, "getQueue adding new Listener= "+ mListenersList);
            //mListenersMap.put(postSnapshot.getRef(), mPickUpCounterListener);
            mCurrentUserRef.addValueEventListener(mUserListener);
            mListenersList.add(new FirebaseListeners(mCurrentUserRef, mUserListener));
        }else{
            Log.d(TAG, "postSnapshot Listeners size is not 0= "+ mListenersList.size());
            //there is an old Listener, need to check if it's on this ref
            for (int i = 0; i < mListenersList.size(); i++) {
                //Log.d(TAG, "getUser Listeners ref= "+ mListenersList.get(i).getQueryOrRef()+ " Listener= "+ mListenersList.get(i).getListener());
                if(mListenersList.get(i).getListener().equals(mUserListener) &&
                        !mListenersList.get(i).getQueryOrRef().equals(mCurrentUserRef)){
                    // We used this listener before, but on another Ref
                    Log.d(TAG, "We used this listener before, is it on the same ref?");
                    Log.d(TAG, "getQueue adding new Listener= "+ mUserListener);
                    mCurrentUserRef.addValueEventListener(mUserListener);
                    mListenersList.add(new FirebaseListeners(mCurrentUserRef, mUserListener));
                }else if((mListenersList.get(i).getListener().equals(mUserListener) &&
                        mListenersList.get(i).getQueryOrRef().equals(mCurrentUserRef))){
                    //there is old Listener on the ref
                    Log.d(TAG, "getQueue Listeners= there is old Listener on the ref= "+mListenersList.get(i).getQueryOrRef()+ " Listener= " + mListenersList.get(i).getListener());
                }else{
                    //userListener is never used
                    Log.d(TAG, "Listener is never created");
                    mCurrentUserRef.addValueEventListener(mUserListener);
                    mListenersList.add(new FirebaseListeners(mCurrentUserRef, mUserListener));
                }
            }
        }
        return mCurrentUser;
    }

    public MutableLiveData<Queue> getQueue(String placeKey, String queueKey) {
        Log.d(TAG, "QueueRepository: placeId= "+ placeKey+ " queueId= "+ queueKey);
        // use received placeKey and queueKey to create a database ref
        mQueueRef = mDatabaseRef.child(DATABASE_REF_PLACES).child(placeKey).child(DATABASE_REF_QUEUES).child(queueKey);
        Log.d(TAG, "getQueue mListenersList size= "+ mListenersList.size());
        if(mListenersList.size()== 0){
            // Need to add a new Listener
            Log.d(TAG, "getQueue adding new Listener= "+ mListenersList);
            //mListenersMap.put(postSnapshot.getRef(), mPickUpCounterListener);
            mQueueRef.addValueEventListener(QueueListener);
            mListenersList.add(new FirebaseListeners(mQueueRef, QueueListener));
        }else{
            Log.d(TAG, "postSnapshot Listeners size is not 0= "+ mListenersList.size());
            //there is an old Listener, need to check if it's on this ref
            for (int i = 0; i < mListenersList.size(); i++) {
                //Log.d(TAG, "getUser Listeners ref= "+ mListenersList.get(i).getQueryOrRef()+ " Listener= "+ mListenersList.get(i).getListener());
                if(mListenersList.get(i).getListener().equals(QueueListener) &&
                        !mListenersList.get(i).getQueryOrRef().equals(mQueueRef)){
                    // We used this listener before, but on another Ref
                    Log.d(TAG, "We used this listener before, is it on the same ref?");
                    Log.d(TAG, "getQueue adding new Listener= "+ QueueListener);
                    mQueueRef.addValueEventListener(QueueListener);
                    mListenersList.add(new FirebaseListeners(mQueueRef, QueueListener));
                }else if((mListenersList.get(i).getListener().equals(QueueListener) &&
                        mListenersList.get(i).getQueryOrRef().equals(mQueueRef))){
                    //there is old Listener on the ref
                    Log.d(TAG, "getQueue Listeners= there is old Listener on the ref= "+mListenersList.get(i).getQueryOrRef()+ " Listener= " + mListenersList.get(i).getListener());
                }else{
                    //QueueListener is never used
                    Log.d(TAG, "Listener is never created");
                    mQueueRef.addValueEventListener(QueueListener);
                    mListenersList.add(new FirebaseListeners(mQueueRef, QueueListener));
                }
            }
        }

        /*for (int i = 0; i < mListenersList.size(); i++) {
            Log.d(TAG, "getChat loop throw ref= "+ mListenersList.get(i).getQueryOrRef()+ " Listener= "+ mListenersList.get(i).getListener());
        }*/

        return mQueue;
    }

    // remove all listeners when the ViewModel is cleared
    public void removeListeners(){
        if(null != mListenersList){
            for (int i = 0; i < mListenersList.size(); i++) {
                //Log.d(TAG, "remove Listeners ref= "+ mListenersList.get(i).getReference()+ " Listener= "+ mListenersList.get(i).getListener());
                //Log.d(TAG, "remove Listeners Query= "+ mListenersList.get(i).getQuery()+ " Listener= "+ mListenersList.get(i).getListener());
                Log.d(TAG, "remove Listeners Query or Ref= "+ mListenersList.get(i).getQueryOrRef()+ " Listener= "+ mListenersList.get(i).getListener());

                if(null != mListenersList.get(i).getListener()){
                    mListenersList.get(i).getQueryOrRef().removeEventListener(mListenersList.get(i).getListener());
                }
            /*if(null != mListenersList.get(i).getReference()){
                mListenersList.get(i).getReference().removeEventListener(mListenersList.get(i).getListener());
            }else if(null != mListenersList.get(i).getQuery()){
                mListenersList.get(i).getQuery().removeEventListener(mListenersList.get(i).getListener());
            }*/
            }
            mListenersList.clear();
        }
    }

}