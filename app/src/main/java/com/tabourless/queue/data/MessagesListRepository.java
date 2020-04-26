package com.tabourless.queue.data;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.DataSource;
import androidx.paging.ItemKeyedDataSource;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.tabourless.queue.models.FirebaseListeners;
import com.tabourless.queue.models.Message;
import com.tabourless.queue.models.User;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessagesListRepository {

    private final static String TAG = MessagesListRepository.class.getSimpleName();
    private static MessagesListRepository messagesListRepository = null;

    // [START declare_database_ref]
    private DatabaseReference mDatabaseRef;
    private DatabaseReference mMessagesRef;
    private DatabaseReference mUsersRef;

    //private User currentUser;
    private String currentUserId;
    private FirebaseUser mFirebaseCurrentUser;

    private static volatile Boolean isInitialFirstLoaded;// = true;
    private static volatile Boolean isAfterFirstLoaded;// = true;
    private static volatile Boolean isBeforeFirstLoaded;// = true;

    private String initialKey;
    private String afterKey;
    private String beforeKey;
    private String chatKey;

    private ValueEventListener MessagesChangesListener;
    private static List<FirebaseListeners> mListenersList;// = new ArrayList<>();
    private static List<Message> totalItemsList;// = new ArrayList<>();
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

    private static final String Message_STATUS_SENDING = "Sending";
    private static final String Message_STATUS_SENT = "Sent";
    private static final String Message_STATUS_DELIVERED = "Delivered";
    private static final String Message_STATUS_SEEN = "Seen";
    private static final String Message_STATUS_REVEALED = "Revealed";

    private ValueEventListener afterMessagesListener = new ValueEventListener() {
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
                List<Message> messagesList = new ArrayList<>();

                // Create a map for all seen messages need to be updated
                Map<String, Object> updateMap = new HashMap<>();

                // loop throw users value
                for (DataSnapshot snapshot: dataSnapshot.getChildren()){
                    if(!getLoadAfterKey().equals(snapshot.getKey())) { // if snapshot key = startAt key? don't add it again
                        Message message = snapshot.getValue(Message.class);
                        if (message != null) {
                            message.setKey(snapshot.getKey());

                            // Add only seen messages by current user to seenItemsList
                            // If current user is not the sender, the other user is seeing this message
                            if(!TextUtils.equals(message.getSenderId(), currentUserId)){
                                //seenItemsList.add(message);
                                if(null == message.getStatus() || !TextUtils.equals(message.getStatus(), Message_STATUS_SEEN)){
                                    Log.d(TAG, "getMessagesAfter. seen messages need to be updated = message"+ message.getMessage()+ "status"+ message.getStatus()+ "key"+ message.getKey());
                                    updateMap.put(snapshot.getKey()+"/status", Message_STATUS_SEEN);
                                    message.setStatus(Message_STATUS_SEEN);
                                }
                            }
                        }
                        messagesList.add(message);
                        // Add messages to totalItemsList ArrayList to be used to get the initial key position
                        totalItemsList.add(message);

                        //Log.d(TAG, "mama getMessage = "+ message.getMessage()+" getSnapshotKey= " +  snapshot.getKey());
                    }
                }
                // Update seen messages
                if(updateMap.size() > 0){
                    mMessagesRef.updateChildren(updateMap);
                    return;
                }
                // Get TotalItems logs
                printTotalItems();
                //printSeenItems();
                if(messagesList.size() != 0){
                    //callback.onResult(messagesList);
                    getLoadAfterCallback().onResult(messagesList);
                    Log.d(TAG, "mama getMessagesAfter  List.size= " +  messagesList.size()+ " last key= "+messagesList.get(messagesList.size()-1).getKey());
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

    private ValueEventListener beforeMessagesListener = new ValueEventListener() {
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
                List<Message> messagesList = new ArrayList<>();

                // Create a map for all seen messages need to be updated
                Map<String, Object> updateMap = new HashMap<>();

                // loop throw users value
                for (DataSnapshot snapshot: dataSnapshot.getChildren()){
                    if(!getLoadBeforeKey().equals(snapshot.getKey())) { // if snapshot key = startAt key? don't add it again
                        Message message = snapshot.getValue(Message.class);
                        if (message != null) {
                            message.setKey(snapshot.getKey());

                            // Add only seen messages by current user to seenItemsList
                            // If current user is not the sender, the other user is seeing this message
                            if(!TextUtils.equals(message.getSenderId(), currentUserId)){
                                //seenItemsList.add(message);
                                if(null == message.getStatus() || !TextUtils.equals(message.getStatus(), Message_STATUS_SEEN)){
                                    Log.d(TAG, "getMessagesBefore. seen messages need to be updated = message"+ message.getMessage()+ "status"+ message.getStatus()+ "key"+ message.getKey());
                                    updateMap.put(snapshot.getKey()+"/status", Message_STATUS_SEEN);
                                    message.setStatus(Message_STATUS_SEEN);
                                }
                            }
                        }
                        messagesList.add(message);
                        //Log.d(TAG, "mama getMessage = "+ message.getMessage()+" getSnapshotKey= " +  snapshot.getKey());
                    }
                }

                // Update seen messages
                if(updateMap.size() > 0){
                    mMessagesRef.updateChildren(updateMap);
                    return;
                }

                if(messagesList.size() != 0){
                    //callback.onResult(messagesList);
                    getLoadBeforeCallback().onResult(messagesList);
                    Log.d(TAG, "mama getMessagesBefore  List.size= " +  messagesList.size()+ " last key= "+messagesList.get(messagesList.size()-1).getKey());

                    // Create a reversed list to add messages to the beginning of totalItemsList
                    List<Message> reversedList = new ArrayList<>(messagesList);
                    Collections.reverse(reversedList);
                    for (int i = 0; i < reversedList.size(); i++) {
                        // Add messages to totalItemsList ArrayList to be used to get the initial key position
                        totalItemsList.add(0, reversedList.get(i));

                        /*// Add only seen messages by current user to seenItemsList
                        // If current user is not the sender, the other user is seeing this message
                        if(null!= reversedList.get(i).getSenderId() && !reversedList.get(i).getSenderId().equals(currentUserId)){
                            seenItemsList.add(0, reversedList.get(i));
                        }*/
                    }
                    // Get TotalItems logs
                    printTotalItems();
                    //printSeenItems();
                }
            } else {
                // no data
                Log.w(TAG, "mama getMessagesBefore no users exist");
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

    public MessagesListRepository(String chatKey, @NonNull DataSource.InvalidatedCallback onInvalidatedCallback){
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        // use received chatKey to create a database ref
        mMessagesRef = mDatabaseRef.child("messages").child(chatKey);
        mUsersRef = mDatabaseRef.child("users");

        //Get current logged in user
        mFirebaseCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = mFirebaseCurrentUser!= null ? mFirebaseCurrentUser.getUid() : null;

        // call back to invalidate data
        this.invalidatedCallback = onInvalidatedCallback;
        this.chatKey = chatKey;

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
    public static void setScrollDirection(int scrollDirection, int lastVisibleItem) {
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
    public void getMessages(String initialKey, final int size,
                            @NonNull final ItemKeyedDataSource.LoadInitialCallback<Message> callback) {

        Log.i(TAG, "getMessages initiated. initialKey= " +  initialKey);
        this.initialKey = initialKey;
        Query messagesQuery;
        isInitialFirstLoaded = true;

        ValueEventListener initialMessagesListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // [START_EXCLUDE]
                Log.d(TAG, "start onDataChange. isInitialFirstLoaded = "+ isInitialFirstLoaded);

                if (!isInitialFirstLoaded){
                    // Remove post value event listener
                    removeListeners();
                    Log.d(TAG, "mama usersChanged Invalidated removeEventListener");
                    //isInitialFirstLoaded =  true;
                    Log.d(TAG, "onInvalidated(). isInitialFirstLoaded = "+ isInitialFirstLoaded);
                    invalidatedCallback.onInvalidated();
                    //UsersDataSource.InvalidatedCallback.class.getMethod("loadInitial", "LoadInitialParams");
                    //UsersDataSource.invalidate();
                    return;
                }

                if (dataSnapshot.exists()) {
                    final List<Message> messagesList = new ArrayList<>();

                    // Create a map for all seen messages need to be updated
                    Map<String, Object> updateMap = new HashMap<>();
                    // loop throw users value
                    for (DataSnapshot snapshot: dataSnapshot.getChildren()){
                        Message message = snapshot.getValue(Message.class);
                        if (message != null) {
                            message.setKey(snapshot.getKey());

                            /*// Add only seen messages by current user to seenItemsList
                            // If current user is not the sender, the other user is seeing this message
                            if(null != message.getSenderId() && !message.getSenderId().equals(currentUserId)){
                                seenItemsList.add(message);
                            }*/

                            // Add only seen messages by current user to seenItemsList
                            // If current user is not the sender, the other user is seeing this message
                            if(!TextUtils.equals(message.getSenderId(), currentUserId)){
                                //seenItemsList.add(message);
                                if(null == message.getStatus() || !TextUtils.equals(message.getStatus(), Message_STATUS_SEEN)){
                                    Log.d(TAG, "initiated. seen messages need to be updated = message"+ message.getMessage()+ "status"+ message.getStatus()+ "key"+ message.getKey());
                                    //updateMap.put(snapshot.getKey()+"/status", Message_STATUS_SEEN);
                                    updateMap.put("/messages/" + chatKey + "/" +snapshot.getKey()+"/status", Message_STATUS_SEEN);
                                    message.setStatus(Message_STATUS_SEEN);
                                }
                            }
                        }
                        messagesList.add(message);
                        // Add messages to totalItemsList ArrayList to be used to get the initial key position
                        totalItemsList.add(message);

                        //Log.d(TAG, "mama getMessage = "+ message.getMessage()+" getSnapshotKey= " +  snapshot.getKey());
                    }

                    // Update seen messages
                    if(updateMap.size() > 0){
                        // Update chats member saw
                        updateMap.put("/userChats/" + currentUserId + "/" + chatKey + "/members/" +currentUserId+ "/read/" , true);
                        updateMap.put("/chats/" + chatKey + "/members/" +currentUserId+ "/read/" , true);

                       /* // Update seen chats count
                        updateMap.put("/counts/" + currentUserId + "/chats/" + chatKey, null);*/
                        mDatabaseRef.updateChildren(updateMap);
                        return;
                    }

                    printTotalItems();
                    //printSeenItems();

                    if(messagesList.size() != 0){
                        /*if(null != getInitialKey()){
                            mMessagesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    int totalCount = (int) dataSnapshot.getChildrenCount();
                                    int i = 0;
                                    for (DataSnapshot snapshot: dataSnapshot.getChildren()){
                                        if(null != getInitialKey() && getInitialKey().equals(snapshot.getKey())){
                                            Log.d(TAG, "mama getMessages InitialKey position= "+ (i-1)+ " totalCount= "+totalCount);
                                            callback.onResult(messagesList, (i-1), totalCount);
                                            break;
                                        }else{
                                            i++;
                                        }

                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                        }else{

                            mMessagesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    int totalCount = (int) dataSnapshot.getChildrenCount();
                                    int posetion = (totalCount - 1)-size;
                                    Log.d(TAG, "mama getMessages null InitialKey posetion= "+ posetion+"totalCount= "+totalCount);
                                    callback.onResult(messagesList, posetion, totalCount);
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                        }*/

                        callback.onResult(messagesList);
                        Log.d(TAG, "mama getMessages  List.size= " +  messagesList.size()+ " last key= "+messagesList.get(messagesList.size()-1).getKey() + " getInitialKey= "+ getInitialKey() );
                    }
                } else {
                    // no data
                    Log.w(TAG, "mama getMessages no users exist");
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
            Log.d(TAG, "mama getMessages initialKey is null");
            messagesQuery = mMessagesRef.orderByKey()//limitToLast to start from the last (page size) items
                    .limitToLast(size);

        } else {// not the first load. Key is the last seen key
            Log.d(TAG, "mama getMessages initialKey= " + initialKey);
            switch (mScrollDirection){
                case REACHED_THE_BOTTOM:
                    Log.d(TAG, "messages query = REACHED_THE_BOTTOM");
                    messagesQuery = mMessagesRef.orderByKey()
                            .limitToLast(size);
                    break;
                case REACHED_THE_TOP:
                    Log.d(TAG, "messages query = REACHED_THE_TOP");
                    messagesQuery = mMessagesRef.orderByKey()
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
                        Log.d(TAG, "messages query = Load data from bottom to top");
                        messagesQuery = mMessagesRef.orderByKey()
                                .endAt(initialKey)
                                .limitToLast(size);

                    }else{
                        // InitialKey is in the top, must load data from top to bottom
                        Log.d(TAG, "messages query = Load data from top to bottom");
                        messagesQuery = mMessagesRef.orderByKey()
                                .startAt(initialKey)
                                .limitToFirst(size);
                    }
                    break;
            }
        }

        getInitialKeyPosition();
        // Clear the list of total items to start all over
        totalItemsList.clear();

        messagesQuery.addValueEventListener(initialMessagesListener);
        mListenersList.add(new FirebaseListeners(messagesQuery, initialMessagesListener));

    }

    // to get next data
    public void getMessagesAfter(final String key, final int size,
                         @NonNull final ItemKeyedDataSource.LoadCallback<Message> callback){

        Log.i(TAG, "getMessagesAfter initiated. AfterKey= " +  key);
        isAfterFirstLoaded = true;
        //this.afterKey = key;
        Query afterMessagesQuery;

        Log.d(TAG, "mama getMessagesAfter. AfterKey= " + key);
        afterMessagesQuery = mMessagesRef.orderByKey()
                            .startAt(key)
                            .limitToFirst(size);

        afterMessagesQuery.addValueEventListener(afterMessagesListener);
        mListenersList.add(new FirebaseListeners(afterMessagesQuery, afterMessagesListener));
        //mUsersRef.addValueEventListener(usersListener);
    }

    // to get previous data
    public void getMessagesBefore(final String key, final int size,
                              @NonNull final ItemKeyedDataSource.LoadCallback<Message> callback){

        Log.i(TAG, "getMessagesBefore initiated. BeforeKey= " +  key);

        isBeforeFirstLoaded = true;
        //this.beforeKey = key;
        Query beforeMessagesQuery;

        beforeMessagesQuery = mMessagesRef.orderByKey()
                                .endAt(key)
                                .limitToLast(size);

        beforeMessagesQuery.addValueEventListener(beforeMessagesListener);
        mListenersList.add(new FirebaseListeners(beforeMessagesQuery, beforeMessagesListener));
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
    public static void removeListeners(){

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
            Log.d(TAG, "totalItemsList : key= "+ totalItemsList.get(i).getKey()+ " message= "+ totalItemsList.get(i).getMessage()+ " size= "+totalItemsList.size());
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

}
