package com.tabourless.queue.data;

import static com.tabourless.queue.App.DATABASE_REF_CHATS_MEMBERS;
import static com.tabourless.queue.App.DATABASE_REF_CHATS_MEMBER_READ;
import static com.tabourless.queue.App.DATABASE_REF_NOTIFICATIONS_SEEN;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.tabourless.queue.models.FirebaseListeners;
import com.tabourless.queue.models.User;

import java.util.ArrayList;
import java.util.List;

public class MainActivityRepository {

    private final static String TAG = "MainActivityRepository";

    // [START declare_database_ref]
    private DatabaseReference mDatabaseRef;
    private DatabaseReference mUsersRef, mChatsRef, mNotificationsRef;
    private Boolean isFirstLoaded = true;

    private MutableLiveData<User> mCurrentUser;
    private MutableLiveData<User> mUser;
    private MutableLiveData<User> mSingleValueUser;
    private MutableLiveData<Long> mChatsCount, mNotificationsCount;

    // HashMap to keep track of Firebase Listeners
    //private HashMap< DatabaseReference , ValueEventListener> mListenersMap;
    // Change mListenersList to static so that it's the same for all instance
    private  List<FirebaseListeners> mListenersList;// = new ArrayList<>();
    private Query chatsCountQuery, notificationCountQuery;


