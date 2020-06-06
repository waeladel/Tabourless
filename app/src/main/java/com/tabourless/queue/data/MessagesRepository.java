package com.tabourless.queue.data;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.tabourless.queue.interfaces.FirebaseMessageCallback;
import com.tabourless.queue.models.Chat;
import com.tabourless.queue.models.FirebaseListeners;
import com.tabourless.queue.models.Message;
import com.tabourless.queue.models.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.tabourless.queue.App.DATABASE_REF_CHATS;
import static com.tabourless.queue.App.DATABASE_REF_MESSAGES;
import static com.tabourless.queue.App.DATABASE_REF_USERS;

public class MessagesRepository {

    private final static String TAG = MessagesRepository.class.getSimpleName();

    // [START declare_database_ref]
    private DatabaseReference mDatabaseRef;
    private DatabaseReference mUsersRef , mMessagesRef, mChatRef;
    private Boolean isFirstLoaded = true;

    // Not static to only remove listeners of this repository instance
    // Start destination fragment is never destroyed , so when clicking on it's bottom navigation icon again it got destroyed to be recreated
    // When that happens clearing listeners is triggered on viewmodel Cleared, which removes that new listeners for the just added query
    // When new listener is removed we got 0 results and have no listeners for updates.
    private List<FirebaseListeners> mListenersList;

    private MutableLiveData<User> mUser, mCurrentUser;
    private MutableLiveData<String> mSenderId;
    private MutableLiveData<Chat> mChat;



