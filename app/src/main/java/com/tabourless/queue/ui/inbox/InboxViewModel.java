package com.tabourless.queue.ui.inbox;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.tabourless.queue.data.InboxDataFactory;
import com.tabourless.queue.data.InboxRepository;
import com.tabourless.queue.models.Chat;

public class InboxViewModel extends ViewModel {

    private final static String TAG = InboxViewModel.class.getSimpleName();

    private InboxDataFactory mInboxDataFactory;
    private PagedList.Config config;
    private  LiveData<PagedList<Chat>> itemPagedList;

    private DatabaseReference mDatabaseRef;
    private DatabaseReference mUserChatsRef;

    public InboxViewModel(String currentUserId) {

        Log.d(TAG, "InboxViewModel: initiated");
        mInboxDataFactory = new InboxDataFactory(currentUserId);

        //Enabling Offline Capabilities//
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        // keepSync UserChatsRef to work offline
        mUserChatsRef = mDatabaseRef.child("userChats").child(currentUserId);
        mUserChatsRef.keepSynced(true);

        config = (new PagedList.Config.Builder())
                .setPageSize(10)//10
                .setInitialLoadSizeHint(10)//30
                //.setPrefetchDistance(10)//10
                .setEnablePlaceholders(false)
                .build();

        itemPagedList = new LivePagedListBuilder<>(mInboxDataFactory, config).build();

    }

    public LiveData<PagedList<Chat>> getItemPagedList(){
        return itemPagedList ;
    }

    // Set scroll direction and last visible item which is used to get initial key's position
    public void setScrollDirection(int scrollDirection, int lastVisibleItem) {
        //MessagesListRepository.setScrollDirection(scrollDirection);
        mInboxDataFactory.setScrollDirection(scrollDirection, lastVisibleItem);
    }

    @Override
    protected void onCleared() {
        Log.d(TAG, "mama ChatsViewModel onCleared:");
        InboxRepository.removeListeners();
        super.onCleared();
    }

}