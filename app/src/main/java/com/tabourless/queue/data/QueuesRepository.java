package com.tabourless.queue.data;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.paging.DataSource;
import androidx.paging.ItemKeyedDataSource;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.tabourless.queue.models.FirebaseListeners;
import com.tabourless.queue.models.UserQueue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueuesRepository {

    private final static String TAG = QueuesRepository.class.getSimpleName();

    // [START declare_database_ref]
    private DatabaseReference mDatabaseRef;
    private DatabaseReference mCurrentUserQueuesRef;
    private Boolean isFirstLoaded = true;
    //public ValueEventListener ChatsChangesListener;
    public ValueEventListener initialListener;

    // Not static to only remove listeners of this repository instance
    // Start destination fragment is never destroyed , so when clicking on it's bottom navigation icon again it got destroyed to be recreated
    // When that happens clearing listeners is triggered on viewmodel Cleared, which removes that new listeners for the just added query
    // When new listener is removed we got 0 results and have no listeners for updates.
    private List<FirebaseListeners> mListenersList;

    // Not static have a new list of each repository instance
    private List<UserQueue> totalItemsList;// = new ArrayList<>();

    private DataSource.InvalidatedCallback invalidatedCallback;
    private ItemKeyedDataSource.LoadInitialCallback loadInitialCallback;
    private ItemKeyedDataSource.LoadCallback loadAfterCallback;
    private ItemKeyedDataSource.LoadCallback loadBeforeCallback;

    private static volatile Boolean isInitialFirstLoaded;// = true;
    private static volatile Boolean isAfterFirstLoaded;// = true;
    private static volatile Boolean isBeforeFirstLoaded;// = true;
    private static volatile Boolean isInitialKey;

    private Long initialKey;
    private Long afterKey;
    private Long beforeKey;

    private static final int REACHED_THE_TOP = 2;
    private static final int SCROLLING_UP = 1;
    private static final int SCROLLING_DOWN = -1;
    private static final int REACHED_THE_BOTTOM = -2;
    private static int mScrollDirection;
    private static int mVisibleItem;

    // A listener for chat changes
    private ValueEventListener afterListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // [START_EXCLUDE]
            Log.d(TAG, "start onDataChange isAfterFirstLoaded = "+ isAfterFirstLoaded);
            if (!isAfterFirstLoaded){
                // Remove post value event listener
                Log.d(TAG, "getAfter Invalidated removeEventListener");
                removeListeners();
                //isAfterFirstLoaded =  true;
                Log.d(TAG, "getAfter onInvalidated(). isAfterFirstLoaded = "+ isAfterFirstLoaded);
                invalidatedCallback.onInvalidated();
                //UsersDataSource.InvalidatedCallback.class.getMethod("loadInitial", "LoadInitialParams");
                //UsersDataSource.invalidate();
                return;
            }

            if (dataSnapshot.exists()) {
                List<UserQueue> resultList = new ArrayList<>();
                // loop throw users value
                for (DataSnapshot snapshot: dataSnapshot.getChildren()){
                    UserQueue userqueue = snapshot.getValue(UserQueue.class);
                    if (userqueue != null) {
                        userqueue.setKey(snapshot.getKey());
                        if(getLoadAfterKey()!= userqueue.getJoinedLong()) { // if snapshot key = startAt key? don't add it again
                            resultList.add(userqueue);
                        }
                    }
                }

                if(resultList.size() != 0){
                    //callback.onResult(messagesList);
                    Log.d(TAG, "mama getAfter  List.size= " +  resultList.size()+ " last key= "+resultList.get(resultList.size()-1).getKey());
                    Collections.reverse(resultList);
                    getLoadAfterCallback().onResult(resultList);

                    // Create a reversed list to add results to the beginning of totalItemsList
                    List<UserQueue> reversedList = new ArrayList<>(resultList);
                    Collections.reverse(reversedList);
                    for (int i = 0; i < reversedList.size(); i++) {
                        // Add results to totalItemsList ArrayList to be used to get the initial key position
                        totalItemsList.add(0, reversedList.get(i));
                    }
                    //totalItemsList.addAll( 0, reversedList);
                    // Get TotalItems logs
                    printTotalItems("After");
                    /*for (int i = 0; i < reversedList.size(); i++) {
                        Log.d(TAG, "After totalItemsList : key= "+ chatsList.get(i).getKey()+ " message= "+ chatsList.get(i).getLastMessage()+ " size= "+chatsList.size());
                    }*/
                }
            } else {
                // no data
                Log.w(TAG, "mama getAfter no result exist");
            }
            printListeners();
            isAfterFirstLoaded =  false;
            Log.d(TAG, "end isAfterFirstLoaded = "+ isAfterFirstLoaded);
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            // Getting Post failed, log a message
            Log.w(TAG, "mama getAfter loadPost:onCancelled", databaseError.toException());
        }
    };


    private ValueEventListener beforeListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // [START_EXCLUDE]
            Log.d(TAG, "start onDataChange isBeforeFirstLoaded = "+ isBeforeFirstLoaded);
            if (!isBeforeFirstLoaded){
                // Remove post value event listener
                Log.d(TAG, "mama getBefore Invalidated removeEventListener");
                removeListeners();
                //isBeforeFirstLoaded =  true;
                Log.d(TAG, "getBefore onInvalidated(). isBeforeFirstLoaded = "+ isBeforeFirstLoaded);
                invalidatedCallback.onInvalidated();
                //UsersDataSource.InvalidatedCallback.class.getMethod("loadInitial", "LoadInitialParams");
                //UsersDataSource.invalidate();
                return;
            }

            if (dataSnapshot.exists()) {
                List<UserQueue> resultList = new ArrayList<>();
                // loop throw users value
                for (DataSnapshot snapshot: dataSnapshot.getChildren()){
                    UserQueue userQueue = snapshot.getValue(UserQueue.class);
                    if (userQueue != null) {
                        userQueue.setKey(snapshot.getKey());
                        if(getLoadBeforeKey()!= userQueue.getJoinedLong()) { // if snapshot key = startAt key? don't add it again
                            resultList.add(userQueue);
                        }
                    }
                }

                if(resultList.size() != 0){
                    //callback.onResult(messagesList);
                    Log.d(TAG, "mama getBefore  List.size= " +  resultList.size()+ " last key= "+resultList.get(resultList.size()-1).getKey());
                    Collections.reverse(resultList);
                    getLoadBeforeCallback().onResult(resultList);
                    totalItemsList.addAll(resultList); // add items to totalItems ArrayList to be used to get the initial key position

                    /*for (int i = 0; i < chatsList.size(); i++) {
                        Log.d(TAG, "before totalItemsList : key= "+ chatsList.get(i).getKey()+ " message= "+ chatsList.get(i).getLastMessage()+ " size= "+chatsList.size());
                    }*/
                    // Get TotalItems logs
                    printTotalItems("Before");
                }
            } else {
                // no data
                Log.w(TAG, "mama getBefore no result exist");
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


    public QueuesRepository(String userKey, @NonNull DataSource.InvalidatedCallback onInvalidatedCallback){
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        // use received chatKey to create a database ref
        mCurrentUserQueuesRef = mDatabaseRef.child("userQueues").child(userKey);
        isFirstLoaded = true;
        Log.d(TAG, "mDatabaseRef init");
        // call back to invalidate data
        this.invalidatedCallback = onInvalidatedCallback;

        isInitialFirstLoaded =  true;
        isAfterFirstLoaded = true;
        isBeforeFirstLoaded = true;

        Log.d(TAG, "mama mDatabaseRef init. isInitialFirstLoaded= " + isInitialFirstLoaded+ " after= "+isAfterFirstLoaded + " before= "+isBeforeFirstLoaded);

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

    }

    // Set the scrolling direction and get the last/first visible item
    public void setScrollDirection(int scrollDirection, int visibleItem) {
        mScrollDirection = scrollDirection;
        mVisibleItem = visibleItem;
        Log.d(TAG, "mScrollDirection = " + mScrollDirection+ " VisibleItem= "+ mVisibleItem);

    }

    // get initial data
    public void getInitial(Long initialKey, final int size,
                           @NonNull final ItemKeyedDataSource.LoadInitialCallback<UserQueue> callback){

        this.initialKey = initialKey;
        Query queuesQuery;
        isInitialFirstLoaded = true;

        initialListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // [START_EXCLUDE]
                    Log.d(TAG, "start onDataChange. isInitialFirstLoaded = " + isInitialFirstLoaded);

                    if (!isInitialFirstLoaded) {
                        // Remove post value event listener
                        Log.d(TAG, "getInitial Invalidated removeEventListener");
                        removeListeners();
                        //isInitialFirstLoaded =  true;
                        Log.d(TAG, "onInvalidated(). isInitialFirstLoaded = " + isInitialFirstLoaded);
                        invalidatedCallback.onInvalidated();
                        //UsersDataSource.InvalidatedCallback.class.getMethod("loadInitial", "LoadInitialParams");
                        //UsersDataSource.invalidate();
                        return;
                    }

                    if (dataSnapshot.exists()) {
                        // loop throw users value
                        List<UserQueue> resultList = new ArrayList<>();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            UserQueue userQueue = snapshot.getValue(UserQueue.class);
                            if (userQueue != null) {
                                userQueue.setKey(snapshot.getKey());
                            }

                            resultList.add(userQueue);
                            //Log.d(TAG, "mama getItems = " + chat.getLastMessage() + " getSnapshotKey= " + snapshot.getKey());

                        }

                        if (resultList.size() != 0) {
                            Collections.reverse(resultList);
                            callback.onResult(resultList);

                            // Add chats to totalItemsList ArrayList to be used to get the initial key position
                            totalItemsList.addAll(resultList);
                            printTotalItems("Initial");
                            Log.d(TAG, "mama getMessages  List.size= " + resultList.size() + " last key= " + resultList.get(resultList.size() - 1).getKey());
                        }

                    } else {
                        // No data exist
                        Log.w(TAG, "isInitialKey. getItems no results exist");
                        // It might failed because the initial key is changed and there is no data above it.
                        // Try to get any data regardless of the initial key
                        Log.d(TAG, "isInitialKey. Try to get any data regardless of the initial key "+ isInitialKey);
                        if(isInitialKey){
                            // If no data and we are doing a query with Initial Key, try another query without it
                            isInitialKey = false; // Make isInitialKey boolean false so that we don't loop forever
                            Query query = mCurrentUserQueuesRef
                                    .orderByChild("joined")//limitToLast to start from the last (page size) items
                                    .limitToLast(size);

                            Log.d(TAG, "isInitialKey. initialListener is added to Query without InitialKey "+ isInitialKey);
                            query.addValueEventListener(initialListener);
                            mListenersList.add(new FirebaseListeners(query, initialListener));
                        }
                    }

                    printListeners();
                    isInitialFirstLoaded =  false;
                    Log.d(TAG, "end isInitialFirstLoaded = "+ isInitialFirstLoaded);
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Getting Post failed, log a message
                    Log.w(TAG, "mama loadPost:onCancelled", databaseError.toException());
                }
            };

        if (initialKey == null) {// if it's loaded for the first time. Key is null
            Log.d(TAG, "mama getChats initialKey= " + initialKey);
            isInitialKey = false;
            queuesQuery = mCurrentUserQueuesRef
                    .orderByChild("joined")//limitToLast to start from the last (page size) items
                    .limitToLast(size);


        } else {// not the first load. Key is the last seen key
            Log.d(TAG, "mama getChats initialKey= " + initialKey);
            isInitialKey = true;
            switch (mScrollDirection){
                // No need to detected reaching to bottom
                /*case REACHED_THE_BOTTOM:
                    Log.d(TAG, "messages query = REACHED_THE_BOTTOM. ScrollDirection= "+mScrollDirection+ " mVisibleItem= "+ mVisibleItem + " totalItemsList size= "+totalItemsList.size());
                    chatsQuery = mChatsRef
                            .orderByChild("joined")//limitToLast to start from the last (page size) items
                            .limitToFirst(size);
                    break;*/
                case REACHED_THE_TOP:
                    Log.d(TAG, "messages query = REACHED_THE_TOP. ScrollDirection= "+mScrollDirection+ " mVisibleItem= "+ mVisibleItem + " totalItemsList size= "+totalItemsList.size());
                    queuesQuery = mCurrentUserQueuesRef
                            .orderByChild("joined")//limitToLast to start from the last (page size) items
                            .limitToLast(size);
                    break;
                case SCROLLING_UP:
                    /*Log.d(TAG, "messages query = Load data from top to bottom (above InitialKey cause list is reversed). ScrollDirection= "+mScrollDirection+ " InitialKey Position= "+getInitialKeyPosition() +" mVisibleItem= "+mVisibleItem+ " Item Message= "+ totalItemsList.get(mVisibleItem).getLastMessage() +" totalItemsList size= "+totalItemsList.size());
                    chatsQuery = mChatsRef
                            .orderByChild("joined")//limitToLast to start from the last (page size) items
                            .endAt(initialKey)
                            .limitToLast(size);*/

                    // list is reversed, smaller Keys are on bottom
                    // InitialKey is in the top, must load data from top to bottom
                    // list is reversed, load data above InitialKey
                    Log.d(TAG, "messages query = Load data from top to bottom (above InitialKey cause list is reversed). ScrollDirection= "+mScrollDirection+ " InitialKey Position= "+getInitialKeyPosition() +" first VisibleItem= "+ mVisibleItem +" totalItemsList size= "+totalItemsList.size());
                    queuesQuery = mCurrentUserQueuesRef
                            .orderByChild("joined")
                            .endAt(getItem(mVisibleItem).getJoinedLong())//Using first visible item key instead of initial key
                            .limitToLast(size);
                    break;
                case SCROLLING_DOWN:
                    /*Log.d(TAG, "messages query = Load data from bottom to top (below InitialKey cause list is reversed). ScrollDirection= "+mScrollDirection+ " InitialKey Position= "+getInitialKeyPosition() +" mVisibleItem= "+mVisibleItem+ " Item Message= "+ totalItemsList.get(mVisibleItem).getLastMessage() +" totalItemsList size= "+totalItemsList.size());
                    chatsQuery = mChatsRef
                            .orderByChild("joined")//limitToLast to start from the last (page size) items
                            .startAt(initialKey)
                            .limitToFirst(size);*/
                    // InitialKey is in the bottom, must load data from bottom to top
                    // list is reversed, load data below InitialKey
                    Log.d(TAG, "messages query = Load data from bottom to top (below InitialKey cause list is reversed). ScrollDirection= "+mScrollDirection+ " InitialKey Position= "+getInitialKeyPosition() +"  last VisibleItem= "+ mVisibleItem + " Item Message= "+" totalItemsList size= "+totalItemsList.size());
                    queuesQuery = mCurrentUserQueuesRef
                            .orderByChild("joined")
                            .startAt(getItem(mVisibleItem).getJoinedLong())//Using last visible item key instead of initial key
                            .limitToFirst(size);
                    break;
                default:
                   /*// list is reversed, greater Keys are on top
                    if(getInitialKeyPosition() >= mVisibleItem){
                        // InitialKey is in the bottom, must load data from bottom to top
                        // list is reversed, load data below InitialKey
                        Log.d(TAG, "messages query = Load data from bottom to top (below InitialKey cause list is reversed). ScrollDirection= "+mScrollDirection+ " InitialKey Position= "+getInitialKeyPosition() +" mVisibleItem= "+ mVisibleItem + " totalItemsList size= "+totalItemsList.size());
                        chatsQuery = mChatsRef
                                .orderByChild("joined")//limitToLast to start from the last (page size) items
                                .startAt(initialKey)
                                .limitToFirst(size);

                    }else{
                        // list is reversed, smaller Keys are on bottom
                        // InitialKey is in the top, must load data from top to bottom
                        // list is reversed, load data above InitialKey
                        Log.d(TAG, "messages query = Load data from top to bottom (above InitialKey cause list is reversed). ScrollDirection= "+mScrollDirection+ " InitialKey Position= "+getInitialKeyPosition() +" mVisibleItem= "+ mVisibleItem + " totalItemsList size= "+totalItemsList.size());
                        chatsQuery = mChatsRef
                                .orderByChild("joined")//limitToLast to start from the last (page size) items
                                .endAt(initialKey)
                                .limitToLast(size);
                    }
                    break;*/
                    Log.d(TAG, "messages query = default. ScrollDirection= "+mScrollDirection+ " mVisibleItem= "+ mVisibleItem + " totalItemsList size= "+totalItemsList.size());
                    queuesQuery = mCurrentUserQueuesRef
                            .orderByChild("joined")//limitToLast to start from the last (page size) items
                            .limitToLast(size);
            }
        }
        // Clear the list of total items to start all over
        totalItemsList.clear();
        Log.d(TAG, "messages query = totalItemsList is cleared");

        queuesQuery.addValueEventListener(initialListener);
        mListenersList.add(new FirebaseListeners(queuesQuery, initialListener));
        //mUsersRef.addValueEventListener(usersListener);

    }

    // to get next data
    public void getAfter(final Long key, final int size,
                         @NonNull final ItemKeyedDataSource.LoadCallback<UserQueue> callback){
        /*if(key == entireUsersList.get(entireUsersList.size()-1).getCreatedLong()){
            Log.d(TAG, "mama getUsersAfter init. afterKey= " +  key+ "entireUsersList= "+entireUsersList.get(entireUsersList.size()-1).getCreatedLong());
            return;
        }*/
        Log.d(TAG, "mama getAfter. AfterKey= " + key);

        isAfterFirstLoaded = true;
        //this.afterKey = key;
        Query afterQuery;

        afterQuery = mCurrentUserQueuesRef
                .orderByChild("joined")
                .startAt(key)
                .limitToFirst(size);

        afterQuery.addValueEventListener(afterListener);
        mListenersList.add(new FirebaseListeners(afterQuery, afterListener));
        //mUsersRef.addValueEventListener(usersListener);
    }

    // to get previous data
    public void getBefore(final Long key, final int size,
                          @NonNull final ItemKeyedDataSource.LoadCallback<UserQueue> callback){
        Log.d(TAG, "mama getBefore. BeforeKey= " +  key);
        /*if(key == entireUsersList.get(0).getCreatedLong()){
            return;
        }*/
        isBeforeFirstLoaded = true;
        //this.beforeKey = key;
        Query beforeQuery;

        beforeQuery = mCurrentUserQueuesRef
                .orderByChild("joined")
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
    /*public void ChatsChanged(final DataSource.InvalidatedCallback InvalidatedCallback) {

        final Query query = mChatsRef.orderByChild("joined");

        ChatsChangesListener = new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!isFirstLoaded){
                    isFirstLoaded = true;
                    Log.d(TAG, "mama entireUsersList Invalidated:");
                    // Remove post value event listener
                    if (ChatsChangesListener != null) {
                        query.removeEventListener(ChatsChangesListener);
                        Log.d(TAG, "mama usersChanged Invalidated removeEventListener");
                    }
                    ((ItemKeyedDataSource.InvalidatedCallback)InvalidatedCallback).onInvalidated();
                    //UsersDataSource.InvalidatedCallback.class.getMethod("loadInitial", "LoadInitialParams");
                    //UsersDataSource.invalidate();
                }

                isFirstLoaded =  false;
                *//*if(entireUsersList.size() > 0){
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
                }*//*
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                // ...
            }
        };
        query.addValueEventListener(ChatsChangesListener);
        //mUsersRef.addValueEventListener(eventListener);
    }*/

    /*public Single<List<User>> getAnimals(int count){
        return RxFirebaseDatabase.data(mUsersRef.orderByKey().limitToFirst(count)).ma

                .map {
            for ArrayValue
            User.getArrayValue(User.class);
        }
    }*/

    public void removeQueue(final String userId, final UserQueue deletedQueue) {

        // Query to get customer key to delete his/her booking
        final DatabaseReference currentCustomerRef = mDatabaseRef.child("customers").child(deletedQueue.getPlaceId()).child(deletedQueue.getKey());
        Query query = currentCustomerRef.orderByChild("userId").equalTo(userId).limitToFirst(1);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String, Object> childUpdates = new HashMap<>();
                if(dataSnapshot.exists()){
                    // loop throw all found results. We should only allow one booking per user anyway
                    String customerKey = "";
                    for (DataSnapshot snapshot: dataSnapshot.getChildren()){
                        customerKey = snapshot.getKey();
                    }

                    if(!TextUtils.isEmpty(customerKey)){
                        childUpdates.put("/customers/" + deletedQueue.getPlaceId() + "/" + deletedQueue.getKey() + "/" + customerKey, null);
                        childUpdates.put("/userQueues/" + userId + "/" + deletedQueue.getKey(), null);
                        // update Data base
                        mDatabaseRef.updateChildren(childUpdates);
                    }
                }else{
                    // can't find the customer in this queue, probably it's an inactive queue and the booking was canceled already
                    childUpdates.put("/userQueues/" + userId + "/" + deletedQueue.getKey(), null);
                    // update Data base
                    mDatabaseRef.updateChildren(childUpdates);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

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

    public void printListeners(){

        for (int i = 0; i < mListenersList.size(); i++) {
            //Log.d(TAG, "Listeners ref= "+ mListenersList.get(i).getReference()+ " Listener= "+ mListenersList.get(i).getListener());
            //Log.d(TAG, "Listeners Query= "+ mListenersList.get(i).getQuery()+ " Listener= "+ mListenersList.get(i).getListener());
            Log.d(TAG, "Listeners Query or Ref= "+ mListenersList.get(i).getQueryOrRef()+ " Listener= "+ mListenersList.get(i).getListener());
        }
    }

    public void printTotalItems(String type){
        Log.d(TAG, "Getting totalItemsList... Type"+ type );
        for (int i = 0; i < totalItemsList.size(); i++) {
            Log.d(TAG, "totalItemsList : key= "+ totalItemsList.get(i).getKey()+ " size= "+totalItemsList.size());
        }
    }

    // get the position of initial key
    public int getInitialKeyPosition(){
        Log.d(TAG, "Getting get InitialKeyPosition... getInitialKey()= "+getInitialKey()+ " totalItemsList size= "+totalItemsList.size());
        int Position = 0;
        for (int i = 0; i < totalItemsList.size(); i++) {
            if(totalItemsList.get(i).getJoinedLong() == getInitialKey()){
                Log.d(TAG, "messages query InitialKeyPosition: key= "+ getInitialKey()+ " Position= " +Position);
                return Position;
            }else{
                Position++;
            }
        }
        return Position;
    }

    // get item and item's key from adapter position
    public UserQueue getItem(int position){
        //Log.d(TAG, "Getting getItem... getInitialKey()= "+getInitialKey()+ " item message= "+ totalItemsList.get(position).getLastMessage());
        return totalItemsList.get(position);
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

    public void setLoadAfterCallback(Long key, ItemKeyedDataSource.LoadCallback loadAfterCallback) {
        this.loadAfterCallback = loadAfterCallback;
        this.afterKey = key;
    }

    public ItemKeyedDataSource.LoadCallback getLoadBeforeCallback() {
        return loadBeforeCallback;
    }

    public void setLoadBeforeCallback(Long key, ItemKeyedDataSource.LoadCallback loadBeforeCallback) {
        this.loadBeforeCallback = loadBeforeCallback;
        this.beforeKey = key;
    }

    public Long getLoadAfterKey() {
        return afterKey;
    }

    public Long getLoadBeforeKey() {
        return beforeKey;
    }

    public Long getInitialKey() {
        return initialKey;
    }

}
