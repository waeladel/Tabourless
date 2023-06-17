package com.tabourless.queue.data;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.paging.ItemKeyedDataSource;

import com.tabourless.queue.models.Message;

public class MessagesDataSource extends ItemKeyedDataSource<String, Message> {

    private final static String TAG = MessagesDataSource.class.getSimpleName();
    private String mMessageKey;
    private MessagesListRepository messagesRepository;
    private boolean isSeeing;

    // get chatKey on the constructor
    public MessagesDataSource(String messageKey){
        //messagesRepository = new MessagesListRepository(chatKey);
        this.mMessageKey = messageKey;
        isSeeing = true;
        Log.d(TAG, " MessagesDataSource initiated ");
       /* usersRepository.getUsersChangeSubject().observeOn(Schedulers.io()).subscribeOn(Schedulers.computation()).subscribe();{
            invalidate();
            Log.d(TAG, "mama invalidate ");
        }*/
    }

    // a callback to invalidate the data whenever a change happen
    @Override
    public void addInvalidatedCallback(@NonNull InvalidatedCallback onInvalidatedCallback) {
        //super.addInvalidatedCallback(onInvalidatedCallback);
        Log.d(TAG, " Callback MessagesDataSource addInvalidatedCallback ");

        // initiate messagesRepository here to pass  onInvalidatedCallback
        //messagesRepository = MessagesListRepository.getInstance();
        //messagesRepository = MessagesListRepository.init(mMessageKey, onInvalidatedCallback);
        messagesRepository = new MessagesListRepository(mMessageKey, isSeeing, onInvalidatedCallback);
        //messagesRepository.MessagesChanged(onInvalidatedCallback);
        //invalidate();
    }
    public void setScrollDirection(int scrollDirection, int lastVisibleItem) {
        messagesRepository.setScrollDirection(scrollDirection, lastVisibleItem);
    }

    // To only update notification's seen when user is opening the notification's tap
    public void setSeeing (boolean seeing) {
        this.isSeeing = seeing;
        messagesRepository.setSeeing(isSeeing);
    }

    public void removeListeners() {
        messagesRepository.removeListeners();
    }

    @Override
    public void invalidate() {
        Log.d(TAG, " MessagesListRepository Invalidated ");
        super.invalidate();
    }

    // When last database message is not loaded, Invalidate messagesDataSource to scroll down
    public void invalidateData() {
        Log.d(TAG, " MessagesListRepository invalidateData ");
        //messagesRepository.setInitialKey(null);
        messagesRepository.invalidateData();
    }

    @Override
    public boolean isInvalid() {
        Log.d(TAG, "isInvalid = "+super.isInvalid());
        return super.isInvalid();
    }

    // load the initial data based on page size and key (key in null on the first load)
    @Override
    public void loadInitial(@NonNull LoadInitialParams<String> params, @NonNull LoadInitialCallback<Message> callback) {
        /*List<User> items = usersRepository.getMessages(params.requestedInitialKey, params.requestedLoadSize);
        callback.onResult(items);*/
        //messagesRepository.setLoadInitialCallback(callback);
        Log.d(TAG, " loadInitial params key" +params.requestedInitialKey+" LoadSize " + params.requestedLoadSize+ " callback= "+callback);
        messagesRepository.getMessages(params.requestedInitialKey, params.requestedLoadSize, callback);
        //usersRepository.getMessages( 0L, params.requestedLoadSize, callback);

    }

    // load next page
    @Override
    public void loadAfter(@NonNull LoadParams<String> params, @NonNull LoadCallback<Message> callback) {
        /*List<User> items = usersRepository.getMessages(params.key, params.requestedLoadSize);
        callback.onResult(items);*/
        messagesRepository.setLoadAfterCallback(params.key, callback);
        Log.d(TAG, " loadAfter params key " + params.key+" LoadSize " + params.requestedLoadSize+ " callback= "+callback);
        messagesRepository.getMessagesAfter(params.key, params.requestedLoadSize, callback);
    }

    // load previous page
    @Override
    public void loadBefore(@NonNull LoadParams<String> params, @NonNull LoadCallback<Message> callback) {
        /*List<User> items = fetchItemsBefore(params.key, params.requestedLoadSize);
        callback.onResult(items);*/
        messagesRepository.setLoadBeforeCallback(params.key, callback);
        Log.d(TAG, " loadBefore params " + params.key+" LoadSize " + params.requestedLoadSize+ " callback= "+callback);
        messagesRepository.getMessagesBefore(params.key, params.requestedLoadSize, callback);
    }

    @NonNull
    @Override
    public String getKey(@NonNull Message message) {
        return  message.getKey();
    }

}
