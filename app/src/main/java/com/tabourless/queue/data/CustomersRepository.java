package com.tabourless.queue.data;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.DataSource;
import androidx.paging.ItemKeyedDataSource;

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
import com.tabourless.queue.models.Customer;
import com.tabourless.queue.models.FirebaseListeners;
import com.tabourless.queue.models.Message;
import com.tabourless.queue.models.Queue;
import com.tabourless.queue.models.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.tabourless.queue.App.DATABASE_REF_CUSTOMERS;
import static com.tabourless.queue.App.DATABASE_REF_CUSTOMER_NUMBER;
import static com.tabourless.queue.App.DATABASE_REF_PLACES;
import static com.tabourless.queue.App.DATABASE_REF_QUEUES;
import static com.tabourless.queue.App.DATABASE_REF_QUEUE_JOINED;
import static com.tabourless.queue.App.DATABASE_REF_USER_QUEUES;

public class CustomersRepository {

    private final static String TAG = CustomersRepository.class.getSimpleName();

    // [START declare_database_ref]
    private DatabaseReference mDatabaseRef, mCustomersRef;

    private String currentUserId;
    private FirebaseUser mFirebaseCurrentUser;

    private static volatile Boolean isInitialFirstLoaded;// = true;
    private static volatile Boolean isAfterFirstLoaded;// = true;
    private static volatile Boolean isBeforeFirstLoaded;// = true;

    private Integer initialKey;
    private Integer afterKey;
    private Integer beforeKey;
    private String mPlaceKey, mQueueKey;


    // Not static to only remove listeners of this repository instance
    // Start destination fragment is never destroyed , so when clicking on it's bottom navigation icon again it got destroyed to be recreated
    // When that happens clearing listeners is triggered on viewmodel Cleared, which removes that new listeners for the just added query
    // When new listener is removed we got 0 results and have no listeners for updates.
    private List<FirebaseListeners> mListenersList;
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

    private ValueEventListener afterListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // [START_EXCLUDE]
            Log.d(TAG, "start onDataChange isAfterFirstLoaded = "+ isAfterFirstLoaded);
            if (!isAfterFirstLoaded){
                // Remove post value event listener
                removeListeners();
                Log.d(TAG, "getAfter Invalidated removeEventListener");
                //isAfterFirstLoaded =  true;
                Log.d(TAG, "getAfter onInvalidated(). isAfterFirstLoaded = "+ isAfterFirstLoaded);
                invalidatedCallback.onInvalidated();
                //UsersDataSource.InvalidatedCallback.class.getMethod("loadInitial", "LoadInitialParams");
                //UsersDataSource.invalidate();
                return;
            }

