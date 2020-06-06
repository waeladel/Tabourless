package com.tabourless.queue.data;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.paging.ItemKeyedDataSource;

import com.tabourless.queue.models.UserQueue;

public class QueuesDataSource extends ItemKeyedDataSource<Long, UserQueue> {

    private final static String TAG = QueuesDataSource.class.getSimpleName();
    private String mUserKey;
    private QueuesRepository mQueuesRepository;

    // get chatKey on the constructor
    public QueuesDataSource(String userKey){
        //chatsRepository = new ChatsRepository(chatKey);
        this.mUserKey = userKey;
        Log.d(TAG, "QueuesDataSource initiated ");
       /* usersRepository.getUsersChangeSubject().observeOn(Schedulers.io()).subscribeOn(Schedulers.computation()).subscribe();{
            invalidate();
            Log.d(TAG, "mama invalidate ");
        }*/

    }
    // Pass scrolling direction and last/first visible item to the repository
    public void setScrollDirection(int scrollDirection, int lastVisibleItem){
        mQueuesRepository.setScrollDirection(scrollDirection, lastVisibleItem);
    }

    public void removeQueue(String userId, UserQueue deletedQueue) {
        mQueuesRepository.removeQueue(userId, deletedQueue);
    }

    // a callback to invalidate the data whenever a change happen
    @Override
    public void addInvalidatedCallback(@NonNull InvalidatedCallback onInvalidatedCallback) {
        //super.addInvalidatedCallback(onInvalidatedCallback);
        Log.d(TAG, "Callback QueuesDataSource addInvalidatedCallback ");
        // pass firebase Callback to ChatsRepository
        mQueuesRepository = new QueuesRepository(mUserKey, onInvalidatedCallback);
        //chatsRepository.ChatsChanged(onInvalidatedCallback);
        //invalidate();
    }

    @Override
    public void invalidate() {
        Log.d(TAG, "Invalidated ");
        super.invalidate();
    }

    @Override
    public boolean isInvalid() {
        Log.d(TAG, "isInvalid = "+super.isInvalid());
        return super.isInvalid();
    }

    // load the initial data based on page size and key (key in null on the first load)
    @Override
    public void loadInitial(@NonNull LoadInitialParams<Long> params, @NonNull LoadInitialCallback<UserQueue> callback) {
        /*List<User> items = usersRepository.getMessages(params.requestedInitialKey, params.requestedLoadSize);
        callback.onResult(items);*/
        Log.d(TAG, "loadInitial params key" +params.requestedInitialKey+" LoadSize " + params.requestedLoadSize);
        mQueuesRepository.getInitial(params.requestedInitialKey, params.requestedLoadSize, callback);
        //usersRepository.getMessages( 0L, params.requestedLoadSize, callback);

    }

    // load next page
    @Override
    public void loadAfter(@NonNull LoadParams<Long> params, @NonNull LoadCallback<UserQueue> callback) {
        /*List<User> items = usersRepository.getMessages(params.key, params.requestedLoadSize);
        callback.onResult(items);*/
        mQueuesRepository.setLoadBeforeCallback(params.key , callback);
        Log.d(TAG, "loadAfter params key " + params.key+" LoadSize " + params.requestedLoadSize);
        // using getBefore instead of getAfter because the order is reversed
        //chatsRepository.getBefore(params.key -1, params.requestedLoadSize, callback);
        mQueuesRepository.getBefore(params.key , params.requestedLoadSize, callback);
    }

    // load previous page
    @Override
    public void loadBefore(@NonNull LoadParams<Long> params, @NonNull LoadCallback<UserQueue> callback) {
        /*List<User> items = fetchItemsBefore(params.key, params.requestedLoadSize);
        callback.onResult(items);*/
        mQueuesRepository.setLoadAfterCallback(params.key , callback);
        Log.d(TAG, "loadBefore params " + params.key+" LoadSize " + params.requestedLoadSize);
        // using getAfter instead of getBefore because the order is reversed
        //chatsRepository.getAfter(params.key +1, params.requestedLoadSize, callback);
        mQueuesRepository.getAfter(params.key , params.requestedLoadSize, callback);
    }

    @NonNull
    @Override
    public Long getKey(@NonNull UserQueue userQueue) {
        return  userQueue.getJoinedLong();
    }

    public void removeListeners() {
        if(mQueuesRepository != null){
            mQueuesRepository.removeListeners();
        }
    }

}
