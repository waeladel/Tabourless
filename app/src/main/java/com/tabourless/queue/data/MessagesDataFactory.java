package com.tabourless.queue.data;

import androidx.paging.DataSource;

import com.tabourless.queue.models.Message;

public class MessagesDataFactory extends DataSource.Factory<String, Message>{

    private String mChatKey;
    private MessagesDataSource messagesDataSource;

    // receive chatKey on the constructor
    public MessagesDataFactory(String chatKey) {
        this.mChatKey = chatKey;
        messagesDataSource = new MessagesDataSource(mChatKey);
    }

    // When last database message is not loaded, Invalidate messagesDataSource to scroll down
    public void invalidateData() {
        messagesDataSource.invalidateData();
        //messagesDataSource.loadInitial();
    }

    @Override
    public DataSource<String, Message> create() {
        return messagesDataSource;
    }



}