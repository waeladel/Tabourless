package com.tabourless.queue.data;

import android.text.TextUtils;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.tabourless.queue.models.FirebaseListeners;
import com.tabourless.queue.models.Relation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.tabourless.queue.Utils.DatabaseKeys.getJoinedKeys;

public class RelationRepository {

    private final static String TAG = RelationRepository.class.getSimpleName();

    // [START declare_database_ref]
    private DatabaseReference mDatabaseRef;
    private DatabaseReference mRelationRef;
    private MutableLiveData<Relation> mRelation;


    // requests and relations status
    private static final String RELATION_STATUS_BLOCKING = "blocking"; // the selected user is blocking me (current user)
    private static final String RELATION_STATUS_BLOCKED= "blocked"; // the selected user is blocked by me (current user)

    // HashMap to keep track of Firebase Listeners
    //private HashMap< DatabaseReference , ValueEventListener> mListenersMap;
    private List<FirebaseListeners> mListenersList;// = new ArrayList<>();

    // a listener for mRelation changes
    private ValueEventListener mRelationListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // Get Post object and use the values to update the UI
            if (dataSnapshot.exists()) {
                // Get user value
                Log.d(TAG, "getRelation dataSnapshot key: "
                        + dataSnapshot.getKey()+" Listener = "+ mRelationListener);
                //mRelation = dataSnapshot.getValue(User.class);
                mRelation.postValue(dataSnapshot.getValue(Relation.class));
            } else {
                // User is null, error out
                mRelation.postValue(null);
                Log.w(TAG, "Relation  is null, no relation exist");
            }
        }
        @Override
        public void onCancelled(DatabaseError databaseError) {
            // Getting Post failed, log a message
            Log.w(TAG, "loadPost:onCancelled", databaseError.toException());

        }
    };

    public RelationRepository(){
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        mRelationRef = mDatabaseRef.child("relations");
        mRelation = new MutableLiveData<>();

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

    // Get relation if any between current user and selected user
    public MutableLiveData<Relation> getRelation(String currentUserId , String userId){

        DatabaseReference currentUserRelationRef = mRelationRef.child(currentUserId).child(userId);
        //final MutableLiveData<User> mRelation = new MutableLiveData<>();
        Log.d(TAG, "getCurrentUserRelation initiated: currentUserId= " + currentUserId+ " userId= "+userId );


        Log.d(TAG, "getCurrentUserRelation Listeners size= "+ mListenersList.size());
        if(mListenersList.size()== 0){
            // Need to add a new Listener
            Log.d(TAG, "getCurrentUserRelation adding new Listener= "+ mRelationListener);
            //mListenersMap.put(postSnapshot.getRef(), mRelationListener);
            currentUserRelationRef.addValueEventListener(mRelationListener);
            mListenersList.add(new FirebaseListeners(currentUserRelationRef, mRelationListener));
        }else{
            Log.d(TAG, "getCurrentUserRelation Listeners size is not 0= "+ mListenersList.size());
            //there is an old Listener, need to check if it's on this ref
            for (int i = 0; i < mListenersList.size(); i++) {
                //Log.d(TAG, "getUser Listeners ref= "+ mListenersList.get(i).getQueryOrRef()+ " Listener= "+ mListenersList.get(i).getListener());
                if(!mListenersList.get(i).getQueryOrRef().equals(currentUserRelationRef)
                        && (mListenersList.get(i).getListener().equals(mRelationListener))){
                    // This ref doesn't has a listener. Need to add a new Listener
                    Log.d(TAG, "getCurrentUserRelation adding new Listener= "+ mRelationListener);
                    currentUserRelationRef.addValueEventListener(mRelationListener);
                    mListenersList.add(new FirebaseListeners(currentUserRelationRef, mRelationListener));
                }else if(mListenersList.get(i).getQueryOrRef().equals(currentUserRelationRef)
                        && (mListenersList.get(i).getListener().equals(mRelationListener))){
                    //there is old Listener on the ref
                    Log.d(TAG, "getCurrentUserRelation Listeners= there is old Listener on the ref= "+mListenersList.get(i).getQueryOrRef()+ " Listener= " + mListenersList.get(i).getListener());
                }else{
                    //CounterListener is never used
                    Log.d(TAG, "Listener is never created");
                    currentUserRelationRef.addValueEventListener(mRelationListener);
                    mListenersList.add(new FirebaseListeners(currentUserRelationRef, mRelationListener));
                }
            }
        }
        for (int i = 0; i < mListenersList.size(); i++) {
            Log.d(TAG, "getCurrentUserRelation loop throw Listeners ref= "+ mListenersList.get(i).getQueryOrRef()+ " Listener= "+ mListenersList.get(i).getListener());
        }
        return mRelation;
    }

    // Block user without deleting conversation
    public void blockUser(String currentUserId, String userId) {
        Map<String, Object> childUpdates = new HashMap<>();
        //Cancel likes i (current user) sent to this (target user). Keep like he sent to me (current user)
        childUpdates.put("/favorites/" + currentUserId + "/" + userId, null);
        //likes is to display who send likes to this particular user
        childUpdates.put("/likes/" + userId + "/" + currentUserId, null);

        // Update relations to blocking (current user) and blocked (target user)
        childUpdates.put("/relations/" + currentUserId + "/" + userId+ "/status", RELATION_STATUS_BLOCKED);
        childUpdates.put("/relations/" + userId + "/" + currentUserId+ "/status", RELATION_STATUS_BLOCKING);

        // Chat ID is not passed from MainFragment, we need to create
        String chatId = getJoinedKeys(currentUserId , userId);
        // update chat active to -1, which means it's blocked chat room
        childUpdates.put("/chats/" + chatId +"/active",-1);

        // Delete chats with this person from chats recycler view
        /*childUpdates.put("/userChats/" + currentUserId + "/" + chatId, null);
        childUpdates.put("/userChats/" + userId + "/" + chatId, null);*/

        // Delete notifications


        mDatabaseRef.updateChildren(childUpdates).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // Write was successful!
                Log.i(TAG, "block onSuccess");
                // ...
            }
        });
    }

    // Block user and delete the conversation (userChat table)
    public void blockDelete(String currentUserId, String userId) {
        Map<String, Object> childUpdates = new HashMap<>();
        //Cancel likes i (current user) sent to this (target user). Keep like he sent to me (current user)
        childUpdates.put("/favorites/" + currentUserId + "/" + userId, null);
        //likes is to display who send likes to this particular user
        childUpdates.put("/likes/" + userId + "/" + currentUserId, null);

        // Update relations to blocking (current user) and blocked (target user)
        childUpdates.put("/relations/" + currentUserId + "/" + userId+ "/status", RELATION_STATUS_BLOCKED);
        childUpdates.put("/relations/" + userId + "/" + currentUserId+ "/status", RELATION_STATUS_BLOCKING);

        // Chat ID is not passed from MainFragment, we need to create
        String chatId = getJoinedKeys(currentUserId , userId);
        // update chat active to -1, which means it's blocked chat room
        childUpdates.put("/chats/" + chatId +"/active",-1);

        // Delete chats with this person from chats recycler view
        childUpdates.put("/userChats/" + currentUserId + "/" + chatId, null);
        childUpdates.put("/userChats/" + userId + "/" + chatId, null);

        // Delete notifications


        mDatabaseRef.updateChildren(childUpdates).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // Write was successful!
                Log.i(TAG, "block onSuccess");
                // ...
            }
        });
    }

    // Delete blocking/blocked relation to start fresh
    public void unblockUser(String currentUserId, String userId) {
        Map<String, Object> childUpdates = new HashMap<>();

        // Update relations to null. To start fresh
        childUpdates.put("/relations/" + currentUserId + "/" + userId, null);
        childUpdates.put("/relations/" + userId + "/" + currentUserId, null);

        // Chat ID is not passed from MainFragment, we need to create
        String chatId = getJoinedKeys(currentUserId , userId);
        // update chat to null, to delete the chat room and start fresh
        childUpdates.put("/chats/" + chatId ,null);

        mDatabaseRef.updateChildren(childUpdates).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // Write was successful!
                Log.i(TAG, "block onSuccess");
                // ...
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

