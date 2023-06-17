package com.tabourless.queue.data;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.paging.ItemKeyedDataSource;

import com.tabourless.queue.models.DatabaseNotification;

public class NotificationsDataSource extends ItemKeyedDataSource<Long, DatabaseNotification> {

    private final static String TAG = NotificationsDataSource.class.getSimpleName();
    private String mUserId;
    private boolean isSeeing;
    private NotificationsRepository mRepository;

    // get chatKey on the constructor
    public NotificationsDataSource(String userId){
        //mRepository = new ChatsRepository(chatKey);
        this.mUserId = userId;
        isSeeing = true;
        Log.d(TAG, "mama ChatsDataSource initiated ");
       /* usersRepository.getUsersChangeSubject().observeOn(Schedulers.io()).subscribeOn(Schedulers.computation()).subscribe();{
            invalidate();
            Log.d(TAG, "mama invalidate ");
        }*/

    }
    // Pass scrolling direction and last/first visible item to the repository
    public void setScrollDirection(int scrollDirection, int lastVisibleItem){
        mRepository.setScrollDirection(scrollDirection, lastVisibleItem);
    }

    // To only update notification's seen when user is opening the notification's tap
    public void setSeeing (boolean seeing) {
        isSeeing = seeing;
        mRepository.setSeeing(isSeeing);
    }

    // removeListeners on viewModel cleared
    public void removeListeners(){
        mRepository.removeListeners();
    }

    // a callback to invalidate the data whenever a change happen
    @Override
    public void addInvalidatedCallback(@NonNull InvalidatedCallback onInvalidatedCallback) {
        //super.addInvalidatedCallback(onInvalidatedCallback);
        Log.d(TAG, "mama Callback ChatsDataSource addInvalidatedCallback ");
        // pass firebase Callback to ChatsRepository
        mRepository = new NotificationsRepository(mUserId, isSeeing, onInvalidatedCallback);
        //mRepository.ChatsChanged(onInvalidatedCallback);
        //invalidate();
    }

    @Override
    public void invalidate() {
        Log.d(TAG, "mama Invalidated ");
        super.invalidate();
    }

    @Override
    public boolean isInvalid() {
        Log.d(TAG, "isInvalid = "+super.isInvalid());
        return super.isInvalid();
    }

    // load the initial data based on page size and key (key in null on the first load)
    @Override
    public void loadInitial(@NonNull LoadInitialParams<Long> params, @NonNull LoadInitialCallback<DatabaseNotification> callback) {
        /*List<User> items = usersRepository.getMessages(params.requestedInitialKey, params.requestedLoadSize);
        callback.onResult(items);*/
        Log.d(TAG, "mama loadInitial params key" +params.requestedInitialKey+" LoadSize " + params.requestedLoadSize);
        mRepository.getItems(params.requestedInitialKey, params.requestedLoadSize, callback);
        //usersRepository.getMessages( 0L, params.requestedLoadSize, callback);

    }

    // load next page
    @Override
    public void loadAfter(@NonNull LoadParams<Long> params, @NonNull LoadCallback<DatabaseNotification> callback) {
        /*List<User> items = usersRepository.getMessages(params.key, params.requestedLoadSize);
        callback.onResult(items);*/
        mRepository.setLoadBeforeCallback(params.key , callback);
        Log.d(TAG, "mama loadAfter params key " + params.key+" LoadSize " + params.requestedLoadSize);
        // using getBefore instead of getAfter because the order is reversed
        //mRepository.getBefore(params.key -1, params.requestedLoadSize, callback);
        mRepository.getBefore(params.key , params.requestedLoadSize, callback);
    }

    // load previous page
    @Override
    public void loadBefore(@NonNull LoadParams<Long> params, @NonNull LoadCallback<DatabaseNotification> callback) {
        /*List<User> items = fetchItemsBefore(params.key, params.requestedLoadSize);
        callback.onResult(items);*/
        mRepository.setLoadAfterCallback(params.key , callback);
        Log.d(TAG, "mama loadBefore params " + params.key+" LoadSize " + params.requestedLoadSize);
        // using getAfter instead of getBefore because the order is reversed
        //mRepository.getAfter(params.key +1, params.requestedLoadSize, callback);
        mRepository.getAfter(params.key , params.requestedLoadSize, callback);
    }

    @NonNull
    @Override
    public Long getKey(@NonNull DatabaseNotification databaseNotification) {
        return  databaseNotification.getSentLong();
    }
}
