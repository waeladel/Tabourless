package com.tabourless.queue.ui.queues;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.tabourless.queue.data.InboxDataFactory;
import com.tabourless.queue.data.InboxRepository;
import com.tabourless.queue.data.QueuesDataFactory;
import com.tabourless.queue.models.Chat;
import com.tabourless.queue.models.Queue;
import com.tabourless.queue.models.UserQueue;
import com.tabourless.queue.ui.inbox.InboxViewModel;

public class QueuesViewModel extends ViewModel {

    private final static String TAG = QueuesViewModel.class.getSimpleName();

    private QueuesDataFactory mQueuesDataFactory;
    private PagedList.Config config;
    private  LiveData<PagedList<UserQueue>> itemPagedList;

    private DatabaseReference mDatabaseRef;
    private DatabaseReference mUserQueuesRef;

    //Get current logged in user
    private FirebaseUser mFirebaseCurrentUser;
    private String mCurrentUserId;

    public QueuesViewModel() {
        //Get current logged in user
        mFirebaseCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mCurrentUserId = mFirebaseCurrentUser != null ? mFirebaseCurrentUser.getUid() : null;

        Log.d(TAG, "QueuesViewModel: initiated");
        mQueuesDataFactory = new QueuesDataFactory(mCurrentUserId);

        //Enabling Offline Capabilities//
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();

        // keepSync UserChatsRef to work offline
        if(mCurrentUserId != null){
            mUserQueuesRef = mDatabaseRef.child("userQueues").child(mCurrentUserId);
            mUserQueuesRef.keepSynced(true);
        }

        config = (new PagedList.Config.Builder())
                .setPageSize(10)//10
                .setInitialLoadSizeHint(10)//30
                //.setPrefetchDistance(10)//10
                .setEnablePlaceholders(false)
                .build();

        itemPagedList = new LivePagedListBuilder<>(mQueuesDataFactory, config).build();

    }

    public LiveData<PagedList<UserQueue>> getItemPagedList(){
        return itemPagedList ;
    }

    // Set scroll direction and last visible item which is used to get initial key's position
    public void setScrollDirection(int scrollDirection, int lastVisibleItem) {
        //MessagesListRepository.setScrollDirection(scrollDirection);
        mQueuesDataFactory.setScrollDirection(scrollDirection, lastVisibleItem);
    }

    public void removeQueue(String userId, UserQueue deletedQueue) {
        mQueuesDataFactory.removeQueue(userId, deletedQueue);
    }

    @Override
    protected void onCleared() {
        Log.d(TAG, "QueuesViewModel onCleared:");
        mQueuesDataFactory.removeListeners();
        super.onCleared();
    }
}