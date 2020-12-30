package com.tabourless.queue.ui.customers;

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
import com.tabourless.queue.data.CustomersDataFactory;
import com.tabourless.queue.data.QueueRepository;
import com.tabourless.queue.interfaces.FirebaseUserCallback;
import com.tabourless.queue.models.Customer;
import com.tabourless.queue.models.Message;
import com.tabourless.queue.models.Queue;
import com.tabourless.queue.models.User;
import com.tabourless.queue.models.UserQueue;

import static com.tabourless.queue.App.DATABASE_REF_CUSTOMERS;

public class CustomersViewModel extends ViewModel {
    private final static String TAG = CustomersViewModel.class.getSimpleName();
    private CustomersDataFactory mCustomersDataFactory;
    public QueueRepository mQueueRepository;
    private PagedList.Config config;
    public LiveData<PagedList<Customer>> itemPagedList;
    public  MutableLiveData<PagedList<Message>> items;
    public  LiveData<User> mCurrentUser;
    public  LiveData<Queue> mQueue;
    public  LiveData<Customer> mCurrentCustomer;

    private DatabaseReference mDatabaseRef;
    private DatabaseReference mCustomersRef;

    //Get current logged in user
    private FirebaseUser mFirebaseCurrentUser;
    private String mCurrentUserId;

    public CustomersViewModel() {

    }

    public CustomersViewModel(String placeKey, String queueKey) {

        //Get current logged in user
        mFirebaseCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mCurrentUserId = mFirebaseCurrentUser != null ? mFirebaseCurrentUser.getUid() : null;

        // pass chatKey to the constructor of MessagesDataFactory
        mCustomersDataFactory = new CustomersDataFactory(placeKey, queueKey);
        mQueueRepository = new QueueRepository();

        Log.d(TAG, "CustomersViewModel init");

        //Enabling Offline Capabilities//
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        mCustomersRef = mDatabaseRef.child(DATABASE_REF_CUSTOMERS).child(placeKey).child(queueKey);
        mCustomersRef.keepSynced(true);

        config = (new PagedList.Config.Builder())
                .setPageSize(10)//10  20
                .setInitialLoadSizeHint(10)//30  20
                //.setPrefetchDistance(10)//10
                .setEnablePlaceholders(false)
                .build();

        itemPagedList = new LivePagedListBuilder<>(mCustomersDataFactory, config).build();
        /*itemPagedList = (new LivePagedListBuilder(messagesDataFactory,config))
                .build();*/
        mQueue = mQueueRepository.getQueue(placeKey, queueKey);
        mCurrentCustomer = mQueueRepository.getCurrentCustomer(placeKey, queueKey, mCurrentUserId);
        mCurrentUser = mQueueRepository.getCurrentUser(mCurrentUserId);
    }

    public LiveData<PagedList<Customer>> getItemPagedList(){
        return itemPagedList ;
    }
    // Set scroll direction and last visible item which is used to get initial key's position
    public void setScrollDirection(int scrollDirection, int lastVisibleItem) {
        //MessagesListRepository.setScrollDirection(scrollDirection);
        mCustomersDataFactory.setScrollDirection(scrollDirection, lastVisibleItem);
    }

    public LiveData<Customer> getCurrentCustomer() {
        return mCurrentCustomer;
    }

    public LiveData<User> getCurrentUser() {
        return mCurrentUser;
    }

    public LiveData<Queue> getQueue() {
        return mQueue;
    }

    public void removeCustomer(Customer customer) {
        mCustomersDataFactory.removeCustomer(customer);
    }

    @Override
    protected void onCleared() {
        Log.d(TAG, "CustomersViewModel onCleared:");

        // Remove all Listeners
        mCustomersDataFactory.removeListeners();
        mQueueRepository.removeListeners();
        super.onCleared();
    }

}