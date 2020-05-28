package com.tabourless.queue.data;

import androidx.paging.DataSource;

import com.tabourless.queue.models.Chat;
import com.tabourless.queue.models.Queue;
import com.tabourless.queue.models.UserQueue;

public class QueuesDataFactory extends DataSource.Factory<Long, UserQueue>{

    private String mUserKey;
    private int scrollDirection;
    private int lastVisibleItem;
    private QueuesDataSource mQueuesDataSource;

    // receive userId on the constructor to get users chats from database
    public QueuesDataFactory(String userId) {
        mUserKey = userId;
        mQueuesDataSource = new QueuesDataSource(userId);
    }

    /*public void setCallback(FirebaseChatsCallback firebaseCallback) {
        this.firebaseCallback = firebaseCallback;
        //this.firebaseCallback = firebaseCallback;
    }*/

    /*public void setUserKey(String userKey) {
        this.mUserKey = userKey;
    }*/

    // Set scroll direction and last visible item which is used to get initial key's position
    public void setScrollDirection(int scrollDirection, int lastVisibleItem) {
        /*MessagesListRepository.setScrollDirection(scrollDirection);
        this.scrollDirection = scrollDirection;
        this.lastVisibleItem = lastVisibleItem;*/
        // Pass scrolling direction and last/first visible item to data source
        mQueuesDataSource.setScrollDirection(scrollDirection, lastVisibleItem);
    }

    public void removeListeners() {
        mQueuesDataSource.removeListeners();
    }

    @Override
    public DataSource<Long, UserQueue> create() {
        // pass firebase Callback to QueueDataSource
        //return new ChatsDataSource(mUserKey);
        return mQueuesDataSource;
    }

}