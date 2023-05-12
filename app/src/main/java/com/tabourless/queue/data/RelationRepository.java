package com.tabourless.queue.data;

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

import static com.tabourless.queue.App.DATABASE_REF_CHATS;
import static com.tabourless.queue.App.DATABASE_REF_CHAT_ACTIVE;
import static com.tabourless.queue.App.DATABASE_REF_NOTIFICATIONS;
import static com.tabourless.queue.App.DATABASE_REF_NOTIFICATIONS_ALERTS;
import static com.tabourless.queue.App.DATABASE_REF_NOTIFICATIONS_MESSAGES;
import static com.tabourless.queue.App.DATABASE_REF_RELATIONS;
import static com.tabourless.queue.App.DATABASE_REF_RELATION_STATUS;
import static com.tabourless.queue.App.DATABASE_REF_USER_CHATS;
import static com.tabourless.queue.App.RELATION_STATUS_BLOCKED;
import static com.tabourless.queue.App.RELATION_STATUS_BLOCKING;
import static com.tabourless.queue.App.RELATION_STATUS_BLOCKING_VS_BLOCKED_BACK;
import static com.tabourless.queue.App.RELATION_STATUS_BLOCKED_VS_BLOCKING_BACK;
import static com.tabourless.queue.Utils.DatabaseHelper.getJoinedKeys;

public class RelationRepository {

    private final static String TAG = RelationRepository.class.getSimpleName();

    // [START declare_database_ref]
    private DatabaseReference mDatabaseRef;
    private DatabaseReference mRelationRef;
    private MutableLiveData<Relation> mRelation;


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
        mRelationRef = mDatabaseRef.child(DATABASE_REF_RELATIONS);
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
    public void blockUser(String currentUserId, String userId, String relation, boolean isDeleteChat) {
        Map<String, Object> childUpdates = new HashMap<>();

        if(relation.equals(RELATION_STATUS_BLOCKING)){
            // this selected user had already blocked me (current user), it's time to block back
            // Update relations to blocking/blocked_back for (current user) and blocked/blocking_back for (selected user)
            childUpdates.put(DATABASE_REF_RELATIONS + "/" + currentUserId + "/" + userId + "/" + DATABASE_REF_RELATION_STATUS, RELATION_STATUS_BLOCKING_VS_BLOCKED_BACK);
            childUpdates.put(DATABASE_REF_RELATIONS + "/" + userId + "/" + currentUserId + "/" + DATABASE_REF_RELATION_STATUS, RELATION_STATUS_BLOCKED_VS_BLOCKING_BACK);
        }else{
            // Update relations to blocking for (current user) and blocked for (selected user)
            childUpdates.put(DATABASE_REF_RELATIONS + "/" + currentUserId + "/" + userId + "/" + DATABASE_REF_RELATION_STATUS, RELATION_STATUS_BLOCKED);
            childUpdates.put(DATABASE_REF_RELATIONS + "/" + userId + "/" + currentUserId + "/" + DATABASE_REF_RELATION_STATUS, RELATION_STATUS_BLOCKING);
        }

        // Chat ID is not passed from MainFragment, we need to create
        String chatId = getJoinedKeys(currentUserId , userId);
        // update chat active to -1, which means it's blocked chat room
        childUpdates.put(DATABASE_REF_CHATS  +"/"+ chatId +"/"+ DATABASE_REF_CHAT_ACTIVE,-1);

        // to remove the conversation room if user selected to hide the conversation too
        if(isDeleteChat){
            // Delete chat room with this person from chats recycler view
            childUpdates.put(DATABASE_REF_USER_CHATS + "/" + currentUserId + "/" + chatId, null);
            childUpdates.put(DATABASE_REF_USER_CHATS + "/" + userId + "/" + chatId, null);
        }

        // Delete notifications
        // remove notifications i (current user) received from the selected user
        childUpdates.put(DATABASE_REF_NOTIFICATIONS + "/" + DATABASE_REF_NOTIFICATIONS_ALERTS + "/" + currentUserId + "/" +userId, null);
        childUpdates.put(DATABASE_REF_NOTIFICATIONS + "/" + DATABASE_REF_NOTIFICATIONS_MESSAGES + "/" + currentUserId + "/" +userId, null);

        // remove my notifications (current user) that had been sent to the selected user, because we are blocking him
        childUpdates.put(DATABASE_REF_NOTIFICATIONS + "/" + DATABASE_REF_NOTIFICATIONS_ALERTS + "/" + userId + "/" +currentUserId, null);
        childUpdates.put(DATABASE_REF_NOTIFICATIONS + "/" + DATABASE_REF_NOTIFICATIONS_MESSAGES + "/" + userId + "/" +currentUserId, null);

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
    public void unblockUser(String currentUserId, String userId, String relationStatus) {
        Map<String, Object> childUpdates = new HashMap<>();

        if(relationStatus.equals(RELATION_STATUS_BLOCKING_VS_BLOCKED_BACK)) {
            // this selected user had already blocked me (current user), and i am (current user) blocked him back and now i am unblocking
            // Update relations to the previous blocking status, blocking/blocked_back for (current user) and blocked/blocking_back for (selected user)
            childUpdates.put(DATABASE_REF_RELATIONS + "/" + currentUserId + "/" + userId +"/" + DATABASE_REF_RELATION_STATUS, RELATION_STATUS_BLOCKING);
            childUpdates.put(DATABASE_REF_RELATIONS + "/" + userId + "/" + currentUserId + "/" + DATABASE_REF_RELATION_STATUS, RELATION_STATUS_BLOCKED);

        }else if(relationStatus.equals(RELATION_STATUS_BLOCKED_VS_BLOCKING_BACK)){
            // This selected user was blocked by me (current user) and he blocked me back and now i am unblocking
            // Now the original block is being removed and we will be only left with the block back, so we need to reverse the original block relation
            childUpdates.put(DATABASE_REF_RELATIONS + "/" + currentUserId + "/" + userId + "/" + DATABASE_REF_RELATION_STATUS, RELATION_STATUS_BLOCKING);
            childUpdates.put(DATABASE_REF_RELATIONS + "/" + userId + "/" + currentUserId + "/" + DATABASE_REF_RELATION_STATUS, RELATION_STATUS_BLOCKED);
        }else{
            // Update relations to null. To start fresh
            childUpdates.put(DATABASE_REF_RELATIONS + "/" + currentUserId + "/" + userId, null);
            childUpdates.put(DATABASE_REF_RELATIONS + "/" + userId + "/" + currentUserId, null);
        }

        // Chat ID is not passed from MainFragment, we need to create
        String chatId = getJoinedKeys(currentUserId , userId);
        // update chat to null, to delete the chat room and start fresh
        childUpdates.put(DATABASE_REF_CHATS +"/"+ chatId ,null);

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

