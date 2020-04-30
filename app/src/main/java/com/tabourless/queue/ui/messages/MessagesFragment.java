package com.tabourless.queue.ui.messages;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.CountDownTimer;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;
import com.tabourless.queue.R;
import com.tabourless.queue.adapters.MessagesAdapter;
import com.tabourless.queue.databinding.FragmentMessagesBinding;
import com.tabourless.queue.interfaces.FirebaseMessageCallback;
import com.tabourless.queue.models.Chat;
import com.tabourless.queue.models.ChatMember;
import com.tabourless.queue.models.DatabaseNotification;
import com.tabourless.queue.models.Message;
import com.tabourless.queue.models.User;
import com.tabourless.queue.ui.ChatBlockedAlertFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.tabourless.queue.Utils.DatabaseKeys.getJoinedKeys;
import static com.tabourless.queue.Utils.StringUtils.getFirstWord;

public class MessagesFragment extends Fragment {

    private final static String TAG = MessagesFragment.class.getSimpleName();

    private FragmentMessagesBinding mBinding;
    private MessagesViewModel mViewModel;
    private NavController navController ;
    private Context mContext;

    private ArrayList<Message> mMessagesArrayList;
    private MessagesAdapter mMessagesAdapter;
    private LinearLayoutManager mLinearLayoutManager;

    private String mCurrentUserId, mChatUserId, mChatId;
    private User mChatUser, mCurrentUser ;
    private Chat mChat;
    private FirebaseUser mFirebaseCurrentUser;
    private Boolean isGroup;
    private Boolean isHitBottom;// = false;
    private boolean isAdjustPan;

    // [START declare_database_ref]
    private DatabaseReference mDatabaseRef;
    private DatabaseReference mChatsRef;
    private DatabaseReference mMessagesRef;
    private DatabaseReference mNotificationsRef;

    private static final int REACHED_THE_TOP = 2;
    private static final int SCROLLING_UP = 1;
    private static final int SCROLLING_DOWN = -1;
    private static final int REACHED_THE_BOTTOM = -2;
    private int mScrollDirection;
    private int bottomVisibleItemCount;

    // DatabaseNotification's types
    private static final String NOTIFICATION_TYPE_MESSAGE = "Message";

    private static final String Message_STATUS_SENDING = "Sending";
    private static final String Message_STATUS_SENT = "Sent";
    private static final String Message_STATUS_DELIVERED = "Delivered";

    private  static final String BLOCKED_CHAT_FRAGMENT = "BlockedChatFragment";

    private static final String IS_HII_BOTTOM = "Hit_Bottom";

    //private Timer mTimer;
    private static CountDownTimer mAgoTimer;
    private long mTimeLiftInMillis;
    private Long mLastOnlineEndTime;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        //Get current logged in user
        mFirebaseCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mCurrentUserId = mFirebaseCurrentUser != null ? mFirebaseCurrentUser.getUid() : null;

        if(null != getArguments() && getArguments().containsKey("chatUserId") && getArguments().containsKey("isGroup")) {
            //mCurrentUserId = MessagesFragmentArgs.fromBundle(getArguments()).getCurrentUserId();//logged in user
            mChatUserId = MessagesFragmentArgs.fromBundle(getArguments()).getChatUserId();// any user
            //mCurrentUserId = MessagesFragmentArgs.fromBundle(getArguments()).getCurrentUserId();
            isGroup = MessagesFragmentArgs.fromBundle(getArguments()).getIsGroup();
            if(null != MessagesFragmentArgs.fromBundle(getArguments()).getChatId()){
                mChatId = MessagesFragmentArgs.fromBundle(getArguments()).getChatId();
            }else{
                // Chat ID is not passed from MainFragment, we need to create
                mChatId = getJoinedKeys(mCurrentUserId , mChatUserId);
            }
            Log.d(TAG, "currentUserId = " + mCurrentUserId + " mChatUserId= " + mChatUserId+ " mChatId= "+ mChatId);
        }

        // prepare the Adapter
        mMessagesArrayList = new ArrayList<>();
        mMessagesAdapter = new MessagesAdapter(); // Pass chat id because it's needed to update message revelation

        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        mChatsRef = mDatabaseRef.child("chats");
        mMessagesRef = mDatabaseRef.child("messages");
        mNotificationsRef = mDatabaseRef.child("notifications");