    // a listener for chat User changes
    private ValueEventListener userListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // Get Post object and use the values to update the UI
            if (dataSnapshot.exists()) {
                // Get user value
                Log.d(TAG, "getUser dataSnapshot key: "
                        + dataSnapshot.getKey()+" Listener = "+userListener);
                //mCurrentUser = dataSnapshot.getValue(User.class);
                mUser.postValue(dataSnapshot.getValue(User.class));
            } else {
                // User is null, error out
                Log.w(TAG, "User is null, no such user");
            }
        }
        @Override
        public void onCancelled(DatabaseError databaseError) {
            // Getting Post failed, log a message
            Log.w(TAG, "loadPost:onCancelled", databaseError.toException());

        }
    };

    // a listener for current User changes
    private ValueEventListener currentUserListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // Get Post object and use the values to update the UI
            if (dataSnapshot.exists()) {
                // Get user value
                Log.d(TAG, "currentUserListener dataSnapshot key: "
                        + dataSnapshot.getKey()+" Listener = "+currentUserListener);
                mCurrentUser.postValue(dataSnapshot.getValue(User.class));
            } else {
                // User is null, error out
                Log.w(TAG, "currentUserListener is null, no such user");
            }
        }
        @Override
        public void onCancelled(DatabaseError databaseError) {
            // Getting Post failed, log a message
            Log.w(TAG, "currentUserListener: loadPost:onCancelled", databaseError.toException());

        }
    };



    // a listener for chat  changes
    private ValueEventListener chatListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // Get Post object and use the values to update the UI
            if (dataSnapshot.exists()) {
                // Get user value
                Log.d(TAG, "chat dataSnapshot key: "
                        + dataSnapshot.getKey()+" Listener = "+chatListener);
                //mCurrentUser = dataSnapshot.getValue(User.class);
                mChat.postValue(dataSnapshot.getValue(Chat.class));
            } else {
                // Chat is null, error out
                Log.w(TAG, "chat is null, no such chat");
                // Return null because we need to know when chat is deleted due to unblocking user
                // We delete the chat node when unblocking a user to start fresh.
                mChat.postValue(null);
            }
        }
        @Override
        public void onCancelled(DatabaseError databaseError) {
            // Getting Post failed, log a message
            Log.w(TAG, "load chat:onCancelled", databaseError.toException());

        }
    };

    public MessagesRepository(){
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        mUsersRef = mDatabaseRef.child(DATABASE_REF_USERS);
        mMessagesRef = mDatabaseRef.child(DATABASE_REF_MESSAGES);
        mChatRef = mDatabaseRef.child(DATABASE_REF_CHATS);
        isFirstLoaded = true;
        Log.d(TAG, "MessagesRepository init. isFirstLoaded is true");
        mUser = new MutableLiveData<>();
        mCurrentUser = new MutableLiveData<>();
        mSenderId = new MutableLiveData<>();
        mChat = new MutableLiveData<>();
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

    public MutableLiveData<User> getUser(String userId){

        DatabaseReference userRef = mUsersRef.child(userId);
        //final MutableLiveData<User> mCurrentUser = new MutableLiveData<>();
        Log.d(TAG, "getUser initiated: " + userId);
        Log.d(TAG, "getUser mListenersList size= "+ mListenersList.size());

        if(mListenersList.size()== 0){
            // Need to add a new Listener
            Log.d(TAG, "getUser adding new Listener= "+ mListenersList);
            //mListenersMap.put(postSnapshot.getRef(), mPickUpCounterListener);
            userRef.addValueEventListener(userListener);
            mListenersList.add(new FirebaseListeners(userRef, userListener));
        }else{
            Log.d(TAG, "postSnapshot Listeners size is not 0= "+ mListenersList.size());
            //there is an old Listener, need to check if it's on this ref
            for (int i = 0; i < mListenersList.size(); i++) {
                //Log.d(TAG, "getUser Listeners ref= "+ mListenersList.get(i).getQueryOrRef()+ " Listener= "+ mListenersList.get(i).getListener());
                if(mListenersList.get(i).getListener().equals(userListener) &&
                        !mListenersList.get(i).getQueryOrRef().equals(userRef)){
                    // We used this listener before, but on another Ref
                    Log.d(TAG, "We used this listener before, is it on the same ref?");
                    Log.d(TAG, "getUser adding new Listener= "+ userListener);
                    userRef.addValueEventListener(userListener);
                    mListenersList.add(new FirebaseListeners(userRef, userListener));
                }else if((mListenersList.get(i).getListener().equals(userListener) &&
                        mListenersList.get(i).getQueryOrRef().equals(userRef))){
                    //there is old Listener on the ref
                    Log.d(TAG, "getUser Listeners= there is old Listener on the ref= "+mListenersList.get(i).getQueryOrRef()+ " Listener= " + mListenersList.get(i).getListener());
                }else{
                    //userListener is never used
                    Log.d(TAG, "Listener is never created");
                    userRef.addValueEventListener(userListener);
                    mListenersList.add(new FirebaseListeners(userRef, userListener));
                }
            }
        }

        for (int i = 0; i < mListenersList.size(); i++) {
            Log.d(TAG, "getUser loop throw ref= " + mListenersList.get(i).getQueryOrRef() + " Listener= " + mListenersList.get(i).getListener());
        }

        Log.d(TAG, "getUser= "+ mUser);

        return mUser;
    }


    public MutableLiveData<User> getCurrentUser(String userId){

        DatabaseReference userRef = mUsersRef.child(userId);
        //final MutableLiveData<User> mCurrentUser = new MutableLiveData<>();
        Log.d(TAG, "getCurrentUser initiated: " + userId);
        Log.d(TAG, "getCurrentUser mListenersList size= "+ mListenersList.size());

        if(mListenersList.size()== 0){
            // Need to add a new Listener
            Log.d(TAG, "getCurrentUser adding new Listener= "+ mListenersList);
            //mListenersMap.put(postSnapshot.getRef(), mPickUpCounterListener);
            userRef.addValueEventListener(currentUserListener);
            mListenersList.add(new FirebaseListeners(userRef, currentUserListener));
        }else{
            Log.d(TAG, "getCurrentUser postSnapshot Listeners size is not 0= "+ mListenersList.size());
            //there is an old Listener, need to check if it's on this ref
            for (int i = 0; i < mListenersList.size(); i++) {
                //Log.d(TAG, "getUser Listeners ref= "+ mListenersList.get(i).getQueryOrRef()+ " Listener= "+ mListenersList.get(i).getListener());
                if(mListenersList.get(i).getListener().equals(currentUserListener) &&
                        !mListenersList.get(i).getQueryOrRef().equals(userRef)){
                    // We used this listener before, but on another Ref
                    Log.d(TAG, "We used this listener before, is it on the same ref?");
                    Log.d(TAG, "getCurrentUser adding new Listener= "+ currentUserListener);
                    userRef.addValueEventListener(currentUserListener);
                    mListenersList.add(new FirebaseListeners(userRef, currentUserListener));
                }else if((mListenersList.get(i).getListener().equals(currentUserListener) &&
                        mListenersList.get(i).getQueryOrRef().equals(userRef))){
                    //there is old Listener on the ref
                    Log.d(TAG, "getCurrentUser Listeners= there is old Listener on the ref= "+mListenersList.get(i).getQueryOrRef()+ " Listener= " + mListenersList.get(i).getListener());
                }else{
                    //currentUserListener is never used
                    Log.d(TAG, "getCurrentUser Listener is never created");
                    userRef.addValueEventListener(currentUserListener);
                    mListenersList.add(new FirebaseListeners(userRef, currentUserListener));
                }
            }
        }

        for (int i = 0; i < mListenersList.size(); i++) {
            Log.d(TAG, "getCurrentUser loop throw ref= " + mListenersList.get(i).getQueryOrRef() + " Listener= " + mListenersList.get(i).getListener());
        }

        Log.d(TAG, "getCurrentUser= "+ mCurrentUser);

        return mCurrentUser;
    }

    /*public MutableLiveData<String> getSenderId (String chatId){

        DatabaseReference MessagesRef = mMessagesRef.child(chatId);
        //final MutableLiveData<User> mCurrentUser = new MutableLiveData<>();
        Log.d(TAG, "getUser initiated: " + chatId);
        Log.d(TAG, "getUser Listeners userListener= "+ mListenersList.size());
        MessagesRef.limitToFirst(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Get Post object and use the values to update the UI
                if (dataSnapshot.exists()) {

                    List<Message> messagesList = new ArrayList<>();
                    // loop throw users value
                    for (DataSnapshot snapshot: dataSnapshot.getChildren()){
                        Message message = snapshot.getValue(Message.class);
                        if (message != null) {
                            message.setKey(snapshot.getKey());
                        }
                        messagesList.add(message);
                    }
                    // Get user value
                    Log.d(TAG, "senderId dataSnapshot key: "
                            + messagesList.get(0).getSenderId());
                    //mCurrentUser = dataSnapshot.getValue(User.class);
                    mSenderId.postValue(messagesList.get(0).getSenderId());
                } else {
                    // User is null, error out
                    Log.w(TAG, "senderId is null, no such user");
                    mSenderId.postValue(null);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
            }
        });


        return mSenderId;
    }*/

    public MutableLiveData<Chat> getChat(String chatId){

        DatabaseReference chatRef = mChatRef.child(chatId);
        //final MutableLiveData<User> mCurrentUser = new MutableLiveData<>();
        Log.d(TAG, "getChat initiated: " + chatId);
        Log.d(TAG, "getChat mListenersList size= "+ mListenersList.size());
        if(mListenersList.size()== 0){
            // Need to add a new Listener
            Log.d(TAG, "getChat adding new Listener= "+ mListenersList);
            //mListenersMap.put(postSnapshot.getRef(), mPickUpCounterListener);
            chatRef.addValueEventListener(chatListener);
            mListenersList.add(new FirebaseListeners(chatRef, chatListener));
        }else{
            Log.d(TAG, "postSnapshot Listeners size is not 0= "+ mListenersList.size());
            //there is an old Listener, need to check if it's on this ref
            for (int i = 0; i < mListenersList.size(); i++) {
                //Log.d(TAG, "getUser Listeners ref= "+ mListenersList.get(i).getQueryOrRef()+ " Listener= "+ mListenersList.get(i).getListener());
                if(mListenersList.get(i).getListener().equals(chatListener) &&
                        !mListenersList.get(i).getQueryOrRef().equals(chatRef)){
                    // We used this listener before, but on another Ref
                    Log.d(TAG, "We used this listener before, is it on the same ref?");
                    Log.d(TAG, "getChat adding new Listener= "+ chatListener);
                    chatRef.addValueEventListener(chatListener);
                    mListenersList.add(new FirebaseListeners(chatRef, chatListener));
                }else if((mListenersList.get(i).getListener().equals(chatListener) &&
                        mListenersList.get(i).getQueryOrRef().equals(chatRef))){
                    //there is old Listener on the ref
                    Log.d(TAG, "getChat Listeners= there is old Listener on the ref= "+mListenersList.get(i).getQueryOrRef()+ " Listener= " + mListenersList.get(i).getListener());
                }else{
                    //chatListener is never used
                    Log.d(TAG, "Listener is never created");
                    chatRef.addValueEventListener(chatListener);
                    mListenersList.add(new FirebaseListeners(chatRef, chatListener));
                }
            }
        }

        /*for (int i = 0; i < mListenersList.size(); i++) {
            Log.d(TAG, "getChat loop throw ref= "+ mListenersList.get(i).getQueryOrRef()+ " Listener= "+ mListenersList.get(i).getListener());
        }*/

        return mChat;
    }

    // get the last message
    public void getLastMessageOnce(String chatId, final FirebaseMessageCallback callback){

        final DatabaseReference messageRef = mMessagesRef.child(chatId);
        Query LastMessageQuery;
        Log.d(TAG, "getLastMessageOnce initiated: chatId= " + chatId);

        LastMessageQuery = messageRef.orderByKey().limitToLast(1);

        LastMessageQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Get user value
                    Log.d(TAG, "getLastMessageOnce dataSnapshot key: "
                            + dataSnapshot.getKey());

                    // loop throw messages value
                    for (DataSnapshot snapshot: dataSnapshot.getChildren()) {
                        Message message = snapshot.getValue(Message.class);
                        if (message != null) {
                            message.setKey(snapshot.getKey());
                            Log.d(TAG, "getLastMessageOnce message key: "
                                    + message.getKey());
                            callback.onCallback(message);
                            return;
                        }
                    }

                } else {
                    // User is null, error out
                    Log.w(TAG, "getUserOnce User is null, no such user");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "getUserOnce User onCancelled" +databaseError);
            }
        });
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
