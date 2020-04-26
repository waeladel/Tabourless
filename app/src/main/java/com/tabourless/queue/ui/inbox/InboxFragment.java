package com.tabourless.queue.ui.inbox;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.tabourless.queue.adapters.InboxAdapter;
import com.tabourless.queue.databinding.FragmentInboxBinding;
import com.tabourless.queue.models.Chat;
import com.tabourless.queue.models.ChatMember;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InboxFragment extends Fragment {

    private final static String TAG = InboxFragment.class.getSimpleName();

    private InboxViewModel mViewModel;
    private FragmentInboxBinding mBinding;
    private Context mContext;
    private  LinearLayoutManager mLinearLayoutManager;

    private FirebaseUser mFirebaseCurrentUser;
    private String mCurrentUserId;

    private ArrayList<Chat> mChatsArrayList;
    private InboxAdapter mInboxAdapter;

    private static final int REACHED_THE_TOP = 2;
    private static final int SCROLLING_UP = 1;
    private static final int SCROLLING_DOWN = -1;
    private static final int REACHED_THE_BOTTOM = -2;
    private static int mScrollDirection;
    private static int mLastVisibleItem;


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        //Get current logged in user
        mFirebaseCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mCurrentUserId = mFirebaseCurrentUser != null ? mFirebaseCurrentUser.getUid() : null;

        // prepare the Adapter in onCreate to use only one Adapter
        mChatsArrayList = new ArrayList<>();
        mInboxAdapter = new InboxAdapter(this);

        mViewModel = new ViewModelProvider(this,  new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T)new InboxViewModel (mCurrentUserId);
            }
        }).get(InboxViewModel.class);

    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        /*View root = inflater.inflate(R.layout.fragment_inbox, container, false);
        final TextView textView = root.findViewById(R.id.text_dashboard);*/
        mBinding = FragmentInboxBinding.inflate(inflater, container, false);
        View view = mBinding.getRoot();

        // It's best to observe on onActivityCreated so that we dona't have to update ViewModel manually.
        // This is because LiveData will not call the observer since it had already delivered the last result to that observer.
        // But recycler adapter is updated any way despite that LiveData delivers updates only when data changes, and only to active observers.
        // Use getViewLifecycleOwner() instead of this, to get only one observer for this view
        mViewModel.getItemPagedList().observe(getViewLifecycleOwner(), new Observer<PagedList<Chat>>() {
            @Override
            public void onChanged(@Nullable final PagedList<Chat> items) {

                if (items != null ){
                    // your code here
                    Log.d(TAG, "chats onChanged submitList size" +  items.size());
                    // Create new Thread to loop until items.size() is greater than 0
                    new Thread(new Runnable() {
                        int sleepCounter = 0;
                        @Override
                        public void run() {
                            try {
                                while(items.size()==0) {
                                    //Keep looping as long as items size is 0
                                    Thread.sleep(20);
                                    Log.d(TAG, "ChatsFragment onChanged. sleep 1000. size= "+items.size()+" sleepCounter="+sleepCounter++);
                                    if(sleepCounter == 1000){
                                        break;
                                    }
                                    //handler.post(this);
                                }
                                //Now items size is greater than 0, let's submit the List
                                Log.d(TAG, "ChatsFragment onChanged. after  sleep finished. size= "+items.size());
                                if(items.size() == 0 && sleepCounter == 1000){
                                    // If we submit List after loop is finish with 0 results
                                    // we may erase another results submitted via newer thread
                                    Log.d(TAG, "ChatsFragment onChanged. Loop finished with 0 items. Don't submitList");
                                }else{
                                    Log.d(TAG, "ChatsFragment onChanged. submitList= "+items.size());
                                    mInboxAdapter.submitList(items);
                                }

                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                        }
                    }).start();

                }
            }
        });

        // Initiate the RecyclerView
        mBinding.chatsRecycler.setHasFixedSize(true);
        mLinearLayoutManager = new LinearLayoutManager(mContext);
        mBinding.chatsRecycler.setLayoutManager(mLinearLayoutManager);
        mBinding.chatsRecycler.setAdapter(mInboxAdapter);

        mBinding.chatsRecycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                Log.d(TAG, "onScrollStateChanged newState= "+newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                Log.d(TAG, "onScrolled dx= "+dx +" dy= "+dy);

                //int lastCompletelyVisibleItem = mLinearLayoutManager.findLastCompletelyVisibleItemPosition(); // the position of last displayed item
                //int firstCompletelyVisibleItem = mLinearLayoutManager.findFirstCompletelyVisibleItemPosition(); // the position of first displayed item
                int lastVisibleItem = mLinearLayoutManager.findLastVisibleItemPosition(); // the position of last displayed item
                int firstVisibleItem = mLinearLayoutManager.findFirstVisibleItemPosition(); // the position of first displayed item
                int totalItemCount = mInboxAdapter.getItemCount(); // total items count from the adapter
                int visibleItemCount = mBinding.chatsRecycler.getChildCount(); // items are shown on screen right now

                Log.d(TAG, "visibleItemCount = "+visibleItemCount +" totalItemCount= "+totalItemCount+" lastVisibleItem "+lastVisibleItem +" firstVisibleItem "+firstVisibleItem);

                //if(lastCompletelyVisibleItem >= (totalItemCount-1)){
                /*if(lastVisibleItem >= (totalItemCount-1)){
                    // The position of last displayed item = total items, witch means we are at the bottom
                    mScrollDirection = REACHED_THE_BOTTOM;
                    Log.i(TAG, "List reached the bottom");
                    // Set scrolling direction and and last visible item which is needed to know
                    // the initial key position weather it's above or below
                    mChatsViewModel.setScrollDirection(mScrollDirection, lastVisibleItem);

                }*/if(firstVisibleItem <= 4){
                    // The position of last displayed item is less than visibleItemCount, witch means we are at the top
                    mScrollDirection = REACHED_THE_TOP;
                    Log.i(TAG, "List reached the top");
                    // Set scrolling direction and and last visible item which is needed to know
                    // the initial key position weather it's above or below
                    mViewModel.setScrollDirection(mScrollDirection, firstVisibleItem);
                }else{
                    if(dy < 0 ){
                        // dy is negative number,  scrolling up
                        Log.i(TAG, "List scrolling up");
                        mScrollDirection = SCROLLING_UP;
                        // Set scrolling direction and and last visible item which is needed to know
                        // the initial key position weather it's above or below
                        mViewModel.setScrollDirection(mScrollDirection, firstVisibleItem);
                    }else{
                        // dy is positive number,  scrolling down
                        Log.i(TAG, "List scrolling down");
                        mScrollDirection = SCROLLING_DOWN;
                        // Set scrolling direction and and last visible item which is needed to know
                        // the initial key position weather it's above or below
                        mViewModel.setScrollDirection(mScrollDirection, lastVisibleItem);
                    }
                }
            }
        });


        return view;
    }


    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");

        // Create a map for all messages need to be updated
        Map<String, Object> updateMap = new HashMap<>();

        // Update all broken avatars on fragment's stop
        if (mInboxAdapter != null) {
            // Get revealed list from the adapter
            List<ChatMember> brokenAvatarsList = mInboxAdapter.getBrokenAvatarsList();


            // We use name as to store the chatId value
            for (int i = 0; i < brokenAvatarsList.size(); i++) {
                Log.d(TAG, "brokenAvatarsList url= " + brokenAvatarsList.get(i).getAvatar() + " key= " + brokenAvatarsList.get(i).getKey() + "name= " + brokenAvatarsList.get(i).getName());
                updateMap.put(brokenAvatarsList.get(i).getName() + "/members/" + brokenAvatarsList.get(i).getKey() + "/avatar", brokenAvatarsList.get(i).getAvatar());
            }


            if (updateMap.size() > 0 && mCurrentUserId != null) {
                Log.d(TAG, "brokenAvatarsList url = updateMap.size= " + updateMap.size() + " mCurrentUserId=" + mCurrentUserId);
                // update senderAvatar to the new uri
                DatabaseReference mDatabaseRef = FirebaseDatabase.getInstance().getReference();
                DatabaseReference mChatsRef = mDatabaseRef.child("userChats").child(mCurrentUserId);
                //mNotificationsRef.child(key).child("senderAvatar").setValue(String.valueOf(uri));
                mChatsRef.updateChildren(updateMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // onSuccess clear the list to start all over
                        Log.d(TAG, "brokenAvatarsList url . onSuccess ");
                        mInboxAdapter.clearBrokenAvatarsList();
                    }
                });
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }
}
