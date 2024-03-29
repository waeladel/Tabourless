package com.tabourless.queue.data;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.DataSource;
import androidx.paging.ItemKeyedDataSource;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.tabourless.queue.models.DatabaseNotification;
import com.tabourless.queue.models.FirebaseListeners;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationsRepository {

    private final static String TAG = NotificationsRepository.class.getSimpleName();

    // [START declare_database_ref]
    private DatabaseReference mDatabaseRef;
    private DatabaseReference mNotificationsRef;
    private Boolean isFirstLoaded = true;
    //public ValueEventListener ChatsChangesListener;
    public ValueEventListener initialListener;

    private static List<FirebaseListeners> mListenersList;// = new ArrayList<>();
    private MutableLiveData<DatabaseNotification> mNotification;
    private static List<DatabaseNotification> totalItemsList;// = new ArrayList<>();

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

    private boolean isSeeing;

    private static Map<String, Object> updateSeenMap;

    // A listener for chat changes
    private ValueEventListener afterListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // [START_EXCLUDE]
            Log.d(TAG, "start onDataChange isAfterFirstLoaded = "+ isAfterFirstLoaded);
            if (!isAfterFirstLoaded){
                // Remove post value event listener
                removeListeners();
                Log.d(TAG, "mama getAfter Invalidated removeEventListener");
                //isAfterFirstLoaded =  true;
                Log.d(TAG, "getAfter onInvalidated(). isAfterFirstLoaded = "+ isAfterFirstLoaded);
                invalidatedCallback.onInvalidated();
                //UsersDataSource.InvalidatedCallback.class.getMethod("loadInitial", "LoadInitialParams");
                //UsersDataSource.invalidate();
                return;
            }

            if (dataSnapshot.exists()) {
                List<DatabaseNotification> list = new ArrayList<>();

                // loop throw users value
                for (DataSnapshot snapshot: dataSnapshot.getChildren()){
                    DatabaseNotification notification = snapshot.getValue(DatabaseNotification.class);
                    if (notification != null) {
                        notification.setKey(snapshot.getKey());
                        if(getLoadAfterKey()!= notification.getSentLong()) { // if snapshot key = startAt key? don't add it again
                            list.add(notification);

                            // If is not seen, update seen to true and add it to updateSeenMap
                            Log.d(TAG, "afterListener: isSeeing= "+ isSeeing);
                            if(!notification.isSeen()){
                                Log.d(TAG, "afterListener: set notifications Seen to true: notification key= "+ notification.getKey());
                                // add notification to the map that holds all seen notifications need to be updated
                                updateSeenMap.put(snapshot.getKey()+"/seen", true);
                                notification.setSeen(true);
                            }

                        }
                    }
                }

                Log.d(TAG, "afterListener: SeenMap size= "+ updateSeenMap.size() + " isSeeing= "+ isSeeing);
                // We already push updateSeenMap to the database when fragment stops and resume but we need to Update seen notifications hear too
                // if user is currently isSeeing, just in case an invalidate happens while the user is opening notifications tap
                if(updateSeenMap.size() > 0 && isSeeing){
                    Log.d(TAG, "afterListener: Updating seen notifications. updateSeenMap size= "+ updateSeenMap.size() + " isSeeing= "+ isSeeing);
                    //Update seen notifications
                    mNotificationsRef.updateChildren(updateSeenMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Log.d(TAG, "afterListener updateSeenMap onSuccess: clearing updateSeenMap after pushing seeing updateSeenMap do database. size before clear= "+ updateSeenMap.size() + " isSeeing= "+ isSeeing);
                            updateSeenMap.clear();
                        }
                    });
                }

                if(list.size() != 0){
                    //callback.onResult(messagesList);
                    Log.d(TAG, "mama getAfter  List.size= " +  list.size()+ " last key= "+list.get(list.size()-1).getKey());
                    Collections.reverse(list);
                    getLoadAfterCallback().onResult(list);

                    // Create a reversed list to add messages to the beginning of totalItemsList
                    List<DatabaseNotification> reversedList = new ArrayList<>(list);
                    Collections.reverse(reversedList);
                    for (int i = 0; i < reversedList.size(); i++) {
                        // Add messages to totalItemsList ArrayList to be used to get the initial key position
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
                Log.w(TAG, "mama getAfter no users exist");
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
                removeListeners();
                Log.d(TAG, "mama getBefore Invalidated removeEventListener");
                //isBeforeFirstLoaded =  true;
                Log.d(TAG, "getBefore onInvalidated(). isBeforeFirstLoaded = "+ isBeforeFirstLoaded);
                invalidatedCallback.onInvalidated();
                //UsersDataSource.InvalidatedCallback.class.getMethod("loadInitial", "LoadInitialParams");
                //UsersDataSource.invalidate();
                return;
            }

            if (dataSnapshot.exists()) {
                List<DatabaseNotification> list = new ArrayList<>();

                // loop throw users value
                for (DataSnapshot snapshot: dataSnapshot.getChildren()){
                    DatabaseNotification notification = snapshot.getValue(DatabaseNotification.class);
                    if (notification != null) {
                        notification.setKey(snapshot.getKey());
                        if(getLoadBeforeKey()!= notification.getSentLong()) { // if snapshot key = startAt key? don't add it again
                            list.add(notification);

                            // If is not seen, update seen to true and add it to updateSeenMap
                            Log.d(TAG, "beforeListener: isSeeing= "+ isSeeing);
                            if(!notification.isSeen()){
                                Log.d(TAG, "beforeListener: set notifications Seen to true: notification key= "+ notification.getKey());
                                // add notification to the map that holds all seen notifications need to be updated
                                updateSeenMap.put(snapshot.getKey()+"/seen", true);
                                notification.setSeen(true);
                            }

                        }
                    }
                }

                Log.d(TAG, "beforeListener: SeenMap size= "+ updateSeenMap.size() + " isSeeing= "+ isSeeing);
                // We already push updateSeenMap to the database when fragment stops and resume but we need to Update seen notifications hear too
                // if user is currently isSeeing, just in case an invalidate happens while the user is opening notifications tap
                if(updateSeenMap.size() > 0 && isSeeing){
                    Log.d(TAG, "beforeListener: Updating seen notifications. updateSeenMap size= "+ updateSeenMap.size() + " isSeeing= "+ isSeeing);
                    //Update seen notifications
                    mNotificationsRef.updateChildren(updateSeenMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Log.d(TAG, "beforeListener updateSeenMap onSuccess: clearing updateSeenMap after pushing seeing updateSeenMap do database. size before clear= "+ updateSeenMap.size() + " isSeeing= "+ isSeeing);
                            updateSeenMap.clear();
                        }
                    });
                }

                if(list.size() != 0){
                    //callback.onResult(messagesList);
                    Log.d(TAG, "mama getBefore  List.size= " +  list.size()+ " last key= "+list.get(list.size()-1).getKey());
                    Collections.reverse(list);
                    getLoadBeforeCallback().onResult(list);
                    totalItemsList.addAll(list); // add items to totalItems ArrayList to be used to get the initial key position

                    /*for (int i = 0; i < chatsList.size(); i++) {
                        Log.d(TAG, "before totalItemsList : key= "+ chatsList.get(i).getKey()+ " message= "+ chatsList.get(i).getLastMessage()+ " size= "+chatsList.size());
                    }*/
                    // Get TotalItems logs
                    printTotalItems("Before");
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


    public NotificationsRepository(String userKey, boolean seeing, @NonNull DataSource.InvalidatedCallback onInvalidatedCallback){
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        // use received chatKey to create a database ref

        mNotificationsRef = mDatabaseRef.child("notifications").child("alerts").child(userKey);
        isFirstLoaded = true;
        Log.d(TAG, "mama mDatabaseRef init. isFirstLoaded is true. userKey= "+userKey);
        // call back to invalidate data
        this.invalidatedCallback = onInvalidatedCallback;

        isInitialFirstLoaded =  true;
        isAfterFirstLoaded = true;
        isBeforeFirstLoaded = true;

        // When we first load isSeeing is true to update seeing fields in fetched notifications to true but then
        // isSeeing should be updated from notification fragment based on it's onResume and OnStop
        // if fragment stopped it should be false so we don't update seen field, when its resumed it should be true to update seen field
        isSeeing = seeing;

        // to hold all notifications that their seen field need to be updated
        updateSeenMap = new HashMap<>();

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

    // To only update notification's seen when user is opening the notification's tap
    public void setSeeing (boolean seeing) {
        isSeeing = seeing;
        Log.d(TAG, "setSeeing function called. isSeeing: "+ isSeeing);
        // Update seen notifications
        if(updateSeenMap.size() > 0){
            Log.d(TAG, "setSeeing function called. updating updateSeenMap because it's size is > 0");
            //Update seen notifications
            mNotificationsRef.updateChildren(updateSeenMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void unused) {
                    Log.d(TAG, "setSeeing function called. updateSeenMap onSuccess. clearing updateSeenMap after pushing seeing updateSeenMap do database. size before clear= "+ updateSeenMap.size() + " isSeeing= "+ isSeeing);
                    updateSeenMap.clear();
                }
            });
        }
    }

    // get initial data
    public void getItems(Long initialKey, final int size,
                         @NonNull final ItemKeyedDataSource.LoadInitialCallback<DatabaseNotification> callback){

        this.initialKey = initialKey;
        Query query;
        isInitialFirstLoaded = true;

        initialListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // [START_EXCLUDE]
                    Log.d(TAG, "start onDataChange. isInitialFirstLoaded = " + isInitialFirstLoaded);

                    if (!isInitialFirstLoaded) {
                        // Remove post value event listener
                        removeListeners();
                        Log.d(TAG, "mama chatsChanged Invalidated removeEventListener");
                        //isInitialFirstLoaded =  true;
                        Log.d(TAG, "onInvalidated(). isInitialFirstLoaded = " + isInitialFirstLoaded);
                        invalidatedCallback.onInvalidated();
                        //UsersDataSource.InvalidatedCallback.class.getMethod("loadInitial", "LoadInitialParams");
                        //UsersDataSource.invalidate();
                        return;
                    }

                    if (dataSnapshot.exists()) {
                        // loop throw users value
                        List<DatabaseNotification> list = new ArrayList<>();

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            DatabaseNotification notification = snapshot.getValue(DatabaseNotification.class);
                            if (notification != null) {
                                notification.setKey(snapshot.getKey());

                                // If is not seen, update seen to true and add it to updateSeenMap
                                Log.d(TAG, "initialListener: isSeeing= "+ isSeeing);
                                if(!notification.isSeen()){
                                    Log.d(TAG, "initialListener: set notifications Seen to true: notification key= "+ notification.getKey());
                                    // add notification to the map that holds all seen notifications need to be updated
                                    updateSeenMap.put(snapshot.getKey()+"/seen", true);
                                    notification.setSeen(true);
                                }
                            }

                            list.add(notification);
                            //Log.d(TAG, "mama getItems = " + notification.getSentLong() + " getSnapshotKey= " + snapshot.getKey());

                        }

                        Log.d(TAG, "initialListener: SeenMap size= "+ updateSeenMap.size() + " isSeeing= "+ isSeeing);
                        // We already push updateSeenMap to the database when fragment stops and resume but we need to Update seen notifications hear too
                        // if user is currently isSeeing, just in case an invalidate happens while the user is opening notifications tap
                        if(updateSeenMap.size() > 0 && isSeeing){
                            Log.d(TAG, "initialListener: Updating seen notifications. updateSeenMap size= "+ updateSeenMap.size() + " isSeeing= "+ isSeeing);
                            //Update seen notifications
                            mNotificationsRef.updateChildren(updateSeenMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    Log.d(TAG, "initialListener updateSeenMap onSuccess: clearing updateSeenMap after pushing seeing updateSeenMap do database. size before clear= "+ updateSeenMap.size() + " isSeeing= "+ isSeeing);
                                    updateSeenMap.clear();
                                }
                            });
                        }

                        if (list.size() != 0) {
                            Collections.reverse(list);
                            callback.onResult(list);

                            // Add messages to totalItemsList ArrayList to be used to get the initial key position
                            totalItemsList.addAll(list);
                            printTotalItems("Initial");
                            Log.d(TAG, "mama getMessages  List.size= " + list.size() + " last key= " + list.get(list.size() - 1).getKey());
                        }

                    } else {
                        // No data exist
                        Log.w(TAG, "isInitialKey. getItems no notifications exist");
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
            Log.d(TAG, "mama getNotifications initialKey= " + initialKey);
            isInitialKey = false;
            query = mNotificationsRef
                    .orderByChild("sent")//limitToLast to start from the last (page size) items
                    .limitToLast(size);


        } else {// not the first load. Key is the last seen key
            Log.d(TAG, "mama getNotifications initialKey= " + initialKey);
            isInitialKey = true;
            switch (mScrollDirection){
                // No need to detected reaching to bottom
                /*case REACHED_THE_BOTTOM:
                    Log.d(TAG, "messages query = REACHED_THE_BOTTOM. ScrollDirection= "+mScrollDirection+ " mVisibleItem= "+ mVisibleItem + " totalItemsList size= "+totalItemsList.size());
                    chatsQuery = mNotificationsRef
                            .orderByChild("sent")//limitToLast to start from the last (page size) items
                            .limitToFirst(size);
                    break;*/
                case REACHED_THE_TOP:
                    Log.d(TAG, "messages query = REACHED_THE_TOP. ScrollDirection= "+mScrollDirection+ " mVisibleItem= "+ mVisibleItem + " totalItemsList size= "+totalItemsList.size());
                    query = mNotificationsRef
                            .orderByChild("sent")//limitToLast to start from the last (page size) items
                            .limitToLast(size);
                    break;
                case SCROLLING_UP:
                    /*Log.d(TAG, "messages query = Load data from top to bottom (above InitialKey cause list is reversed). ScrollDirection= "+mScrollDirection+ " InitialKey Position= "+getInitialKeyPosition() +" mVisibleItem= "+mVisibleItem+ " Item Message= "+ totalItemsList.get(mVisibleItem).getLastMessage() +" totalItemsList size= "+totalItemsList.size());
                    chatsQuery = mNotificationsRef
                            .orderByChild("sent")//limitToLast to start from the last (page size) items
                            .endAt(initialKey)
                            .limitToLast(size);*/

                    // list is reversed, smaller Keys are on bottom
                    // InitialKey is in the top, must load data from top to bottom
                    // list is reversed, load data above InitialKey
                    Log.d(TAG, "messages query = Load data from top to bottom (above InitialKey cause list is reversed). ScrollDirection= "+mScrollDirection+ " InitialKey Position= "+getInitialKeyPosition() +" first VisibleItem= "+ mVisibleItem + " Item Message= "+ totalItemsList.get(mVisibleItem).getSentLong() +" totalItemsList size= "+totalItemsList.size());
                    query = mNotificationsRef
                            .orderByChild("sent")
                            .endAt(getItem(mVisibleItem).getSentLong())//Using first visible item key instead of initial key
                            .limitToLast(size);
                    break;
                case SCROLLING_DOWN:
                    /*Log.d(TAG, "messages query = Load data from bottom to top (below InitialKey cause list is reversed). ScrollDirection= "+mScrollDirection+ " InitialKey Position= "+getInitialKeyPosition() +" mVisibleItem= "+mVisibleItem+ " Item Message= "+ totalItemsList.get(mVisibleItem).getLastMessage() +" totalItemsList size= "+totalItemsList.size());
                    chatsQuery = mNotificationsRef
                            .orderByChild("sent")//limitToLast to start from the last (page size) items
                            .startAt(initialKey)
                            .limitToFirst(size);*/
                    // InitialKey is in the bottom, must load data from bottom to top
                    // list is reversed, load data below InitialKey
                    Log.d(TAG, "messages query = Load data from bottom to top (below InitialKey cause list is reversed). ScrollDirection= "+mScrollDirection+ " InitialKey Position= "+getInitialKeyPosition() +"  last VisibleItem= "+ mVisibleItem + " Item Message= "+ totalItemsList.get(mVisibleItem).getSentLong() +" totalItemsList size= "+totalItemsList.size());
                    query = mNotificationsRef
                            .orderByChild("sent")
                            .startAt(getItem(mVisibleItem).getSentLong())//Using last visible item key instead of initial key
                            .limitToFirst(size);
                    break;
                default:
                   /*// list is reversed, greater Keys are on top
                    if(getInitialKeyPosition() >= mVisibleItem){
                        // InitialKey is in the bottom, must load data from bottom to top
                        // list is reversed, load data below InitialKey
                        Log.d(TAG, "messages query = Load data from bottom to top (below InitialKey cause list is reversed). ScrollDirection= "+mScrollDirection+ " InitialKey Position= "+getInitialKeyPosition() +" mVisibleItem= "+ mVisibleItem + " totalItemsList size= "+totalItemsList.size());
                        chatsQuery = mNotificationsRef
                                .orderByChild("sent")//limitToLast to start from the last (page size) items
                                .startAt(initialKey)
                                .limitToFirst(size);

                    }else{
                        // list is reversed, smaller Keys are on bottom
                        // InitialKey is in the top, must load data from top to bottom
                        // list is reversed, load data above InitialKey
                        Log.d(TAG, "messages query = Load data from top to bottom (above InitialKey cause list is reversed). ScrollDirection= "+mScrollDirection+ " InitialKey Position= "+getInitialKeyPosition() +" mVisibleItem= "+ mVisibleItem + " totalItemsList size= "+totalItemsList.size());
                        chatsQuery = mNotificationsRef
                                .orderByChild("sent")//limitToLast to start from the last (page size) items
                                .endAt(initialKey)
                                .limitToLast(size);
                    }
                    break;*/
                    Log.d(TAG, "messages query = default. ScrollDirection= "+mScrollDirection+ " mVisibleItem= "+ mVisibleItem + " totalItemsList size= "+totalItemsList.size());
                    query = mNotificationsRef
                            .orderByChild("sent")//limitToLast to start from the last (page size) items
                            .limitToLast(size);
            }
        }
        // Clear the list of total items to start all over
        totalItemsList.clear();
        Log.d(TAG, "messages query = totalItemsList is cleared");

        query.addValueEventListener(initialListener);
        mListenersList.add(new FirebaseListeners(query, initialListener));
        //mUsersRef.addValueEventListener(usersListener);

    }

    // to get next data
    public void getAfter(final Long key, final int size,
                         @NonNull final ItemKeyedDataSource.LoadCallback<DatabaseNotification> callback){
        /*if(key == entireUsersList.get(entireUsersList.size()-1).getCreatedLong()){
            Log.d(TAG, "mama getUsersAfter init. afterKey= " +  key+ "entireUsersList= "+entireUsersList.get(entireUsersList.size()-1).getCreatedLong());
            return;
        }*/
        Log.d(TAG, "mama getAfter. AfterKey= " + key);

        isAfterFirstLoaded = true;
        //this.afterKey = key;
        Query afterQuery;

        afterQuery = mNotificationsRef
                .orderByChild("sent")
                .startAfter(key)
                .limitToFirst(size);

        afterQuery.addValueEventListener(afterListener);
        mListenersList.add(new FirebaseListeners(afterQuery, afterListener));
        //mUsersRef.addValueEventListener(usersListener);
    }

    // to get previous data
    public void getBefore(final Long key, final int size,
                          @NonNull final ItemKeyedDataSource.LoadCallback<DatabaseNotification> callback){
        Log.d(TAG, "mama getBefore. BeforeKey= " +  key);
        /*if(key == entireUsersList.get(0).getCreatedLong()){
            return;
        }*/
        isBeforeFirstLoaded = true;
        //this.beforeKey = key;
        Query beforeQuery;

        beforeQuery = mNotificationsRef
                .orderByChild("sent")
                .endBefore(key)
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

        final Query query = mNotificationsRef.orderByChild("sent");

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
            Log.d(TAG, "totalItemsList : key= "+ totalItemsList.get(i).getKey()+ " message= "+ totalItemsList.get(i).getSentLong()+ " size= "+totalItemsList.size());
        }
    }

    // get the position of initial key
    public int getInitialKeyPosition(){
        Log.d(TAG, "Getting get InitialKeyPosition... getInitialKey()= "+getInitialKey()+ " totalItemsList size= "+totalItemsList.size());
        int Position = 0;
        for (int i = 0; i < totalItemsList.size(); i++) {
            if(totalItemsList.get(i).getSentLong() == getInitialKey()){
                Log.d(TAG, "messages query InitialKeyPosition: key= "+ getInitialKey()+ " message= "+totalItemsList.get(i).getSentLong() +" Position= " +Position);
                return Position;
            }else{
                Position++;
            }
        }
        return Position;
    }

    // get item and item's key from adapter position
    public DatabaseNotification getItem(int position){
        Log.d(TAG, "Getting getItem... getInitialKey()= "+getInitialKey()+ " item message= "+ totalItemsList.get(position).getSentLong());
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
