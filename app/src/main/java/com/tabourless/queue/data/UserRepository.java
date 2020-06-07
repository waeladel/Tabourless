package com.tabourless.queue.data;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.tabourless.queue.interfaces.FirebaseUserCallback;
import com.tabourless.queue.models.FirebaseListeners;
import com.tabourless.queue.models.User;
import java.util.ArrayList;
import java.util.List;

import static com.tabourless.queue.App.DATABASE_REF_CHATS_MEMBERS;
import static com.tabourless.queue.App.DATABASE_REF_CHATS_MEMBER_READ;
import static com.tabourless.queue.App.DATABASE_REF_NOTIFICATIONS;
import static com.tabourless.queue.App.DATABASE_REF_NOTIFICATIONS_ALERTS;
import static com.tabourless.queue.App.DATABASE_REF_NOTIFICATIONS_SEEN;
import static com.tabourless.queue.App.DATABASE_REF_USERS;
import static com.tabourless.queue.App.DATABASE_REF_USER_CHATS;
import static com.tabourless.queue.App.DATABASE_REF_USER_AVATAR;
import static com.tabourless.queue.App.DATABASE_REF_USER_COVER;

public class UserRepository {

    private final static String TAG = UserRepository.class.getSimpleName();

    // [START declare_database_ref]
    private DatabaseReference mDatabaseRef;
    private DatabaseReference mUsersRef, mChatsRef, mNotificationsRef;
    private Boolean isFirstLoaded = true;

    private MutableLiveData<User> mCurrentUser;
    private MutableLiveData<User> mUser;
    private MutableLiveData<User> mSingleValueUser;
    private MutableLiveData<Long> mChatsCount, mNotificationsCount;