            if (dataSnapshot.exists()) {
                List<Customer> resultList = new ArrayList<>();

                // loop throw results value
                for (DataSnapshot snapshot: dataSnapshot.getChildren()){
                    Customer customer = snapshot.getValue(Customer.class);
                    if (customer != null) {
                        customer.setKey(snapshot.getKey());
                        if(getLoadAfterKey() != customer.getNumber() || (null != getLoadAfterKey() && !getLoadAfterKey().equals(customer.getNumber()))) { // if snapshot key = startAt key? don't add it again
                            if(TextUtils.equals(customer.getKey(), currentUserId) || (null != customer.getNumber() && customer.getNumber() != 0)){
                                // Only add users that have received a number from the server, don't add users with the default 0 number
                                // If the user is the current user add him even if number is 0, so user see himself waiting for receiving token when reconnect
                                resultList.add(customer);
                                // Add messages to totalItemsList ArrayList to be used to get the initial key position
                                totalItemsList.add(customer);
                            }
                        }
                        //Log.d(TAG, "mama getMessage = "+ message.getMessage()+" getSnapshotKey= " +  snapshot.getKey());
                    }
                }

                // Get TotalItems logs
                printTotalItems();
                //printSeenItems();
                if(resultList.size() != 0){
                    //callback.onResult(messagesList);
                    getLoadAfterCallback().onResult(resultList);
                    Log.d(TAG, "getMessagesAfter  List.size= " +  resultList.size()+ " last key= "+resultList.get(resultList.size()-1).getKey());
                }
            } else {
                // no data
                Log.w(TAG, "getAfter no users exist");
            }
            printListeners();
            isAfterFirstLoaded =  false;
            Log.d(TAG, "end isAfterFirstLoaded = "+ isAfterFirstLoaded);
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            // Getting Post failed, log a message
            Log.w(TAG, "getAfter loadPost:onCancelled", databaseError.toException());
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
                Log.d(TAG, "getMBefore Invalidated removeEventListener");
                //isBeforeFirstLoaded =  true;
                Log.d(TAG, "getBefore onInvalidated(). isBeforeFirstLoaded = "+ isBeforeFirstLoaded);
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
                        if(getLoadBeforeKey() != customer.getNumber() || (null != getLoadBeforeKey() && !getLoadBeforeKey().equals(customer.getNumber()))) { // if snapshot key = startAt key? don't add it again
                            if(TextUtils.equals(customer.getKey(), currentUserId) || (null != customer.getNumber() && customer.getNumber() != 0)){
                                // Only add users that have received a number from the server, don't add users with the default 0 number
                                // If the user is the current user add him even if number is 0, so user see himself waiting for receiving token when reconnect
                                resultList.add(customer);
                            }
                        }
                        //Log.d(TAG, "mama getMessage = "+ message.getMessage()+" getSnapshotKey= " +  snapshot.getKey());
                    }
                }

                if(resultList.size() != 0){
                    //callback.onResult(messagesList);
                    getLoadBeforeCallback().onResult(resultList);
                    Log.d(TAG, "getBefore  List.size= " +  resultList.size()+ " last key= "+resultList.get(resultList.size()-1).getKey());

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
                Log.w(TAG, "getBefore no users exist");
            }
            printListeners();
            isBeforeFirstLoaded =  false;
            Log.d(TAG, "end isBeforeFirstLoaded = "+ isBeforeFirstLoaded);
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            // Getting Post failed, log a message
            Log.w(TAG, "getBefore: onCancelled", databaseError.toException());
        }
    };

    public CustomersRepository(String placeKey, String queueKey){
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        // use received placeKey and queueKey to create a database ref
        Log.d(TAG, "CustomersRepository: placeId= "+ placeKey+ " queueId= "+ queueKey);
        mCustomersRef = mDatabaseRef.child(DATABASE_REF_CUSTOMERS).child(placeKey).child(queueKey);

        //Get current logged in user
        mFirebaseCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = mFirebaseCurrentUser!= null ? mFirebaseCurrentUser.getUid() : null;

        // call back to invalidate data
        this.mPlaceKey = placeKey;
        this.mQueueKey = queueKey;

        isInitialFirstLoaded =  true;
        isAfterFirstLoaded = true;
        isBeforeFirstLoaded = true;

        Log.d(TAG, "Repository init. isInitialFirstLoaded= " + isInitialFirstLoaded+ " after= "+isAfterFirstLoaded + " before= "+isBeforeFirstLoaded);

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
    public void getInitial(Integer initialKey, final int size,
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
                            if(dataSnapshot.getChildrenCount() == 1 || TextUtils.equals(customer.getKey(), currentUserId) || (null != customer.getNumber() && customer.getNumber() != 0)){
                                // Only add users that have received a number from the server, don't add users with the default 0 number
                                // If the user is the current user add him even if number is 0, so user see himself waiting for receiving token when reconnect

                                // Add the customer how did't received a number from the server anyway because there is only one customer found
                                // The observer is fired twice, first when customer added and then when customer got hid number,
                                // if we don't add the unnumbered customer the first while loop will keep looping because it has zero item size which eventually will erase the added customer from adapter
                                resultList.add(customer);
                                // Add results to totalItemsList ArrayList to be used to get the initial key position
                                totalItemsList.add(customer);
                                //Log.d(TAG, "mama getMessage = "+ message.getMessage()+" getSnapshotKey= " +  snapshot.getKey());
                            }
                        }
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
            query = mCustomersRef.orderByChild(DATABASE_REF_CUSTOMER_NUMBER)//limitToFist to start from the fist (page size) items
                    .limitToFirst(size);

        } else {// not the first load. Key is the last seen key
            Log.d(TAG, "initialKey= " + initialKey);
            switch (mScrollDirection){
                case REACHED_THE_BOTTOM:
                    Log.d(TAG, "query = REACHED_THE_BOTTOM");
                    query = mCustomersRef.orderByChild(DATABASE_REF_CUSTOMER_NUMBER)
                            .limitToLast(size);
                    break;
                case REACHED_THE_TOP:
                    Log.d(TAG, "query = REACHED_THE_TOP");
                    query = mCustomersRef.orderByChild(DATABASE_REF_CUSTOMER_NUMBER)
                            .limitToFirst(size);
                    break;
                /*case SCROLLING_UP:
                    messagesQuery = mMessagesRef.orderByChild(DATABASE_REF_CUSTOMER_NUMBER)
                            .startAt(initialKey)
                            .limitToFirst(size);
                    break;
                case SCROLLING_DOWN:
                    messagesQuery = mMessagesRef.orderByChild(DATABASE_REF_CUSTOMER_NUMBER)
                            .endAt(initialKey)
                            .limitToLast(size);
                    break;*/
                default:
                    if(getInitialKeyPosition() >= mLastVisibleItem){
                        // InitialKey is in the bottom, must load data from bottom to top
                        Log.d(TAG, "query = Load data from bottom to top");
                        query = mCustomersRef.orderByChild(DATABASE_REF_CUSTOMER_NUMBER)
                                .endAt(initialKey)
                                .limitToLast(size);

                    }else{
                        // InitialKey is in the top, must load data from top to bottom
                        Log.d(TAG, "query = Load data from top to bottom");
                        query = mCustomersRef.orderByChild(DATABASE_REF_CUSTOMER_NUMBER)
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
    public void getAfter(final Integer key, final int size,
                         @NonNull final ItemKeyedDataSource.LoadCallback<Customer> callback){

        Log.i(TAG, "getAfter initiated. AfterKey= " +  key);
        isAfterFirstLoaded = true;
        //this.afterKey = key;
        Query afterQuery;

        Log.d(TAG, "getAfter. AfterKey= " + key);
        afterQuery = mCustomersRef.orderByChild(DATABASE_REF_CUSTOMER_NUMBER)
                            .startAt(key)
                            .limitToFirst(size);

        afterQuery.addValueEventListener(afterListener);
        mListenersList.add(new FirebaseListeners(afterQuery, afterListener));
        //mUsersRef.addValueEventListener(usersListener);
    }

    // to get previous data
    public void getBefore(final Integer key, final int size,
                          @NonNull final ItemKeyedDataSource.LoadCallback<Customer> callback){

        Log.i(TAG, "getBefore initiated. BeforeKey= " +  key);

        isBeforeFirstLoaded = true;
        //this.beforeKey = key;
        Query beforeQuery;

        beforeQuery = mCustomersRef.orderByChild(DATABASE_REF_CUSTOMER_NUMBER)
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

        final Query query = mMessagesRef.orderByChild(DATABASE_REF_CUSTOMER_NUMBER)

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
        return RxFirebaseDatabase.data(mUsersRef.orderByChild(DATABASE_REF_CUSTOMER_NUMBER).limitToFirst(count)).ma

                .map {
            for ArrayValue
            User.getArrayValue(User.class);
        }
    }*/

    public void removeCustomer(Customer customer) {
        Map<String, Object> childUpdates = new HashMap<>();

        // Update customer to null to remove it from the customers node
        childUpdates.put(DATABASE_REF_CUSTOMERS +"/" + mPlaceKey + "/" + mQueueKey + "/" + customer.getKey(), null);
        childUpdates.put(DATABASE_REF_USER_QUEUES +"/" + customer.getKey() + "/" + mQueueKey+ "/"+ DATABASE_REF_QUEUE_JOINED , 0);

        // update Data base
        mDatabaseRef.updateChildren(childUpdates);
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

    public void setLoadAfterCallback(Integer key, ItemKeyedDataSource.LoadCallback loadAfterCallback) {
        this.loadAfterCallback = loadAfterCallback;
        this.afterKey = key;
    }

    public ItemKeyedDataSource.LoadCallback getLoadBeforeCallback() {
        return loadBeforeCallback;
    }

    public void setLoadBeforeCallback(Integer key, ItemKeyedDataSource.LoadCallback loadBeforeCallback) {
        this.loadBeforeCallback = loadBeforeCallback;
        this.beforeKey = key;
    }

    public Integer getLoadAfterKey() {
        return afterKey;
    }

    public Integer getLoadBeforeKey() {
        return beforeKey;
    }

    public Integer getInitialKey() {
        return initialKey;
    }

    /*public void setInitialKey(String initialKey) {
        this.initialKey = initialKey;
    }*/

    // When last database message is not loaded, Invalidate messagesDataSource to scroll down
    public void invalidateData() {
        invalidatedCallback.onInvalidated();
    }

    public void setInvalidatedCallback(DataSource.InvalidatedCallback onInvalidatedCallback) {
        this.invalidatedCallback = onInvalidatedCallback;
    }
}
