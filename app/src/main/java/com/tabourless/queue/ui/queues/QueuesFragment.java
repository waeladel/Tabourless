package com.tabourless.queue.ui.queues;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.tabourless.queue.R;
import com.tabourless.queue.adapters.QueuesAdapter;
import com.tabourless.queue.databinding.FragmentQueuesBinding;
import com.tabourless.queue.interfaces.ItemClickListener;
import com.tabourless.queue.models.UserQueue;

import java.util.ArrayList;

import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

public class QueuesFragment extends Fragment implements ItemClickListener {

    private final static String TAG = QueuesFragment.class.getSimpleName();

    private QueuesViewModel mViewModel;
    private FragmentQueuesBinding mBinding;
    private NavController navController;
    private Context mContext;

    private LinearLayoutManager mLinearLayoutManager;

    private FirebaseUser mFirebaseCurrentUser;
    private String mCurrentUserId;

    private ArrayList<UserQueue> mArrayList;
    private QueuesAdapter mAdapter;

    private static final int REACHED_THE_TOP = 2;
    private static final int SCROLLING_UP = 1;
    private static final int SCROLLING_DOWN = -1;
    private static final int REACHED_THE_BOTTOM = -2;
    private static int mScrollDirection;
    private static int mLastVisibleItem;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

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
        mArrayList = new ArrayList<>();
        mAdapter = new QueuesAdapter(this);

        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                // User is signed in
                if (user != null) {
                    // If user is logged in display his or her queues. It's better to use a listener because user may change profile or re login
                    mViewModel = new ViewModelProvider(QueuesFragment.this).get(QueuesViewModel.class);
                    // It's best to observe on onActivityCreated so that we dona't have to update ViewModel manually.
                    // This is because LiveData will not call the observer since it had already delivered the last result to that observer.
                    // But recycler adapter is updated any way despite that LiveData delivers updates only when data changes, and only to active observers.
                    // Use getViewLifecycleOwner() instead of this, to get only one observer for this view
                    mViewModel.getItemPagedList().observe(getViewLifecycleOwner(), new Observer<PagedList<UserQueue>>() {
                        @Override
                        public void onChanged(@Nullable final PagedList<UserQueue> items) {

                            if (items != null ){
                                // your code here
                                Log.d(TAG, "queues onChanged submitList size" +  items.size());
                                // Create new Thread to loop until items.size() is greater than 0
                                new Thread(new Runnable() {
                                    int sleepCounter = 0;
                                    @Override
                                    public void run() {
                                        try {
                                            while(items.size()==0) {
                                                //Keep looping as long as items size is 0
                                                Thread.sleep(20);
                                                Log.d(TAG, "queues onChanged. sleep 1000. size= "+items.size()+" sleepCounter="+sleepCounter++);
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
                                                mAdapter.submitList(items);
                                            }

                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }

                                    }
                                }).start();

                            }
                        }
                    });
                }
            }
        };

    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        mBinding = FragmentQueuesBinding.inflate(inflater, container, false);
        View view = mBinding.getRoot();

        navController = NavHostFragment.findNavController(this);

        mBinding.searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navController.navigate(R.id.search);
            }
        });

        // Initiate the RecyclerView
        mBinding.queuesRecycler.setHasFixedSize(true);
        mLinearLayoutManager = new LinearLayoutManager(mContext);
        mBinding.queuesRecycler.setLayoutManager(mLinearLayoutManager);
        mBinding.queuesRecycler.setAdapter(mAdapter);

        mBinding.queuesRecycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
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
                int visibleItemCount = mBinding.queuesRecycler.getChildCount(); // items are shown on screen right now
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

        // Swipe to delete
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.START | ItemTouchHelper.END) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            // To only enable swipe for front customers
            /*@Override
            public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                int position = viewHolder.getAdapterPosition(); // Get Position of to be deleted item
                Customer deletedCustomer = mAdapter.getItem(position); // Get customer to be deleted, it is also useful if user undo
                if(deletedCustomer != null && !TextUtils.equals(deletedCustomer.getStatus(), CUSTOMER_STATUS_FRONT)){
                    Toast.makeText(mContext, R.string.swipe_item_disabled_toast, Toast.LENGTH_SHORT).show();
                    return 0;
                }else{
                    return super.getSwipeDirs(recyclerView, viewHolder);
                }
            }*/

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                Log.d(TAG, "onSwiped: AdapterPosition="+viewHolder.getAdapterPosition());
                //PagedList<Customer> items = mAdapter.getCurrentList(); // Get current list to remove deleted customer from it
                /*switch (direction){
                    case ItemTouchHelper.START:
                        Log.d(TAG, "onSwiped: lift");
                        break;
                    case ItemTouchHelper.END:
                        Log.d(TAG, "onSwiped: right");
                        break;
                }*/
                // Lets remove item for items list
                //mAdapter.notifyItemRemoved(position); // don't call remove because user might want to undo, in this case we can return item by notify changed
                if(viewHolder.getAdapterPosition() != RecyclerView.NO_POSITION){
                    final int position = viewHolder.getAdapterPosition(); // Get Position of to be deleted item
                    final UserQueue deletedQueue = mAdapter.getItem(position); // Get customer to be deleted, it is also useful if user undo
                    String shortenName; // To use user's first name instead of Customer word
                    if(deletedQueue != null) {
                        // If current user deleting himself we must change Snackbar message
                        String SnackMessage;
                        if(deletedQueue.getJoinedLong() == 0){
                            // the queue's booking is ended
                            SnackMessage = getString(R.string.alert_confirm_removing_queue);
                        }else{
                            // it's an active reservation
                            SnackMessage = getString(R.string.alert_confirm_removing_booking);
                        }

                        Snackbar.make(mBinding.queuesRecycler, SnackMessage, Snackbar.LENGTH_LONG)
                                .setAction(R.string.confirm_undo_button, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Log.d(TAG, "onClick: ");
                                    }
                                })
                                .addCallback(new Snackbar.Callback() {
                                    @Override
                                    public void onDismissed(Snackbar snackbar, int event) {
                                        Log.d(TAG, "onDismissed: event= "+event);
                                        if(event == DISMISS_EVENT_ACTION){
                                            mAdapter.notifyItemChanged(position);
                                        }else{
                                            // it's dismissed due to time out DISMISS_EVENT_TIMEOUT. Lets remove customer from database
                                            Log.d(TAG, "onDismissed: event is not click undo deletedCustomer= "+deletedQueue.getKey());
                                            mViewModel.removeQueue(mCurrentUserId, deletedQueue);
                                        }
                                    }

                                    @Override
                                    public void onShown(Snackbar snackbar) {
                                        Log.d(TAG, "onShown: ");
                                    }
                                })
                                .show();

                    }
                }
            }

            public void onChildDraw(Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                Log.d(TAG, "onChildDraw: isCurrentlyActive= "+isCurrentlyActive);
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && viewHolder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                    int position = viewHolder.getAdapterPosition(); // Get Position of to be deleted item
                    UserQueue deletedQueue = mAdapter.getItem(position); // Get customer to be deleted, it is also useful if user undo
                    // proceed with removing the queue
                    // Decorate swipe background
                    new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                            .addBackgroundColor(ContextCompat.getColor(mContext, R.color.colorError))
                            .addActionIcon(R.drawable.ic_delete_sweep_24dp)
                            .setActionIconTint(R.color.colorOnError)
                            .create()
                            .decorate();
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                }
            }
        });

        itemTouchHelper.attachToRecyclerView(mBinding.queuesRecycler);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "MainActivity onStop");
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView");
        super.onDestroyView();
        mBinding = null;
    }

    @Override
    public void onClick(View view, int position, boolean isLongClick) {
        //Log.d(TAG, "onClick: item id="+mAdapter.getItemId(position));
        UserQueue userQueue = mAdapter.getItem(position);
        if(userQueue != null){
            Log.d(TAG, "onClick: userQueue name= "+userQueue.getName()+ " placeId= "+ userQueue.getPlaceId()+ " queueId= "+ userQueue.getKey());
            NavDirections direction = QueuesFragmentDirections.actionQueuesToCustomers(userQueue.getPlaceId(), userQueue.getKey());
            navController.navigate(direction);
        }
    }
}