    // HashMap to keep track of Firebase Listeners
    // Not static to only remove listeners of this repository instance
    // Start destination fragment is never destroyed , so when clicking on it's bottom navigation icon again it got destroyed to be recreated
    // When that happens clearing listeners is triggered on viewmodel Cleared, which removes that new listeners for the just added query
    // When new listener is removed we got 0 results and have no listeners for updates.
    private List<FirebaseListeners> mListenersList;

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
                Log.d(TAG, "getChatCount dataSnapshot key: "
                        + dataSnapshot.getKey()+ " count= "+dataSnapshot.getChildrenCount()+" Listener = "+chatsCountListener);
                //mCurrentUser = dataSnapshot.getValue(User.class);
                mChatsCount.postValue(dataSnapshot.getChildrenCount());
            } else {
                // User is null, error out
                mChatsCount.postValue(0L);
                Log.w(TAG, "User is null, no such user");
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
                Log.d(TAG, "getNotificationsCount dataSnapshot key: "
                        + dataSnapshot.getKey()+ " count= "+dataSnapshot.getChildrenCount()+" Listener = "+notificationsCountListener);
                //mCurrentUser = dataSnapshot.getValue(User.class);
                mNotificationsCount.postValue(dataSnapshot.getChildrenCount());
            } else {
                // User is null, error out
                mNotificationsCount.postValue(0L);
                Log.w(TAG, "User is null, no such user");
            }
        }
        @Override
        public void onCancelled(DatabaseError databaseError) {
            // Getting Post failed, log a message
            Log.w(TAG, "loadPost:onCancelled", databaseError.toException());

        }
    };

    public UserRepository(){
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        mUsersRef = mDatabaseRef.child(DATABASE_REF_USERS);
        mChatsRef = mDatabaseRef.child(DATABASE_REF_USER_CHATS);
        mNotificationsRef = mDatabaseRef.child(DATABASE_REF_NOTIFICATIONS).child(DATABASE_REF_NOTIFICATIONS_ALERTS);
        //usersList = new ArrayList<>();
        //entireUsersList = new ArrayList<>();
        isFirstLoaded = true;
        mCurrentUser = new MutableLiveData<>();
        mUser = new MutableLiveData<>();
        mSingleValueUser = new MutableLiveData<>();
        mChatsCount = new MutableLiveData<>();
        mNotificationsCount = new MutableLiveData<>();
        //mListenersMap =  new HashMap<>();
        /*if(mListenersList == null && mListenersList.size() == 0){
            mListenersList = new ArrayList<>();
        }
        if(mListenersList != null){
            Log.d(TAG, "mama UsersRepository init. isFirstLoaded= " + mListenersList.size());
        }
        mListenersList = new ArrayList<>();*/
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

    public MutableLiveData<User> getCurrentUser(String userId){

        DatabaseReference currentUserRef = mUsersRef.child(userId);
        //final MutableLiveData<User> mCurrentUser = new MutableLiveData<>();
        Log.d(TAG, "getUser initiated: " + userId);

        Log.d(TAG, "getUser Listeners size= "+ mListenersList.size());
        if(mListenersList.size()== 0){
            // Need to add a new Listener
            Log.d(TAG, "getChatsCount adding new Listener= "+ mListenersList);
            //mListenersMap.put(postSnapshot.getRef(), mPickUpCounterListener);
            currentUserRef.addValueEventListener(currentUserListener);
            mListenersList.add(new FirebaseListeners(currentUserRef, currentUserListener));
        }else{
            Log.d(TAG, "postSnapshot Listeners size is not 0= "+ mListenersList.size());
            //there is an old Listener, need to check if it's on this ref
            for (int i = 0; i < mListenersList.size(); i++) {
                //Log.d(TAG, "getChatsCount Listeners ref= "+ mListenersList.get(i).getQueryOrRef()+ " Listener= "+ mListenersList.get(i).getListener());
                if(mListenersList.get(i).getListener().equals(currentUserListener) &&
                        !mListenersList.get(i).getQueryOrRef().equals(currentUserRef)){
                    // We used this listener before, but on another Ref
                    Log.d(TAG, "We used this listener before, is it on the same ref?");
                    Log.d(TAG, "getChatsCount adding new Listener= "+ currentUserListener);
                    currentUserRef.addValueEventListener(currentUserListener);
                    mListenersList.add(new FirebaseListeners(currentUserRef, currentUserListener));
                }else if((mListenersList.get(i).getListener().equals(currentUserListener) &&
                        mListenersList.get(i).getQueryOrRef().equals(currentUserRef))){
                    //there is old Listener on the ref
                    Log.d(TAG, "getChatsCount Listeners= there is old Listener on the ref= "+mListenersList.get(i).getQueryOrRef()+ " Listener= " + mListenersList.get(i).getListener());
                }else{
                    //userListener is never used
                    Log.d(TAG, "getChatsCount Listener is never created");
                    currentUserRef.addValueEventListener(currentUserListener);
                    mListenersList.add(new FirebaseListeners(currentUserRef, currentUserListener));
                }
            }
        }

        for (int i = 0; i < mListenersList.size(); i++) {
            Log.d(TAG, "getUser loop throw Listeners ref= "+ mListenersList.get(i).getQueryOrRef()+ " Listener= "+ mListenersList.get(i).getListener());
        }
        return mCurrentUser;
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
                    Log.d(TAG, "getUserOnce dataSnapshot key: "
                            + dataSnapshot.getKey()+" Listener = "+currentUserListener);
                    //mSingleValueUser = dataSnapshot.getValue(User.class);
                    //mSingleValueUser.postValue(dataSnapshot.getValue(User.class));
                    //int sID = dataSnapshot.child("soundId").getValue(Integer.class);
                    //Log.d(TAG, "getUserOnce User sound id= "+ sID);
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

    public MutableLiveData<Long> getInboxCount(String userId){

        // order by members/userKey/read get only false results
        Query chatsQuery = mChatsRef.child(userId)
                .orderByChild(DATABASE_REF_CHATS_MEMBERS +"/"+ userId +"/"+ DATABASE_REF_CHATS_MEMBER_READ).equalTo(false);

        Log.d(TAG, "getChatsCount initiated: " + userId+ " chatsQuery= "+chatsQuery);

        Log.d(TAG, "getChatsCount ListenersList size= "+ mListenersList.size());

        if(mListenersList.size()== 0){
            // Need to add a new Listener
            Log.d(TAG, "getChatsCount adding new Listener= "+ mListenersList);
            //mListenersMap.put(postSnapshot.getRef(), mPickUpCounterListener);
            chatsQuery.addValueEventListener(chatsCountListener);
            mListenersList.add(new FirebaseListeners(chatsQuery, chatsCountListener));
        }else{
            Log.d(TAG, "postSnapshot Listeners size is not 0= "+ mListenersList.size());
            //there is an old Listener, need to check if it's on this ref
            for (int i = 0; i < mListenersList.size(); i++) {
                //Log.d(TAG, "getChatsCount Listeners ref= "+ mListenersList.get(i).getQueryOrRef()+ " Listener= "+ mListenersList.get(i).getListener());
                if(mListenersList.get(i).getListener().equals(chatsCountListener) &&
                        !mListenersList.get(i).getQueryOrRef().equals(chatsQuery)){
                    // We used this listener before, but on another Ref
                    Log.d(TAG, "We used this listener before, is it on the same ref?");
                    Log.d(TAG, "getChatsCount adding new Listener= "+ chatsCountListener);
                    chatsQuery.addValueEventListener(chatsCountListener);
                    mListenersList.add(new FirebaseListeners(chatsQuery, chatsCountListener));
                }else if((mListenersList.get(i).getListener().equals(chatsCountListener) &&
                        mListenersList.get(i).getQueryOrRef().equals(chatsQuery))){
                    //there is old Listener on the ref
                    Log.d(TAG, "getChatsCount Listeners= there is old Listener on the ref= "+mListenersList.get(i).getQueryOrRef()+ " Listener= " + mListenersList.get(i).getListener());
                }else{
                    //userListener is never used
                    Log.d(TAG, "getChatsCount Listener is never created");
                    chatsQuery.addValueEventListener(chatsCountListener);
                    mListenersList.add(new FirebaseListeners(chatsQuery, chatsCountListener));
                }
            }
        }

        for (int i = 0; i < mListenersList.size(); i++) {
            Log.d(TAG, "getChatsCount loop throw Listeners ref= "+ mListenersList.get(i).getQueryOrRef()+ " Listener= "+ mListenersList.get(i).getListener());
        }
        return mChatsCount;
    }

    public MutableLiveData<Long> getNotificationsCount(String userId){

        // order by members/userKey/read get only false results
        Query query = mNotificationsRef.child(userId)
                .orderByChild(DATABASE_REF_NOTIFICATIONS_SEEN).equalTo(false);

        Log.d(TAG, "getChatsCount initiated: " + userId+ " chatsQuery= "+query);

        Log.d(TAG, "getChatsCount ListenersList size= "+ mListenersList.size());

        if(mListenersList.size()== 0){
            // Need to add a new Listener
            Log.d(TAG, "getNotificationsCount adding new Listener= "+ mListenersList);
            //mListenersMap.put(postSnapshot.getRef(), mPickUpCounterListener);
            query.addValueEventListener(notificationsCountListener);
            mListenersList.add(new FirebaseListeners(query, notificationsCountListener));
        }else{
            Log.d(TAG, "postSnapshot Listeners size is not 0= "+ mListenersList.size());
            //there is an old Listener, need to check if it's on this ref
            for (int i = 0; i < mListenersList.size(); i++) {
                //Log.d(TAG, "getChatsCount Listeners ref= "+ mListenersList.get(i).getQueryOrRef()+ " Listener= "+ mListenersList.get(i).getListener());
                if(mListenersList.get(i).getListener().equals(notificationsCountListener) &&
                        !mListenersList.get(i).getQueryOrRef().equals(query)){
                    // We used this listener before, but on another Ref
                    Log.d(TAG, "We used this listener before, is it on the same ref?");
                    Log.d(TAG, "getChatsCount adding new Listener= "+ notificationsCountListener);
                    query.addValueEventListener(notificationsCountListener);
                    mListenersList.add(new FirebaseListeners(query, notificationsCountListener));
                }else if((mListenersList.get(i).getListener().equals(notificationsCountListener) &&
                        mListenersList.get(i).getQueryOrRef().equals(query))){
                    //there is old Listener on the ref
                    Log.d(TAG, "getChatsCount Listeners= there is old Listener on the ref= "+mListenersList.get(i).getQueryOrRef()+ " Listener= " + mListenersList.get(i).getListener());
                }else{
                    //userListener is never used
                    Log.d(TAG, "getChatsCount Listener is never created");
                    query.addValueEventListener(notificationsCountListener);
                    mListenersList.add(new FirebaseListeners(query, notificationsCountListener));
                }
            }
        }

        for (int i = 0; i < mListenersList.size(); i++) {
            Log.d(TAG, "getChatsCount loop throw Listeners ref= "+ mListenersList.get(i).getQueryOrRef()+ " Listener= "+ mListenersList.get(i).getListener());
        }
        return mNotificationsCount;
    }

    public void updateBrokenImage(String userId, Uri uri, boolean isAvatar) {
        DatabaseReference UserRef = mUsersRef.child(userId);
        if(isAvatar){
            // Lets update avatar
            UserRef.child(DATABASE_REF_USER_AVATAR).setValue(String.valueOf(uri));
        }else{
            // Lets update Cover
            UserRef.child(DATABASE_REF_USER_COVER).setValue(String.valueOf(uri));
        }
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

