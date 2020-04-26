package com.tabourless.queue.ui.main;
import android.util.Log;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.tabourless.queue.data.UserRepository;
import com.tabourless.queue.models.User;


public class MainViewModel extends ViewModel {

    private final static String TAG = MainViewModel.class.getSimpleName();
    private UserRepository userRepository;
    private MutableLiveData<User> currentUser;
    private MutableLiveData<String> currentUserId;
    private MutableLiveData<Long> inboxCount, notificationCount;

    public MainViewModel() {

        // pass userId to the constructor of MessagesDataFactory
        userRepository = new UserRepository();

        //currentUserId = new MutableLiveData<>();
        //chatCount = new MutableLiveData<>();
        //notificationCount = new MutableLiveData<>();

        //liveDataSource = messagesDataFactory.getItemLiveDataSource();
        Log.d(TAG, "MainActivityViewModel init");
    }

    public MutableLiveData<String> getCurrentUserId() {
        Log.d(TAG, "getCurrentUserId initiated");
        if(currentUserId == null){
            currentUserId = new MutableLiveData<>();
        }
        return currentUserId;
    }

    public void updateCurrentUserId(String userId) {
        Log.d(TAG, "updateCurrentUserId initiated: userId= "+ userId);
        if(currentUserId == null){
            currentUserId = new MutableLiveData<>();
        }

        Log.d(TAG, "updateCurrentUserId currentUserId= "+ currentUserId.getValue());
        // if currentUser id is changed, update chat count
        if(!userId.equals(currentUserId.getValue())){
            // Get the chat counts of the new user
            if(inboxCount == null){
                inboxCount = new MutableLiveData<>();
            }
            inboxCount = userRepository.getInboxCount(userId);
            Log.d(TAG, "updateCurrentUserId chatCount= "+ inboxCount.getValue());

            // update notification count of the new user
            if(notificationCount == null){
                notificationCount = new MutableLiveData<>();
            }
            notificationCount = userRepository.getNotificationsCount(userId);

        }
        currentUserId.setValue(userId);
        //getCurrentUser(); // to fitch user with the new id
    }

    public MutableLiveData<User> getCurrentUser() {
        Log.d(TAG, "getUser"+ currentUserId);
        currentUser = userRepository.getCurrentUser(currentUserId.getValue());
        return currentUser;
    }

    // Get counts for unread chats
    public MutableLiveData<Long> getInboxCount(String userId) {
        Log.d(TAG, "getChatsCount"+ userId);
        if(inboxCount == null){
            Log.d(TAG, "chatCount is null, get relation from database");
            inboxCount = new MutableLiveData<>();
            inboxCount = userRepository.getInboxCount(userId);
        }
        Log.d(TAG, "getChatsCount chatCount Count= "+ inboxCount.getValue());
        return inboxCount;
    }

    // Get counts for unread chats
    public MutableLiveData<Long> getNotificationsCount(String userId) {
        Log.d(TAG, "getNotificationsCount"+ userId);
        if(notificationCount == null){
            Log.d(TAG, "notificationCount is null, get relation from database");
            notificationCount = new MutableLiveData<>();
            notificationCount = userRepository.getNotificationsCount(userId);
        }
        Log.d(TAG, "getNotificationsCount notification Count= "+ notificationCount.getValue());
        return notificationCount;
    }

    public void clearViewModel() {
        Log.d(TAG, "removeListeners");
        onCleared();
    }

    @Override
    protected void onCleared() {
        Log.d(TAG, "mama MainActivityViewModel onCleared:");
        super.onCleared();
        userRepository.removeListeners();
    }
}
