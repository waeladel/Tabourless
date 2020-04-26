package com.tabourless.queue.data;

import androidx.paging.DataSource;

import com.tabourless.queue.models.Chat;

public class InboxDataFactory extends DataSource.Factory<Long, Chat>{

    private String mUserKey;
    private int scrollDirection;
    private int lastVisibleItem;
    private InboxDataSource inboxDataSource;

    // receive userId on the constructor to get users chats from database
    public InboxDataFactory(String userId) {
        mUserKey = userId;
        inboxDataSource = new InboxDataSource(userId);
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
        inboxDataSource.setScrollDirection(scrollDirection, lastVisibleItem);
    }


    @Override
    public DataSource<Long, Chat> create() {
        // pass firebase Callback to ChatsDataSource
        //return new ChatsDataSource(mUserKey);
        return inboxDataSource;
    }

}