        // start init  mMessagesViewModel here after mCurrentUserId and chat user is received//
        // extend mMessagesViewModel to pass Chat Key value and chat user key //
        mViewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T)new MessagesViewModel (mChatId, mChatUserId, mCurrentUserId);
            }
        }).get(MessagesViewModel.class);


    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, final Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mBinding = FragmentMessagesBinding.inflate(inflater, container, false);
        View view = mBinding.getRoot();

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.queues, R.id.inbox, R.id.notifications, R.id.complete_profile)
                .build();
        navController = NavHostFragment.findNavController(this);

        // Setup toolbar
        NavigationUI.setupWithNavController(mBinding.toolbar, navController, appBarConfiguration);

        mBinding.toolbar.setTitle("Ahmed Adel");
        mBinding.toolbar.setLogoDescription("Online 5 m ago");
        //mBinding.toolbar.setNavigationIcon(R.drawable.ic_business_man);
        mBinding.toolbar.setSubtitle("Online 5 m ago");

        // Initiate the RecyclerView
        mBinding.messagesRecycler.setHasFixedSize(true);
        /* setStackFromEnd is useful to start stacking recycler from it's last
        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(mActivityContext);
        mLinearLayoutManager.setStackFromEnd(true);*/
        mLinearLayoutManager = new LinearLayoutManager(mContext);
        mBinding.messagesRecycler.setLayoutManager(mLinearLayoutManager);

        //viewModel.usersList.observe(this, mUsersAdapter::submitList);

        //observe when a change happen to usersList live data
        mBinding.messagesRecycler.setAdapter(mMessagesAdapter);

        // Listen for Edit text Touch event, to scroll down when focused
        mBinding.messageInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onTouch: Action was UP");
                // Set soft input to pan if items is > 3 and if never set before
                if(!isAdjustPan){
                    if ((getActivity()) != null) {
                        if(mMessagesAdapter.getItemCount() > 3){
                            getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
                            isAdjustPan = true;
                        }
                    }
                }
                scrollToBottom();
            }
        });

        // Listen for Edit text OnFocusChange, to scroll down when focused (first time click)
        mBinding.messageInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    Log.d(TAG, "onFocusChange:  Action was UP");
                    // Set soft input to pan if items is > 3
                    if ((getActivity()) != null) {
                        if(mMessagesAdapter.getItemCount() > 3){
                            getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
                            isAdjustPan = true;
                        }
                    }
                    scrollToBottom();
                }
            }
        });

        mBinding.toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.i(TAG, "user avatar or name clicked. mChatUser getKey()= " + mChatUserId);
                NavDirections ProfileDirection = MessagesFragmentDirections.actionMessagesToProfile(mChatUserId);
                navController.navigate(ProfileDirection);
            }
        });

        /*Picasso.get()
                .load("https://firebasestorage.googleapis.com/v0/b/tabourless-queue.appspot.com/o/images%2FuZUsaqEbfpTbFuO3mzcIeuiVqcx1%2Favatar.jpg?alt=media&token=2b9dfebd-7a05-42b4-9cf9-97bd6d95d1ef")
                .into(new Target() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                        Drawable d = new BitmapDrawable(getResources(), bitmap);
                        //mBinding.toolbar.setLogo(d);
                    }

                    @Override
                    public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {
                    }
                });*/

        // It's best to observe on onActivityCreated so that we dona't have to update ViewModel manually.
        // This is because LiveData will not call the observer since it had already delivered the last result to that observer.
        // But recycler adapter is updated any way despite that LiveData delivers updates only when data changes, and only to active observers.
        // Use getViewLifecycleOwner() instead of this, to get only one observer for this view
        mViewModel.itemPagedList.observe(getViewLifecycleOwner(), new Observer<PagedList<Message>>() {
            @Override
            public void onChanged(@Nullable final PagedList<Message> items) {
                System.out.println("mama onChanged");
                if (items != null ){
                    // your code here
                    // Create new Thread to loop until items.size() is greater than 0
                    new Thread(new Runnable() {
                        int sleepCounter = 0;
                        @Override
                        public void run() {
                            try {
                                while(items.size()==0) {
                                    //Keep looping as long as items size is 0
                                    Thread.sleep(20);
                                    Log.d(TAG, "sleep 1000. size= "+items.size()+" sleepCounter="+sleepCounter++);
                                    if(sleepCounter == 1000){
                                        break;
                                    }
                                    //handler.post(this);
                                }
                                //Now items size is greater than 0, let's submit the List
                                Log.d(TAG, "after  sleep finished. size= "+items.size());
                                if(items.size() == 0 && sleepCounter == 1000){
                                    // If we submit List after loop is finish with 0 results
                                    // we may erase another results submitted via newer thread
                                    Log.d(TAG, "Loop finished with 0 items. Don't submitList");
                                }else{
                                    Log.d(TAG, "submitList");
                                    // Scroll to last item
                                    // Only scroll to bottom if user is not reading messages above
                                    Log.d(TAG, "scroll to bottom if user is not above. isHitBottom= "+ isHitBottom+ " items.size= "+items.size()+ " ItemCount= "+mMessagesAdapter.getItemCount());

                                    // Check if we have isHitBottom saved when change configuration occur or not
                                    if(savedInstanceState != null){
                                        isHitBottom = savedInstanceState.getBoolean(IS_HII_BOTTOM);
                                    }

                                    Log.d(TAG, "isHitBottom= "+isHitBottom +" adapter getItemCount= "+ mMessagesAdapter.getItemCount());

                                    mMessagesAdapter.submitList(items);
                                        /*mMessagesViewModel.getLastMessageOnce(mChatId, new FirebaseMessageCallback() {
                                            @Override
                                            public void onCallback(Message message) {
                                                if(message != null){
                                                    LastMessageKey = message.getKey();
                                                }
                                            }
                                        });*/

                                        /*if( null == isHitBottom){
                                            if(mMessagesAdapter.getItemCount()>0 ){// stop scroll to bottom if there are no items
                                                //mMessagesRecycler.smoothScrollToPosition(items.size()-1);
                                                Log.d(TAG, "isHitBottom adapter getItemCount= "+mMessagesAdapter.getItemCount());
                                                //mMessagesRecycler.smoothScrollToPosition(mMessagesAdapter.getItemCount()-1);
                                                //mMessagesRecycler.smoothScrollToPosition(mMessagesAdapter.getItemCount());
                                                mMessagesRecycler.postDelayed(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        mMessagesRecycler.scrollToPosition(mMessagesAdapter.getItemCount()-1);
                                                    }
                                                }, 500);
                                            }
                                        }else if(isHitBottom){
                                            if(mMessagesAdapter.getItemCount()>0 ){// stop scroll to bottom if there are no items
                                                //mMessagesRecycler.smoothScrollToPosition(items.size()-1);
                                                Log.d(TAG, "isHitBottom adapter getItemCount= "+mMessagesAdapter.getItemCount());
                                                //mMessagesRecycler.smoothScrollToPosition(mMessagesAdapter.getItemCount()-1);
                                                //mMessagesRecycler.smoothScrollToPosition(mMessagesAdapter.getItemCount());
                                                mMessagesRecycler.postDelayed(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        mMessagesRecycler.smoothScrollToPosition(mMessagesAdapter.getItemCount()-1);
                                                    }
                                                }, 500);
                                            }
                                        }*/

                                    if( null == isHitBottom || isHitBottom){
                                        if(mMessagesAdapter.getItemCount()>0 ){// stop scroll to bottom if there are no items
                                            //mMessagesRecycler.smoothScrollToPosition(items.size()-1);
                                            Log.d(TAG, "adapter getItemCount= "+mMessagesAdapter.getItemCount());
                                            //mMessagesRecycler.smoothScrollToPosition(mMessagesAdapter.getItemCount()-1);
                                            //mMessagesRecycler.smoothScrollToPosition(mMessagesAdapter.getItemCount());
                                            mBinding.messagesRecycler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    mBinding.messagesRecycler.smoothScrollToPosition(mMessagesAdapter.getItemCount()-1);
                                                }
                                            }, 500);
                                        }
                                    }

                                }

                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                        }
                    }).start();

                }
            }
        });// End init itemPagedList here after mCurrentUserId is received//

        // Listen for scroll events
        mBinding.messagesRecycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                Log.d(TAG, "onScrollStateChanged newState= "+newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                Log.d(TAG, "onScrolled dx= "+dx +" dy= "+dy);
                // set scrolling direction. it's needed for the initial key
                //scrollDirectionY = dy;
                //mMessagesViewModel.setScrollDirection(dy);

                //int visibleItemCount = mMessagesRecycler.getChildCount(); // items are shown on screen right now
                int firstCompletelyVisibleItem = mLinearLayoutManager.findFirstCompletelyVisibleItemPosition(); // the position of first displayed item
                //int totalItemCount = mLinearLayoutManager.getItemCount();
                int totalItemCount = mMessagesAdapter.getItemCount(); // total items count from the adapter
                int lastCompletelyVisibleItem = mLinearLayoutManager.findLastCompletelyVisibleItemPosition(); // the position of last displayed item
                int lastVisibleItem = mLinearLayoutManager.findLastVisibleItemPosition(); // the position of last displayed item

                //int firstVisibleItem = mLinearLayoutManager.findFirstVisibleItemPosition(); // the position of last displayed item

                //int pastVisibleItems = mLinearLayoutManager.findFirstCompletelyVisibleItemPosition();
                Log.d(TAG, " totalItemCount= "+totalItemCount+" lastVisibleItem "+lastCompletelyVisibleItem);

                if(lastCompletelyVisibleItem >= (totalItemCount-1)){
                    //if(lastCompletelyVisibleItem >= (totalItemCount-5)){
                    // The position of last displayed item = total items, witch means we are at the bottom
                    mScrollDirection = REACHED_THE_BOTTOM;
                    Log.i(TAG, "List reached the bottom");
                    //}else if(lastCompletelyVisibleItem <= visibleItemCount){
                }else if(firstCompletelyVisibleItem <= 4){
                    // The position of last displayed item is less than visibleItemCount, witch means we are at the top
                    mScrollDirection = REACHED_THE_TOP;
                    Log.i(TAG, "List reached the top");
                }else{
                    if(dy < 0 ){
                        // dy is negative number,  scrolling up
                        Log.i(TAG, "List scrolling up");
                        mScrollDirection = SCROLLING_UP;
                    }else{
                        // dy is positive number,  scrolling down
                        Log.i(TAG, "List scrolling down");
                        mScrollDirection = SCROLLING_DOWN;
                    }
                }

                //mMessagesViewModel.setScrollDirection(mScrollDirection);
                // Set scrolling direction and and last visible item which is needed to know
                // the initial key position weather it's above or below
                mViewModel.setScrollDirection(mScrollDirection, lastCompletelyVisibleItem);

                // The position of last displayed item = total items, witch means we are at the bottom
                if(lastCompletelyVisibleItem >= (totalItemCount-1)){
                    // End of the list is here.
                    Log.i(TAG, "List reached the End. isHitBottom="+isHitBottom);
                    isHitBottom = true;
                    bottomVisibleItemCount = mBinding.messagesRecycler.getChildCount();
                }else{
                    isHitBottom = false;
                }
                Log.i(TAG, "isHitBottom = "+isHitBottom);

                // The position of first displayed item = total items - visible count
                // witch means user starts scrolling up and the first visible item is now on the bottom
                Log.d(TAG, "last displayed item  lastVisibleItem = " + lastVisibleItem + " totalItemCount= "+  totalItemCount+ " bottomVisibleItemCou= "+bottomVisibleItemCount);
                if((lastVisibleItem <= ((totalItemCount-1) - bottomVisibleItemCount) && (bottomVisibleItemCount > 0))){ // check if bottomVisibleItemCount is greater than 0 to hide fab on start
                    mBinding.scrollFab.setVisibility(View.VISIBLE);
                }else{
                    mBinding.scrollFab.setVisibility(View.INVISIBLE);
                    // End of the list is here.
                    Log.i(TAG, "End of the list is here");
                }

            }
        });

        mBinding.scrollFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "mScrollFab is clicked");
                // Scroll to bottom
                scrollToBottom();
            }
        });

        mBinding.sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "mSendButton is clicked ");
                String messageText = mBinding.messageInput.getText().toString().trim();
                Log.d(TAG, "getJoinedKeys ="+getJoinedKeys(mCurrentUserId , mChatUserId));
                if(!TextUtils.isEmpty(messageText)){
                    // clear text before sending the message successfully for offline capabilities
                    //mMessage.setText(null);
                    //sendMessage(mChatId, messageText);
                    validateSendMessage(messageText);
                }
            }
        });

        // Get chat before getting chat user, to know if chat is blocked or not
        mViewModel.getChat(mChatId).observe(getViewLifecycleOwner(), new Observer<Chat>() {
            @Override
            public void onChanged(Chat chat) {
                mChat = chat;// to get chat even if null, it helps to detect blocked chat
            }
        });

        // get Chat User
        if(!isGroup){
            mViewModel.getChatUser(mChatUserId).observe(getViewLifecycleOwner(), new Observer<User>() {
                @Override
                public void onChanged(User user) {
                    Log.d(TAG, "mMessagesViewModel onChanged chatUser name= "+user.getName()+ " hashcode= "+ hashCode());
                    mChatUser = user;
                    mChatUser.setKey(mChatUserId);
                    // display ChatUser name
                    if(null != mChatUser.getName()){
                        mBinding.toolbar.setTitle(getFirstWord(mChatUser.getName()));
                    }

                    // display last online
                    if(null != mChatUser.getLastOnline()){

                        Log.d(TAG, "getLastOnline()= "+mChatUser.getLastOnline());
                        mLastOnlineEndTime = mChatUser.getLastOnline();

                        // check if chat is exist or not, if not exist show last online
                        if(null == mChat || null == mChat.getActive()){
                            // show active now because there are no blocking relation
                            if(mChatUser.getLastOnline() == 0){
                                //user is active now
                                Log.d(TAG, "LastOnline() == 0");
                                mBinding.toolbar.setSubtitle(R.string.user_active_now);
                            }else{
                                // Display last online
                                UpdateTimeAgo(mLastOnlineEndTime);
                            }
                        }else{
                            // Don't show active now if blocked
                            if(mChat.getActive() != -1){
                                if(mChatUser.getLastOnline() == 0){
                                    //user is active now
                                    Log.d(TAG, "LastOnline() == 0");
                                    mBinding.toolbar.setSubtitle(R.string.user_active_now);
                                }else{
                                    // Display last online
                                    UpdateTimeAgo(mLastOnlineEndTime);
                                }
                            }else{
                                // Don't show active now because there are no blocking relation
                                mBinding.toolbar.setSubtitle(null);
                            }
                        }
                    }
                    // check if chat is exist or not, if not exist show avatar
                    if(null == mChat || null == mChat.getActive()){
                        // Display user avatar
                        showAvatar();
                    }else{
                        if(null != mChat.getActive() && mChat.getActive() == -1){
                            //Chat is blocked, return so that we don't display avatar
                            // To display placeholder if user is blocked
                            mBinding.userImage.setImageResource(R.drawable.ic_round_account_filled_emphasis_high_72);
                        }else{
                            // Display user avatar
                            showAvatar();
                        }
                    }
                }
            });
        }


        mViewModel.getCurrentUser(mCurrentUserId).observe(getViewLifecycleOwner(), new Observer<User>() {
            @Override
            public void onChanged(User user) {
                Log.d(TAG, "mMessagesViewModel onChanged chatUser userId name= " + user.getName());
                mCurrentUser = user;
                if(null == mCurrentUser.getKey()){
                    mCurrentUser.setKey(mCurrentUserId);
                }
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        long now = System.currentTimeMillis();

        // Re-start Last online countdown timer on fragment start
        if(mLastOnlineEndTime != null){
            UpdateTimeAgo(mLastOnlineEndTime);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        // Cancel all countdown timers on fragment stop
        CancelLastOnlineTimer();

        // Create a map for all messages need to be updated
        Map<String, Object> updateMap = new HashMap<>();

        // Update all revealed messages on fragment's stop
        if(mMessagesAdapter != null){

            // Get status list from the adapter
            List<Message> statusList = mMessagesAdapter.getStatusList();
            //mMessagesAdapter.getCurrentList();

            for (int i = 0; i < statusList.size(); i++) {
                Log.d(TAG, "statusList message= "+statusList.get(i).getMessage() + " key= "+statusList.get(i).getKey() + " status= "+statusList.get(i).getStatus());
                updateMap.put(statusList.get(i).getKey()+"/status", statusList.get(i).getStatus());
            }

            // Get broken avatars list from the adapter
            List<Message> brokenAvatarsList = mMessagesAdapter.getBrokenAvatarsList();
            //mMessagesAdapter.getCurrentList();

            for (int i = 0; i < brokenAvatarsList.size(); i++) {
                Log.d(TAG, "brokenAvatarsList message= "+brokenAvatarsList.get(i).getMessage() + " key= "+brokenAvatarsList.get(i).getKey() + " avatar= "+brokenAvatarsList.get(i).getSenderAvatar());
                updateMap.put(brokenAvatarsList.get(i).getKey()+"/senderAvatar", brokenAvatarsList.get(i).getSenderAvatar());
            }

        }// End of if mMessagesAdapter is not null

        mMessagesRef.child(mChatId).updateChildren(updateMap).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // onSuccess clear the list to start all over
                mMessagesAdapter.clearStatusList();
                mMessagesAdapter.clearBrokenAvatarsList();
            }
        });

    }

    // Fires when a configuration change occurs and fragment needs to save state
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if(isHitBottom != null){
            outState.putBoolean (IS_HII_BOTTOM, isHitBottom);
        }
    }

    private void scrollToBottom() {
        // Get the last Message of this chat room
        mViewModel.getLastMessageOnce(mChatId, new FirebaseMessageCallback() {
            @Override
            public void onCallback(Message message) {
                Log.d(TAG, "getLastMessageOnce  onCallback = "+ message.getMessage() + " Current List= "+mMessagesAdapter.getCurrentList());
                if(null != mMessagesAdapter.getCurrentList()){
                    // This is the last displayed message on the adapter
                    //Message LastDisplayedMessage =  mMessagesAdapter.getCurrentList().get(mMessagesAdapter.getItemCount()-1);
                    Message LastDisplayedMessage =  mMessagesAdapter.getItem(mMessagesAdapter.getItemCount()-1);

                    int totalItemCount = mMessagesAdapter.getItemCount(); // total items count from the adapter
                    int lastCompletelyVisibleItem = mLinearLayoutManager.findLastCompletelyVisibleItemPosition(); // the position of last displayed item
                    
                    // Check if the key of database last message is equal to the adapter last message
                    if(TextUtils.equals(message.getKey(), LastDisplayedMessage.getKey())){
                        Log.d(TAG, "getLastMessageOnce  LastDisplayedMessage = "+ LastDisplayedMessage.getMessage() + " Last Message= "+ message.getMessage());
                        // Just scroll down
                        if (totalItemCount > 0) {// stop scroll to bottom if there are no items
                            //mMessagesRecycler.smoothScrollToPosition(items.size()-1);
                            Log.d(TAG, "adapter getItemCount= " + mMessagesAdapter.getItemCount());

                            if((lastCompletelyVisibleItem + 20) < totalItemCount){
                                Log.d(TAG, "it's a long distance. scrollToPosition");
                                mBinding.messagesRecycler.scrollToPosition(mMessagesAdapter.getItemCount() - 1);
                            }else{
                                Log.d(TAG, "it's a short distance. smoothScrollToPosition");
                                mBinding.messagesRecycler.smoothScrollToPosition(mMessagesAdapter.getItemCount() - 1);
                            }
                        }

                    }else{
                        // Invalidate to reload latest data with null initial key instead of scrolling down
                        mScrollDirection = REACHED_THE_BOTTOM;
                        isHitBottom = true;

                        mViewModel.setScrollDirection(mScrollDirection, lastCompletelyVisibleItem);
                        Log.d(TAG, "invalidating the dataSource...");
                        //mItems.getDataSource().invalidate();
                        //mMessagesAdapter.getCurrentList().getDataSource().invalidate();
                        mViewModel.invalidateData();

                    }
                }
            }
        });
    }

    // Cancel last online countdown timer
    private void CancelLastOnlineTimer() {
        if(mAgoTimer != null){
            mAgoTimer.cancel();
            Log.d(TAG, "mAgoTimer canceled");
        }
    }

    // To Display avatar if exists or a placeholder
    private void showAvatar() {
        Log.d(TAG, "showAvatar starts");
        if(mChatUser != null){
            if (null != mChatUser.getAvatar()) {
                Picasso.get()
                        .load(mChatUser.getAvatar())
                        .placeholder(R.mipmap.account_circle_72dp)
                        .error(R.drawable.ic_round_broken_image_72px)
                        .into(mBinding.userImage);
            }else{
                // To display placeholder if user has no avatar
                mBinding.userImage.setImageResource(R.drawable.ic_round_account_filled_emphasis_high_72);
            }
        }
    }

    // A countdown timer to update last online time every minute
    private void UpdateTimeAgo(final Long lastOnline) {
        long now = System.currentTimeMillis();
        Log.d(TAG, "now = "+now);

        if (mAgoTimer != null) {
            CancelLastOnlineTimer();
        }

        mAgoTimer = new CountDownTimer(10*60*1000, 60*1000) {
            @Override
            public void onTick(long millisUntilFinished) {

                if(mChatUser.getLastOnline() == 0){
                    mBinding.toolbar.setSubtitle(R.string.user_active_now);
                }else{
                    long now = System.currentTimeMillis();
                    Log.d(TAG, "mAgoTimer onTick: now = "+now + " getLastOnline= "+mChatUser.getLastOnline());

                    // check if now is greater than last online value
                    // if now is less, ago message will me active in 1 minutes
                    if(now > mChatUser.getLastOnline()){
                        CharSequence ago =
                                DateUtils.getRelativeTimeSpanString(lastOnline, now, DateUtils.MINUTE_IN_MILLIS);
                        Activity activity = getActivity();
                        if(activity != null && isAdded()){
                            mBinding.toolbar.setSubtitle(getString(R.string.user_active_ago, ago));
                        }
                    }else{
                        mBinding.toolbar.setSubtitle(R.string.user_active_now);
                    }

                }

            }

            @Override
            public void onFinish() {
                //restart countDownTimer again
                Log.d(TAG, "mAgoTimer onFinish. We will restart it");
                mAgoTimer.start();
            }
        }.start();

    }


    private void validateSendMessage(String messageText) {

        if(mChat == null){
            Log.d(TAG, "sendMessage: It's the first message, send it");
            sendMessage(mChatId, messageText);
        }else{
            // it's not the first message
            Log.d(TAG, "sendMessage: it's not the first message");
            if(null != mChat.getActive()) {
                // Active is set
                Log.d(TAG, "sendMessage: Active is set");
                if (mChat.getActive() == -1) {
                    // This chat is blocked
                    Log.d(TAG, "sendMessage: You can't Communicate with this user");
                    //mSendButton.setImageAlpha(0x3F); 0xFF
                    showChatBlockedDialog();
                } else {
                    // This chat is active forever
                    Log.d(TAG, "sendMessage: This chat is active forever, send message");
                    sendMessage(mChatId, messageText);
                }
            }else{// end of null != mChat.getActive()
                // Active is never set, send message anyway
                Log.d(TAG, "sendMessage: Active is never set, and isSender is null for a strange reason, send message anyway");
                sendMessage(mChatId, messageText);
            }

        }// end of mChat == null && isSender == null
    }

    //Show blocked alert dialog
    private void showChatBlockedDialog() {
        ChatBlockedAlertFragment chatBlockedAlert = ChatBlockedAlertFragment.newInstance(mContext);
        if (getParentFragmentManager() != null) {
            chatBlockedAlert.show(getParentFragmentManager(), BLOCKED_CHAT_FRAGMENT);
            Log.i(TAG, "ChatBlockedAlertFragment show clicked ");
        }
    }

    private void sendMessage(String mChatId, String messageText) {

        mBinding.messageInput.setText(null);// Remove text from EditText

        final String messageKey = mMessagesRef.child(mChatId).push().getKey();

        final Message message = new Message(messageText, mCurrentUserId, mCurrentUser.getName(),mCurrentUser.getAvatar(),Message_STATUS_SENDING);

        Map<String, Object> childUpdates = new HashMap<>();// Map to update all
        Map<String, Object> messageValues = message.toMap(); // message map

        Log.d(TAG, "sendMessage CurrentUserId= "+mCurrentUserId+ " userId= "+ mChatUserId + " chatUser= "+ mChatUser);


        // Create members Hash list, it's better to loop throw  selected members
        ChatMember currentMember = new ChatMember(mCurrentUserId, mCurrentUser.getName(), mCurrentUser.getAvatar(), mCurrentUser.getLastOnline(), true);
        ChatMember chatMember = new ChatMember(mChatUserId, mChatUser.getName(), mChatUser.getAvatar(), mChatUser.getLastOnline(), false);


        Map<String, ChatMember> members = new HashMap<>();
        members.put(mCurrentUserId, currentMember);
        members.put(mChatUserId, chatMember);

        // Update notifications
        String notificationKey;
        DatabaseNotification databaseNotification;
        Map<String, Object> notificationValues;

        // Create chat map
        Map<String, Object> chatValues;
        if(mChat != null){
            // get the existing chat and post again after changing last message
            Log.d(TAG, "sendMessage: chat exist, get the existing chat and post again ");
            mChat.setLastMessage(messageText);
            mChat.setMembers(members);// to update user name and avatar when sending new message
            if(null == mChat.getSender()){
                mChat.setSender(mCurrentUserId);
            }
            chatValues = mChat.toMap();

            // Update MESSAGE notifications
            //notificationKey = mNotificationsRef.child(mChatUserId).push().getKey();
            notificationKey = mCurrentUserId + NOTIFICATION_TYPE_MESSAGE;
            //DatabaseNotification notification = new DatabaseNotification(getContext().getString(R.string.notification_like_title), getContext().getString(R.string.notification_like_message, name), "like", currentUserId, name, avatar);
            databaseNotification = new DatabaseNotification(NOTIFICATION_TYPE_MESSAGE, mCurrentUserId, mCurrentUser.getName(), mCurrentUser.getAvatar(), mChatId);
            notificationValues = databaseNotification.toMap();
            childUpdates.put("/notifications/messages/" + mChatUserId + "/" +notificationKey, notificationValues);

        }else{
            // Create new chat from scratch
            Log.d(TAG, "sendMessage: chat is null, create new chat from scratch");
            Chat chat = new Chat(messageText, mCurrentUserId, members);
            chatValues = chat.toMap();

            // Update PICK_UP notifications
            //notificationKey = mNotificationsRef.child(mChatUserId).push().getKey();
            notificationKey = mCurrentUserId + NOTIFICATION_TYPE_MESSAGE;
            //DatabaseNotification notification = new DatabaseNotification(getContext().getString(R.string.notification_like_title), getContext().getString(R.string.notification_like_message, name), "like", currentUserId, name, avatar);
            databaseNotification = new DatabaseNotification(NOTIFICATION_TYPE_MESSAGE, mCurrentUserId, mCurrentUser.getName(), mCurrentUser.getAvatar(), mChatId);
            notificationValues = databaseNotification.toMap();
            childUpdates.put("/notifications/alerts/" + mChatUserId + "/" +notificationKey, notificationValues);
        }

        /*Map<String, Object> chatValues = new HashMap<>();
        chatValues.put("lastMessage", messageText);
        chatValues.put("lastSent", ServerValue.TIMESTAMP);

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/messages/" + mChatId + "/" + messageKey, messageValues);
        childUpdates.put("/chats/" + mChatId + "/lastMessage/", messageText);
        childUpdates.put("/chats/" + mChatId + "/lastSent/", ServerValue.TIMESTAMP);*/


        childUpdates.put("/messages/" + mChatId + "/" + messageKey, messageValues);
        childUpdates.put("/chats/" + mChatId ,chatValues);

        // only if Lookup is needed
        childUpdates.put("/userChats/" + mCurrentUserId + "/" + mChatId, chatValues);
        childUpdates.put("/userChats/" + mChatUserId + "/" + mChatId, chatValues);

        /*// Update counts
        childUpdates.put("/counts/" + mCurrentUserId + "/chats/" + mChatId, null);
        childUpdates.put("/counts/" + mChatUserId + "/chats/" + mChatId, true);*/

        //mScrollDirection = REACHED_THE_BOTTOM;
        //mMessagesViewModel.setScrollDirection(mScrollDirection, lastCompletelyVisibleItem);
        isHitBottom = true;

        mDatabaseRef.updateChildren(childUpdates).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // Write was successful!
                Log.i(TAG, "send message onSuccess");
                // Add message to sent array list
                message.setKey(messageKey);
                message.setStatus(Message_STATUS_SENT);
                // Add successfully sent message to adapter sent array list
                // It's used to notify the adapter to update item position
                mMessagesAdapter.addSentToStatusList(message);

                /*for (int i = 0; i < mItems.size(); i++) {
                    if (mItems.get(i) != null && mItems.get(i).getKey().equals(messageKey)) {
                        Log.d(TAG, "onSuccess. mItems. message= " + mItems.get(i).getMessage() + " key= " + mItems.get(i).getKey());
                        if (mItems.get(i) != null) {
                            mItems.get(i).setSent(true);
                        }

                    }
                }

                mMessagesAdapter.submitList(mItems);*/
                //mMessagesAdapter.submitList(mItems);
                //mMessagesAdapter.notifyDataSetChanged();
                // To scroll to bottom when user send new message
                /*mScrollDirection = REACHED_THE_BOTTOM;
                mMessagesViewModel.setScrollDirection(mScrollDirection);
                isHitBottom = true;*/
                // ...
            }
        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Write failed
                        Toast.makeText(getActivity(), R.string.send_message_error,
                                Toast.LENGTH_LONG).show();
                        // ...
                    }
                });

        mBinding.messagesRecycler.scrollToPosition(mMessagesAdapter.getItemCount()-1);
    }
}
