package com.tabourless.queue.ui.notifications;

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
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.tabourless.queue.adapters.NotificationsAdapter;
import com.tabourless.queue.databinding.FragmentNotificationsBinding;
import com.tabourless.queue.interfaces.ItemClickListener;
import com.tabourless.queue.models.Chat;
import com.tabourless.queue.models.DatabaseNotification;

import java.util.ArrayList;

import static com.tabourless.queue.App.NOTIFICATION_TYPE_QUEUE_FRONT;
import static com.tabourless.queue.App.NOTIFICATION_TYPE_QUEUE_NEXT;
import static com.tabourless.queue.App.NOTIFICATION_TYPE_MESSAGE;

public class NotificationsFragment extends Fragment implements ItemClickListener {

    private final static String TAG = NotificationsFragment.class.getSimpleName();

    private NotificationsViewModel mViewModel;
    private FragmentNotificationsBinding mBinding;
    private NavController navController;

    private RecyclerView mRecycler;
    private ArrayList<Chat> mArrayList;
    private NotificationsAdapter mAdapter;

    private FirebaseUser mFirebaseCurrentUser;
    private String mCurrentUserId;

    // [START declare_database_ref]
    private DatabaseReference mDatabaseRef;
    private DatabaseReference mNotificationsRef;

    private Context mContext;
    private LinearLayoutManager mLinearLayoutManager;

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
        //Get current logged in user
        mFirebaseCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mCurrentUserId = mFirebaseCurrentUser != null ? mFirebaseCurrentUser.getUid() : null;

        // prepare the Adapter in onCreate to use only one Adapter
        mArrayList = new ArrayList<>();
        mAdapter = new NotificationsAdapter(mContext,this);


        mViewModel = new ViewModelProvider(this).get(NotificationsViewModel.class);
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        mBinding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View view = mBinding.getRoot();

        navController = NavHostFragment.findNavController(this);
        // It's best to observe on onActivityCreated so that we dona't have to update ViewModel manually.
        // This is because LiveData will not call the observer since it had already delivered the last result to that observer.
        // But recycler adapter is updated any way despite that LiveData delivers updates only when data changes, and only to active observers.
        // Use getViewLifecycleOwner() instead of this, to get only one observer for this view
        mViewModel.getItemPagedList().observe(getViewLifecycleOwner(), new Observer<PagedList<DatabaseNotification>>() {
            @Override
            public void onChanged(final PagedList<DatabaseNotification> items) {
                if (items != null ){
                    // your code here
                    Log.d(TAG, "Notifications onChanged submitList size" +  items.size());
                    // Create new Thread to loop until items.size() is greater than 0
                    new Thread(new Runnable() {
                        int sleepCounter = 0;
                        @Override
                        public void run() {
                            try {
                                while(items.size()==0) {
                                    //Keep looping as long as items size is 0
                                    Thread.sleep(20);
                                    Log.d(TAG, "Notifications onChanged. sleep 1000. size= "+items.size()+" sleepCounter="+sleepCounter++);
                                    if(sleepCounter == 1000){
                                        break;
                                    }
                                    //handler.post(this);
                                }
                                //Now items size is greater than 0, let's submit the List
                                Log.d(TAG, "Notifications onChanged. after  sleep finished. size= "+items.size());
                                if(items.size() == 0 && sleepCounter == 1000){
                                    // If we submit List after loop is finish with 0 results
                                    // we may erase another results submitted via newer thread
                                    Log.d(TAG, "Notifications onChanged. Loop finished with 0 items. Don't submitList");
                                }else{
                                    Log.d(TAG, "Notifications onChanged. submitList= "+items.size());
                                    mAdapter.submitList(items);
                                }

                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                        }
                    }).start();
                                /*Thread thread = new Thread() {
                                    @Override
                                    public void run() {
                                        try {
                                            while(items.size()==0) {
                                                //Keep looping as long as items size is 0
                                                sleep(10);
                                                Log.d(TAG, "sleep 1000. size= "+items.size());
                                                //handler.post(this);
                                            }
                                            //Now items size is greater than 0, let's submit the List
                                            Log.d(TAG, "after  sleep finished. size= "+items.size());
                                            mAdapter.submitList(items);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                };
                                thread.start();*/
                }
            }
        });

        // Initiate the RecyclerView
        mBinding.notificationsRecycler.setHasFixedSize(true);
        mLinearLayoutManager = new LinearLayoutManager(mContext);
        mBinding.notificationsRecycler.setLayoutManager(mLinearLayoutManager);
        mBinding.notificationsRecycler.setAdapter(mAdapter);

        mBinding.notificationsRecycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
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
                int totalItemCount = mAdapter.getItemCount(); // total items count from the adapter
                int visibleItemCount = mBinding.notificationsRecycler.getChildCount(); // items are shown on screen right now

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
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }

    @Override
    public void onClick(View view, int position, boolean isLongClick) {
        // get clicked notification
        DatabaseNotification notification = mAdapter.getItem(position);
        if(notification != null){
            notification.setClicked(true); // set clicked notification to true
            mDatabaseRef = FirebaseDatabase.getInstance().getReference();
            // use received chatKey to create a database ref
            mNotificationsRef = mDatabaseRef.child("notifications").child("alerts").child(mCurrentUserId);
            mNotificationsRef.child(notification.getKey()).child("clicked").setValue(true);// update clicked field on database

            // to open customers when notification type is not a message
            //userQueue.getPlaceId(), userQueue.getKey()
            NavDirections customersDirection = NotificationsFragmentDirections.actionNotificationsToCustomers(notification.getPlaceId(), notification.getQueueId());

            if (null != notification.getType()) {
                switch (notification.getType()){
                    case NOTIFICATION_TYPE_QUEUE_FRONT:
                        navController.navigate(customersDirection);
                        break;
                    case NOTIFICATION_TYPE_QUEUE_NEXT:
                        navController.navigate(customersDirection);
                        break;
                }

            }
        }

    }
}
