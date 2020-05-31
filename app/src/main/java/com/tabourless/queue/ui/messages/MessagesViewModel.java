package com.tabourless.queue.ui.messages;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.paging.ItemKeyedDataSource;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.tabourless.queue.data.MessagesDataFactory;
import com.tabourless.queue.data.MessagesListRepository;
import com.tabourless.queue.data.MessagesRepository;
import com.tabourless.queue.interfaces.FirebaseMessageCallback;
import com.tabourless.queue.models.Chat;
import com.tabourless.queue.models.Message;
import com.tabourless.queue.models.User;

import java.util.Timer;

public class MessagesViewModel extends ViewModel {
    private final static String TAG = MessagesViewModel.class.getSimpleName();
    private MessagesDataFactory messagesDataFactory;
    private PagedList.Config config;
    public LiveData<PagedList<Message>> itemPagedList;
    public  MutableLiveData<PagedList<Message>> items;
    LiveData<ItemKeyedDataSource<String, Message>> liveDataSource;
    public  LiveData<User> chatUser, currentUser;
    public  LiveData<Chat> chat;
    private String chatId;
    private MessagesRepository messagesRepository;

    private DatabaseReference mDatabaseRef;
    private DatabaseReference mUserMessagesRef;
    private Timer mTimer;
    private  MutableLiveData<CharSequence> agoTime;

    public MessagesViewModel() {

    }

    public MessagesViewModel(String chatKey, String chatUserId, String currentUserId) {

        // pass chatKey to the constructor of MessagesDataFactory
        messagesDataFactory = new MessagesDataFactory(chatKey);
        messagesRepository = new MessagesRepository();

        agoTime = new MutableLiveData<>();
        //liveDataSource = messagesDataFactory.getItemLiveDataSource();
        Log.d(TAG, "MessagesViewModel init");

        this.chatId = chatKey;

        //Enabling Offline Capabilities//
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        mUserMessagesRef = mDatabaseRef.child("messages").child(chatKey);
        mUserMessagesRef.keepSynced(true);

        config = (new PagedList.Config.Builder())
                .setPageSize(10)//10  20
                .setInitialLoadSizeHint(10)//30  20
                //.setPrefetchDistance(10)//10
                .setEnablePlaceholders(false)
                .build();

        itemPagedList = new LivePagedListBuilder<>(messagesDataFactory, config).build();
        /*itemPagedList = (new LivePagedListBuilder(messagesDataFactory,config))
                .build();*/
        chat = messagesRepository.getChat(chatKey);
        chatUser = messagesRepository.getUser(chatUserId);
        currentUser = messagesRepository.getCurrentUser(currentUserId);

    }

    public LiveData<User> getChatUser(String userId) {
        Log.d(TAG, "getUser"+ userId);
        return chatUser ;
        //chatUser = messagesRepository.getUser(userId);
        //return messagesRepository.getUser(userId);
    }

    public LiveData<User> getCurrentUser(String userId) {
        Log.d(TAG, "getCurrentUser: "+ userId);
        return currentUser ;
        //chatUser = messagesRepository.getUser(userId);
        //return messagesRepository.getUser(userId);
    }

    /*public LiveData<String> getSenderId(String chatId) {
        Log.d(TAG, "getSenderId chatId"+ chatId);
        return  messagesRepository.getSenderId(chatId);
    }*/

    // When last database message is not loaded, Invalidate messagesDataSource to scroll down
    public void invalidateData() {
        // invalidate messagesDataSource
        messagesDataFactory.invalidateData();
    }

    public LiveData<Chat> getChat(String chatId) {
        Log.d(TAG, "chatId"+ chatId);
        return chat;
        //return  messagesRepository.getChat(chatId);
    }


    // Set scroll direction and last visible item which is used to get initial key's position
    public void setScrollDirection(int scrollDirection, int lastVisibleItem) {
        messagesDataFactory.setScrollDirection(scrollDirection, lastVisibleItem);
    }


    public void getLastMessageOnce(String chatId, FirebaseMessageCallback  callback) {
        messagesRepository.getLastMessageOnce(chatId, callback);
    }

    @Override
    protected void onCleared() {
        Log.d(TAG, "mama MessagesViewModel onCleared:");

        // Remove all Listeners from messagesRepository
        messagesRepository.removeListeners();

        // Remove all Listeners from MessagesListRepository
        messagesDataFactory.removeListeners();
        /*// Update all seen messages by currentUser before onCleared
        updateSeenMessages(chatId);*/
        super.onCleared();
    }

}