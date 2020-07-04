package com.tabourless.queue.ui.notifications;

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
import com.tabourless.queue.data.NotificationsDataFactory;
import com.tabourless.queue.models.DatabaseNotification;

import static com.tabourless.queue.App.DATABASE_REF_NOTIFICATIONS;
import static com.tabourless.queue.App.DATABASE_REF_NOTIFICATIONS_ALERTS;

public class NotificationsViewModel extends ViewModel {

    private final static String TAG = NotificationsViewModel.class.getSimpleName();

    private NotificationsDataFactory mDataFactory;
    private PagedList.Config config;
    private  LiveData<PagedList<DatabaseNotification>> itemPagedList;

    private DatabaseReference mDatabaseRef;
    private DatabaseReference mNotificationsRef;

    private FirebaseUser mFirebaseCurrentUser;
    private String mCurrentUserId;

    public NotificationsViewModel() {

        Log.d(TAG, "NotificationsViewModel init");
        //Get current logged in user
        mFirebaseCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mCurrentUserId = mFirebaseCurrentUser != null ? mFirebaseCurrentUser.getUid() : null;

        mDataFactory = new NotificationsDataFactory(mCurrentUserId);

        //Enabling Offline Capabilities//
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        // keepSync UserChatsRef to work offline
        mNotificationsRef = mDatabaseRef.child(DATABASE_REF_NOTIFICATIONS).child(DATABASE_REF_NOTIFICATIONS_ALERTS).child(mCurrentUserId);
        mNotificationsRef.keepSynced(true);

        config = (new PagedList.Config.Builder())
                .setPageSize(10)//10
                .setInitialLoadSizeHint(10)//30
                //.setPrefetchDistance(10)//10
                .setEnablePlaceholders(false)
                .build();

        itemPagedList = new LivePagedListBuilder<>(mDataFactory, config).build();
    }

    public LiveData<PagedList<DatabaseNotification>> getItemPagedList(){
        return itemPagedList ;
    }

    // Set scroll direction and last visible item which is used to get initial key's position
    public void setScrollDirection(int scrollDirection, int lastVisibleItem) {
        //MessagesListRepository.setScrollDirection(scrollDirection);
        mDataFactory.setScrollDirection(scrollDirection, lastVisibleItem);
    }

    @Override
    protected void onCleared() {
        Log.d(TAG, "mama NotificationsViewModel onCleared:");
        //NotificationsRepository.removeListeners();
        // Remove all listeners on viewModel cleared
        mDataFactory.removeListeners();
        super.onCleared();
    }

}