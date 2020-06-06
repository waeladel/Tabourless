package com.tabourless.queue.data;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.paging.ItemKeyedDataSource;

import com.tabourless.queue.interfaces.FirebaseOnCompleteCallback;
import com.tabourless.queue.models.Customer;

import java.util.List;

public class CustomersDataSource extends ItemKeyedDataSource<String, Customer> {

    private final static String TAG = CustomersDataSource.class.getSimpleName();
    private String mPlaceKey, mQueueKey;
    private CustomersRepository mCustomersRepository;

    // get chatKey on the constructor
    public CustomersDataSource(String placeKey, String queueKey){
        //messagesRepository = new MessagesListRepository(chatKey);
        this.mPlaceKey = placeKey;
        this.mQueueKey = queueKey;
        Log.d(TAG, "CustomersDataSource initiated ");
       /* usersRepository.getUsersChangeSubject().observeOn(Schedulers.io()).subscribeOn(Schedulers.computation()).subscribe();{
            invalidate();
            Log.d(TAG, "mama invalidate ");
        }*/
    }

    // a callback to invalidate the data whenever a change happen
    @Override
    public void addInvalidatedCallback(@NonNull InvalidatedCallback onInvalidatedCallback) {
        //super.addInvalidatedCallback(onInvalidatedCallback);
        Log.d(TAG, "CustomersDataSource addInvalidatedCallback ");

        // initiate messagesRepository here to pass  onInvalidatedCallback
        //messagesRepository = MessagesListRepository.getInstance();
        //messagesRepository = MessagesListRepository.init(mMessageKey, onInvalidatedCallback);
        mCustomersRepository = new CustomersRepository(mPlaceKey, mQueueKey, onInvalidatedCallback);
        //messagesRepository.MessagesChanged(onInvalidatedCallback);
        //invalidate();
    }

    @Override
    public void invalidate() {
        Log.d(TAG, "CustomersDataSource Invalidated ");
        super.invalidate();
    }

    // When last database message is not loaded, Invalidate messagesDataSource to scroll down
    public void invalidateData() {
        Log.d(TAG, "CustomersDataSource invalidateData ");
        //messagesRepository.setInitialKey(null);
        mCustomersRepository.invalidateData();
    }

    @Override
    public boolean isInvalid() {
        Log.d(TAG, "isInvalid = "+super.isInvalid());
        return super.isInvalid();
    }

    // load the initial data based on page size and key (key in null on the first load)
    @Override
    public void loadInitial(@NonNull LoadInitialParams<String> params, @NonNull LoadInitialCallback<Customer> callback) {
        /*List<User> items = usersRepository.getMessages(params.requestedInitialKey, params.requestedLoadSize);
        callback.onResult(items);*/
        //messagesRepository.setLoadInitialCallback(callback);
        Log.d(TAG, "loadInitial params key" +params.requestedInitialKey+" LoadSize " + params.requestedLoadSize+ " callback= "+callback);
        mCustomersRepository.getInitial(params.requestedInitialKey, params.requestedLoadSize, callback);
        //usersRepository.getMessages( 0L, params.requestedLoadSize, callback);

    }

    // load next page
    @Override
    public void loadAfter(@NonNull LoadParams<String> params, @NonNull LoadCallback<Customer> callback) {
        /*List<User> items = usersRepository.getMessages(params.key, params.requestedLoadSize);
        callback.onResult(items);*/
        mCustomersRepository.setLoadAfterCallback(params.key, callback);
        Log.d(TAG, "loadAfter params key " + params.key+" LoadSize " + params.requestedLoadSize+ " callback= "+callback);
        mCustomersRepository.getAfter(params.key, params.requestedLoadSize, callback);
    }

    // load previous page
    @Override
    public void loadBefore(@NonNull LoadParams<String> params, @NonNull LoadCallback<Customer> callback) {
        /*List<User> items = fetchItemsBefore(params.key, params.requestedLoadSize);
        callback.onResult(items);*/
        mCustomersRepository.setLoadBeforeCallback(params.key, callback);
        Log.d(TAG, "loadBefore params " + params.key+" LoadSize " + params.requestedLoadSize+ " callback= "+callback);
        mCustomersRepository.getBefore(params.key, params.requestedLoadSize, callback);
    }

    @NonNull
    @Override
    public String getKey(@NonNull Customer customer) {
        return  customer.getKey();
    }

    public void setScrollDirection(int scrollDirection, int lastVisibleItem) {
        mCustomersRepository.setScrollDirection(scrollDirection, lastVisibleItem);
    }

    public void removeCustomer(Customer customer) {
        mCustomersRepository.removeCustomer(customer);
    }
    public void removeListeners() {
        mCustomersRepository.removeListeners();
    }

}
