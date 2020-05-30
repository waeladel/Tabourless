package com.tabourless.queue.data;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.DataSource;
import androidx.paging.ItemKeyedDataSource;

import com.google.android.gms.tasks.OnCompleteListener;
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
import com.tabourless.queue.models.Customer;
import com.tabourless.queue.models.FirebaseListeners;
import com.tabourless.queue.models.Message;
import com.tabourless.queue.models.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomersRepository {

    private final static String TAG = CustomersRepository.class.getSimpleName();

    // [START declare_database_ref]
    private DatabaseReference mDatabaseRef, mCustomersRef;

    private String currentUserId;
    private FirebaseUser mFirebaseCurrentUser;

    private static volatile Boolean isInitialFirstLoaded;// = true;
    private static volatile Boolean isAfterFirstLoaded;// = true;
    private static volatile Boolean isBeforeFirstLoaded;// = true;

    private String initialKey;
    private String afterKey;
    private String beforeKey;
    private String mPlaceKey, mQueueKey;

    private List<FirebaseListeners> mListenersList;// = not static to only remove listeners of this instance
    private List<Customer> totalItemsList;// = new ArrayList<>();
    //private static List<Message> seenItemsList;// = new ArrayList<>() for seen messages by current user;

    private MutableLiveData<User> mUser;

    private DataSource.InvalidatedCallback invalidatedCallback;
    private ItemKeyedDataSource.LoadInitialCallback loadInitialCallback;
    private ItemKeyedDataSource.LoadCallback loadAfterCallback;
    private ItemKeyedDataSource.LoadCallback loadBeforeCallback;

    /*private ValueEventListener initialListener;
    private ValueEventListener afterMessagesListener;
    private ValueEventListener beforeMessagesListener;*/

    private static final int REACHED_THE_TOP = 2;
    private static final int SCROLLING_UP = 1;
    private static final int SCROLLING_DOWN = -1;
    private static final int REACHED_THE_BOTTOM = -2;
    private static int mScrollDirection;
    private static int mLastVisibleItem;

    private static final String CUSTOMER_STATUS_WAITING = "waiting";
    private static final String CUSTOMER_STATUS_NEXT = "next";
    private static final String CUSTOMER_STATUS_FRONT = "front";
    private static final String CUSTOMER_STATUS_AWAY = "away";

    private ValueEventListener afterListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // [START_EXCLUDE]
            Log.d(TAG, "start onDataChange isAfterFirstLoaded = "+ isAfterFirstLoaded);
            if (!isAfterFirstLoaded){
                // Remove post value event listener
                removeListeners();
                Log.d(TAG, "mama getMessagesAfter Invalidated removeEventListener");
                //isAfterFirstLoaded =  true;
                Log.d(TAG, "getMessagesAfter onInvalidated(). isAfterFirstLoaded = "+ isAfterFirstLoaded);
                invalidatedCallback.onInvalidated();
                //UsersDataSource.InvalidatedCallback.class.getMethod("loadInitial", "LoadInitialParams");
                //UsersDataSource.invalidate();
                return;
            }

            if (dataSnapshot.exists()) {
                List<Customer> resultList = new ArrayList<>();

                // loop throw results value
                for (DataSnapshot snapshot: dataSnapshot.getChildren()){
                    if(!getLoadAfterKey().equals(snapshot.getKey())) { // if snapshot key = startAt key? don't add it again
                        Customer customer = snapshot.getValue(Customer.class);
                        if (customer != null) {
                            customer.setKey(snapshot.getKey());
                        }
                        resultList.add(customer);
                        // Add messages to totalItemsList ArrayList to be used to get the initial key position
                        totalItemsList.add(customer);

                        //Log.d(TAG, "mama getMessage = "+ message.getMessage()+" getSnapshotKey= " +  snapshot.getKey());
                    }
                }

                // Get TotalItems logs
                printTotalItems();
                //printSeenItems();
                if(resultList.size() != 0){
                    //callback.onResult(messagesList);
                    getLoadAfterCallback().onResult(resultList);
                    Log.d(TAG, "mama getMessagesAfter  List.size= " +  resultList.size()+ " last key= "+resultList.get(resultList.size()-1).getKey());
                }
            } else {
                // no data
                Log.w(TAG, "mama getMessagesAfter no users exist");
            }
            printListeners();
            isAfterFirstLoaded =  false;
            Log.d(TAG, "end isAfterFirstLoaded = "+ isAfterFirstLoaded);
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            // Getting Post failed, log a message
            Log.w(TAG, "mama getMessagesAfter loadPost:onCancelled", databaseError.toException());
        }
    };

    private ValueEventListener beforeListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // [START_EXCLUDE]
            Log.d(TAG, "start onDataChange isBeforeFirstLoaded = "+ isBeforeFirstLoaded);
            if (!isBeforeFirstLoaded){
                // Remove post value event listener
                removeListeners();
                Log.d(TAG, "mama getMessagesBefore Invalidated removeEventListener");
                //isBeforeFirstLoaded =  true;
                Log.d(TAG, "getMessagesBefore onInvalidated(). isBeforeFirstLoaded = "+ isBeforeFirstLoaded);
                invalidatedCallback.onInvalidated();
                //UsersDataSource.InvalidatedCallback.class.getMethod("loadInitial", "LoadInitialParams");
                //UsersDataSource.invalidate();
                return;
            }

            if (dataSnapshot.exists()) {
                List<Customer> resultList = new ArrayList<>();

                // loop throw users value
                for (DataSnapshot snapshot: dataSnapshot.getChildren()){
                    if(!getLoadBeforeKey().equals(snapshot.getKey())) { // if snapshot key = startAt key? don't add it again
                        Customer customer = snapshot.getValue(Customer.class);
                        if (customer != null) {
                            customer.setKey(snapshot.getKey());
                        }
                        resultList.add(customer);
                        //Log.d(TAG, "mama getMessage = "+ message.getMessage()+" getSnapshotKey= " +  snapshot.getKey());
                    }
                }

                if(resultList.size() != 0){
                    //callback.onResult(messagesList);
                    getLoadBeforeCallback().onResult(resultList);
                    Log.d(TAG, "mama getMessagesBefore  List.size= " +  resultList.size()+ " last key= "+resultList.get(resultList.size()-1).getKey());

                    // Create a reversed list to add messages to the beginning of totalItemsList
                    List<Customer> reversedList = new ArrayList<>(resultList);
                    Collections.reverse(reversedList);
                    for (int i = 0; i < reversedList.size(); i++) {
                        // Add results to totalItemsList ArrayList to be used to get the initial key position
                        totalItemsList.add(0, reversedList.get(i));
                    }
                    // Get TotalItems logs
                    printTotalItems();
                    //printSeenItems();
                }
            } else {
                // no data
                Log.w(TAG, "mama getBefore no users exist");
            }
            printListeners();
            isBeforeFirstLoaded =  false;
            Log.d(TAG, "end isBeforeFirstLoaded = "+ isBeforeFirstLoaded);
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            // Getting Post failed, log a message
            Log.w(TAG, "mama getMessagesBefore:onCancelled", databaseError.toException());
        }
    };

    //private Query getMessagesQuery;

    public CustomersRepository(String placeKey, String queueKey, @NonNull DataSource.InvalidatedCallback onInvalidatedCallback){
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        // use received placeKey and queueKey to create a database ref
        Log.d(TAG, "CustomersRepository: placeId= "+ placeKey+ " queueId= "+ queueKey);
        mCustomersRef = mDatabaseRef.child("customers").child(placeKey).child(queueKey);

        //Get current logged in user
        mFirebaseCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = mFirebaseCurrentUser!= null ? mFirebaseCurrentUser.getUid() : null;

        // call back to invalidate data
        this.invalidatedCallback = onInvalidatedCallback;
        this.mPlaceKey = placeKey;
        this.mQueueKey = queueKey;

        isInitialFirstLoaded =  true;
        isAfterFirstLoaded = true;
        isBeforeFirstLoaded = true;

        Log.d(TAG, "mama MessagesListRepository init. isInitialFirstLoaded= " + isInitialFirstLoaded+ " after= "+isAfterFirstLoaded + " before= "+isBeforeFirstLoaded);

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

        if(totalItemsList == null){
            totalItemsList = new ArrayList<>();
            Log.d(TAG, "totalItemsList is null. new ArrayList is created= " + totalItemsList.size());
        }else{
            Log.d(TAG, "totalItemsList is not null. Size= " + totalItemsList.size());
            if(totalItemsList.size() >0){
                Log.d(TAG, "totalItemsList is not null and not empty. Size= " + totalItemsList.size());
                // Clear the list of total items to start all over
                //totalItemsList.clear();
            }
        }

        /*if(seenItemsList == null){
            seenItemsList = new ArrayList<>();
            Log.d(TAG, "seenItemsList is null. new ArrayList is created= " + seenItemsList.size());
        }else{
            Log.d(TAG, "seenItemsList is not null. Size= " + seenItemsList.size());
            if(seenItemsList.size() >0){
                Log.d(TAG, "seenItemsList is not null and not empty. Size= " + seenItemsList.size());
                // Update seen messages on the database to clear seenItemsList and start over
                updateSeenMessages(chatKey);
            }
        }*/


    }

    // Set the scrolling direction and get the last visible item
    public void setScrollDirection(int scrollDirection, int lastVisibleItem) {
        Log.d(TAG, "mScrollDirection = " + scrollDirection+ " lastVisibleItem= "+ lastVisibleItem);
        mScrollDirection = scrollDirection;
        mLastVisibleItem = lastVisibleItem;
    }

    /*public static MessagesListRepository getInstance() {
        if(messagesListRepository == null) {
            //throw new AssertionError("You have to call init first");
            Log.w(TAG, "You have to call init first");
            return null;
        }else{
            return messagesListRepository;
        }
    }


    public synchronized static MessagesListRepository init(String chatKey, @NonNull DataSource.InvalidatedCallback onInvalidatedCallback) {
        if (messagesListRepository != null){

            // in my opinion this is optional, but for the purists it ensures
            // that you only ever get the same instance when you call getInstance
            //throw new AssertionError("You already initialized me");
            Log.w(TAG, "You already initialized me");
            return messagesListRepository;
        }else{
            messagesListRepository = new MessagesListRepository(chatKey, onInvalidatedCallback);
            return messagesListRepository;
        }
    }*/

    // get initial data
    public void getInitial(String initialKey, final int size,
                            @NonNull final ItemKeyedDataSource.LoadInitialCallback<Customer> callback) {

        Log.i(TAG, "getInitial initiated. initialKey= " +  initialKey);
        this.initialKey = initialKey;
        Query query;
        isInitialFirstLoaded = true;

        ValueEventListener initialListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // [START_EXCLUDE]
                Log.d(TAG, "start onDataChange. isInitialFirstLoaded = "+ isInitialFirstLoaded);

                if (!isInitialFirstLoaded){
                    // Remove post value event listener
                    removeListeners();
                    Log.d(TAG, "usersChanged Invalidated removeEventListener");
                    //isInitialFirstLoaded =  true;
                    Log.d(TAG, "onInvalidated(). isInitialFirstLoaded = "+ isInitialFirstLoaded);
                    invalidatedCallback.onInvalidated();
                    //UsersDataSource.InvalidatedCallback.class.getMethod("loadInitial", "LoadInitialParams");
                    //UsersDataSource.invalidate();
                    return;
                }

                if (dataSnapshot.exists()) {
                    List<Customer> resultList = new ArrayList<>();
                    // loop throw users value
                    for (DataSnapshot snapshot: dataSnapshot.getChildren()){
                        Customer customer = snapshot.getValue(Customer.class);
                        if (customer != null) {
                            customer.setKey(snapshot.getKey());
                        }
                        resultList.add(customer);
                        // Add results to totalItemsList ArrayList to be used to get the initial key position
                        totalItemsList.add(customer);
                        //Log.d(TAG, "mama getMessage = "+ message.getMessage()+" getSnapshotKey= " +  snapshot.getKey());
                    }

                    printTotalItems();
                    //printSeenItems();

                    if(resultList.size() != 0){
                        callback.onResult(resultList);
                        Log.d(TAG, "getInitial  List.size= " +  resultList.size()+ " last key= "+resultList.get(resultList.size()-1).getKey() + " getInitialKey= "+ getInitialKey() );
                    }
                } else {
                    // no data
                    Log.w(TAG, "getInitial no users exist");
                }
                printListeners();
                isInitialFirstLoaded =  false;
                Log.d(TAG, "end isInitialFirstLoaded = "+ isInitialFirstLoaded);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
            }
        };

        if (initialKey == null) {// if it's loaded for the first time. Key is null
            Log.d(TAG, "getInitial() initialKey is null");
            query = mCustomersRef.orderByKey()//limitToFist to start from the fist (page size) items
                    .limitToFirst(size);

        } else {// not the first load. Key is the last seen key
            Log.d(TAG, "mama getMessages initialKey= " + initialKey);
            switch (mScrollDirection){
                case REACHED_THE_BOTTOM:
                    Log.d(TAG, "query = REACHED_THE_BOTTOM");
                    query = mCustomersRef.orderByKey()
                            .limitToLast(size);
                    break;
                case REACHED_THE_TOP:
                    Log.d(TAG, "query = REACHED_THE_TOP");
                    query = mCustomersRef.orderByKey()
                            .limitToFirst(size);
                    break;
                /*case SCROLLING_UP:
                    messagesQuery = mMessagesRef.orderByKey()
                            .startAt(initialKey)
                            .limitToFirst(size);
                    break;
                case SCROLLING_DOWN:
                    messagesQuery = mMessagesRef.orderByKey()
                            .endAt(initialKey)
                            .limitToLast(size);
                    break;*/
                default:
                    if(getInitialKeyPosition() >= mLastVisibleItem){
                        // InitialKey is in the bottom, must load data from bottom to top
                        Log.d(TAG, "query = Load data from bottom to top");
                        query = mCustomersRef.orderByKey()
                                .endAt(initialKey)
                                .limitToLast(size);

                    }else{
                        // InitialKey is in the top, must load data from top to bottom
                        Log.d(TAG, "query = Load data from top to bottom");
                        query = mCustomersRef.orderByKey()
                                .startAt(initialKey)
                                .limitToFirst(size);
                    }
                    break;
            }
        }

        getInitialKeyPosition();
        // Clear the list of total items to start all over
        totalItemsList.clear();

        query.addValueEventListener(initialListener);
        mListenersList.add(new FirebaseListeners(query, initialListener));

    }

    // to get next data
    public void getAfter(final String key, final int size,
                         @NonNull final ItemKeyedDataSource.LoadCallback<Customer> callback){

        Log.i(TAG, "getAfter initiated. AfterKey= " +  key);
        isAfterFirstLoaded = true;
        //this.afterKey = key;
        Query afterQuery;

        Log.d(TAG, "mama getAfter. AfterKey= " + key);
        afterQuery = mCustomersRef.orderByKey()
                            .startAt(key)
                            .limitToFirst(size);

        afterQuery.addValueEventListener(afterListener);
        mListenersList.add(new FirebaseListeners(afterQuery, afterListener));
        //mUsersRef.addValueEventListener(usersListener);
    }

    // to get previous data
    public void getBefore(final String key, final int size,
                              @NonNull final ItemKeyedDataSource.LoadCallback<Customer> callback){

        Log.i(TAG, "getBefore initiated. BeforeKey= " +  key);

        isBeforeFirstLoaded = true;
        //this.beforeKey = key;
        Query beforeQuery;

        beforeQuery = mCustomersRef.orderByKey()
                                .endAt(key)
                                .limitToLast(size);

        beforeQuery.addValueEventListener(beforeListener);
        mListenersList.add(new FirebaseListeners(beforeQuery, beforeListener));
        //mUsersRef.addValueEventListener(usersListener);
    }

    /*public PublishSubject getUsersChangeSubject() {
        return userAdapterInvalidation;
    }*/

    // to invalidate the data whenever a change happen
   /* public void MessagesChanged(final DataSource.InvalidatedCallback InvalidatedCallback) {

        final Query query = mMessagesRef.orderByKey();

        MessagesChangesListener = new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!isInitialFirstLoaded){
                    isInitialFirstLoaded = true;
                    Log.d(TAG, "mama entireUsersList Invalidated:");
                    // Remove post value event listener
                    if (MessagesChangesListener != null) {
                        query.removeEventListener(MessagesChangesListener);
                        Log.d(TAG, "mama usersChanged Invalidated removeEventListener");
                    }
                    ((ItemKeyedDataSource.InvalidatedCallback)InvalidatedCallback).onInvalidated();
                    //UsersDataSource.InvalidatedCallback.class.getMethod("loadInitial", "LoadInitialParams");
                    //UsersDataSource.invalidate();
                }

                isInitialFirstLoaded =  false;
                if(entireUsersList.size() > 0){
                    entireUsersList.clear();
                    ((ItemKeyedDataSource.InvalidatedCallback)onInvalidatedCallback).onInvalidated();
                    Log.d(TAG, "mama entireUsersList Invalidated:");
                    return;
                }
                if (dataSnapshot.exists()) {
                    // loop throw users value
                    for (DataSnapshot userSnapshot: dataSnapshot.getChildren()){
                        entireUsersList.add(userSnapshot.getValue(User.class));
                    }
                    Log.d(TAG, "mama entireUsersList size= "+entireUsersList.size()+"dataSnapshot count= "+dataSnapshot.getChildrenCount());
                } else {
                    Log.w(TAG, "mama usersChanged no users exist");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                // ...
            }
        };

        query.addValueEventListener(MessagesChangesListener);
        //mUsersRef.addValueEventListener(eventListener);
        mListenersList.add(new FirebaseListeners(query, MessagesChangesListener));

        for (int i = 0; i < mListenersList.size(); i++) {
            Log.d(TAG, "MessagesListRepository Listeners ref= "+ mListenersList.get(i).getReference()+ " Listener= "+ mListenersList.get(i).getListener());
            Log.d(TAG, "MessagesListRepository Listeners Query= "+ mListenersList.get(i).getQuery()+ " Listener= "+ mListenersList.get(i).getListener());
            Log.d(TAG, "MessagesListRepository Listeners Query or Ref= "+ mListenersList.get(i).getQueryOrRef()+ " Listener= "+ mListenersList.get(i).getListener());
        }
    }

    public Single<List<User>> getAnimals(int count){
        return RxFirebaseDatabase.data(mUsersRef.orderByKey().limitToFirst(count)).ma

                .map {
            for ArrayValue
            User.getArrayValue(User.class);
        }
    }*/

   //removeListeners is static so it can be triggered when ViewModel is onCleared
    public void removeListeners(){

        if(null != mListenersList){
            for (int i = 0; i < mListenersList.size(); i++) {
                //Log.d(TAG, "removed Listeners ref= "+ mListenersList.get(i).getReference()+ " Listener= "+ mListenersList.get(i).getListener());
                //Log.d(TAG, "removed Listeners Query= "+ mListenersList.get(i).getQuery()+ " Listener= "+ mListenersList.get(i).getListener());
                Log.d(TAG, "removed Listeners Query or Ref= "+ mListenersList.get(i).getQueryOrRef()+ " Listener= "+ mListenersList.get(i).getListener());

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

    /*public static void updateSeenMessages(String chatId){

        // Create a map for all messages need to be updated
        Map<String, Object> updateMap = new HashMap<>();
        for (int i = 0; i < seenItemsList.size(); i++) {
            Log.d(TAG, "updateSeenMessages seenItemsList size= "+ seenItemsList.size());
            updateMap.put(seenItemsList.get(i).getKey()+"/seen", true);
        }
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference();
        DatabaseReference messagesRef =  databaseRef.child("messages").child(chatId);

        messagesRef.updateChildren(updateMap).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // onSuccess clear the list to start all over
                seenItemsList.clear();
            }
        });
    }*/

    public void printListeners(){

        for (int i = 0; i < mListenersList.size(); i++) {
            //Log.d(TAG, "Listeners ref= "+ mListenersList.get(i).getReference()+ " Listener= "+ mListenersList.get(i).getListener());
            //Log.d(TAG, "Listeners Query= "+ mListenersList.get(i).getQuery()+ " Listener= "+ mListenersList.get(i).getListener());
            Log.d(TAG, "Listeners Query or Ref= "+ mListenersList.get(i).getQueryOrRef()+ " Listener= "+ mListenersList.get(i).getListener());
        }
    }

    public void printTotalItems(){

        Log.d(TAG, "Getting totalItemsList... ");
        for (int i = 0; i < totalItemsList.size(); i++) {
            Log.d(TAG, "totalItemsList : key= "+ totalItemsList.get(i).getKey()+ " name= "+ totalItemsList.get(i).getName()+ " size= "+totalItemsList.size());
        }
    }

    /*public void printSeenItems(){
        Log.d(TAG, "Getting seenItemsList... ");
        for (int i = 0; i < seenItemsList.size(); i++) {
            Log.d(TAG, "seenItemsList : key= "+ seenItemsList.get(i).getKey()+ " message= "+ seenItemsList.get(i).getMessage()+ " senderId = "+seenItemsList.get(i).getSenderId() + " size= "+seenItemsList.size());
        }
    }*/

    public int getInitialKeyPosition(){

        Log.d(TAG, "Getting get InitialKeyPosition... ");
        int Position = 0;
        for (int i = 0; i < totalItemsList.size(); i++) {
            if(totalItemsList.get(i).getKey().equals(getInitialKey())){
                Log.d(TAG, "InitialKeyPosition: key= "+ getInitialKey()+" Position= " +Position);
                return Position;
            }else{
                Position++;
            }
        }
        return Position;
    }

    public ItemKeyedDataSource.LoadInitialCallback getLoadInitialCallback() {
        return loadInitialCallback;
    }

    public void setLoadInitialCallback(ItemKeyedDataSource.LoadInitialCallback loadInitialCallback) {
        this.loadInitialCallback = loadInitialCallback;
    }

    public ItemKeyedDataSource.LoadCallback getLoadAfterCallback() {
        return loadAfterCallback;
    }

    public void setLoadAfterCallback(String key, ItemKeyedDataSource.LoadCallback loadAfterCallback) {
        this.loadAfterCallback = loadAfterCallback;
        this.afterKey = key;
    }

    public ItemKeyedDataSource.LoadCallback getLoadBeforeCallback() {
        return loadBeforeCallback;
    }

    public void setLoadBeforeCallback(String key, ItemKeyedDataSource.LoadCallback loadBeforeCallback) {
        this.loadBeforeCallback = loadBeforeCallback;
        this.beforeKey = key;
    }

    public String getLoadAfterKey() {
        return afterKey;
    }

    public String getLoadBeforeKey() {
        return beforeKey;
    }

    public String getInitialKey() {
        return initialKey;
    }

    /*public void setInitialKey(String initialKey) {
        this.initialKey = initialKey;
    }*/

    // When last database message is not loaded, Invalidate messagesDataSource to scroll down
    public void invalidateData() {
        invalidatedCallback.onInvalidated();
    }

    public void updateBrokenAvatars(List<Customer> brokenAvatarsList, final FirebaseOnCompleteCallback callback) {
        // Create a map for all messages need to be updated
        Map<String, Object> updateMap = new HashMap<>();

        // We use userId to store the placeId value, not good for logic but it's a quick work around
        for (int i = 0; i < brokenAvatarsList.size(); i++) {
            Log.d(TAG, "brokenAvatarsList url= " + brokenAvatarsList.get(i).getAvatar() + " key= " + brokenAvatarsList.get(i).getKey() + "name= " + brokenAvatarsList.get(i).getName());
            updateMap.put(brokenAvatarsList.get(i).getKey() +"/avatar", brokenAvatarsList.get(i).getAvatar());
        }

        if (updateMap.size() > 0 ) {
            // update customer's Avatars to the new urls
            mCustomersRef.updateChildren(updateMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    callback.onCallback(task); // A call back to clear the broken avatar's list when it's updated successfully
                }
            });
        }
    }
}