    // A listener for mCurrentUser changes
    private ValueEventListener currentUserListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // Get Post object and use the values to update the UI
            if (dataSnapshot.exists()) {
                // Get user value
                Log.d(TAG, "getUser dataSnapshot key: "
                        + dataSnapshot.getKey()+" Listener = "+currentUserListener);
                //mCurrentUser = dataSnapshot.getValue(User.class);
                mCurrentUser.postValue(dataSnapshot.getValue(User.class));
            } else {
                // User is null, error out
                mCurrentUser.postValue(null); // return null to disable buttons when unsaved new user opened his profile
                Log.w(TAG, "User is null, no such user");
            }
        }
        @Override
        public void onCancelled(DatabaseError databaseError) {
            // Getting Post failed, log a message
            Log.w(TAG, "loadPost:onCancelled", databaseError.toException());

        }
    };

    // A listener for chats count changes
    private ValueEventListener chatsCountListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // Get Post object and use the values to update the UI
            if (dataSnapshot.exists()) {
                // Get chats count value
                Log.d(TAG, "getChatCount dataSnapshot key: " + dataSnapshot.getKey()+ " count= "+dataSnapshot.getChildrenCount()
                        +" Query= "+chatsCountQuery + " Listener= "+chatsCountListener);

                //mCurrentUser = dataSnapshot.getValue(User.class);
                mChatsCount.postValue(dataSnapshot.getChildrenCount());
            } else {
                // User is null, error out
                mChatsCount.postValue(0L);
                Log.w(TAG, "chats are null, no such dataSnapshot for chats counter");
            }
        }
        @Override
        public void onCancelled(DatabaseError databaseError) {
            // Getting Post failed, log a message
            Log.w(TAG, "loadPost:onCancelled", databaseError.toException());

        }
    };

    // A listener for notifications count changes
    private ValueEventListener notificationsCountListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // Get Post object and use the values to update the UI
            if (dataSnapshot.exists()) {
                // Get chats count value
                Log.d(TAG, "getNotificationsCount dataSnapshot key: " + dataSnapshot.getKey()+ " count= "+ dataSnapshot.getChildrenCount()
                        +" Query= "+notificationCountQuery + " Listener= "+notificationsCountListener);
                //mCurrentUser = dataSnapshot.getValue(User.class);
                mNotificationsCount.postValue(dataSnapshot.getChildrenCount());
            } else {
                // Notifications don't exist, send 0
                mNotificationsCount.postValue(0L);
                Log.w(TAG, "Notifications are null, no such dataSnapshot for notification counter");
            }
        }
        @Override
        public void onCancelled(DatabaseError databaseError) {
            // Getting Post failed, log a message
            Log.e(TAG, "loadPost:onCancelled", databaseError.toException());

        }
    };

    public MainActivityRepository(){
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        mChatsRef = mDatabaseRef.child("userChats");
        mNotificationsRef = mDatabaseRef.child("notifications").child("alerts");
        isFirstLoaded = true;
        mChatsCount = new MutableLiveData<>();
        mNotificationsCount = new MutableLiveData<>();

        if(mListenersList == null){
            mListenersList = new ArrayList<>();
            Log.d(TAG, "mListenersList is null. new ArrayList is created= " + mListenersList.size());
        }else{
            Log.d(TAG, "mListenersList is not null. Size= " + mListenersList.size());
            if(mListenersList.size() >0){
                Log.d(TAG, "mListenersList is not null and not empty. Size= " + mListenersList.size()+" Remove previous listeners");
                // No need to remove old Listeners, we are gonna reuse them
                removeListeners();
            }
        }

    }

    public MutableLiveData<User> getCurrentUser(String userId){

        DatabaseReference currentUserRef = mUsersRef.child(userId);
        //final MutableLiveData<User> mCurrentUser = new MutableLiveData<>();
        Log.d(TAG, "getUser: initiated: " + userId);

        Log.d(TAG, "getUser: Listeners size= "+ mListenersList.size());
        if(mListenersList.size()== 0){
            // Need to add a new Listener
            Log.d(TAG, "getUser: adding new Listener= "+ mListenersList);
            //mListenersMap.put(postSnapshot.getRef(), mPickUpCounterListener);
            currentUserRef.addValueEventListener(currentUserListener);
            mListenersList.add(new FirebaseListeners(currentUserRef, currentUserListener));
        }else{
            Log.d(TAG, "getUser: postSnapshot Listeners size is not 0= "+ mListenersList.size());
            //there is an old Listener, need to check if it's on this ref
            for (int i = 0; i < mListenersList.size(); i++) {
                //Log.d(TAG, "getChatsCount Listeners ref= "+ mListenersList.get(i).getQueryOrRef()+ " Listener= "+ mListenersList.get(i).getListener());
                if(mListenersList.get(i).getListener().equals(currentUserListener) &&
                        !mListenersList.get(i).getQueryOrRef().equals(currentUserRef)){
                    // We used this listener before, but on another Ref
                    Log.d(TAG, "getUser: We used this listener before, is it on the same ref?");
                    Log.d(TAG, "getUser: adding new Listener= "+ currentUserListener);
                    currentUserRef.addValueEventListener(currentUserListener);
                    mListenersList.add(new FirebaseListeners(currentUserRef, currentUserListener));
                }else if((mListenersList.get(i).getListener().equals(currentUserListener) &&
                        mListenersList.get(i).getQueryOrRef().equals(currentUserRef))){
                    //there is old Listener on the ref
                    Log.d(TAG, "getUser: Listeners= there is old Listener on the ref= "+mListenersList.get(i).getQueryOrRef()+ " Listener= " + mListenersList.get(i).getListener());
                }else{
                    //userListener is never used
                    Log.d(TAG, "getUser: Listener is never created");
                    currentUserRef.addValueEventListener(currentUserListener);
                    mListenersList.add(new FirebaseListeners(currentUserRef, currentUserListener));
                }
            }
        }

        for (int i = 0; i < mListenersList.size(); i++) {
            Log.d(TAG, "getUser: loop throw Listeners ref= "+ mListenersList.get(i).getQueryOrRef()+ " Listener= "+ mListenersList.get(i).getListener());
        }
        return mCurrentUser;
    }
    public MutableLiveData<Long> getInboxCount(String userId){

        // order by members/userKey/read get only false results
        Query chatsQuery = mChatsRef.child(userId)
                .orderByChild(DATABASE_REF_CHATS_MEMBERS +"/"+ userId +"/"+ DATABASE_REF_CHATS_MEMBER_READ).equalTo(false);

        Log.d(TAG, "getInboxCount: initiated: " + userId+ " chatsQuery= "+chatsQuery);

        Log.d(TAG, "getInboxCount: ListenersList size= "+ mListenersList.size());

        if(mListenersList.size()== 0){
            // Need to add a new Listener
            Log.d(TAG, "getInboxCount: adding new Listener= "+ mListenersList);
            //mListenersMap.put(postSnapshot.getRef(), mPickUpCounterListener);
            chatsQuery.addValueEventListener(chatsCountListener);
            mListenersList.add(new FirebaseListeners(chatsQuery, chatsCountListener));
        }else{
            Log.d(TAG, "getInboxCount: postSnapshot Listeners size is not 0= "+ mListenersList.size());
            //there is an old Listener, need to check if it's on this ref
            for (int i = 0; i < mListenersList.size(); i++) {
                //Log.d(TAG, "getChatsCount Listeners ref= "+ mListenersList.get(i).getQueryOrRef()+ " Listener= "+ mListenersList.get(i).getListener());
                if(mListenersList.get(i).getListener().equals(chatsCountListener) &&
                        !mListenersList.get(i).getQueryOrRef().equals(chatsQuery)){
                    // We used this listener before, but on another Ref
                    Log.d(TAG, "getInboxCount: We used this listener before, is it on the same ref?");
                    Log.d(TAG, "getInboxCount: adding new Listener= "+ chatsCountListener);
                    chatsQuery.addValueEventListener(chatsCountListener);
                    mListenersList.add(new FirebaseListeners(chatsQuery, chatsCountListener));
                }else if((mListenersList.get(i).getListener().equals(chatsCountListener) &&
                        mListenersList.get(i).getQueryOrRef().equals(chatsQuery))){
                    //there is old Listener on the ref
                    Log.d(TAG, "getInboxCount: Listeners= there is old Listener on the ref= "+mListenersList.get(i).getQueryOrRef()+ " Listener= " + mListenersList.get(i).getListener());
                }else{
                    //userListener is never used
                    Log.d(TAG, "getInboxCount: Listener is never created");
                    chatsQuery.addValueEventListener(chatsCountListener);
                    mListenersList.add(new FirebaseListeners(chatsQuery, chatsCountListener));
                }
            }
        }

        for (int i = 0; i < mListenersList.size(); i++) {
            Log.d(TAG, "getInboxCount: loop throw Listeners ref= "+ mListenersList.get(i).getQueryOrRef()+ " Listener= "+ mListenersList.get(i).getListener());
        }
        return mChatsCount;
    }

    public MutableLiveData<Long> getNotificationsCount(String userId){

        // order by members/userKey/read get only false results
        notificationCountQuery = mNotificationsRef.child(userId)
                .orderByChild(DATABASE_REF_NOTIFICATIONS_SEEN).equalTo(false);

        Log.d(TAG, "getNotificationsCount initiated: " + userId+ " chatsQuery= "+notificationCountQuery);
        Log.d(TAG, "getNotificationsCount ListenersList size= "+ mListenersList.size());

        if(mListenersList.size()== 0){
            // Need to add a new Listener
            Log.d(TAG, "getNotificationsCount adding new Listener= "+ mListenersList);
            //mListenersMap.put(postSnapshot.getRef(), mPickUpCounterListener);
            notificationCountQuery.addValueEventListener(notificationsCountListener);
            mListenersList.add(new FirebaseListeners(notificationCountQuery, notificationsCountListener));
        }else{
            Log.d(TAG, "getNotificationsCount: postSnapshot Listeners size is not 0= "+ mListenersList.size());
            //there is an old Listener, need to check if it's on this ref
            for (int i = 0; i < mListenersList.size(); i++) {
                //Log.d(TAG, "getChatsCount Listeners ref= "+ mListenersList.get(i).getQueryOrRef()+ " Listener= "+ mListenersList.get(i).getListener());
                if(mListenersList.get(i).getListener().equals(notificationsCountListener) &&
                        !mListenersList.get(i).getQueryOrRef().equals(notificationCountQuery)){
                    // We used this listener before, but on another Ref
                    Log.d(TAG, "getNotificationsCount: We used this Listener before, is it on the same ref?");
                    Log.d(TAG, "getNotificationsCount: adding new Listener = "+ notificationsCountListener);
                    notificationCountQuery.addValueEventListener(notificationsCountListener);
                    mListenersList.add(new FirebaseListeners(notificationCountQuery, notificationsCountListener));
                }else if((mListenersList.get(i).getListener().equals(notificationsCountListener) &&
                        mListenersList.get(i).getQueryOrRef().equals(notificationCountQuery))){
                    //there is old Listener on the ref
                    Log.d(TAG, "getNotificationsCount: Listeners= there is old Listener on the ref= "+mListenersList.get(i).getQueryOrRef()+ " Listener= " + mListenersList.get(i).getListener());
                }else{
                    //userListener is never used
                    Log.d(TAG, "getNotificationsCount: Listener is never created");
                    notificationCountQuery.addValueEventListener(notificationsCountListener);
                    mListenersList.add(new FirebaseListeners(notificationCountQuery, notificationsCountListener));
                }
            }
        }

        for (int i = 0; i < mListenersList.size(); i++) {
            Log.d(TAG, "getNotificationsCount: loop throw Listeners ref= "+ mListenersList.get(i).getQueryOrRef()+ " Listener= "+ mListenersList.get(i).getListener());
        }
        return mNotificationsCount;
    }

    public void printListeners(){
        for (int i = 0; i < mListenersList.size(); i++) {
            Log.d(TAG, "Listeners Query or Ref= "+ mListenersList.get(i).getQueryOrRef()+ " Listener= "+ mListenersList.get(i).getListener());
        }
    }


    public void removeListeners(){
        if(null != mListenersList){
            for (int i = 0; i < mListenersList.size(); i++) {
                Log.d(TAG, "remove Listeners Query or Ref= "+ mListenersList.get(i).getQueryOrRef()+ " Listener= "+ mListenersList.get(i).getListener());
                if(null != mListenersList.get(i).getListener()){
                    mListenersList.get(i).getQueryOrRef().removeEventListener(mListenersList.get(i).getListener());
                }
            }
            mListenersList.clear();
        }
    }
 }

