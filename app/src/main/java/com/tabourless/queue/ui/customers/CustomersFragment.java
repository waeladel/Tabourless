package com.tabourless.queue.ui.customers;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.tabourless.queue.R;
import com.tabourless.queue.adapters.CustomersAdapter;
import com.tabourless.queue.databinding.CustomersBottomSheetBinding;
import com.tabourless.queue.databinding.FragmentCustomersBinding;
import com.tabourless.queue.interfaces.ItemClickListener;
import com.tabourless.queue.models.Counter;
import com.tabourless.queue.models.Customer;
import com.tabourless.queue.models.Queue;
import com.tabourless.queue.models.User;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

import static com.tabourless.queue.App.CUSTOMER_STATUS_FRONT;
import static com.tabourless.queue.App.CUSTOMER_STATUS_WAITING;
import static com.tabourless.queue.App.DIRECTION_ARGUMENTS_KEY_PLACE_ID;
import static com.tabourless.queue.App.DIRECTION_ARGUMENTS_KEY_QUEUE_ID;
import static com.tabourless.queue.Utils.DatabaseHelper.getMatchedCounters;
import static com.tabourless.queue.Utils.DateHelper.getRelativeTime;
import static com.tabourless.queue.Utils.StringUtils.getFirstWord;


/**
 * A simple {@link Fragment} subclass.
 */
public class CustomersFragment extends Fragment implements ItemClickListener {

    private final static String TAG = CustomersFragment.class.getSimpleName();

    private FragmentCustomersBinding mBinding;
    private CustomersViewModel mViewModel;
    private NavController navController ;
    private Context mContext;

    private ArrayList<Customer> mArrayList;
    private CustomersAdapter mAdapter;
    private LinearLayoutManager mLinearLayoutManager;

    private String mCurrentUserId, mPlaceId, mQueueId;
    private FirebaseUser mFirebaseCurrentUser;

    private static final int REACHED_THE_TOP = 2;
    private static final int SCROLLING_UP = 1;
    private static final int SCROLLING_DOWN = -1;
    private static final int REACHED_THE_BOTTOM = -2;
    private static int mScrollDirection;
    private static int mLastVisibleItem;

    private CustomersBottomSheetBinding mBottomSheetBinding;
    private BottomSheetBehavior mBottomSheetBehavior;

    private Customer mCurrentCustomer, mTempCustomer;
    private User mCurrentUser;
    private Queue mQueue;

    private Future mItemDelayFuture;
    private Runnable mRunnable;
    private ExecutorService mExecutorService;

    public CustomersFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Context context) {
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

        // Get placeId and queueId from Arguments
        if(null != getArguments() && getArguments().containsKey(DIRECTION_ARGUMENTS_KEY_PLACE_ID)) {
            // Get PlaceID
            mPlaceId = CustomersFragmentArgs.fromBundle(getArguments()).getPlaceId();
        }

        if(null != getArguments() && getArguments().containsKey(DIRECTION_ARGUMENTS_KEY_QUEUE_ID)){
            // Get Queue
            mQueueId = CustomersFragmentArgs.fromBundle(getArguments()).getQueueId();
        }
        Log.d(TAG, "placeId = " + mPlaceId + " queueId= " + mQueueId);

        // prepare the Adapter
        mArrayList = new ArrayList<>();
        mAdapter = new CustomersAdapter(mContext,this); // Pass itemClickListener to open user profile when clicked

        // start init  mViewModel here after placeId and queueID is received//
        // extend mMessagesViewModel to pass place and queue's Keys value
        if(!TextUtils.isEmpty(mPlaceId) && !TextUtils.isEmpty(mQueueId)){
            mViewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
                @NonNull
                @Override
                public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                    return (T)new CustomersViewModel (mPlaceId, mQueueId);
                }
            }).get(CustomersViewModel.class);
        }


        mRunnable = new Runnable() {
            @Override
            public void run() {

            }
        };
        // ExecutorService to help in starting and stopping the thread for items
        mExecutorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mBinding = FragmentCustomersBinding.inflate(inflater, container, false);
        mBottomSheetBinding = mBinding.bottomSheetLayout;
        mBottomSheetBehavior = BottomSheetBehavior.from(mBottomSheetBinding.bottomSheetLayout);
        View view = mBinding.getRoot();

        // Expand the bottom sheet when clicked
        mBottomSheetBinding.bottomSheetLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mBottomSheetBehavior.getState()== BottomSheetBehavior.STATE_EXPANDED){
                    mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }else{
                    mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
            }
        });

        //mBottomSheetBinding.aheadCustomers.setText(getString(R.string.queue_info_ahead_customers, 2));

        navController = NavHostFragment.findNavController(this);

        // Initiate the RecyclerView
        mBinding.customersRecycler.setHasFixedSize(true);
        mLinearLayoutManager = new LinearLayoutManager(mContext);
        mBinding.customersRecycler.setLayoutManager(mLinearLayoutManager);
        mBinding.customersRecycler.setAdapter(mAdapter);

        // It's best to observe on onActivityCreated so that we dona't have to update ViewModel manually.
        // This is because LiveData will not call the observer since it had already delivered the last result to that observer.
        // But recycler adapter is updated any way despite that LiveData delivers updates only when data changes, and only to active observers.
        // Use getViewLifecycleOwner() instead of this, to get only one observer for this view
        mViewModel.getItemPagedList().observe(getViewLifecycleOwner(), new Observer<PagedList<Customer>>() {
            @Override
            public void onChanged(@Nullable final PagedList<Customer> items) {
                Log.d(TAG, "mama customers items size =" +  items.size());
                if (items != null ){
                    // your code here
                    Log.d(TAG, "queues onChanged submitList size" +  items.size());
                    // Create new Thread to loop until items.size() is greater than 0
                    // kill the previous task before start it again with the latest data
                    if(null != mItemDelayFuture && !mItemDelayFuture.isDone()){
                        mItemDelayFuture.cancel(true);
                    }

                    mRunnable = new Runnable() {
                        @Override
                        public void run() {
                            int sleepCounter = 0;
                            Log.d(TAG, "queues onChanged. new Runnable starts= "+items.size()+" sleepCounter="+sleepCounter++);
                            try {
                                while(items.size()==0) {
                                    //Keep looping as long as items size is 0
                                    Thread.sleep(20);
                                    Log.d(TAG, "queues onChanged. sleep 1000. size= "+items.size()+" sleepCounter="+sleepCounter++);
                                    if(sleepCounter == 500){
                                        break;
                                    }
                                    //handler.post(this);
                                }
                                //Now items size is greater than 0, let's submit the List
                                Log.d(TAG, "onChanged. after  sleep finished. size= "+items.size());
                                if(items.size() == 0 && sleepCounter == 500){
                                    // If we submit List after loop is finish with 0 results
                                    // we may erase another results submitted via newer thread

                                    //  Loop finished with 0 items. We must submitList to remove all items. also i can't count on just submitting null to adapter
                                    //  when swipe last item because last item maybe deleted by another user.
                                    Log.d(TAG, "onChanged. Loop finished with 0 items. We must submitList to remove all");
                                    items.clear();
                                    mAdapter.submitList(items);
                                }else{
                                    Log.d(TAG, "onChanged. submitList= "+items.size());
                                    mAdapter.submitList(items);
                                }

                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    };

                    // submit task to threadpool:
                    mItemDelayFuture = mExecutorService.submit(mRunnable);

                }
            }
        }); // End of get itemPagedList//

        // Get current Customer/user
         /*mViewModel.getCurrentCustomer().observe(getViewLifecycleOwner(), new Observer<Customer>() {
            @Override
            public void onChanged(Customer customer) {
                LiveData<Customer> mCurrentCustomer = customer;
            }
        });*/

        // To listen to changes of current customer and queue at the same time
        final MediatorLiveData liveDataMerger  = new MediatorLiveData<>();
        liveDataMerger.addSource(mViewModel.getCurrentUser(), new Observer<User>() {
            @Override
            public void onChanged(User user) {
                if (user != null){
                    Log.d(TAG, "getCurrentUser onChanged: user name= "+ user.getName()+ " userId= "+ user.getKey());
                    mCurrentUser = user;

                    // Set customer object properties
                    // set user age
                    Calendar c = Calendar.getInstance();
                    int year = c.get(Calendar.YEAR);
                    int age = year- user.getBirthYear();
                    mTempCustomer = new Customer(user.getAvatar(), user.getName(), user.getGender(), age, user.getDisabled(), 0, CUSTOMER_STATUS_WAITING);

                    liveDataMerger.setValue(user);
                }else{
                    // To hide "Your token" and update customers ahead when user unbook
                    mCurrentUser = null;
                    liveDataMerger.setValue(null);
                }
            }
        });


        liveDataMerger.addSource(mViewModel.getCurrentCustomer(), new Observer<Customer>() {
            @Override
            public void onChanged(Customer customer) {
                if (customer != null){
                    Log.d(TAG, "getCurrentCustomer onChanged: customer number= "+ customer.getNumber()+ " customerId= "+ customer.getKey());
                    mCurrentCustomer = customer;
                    liveDataMerger.setValue(customer);
                }else{
                    // To hide "Your token" and update customers ahead when user unbook
                    mCurrentCustomer = null;
                    liveDataMerger.setValue(null);
                }
            }
        });

        liveDataMerger.addSource(mViewModel.getQueue(), new Observer<Queue>() {
            @Override
            public void onChanged(Queue queue) {
                if(queue != null){
                    Log.d(TAG, "getQueue onChanged: getTotalCustomers= "+ queue.getTotalCustomers());
                    mQueue = queue;
                    liveDataMerger.setValue(queue);
                }
            }
        });

        // live data merger can now listen to changes in queue and current customers
        liveDataMerger.observe(getViewLifecycleOwner(), new Observer() {
            @Override
            public void onChanged(Object object) {
                Log.d(TAG, "liveDataMerger onChanged");
                if (mQueue != null) {
                    Log.d(TAG, "liveDataMerger. getQueue onChanged: getTotalCustomers= "+ mQueue.getTotalCustomers());

                    // Display total customers
                    mBottomSheetBinding.totalCustomers.setText(getString(R.string.queue_info_total_customers, mQueue.getTotalCustomers()));

                    // Display total ahead customers
                    int biggestFrontNumber = 0;
                    long shortestWaitingTime = 0;
                    long shortestServiceTime = 0;
                    long expectedWaitingTime = 0;

                    // Check if there is the open counters and suitable for the current user
                    Map<String, Counter> suitableCounters = null;
                    if(mCurrentCustomer != null){
                        Log.d(TAG, "liveDataMerger. getCurrentCustomer onChanged: customer number= "+ mCurrentCustomer.getNumber()+ " customerId= "+ mCurrentCustomer.getKey());
                        suitableCounters = getMatchedCounters(mQueue.getCounters() , mCurrentCustomer);
                    }else if(mCurrentUser != null){
                        // User didn't book the current queue, lets get the user's data instead of customers data
                        Log.d(TAG, "liveDataMerger. getCurrentUser onChanged: user name= "+ mCurrentUser.getName()+ " userId= "+ mCurrentUser.getKey());
                        suitableCounters = getMatchedCounters(mQueue.getCounters() , mTempCustomer);
                    }

                    if(null != suitableCounters && suitableCounters.size() > 0){
                        // loop throw all suitable counters for this customer to get the biggest front number
                        for (Object o : suitableCounters.entrySet()) {
                            Map.Entry pair = (Map.Entry) o;
                            Log.d(TAG, "queue.getCounters() map key/val = " + pair.getKey() + " = " + pair.getValue());
                            Counter counter = mQueue.getCounters().get(String.valueOf(pair.getKey()));
                            if (counter != null) {
                                // get the biggest front number that doesn't exceed current customer number
                                if(mCurrentCustomer != null && null != mCurrentCustomer.getNumber()){
                                    // current user is an existing customer in the queue
                                    if (counter.getFrontNumber() >= biggestFrontNumber && counter.getFrontNumber() <= mCurrentCustomer.getNumber()) {
                                        biggestFrontNumber = counter.getFrontNumber();
                                    }
                                }else{
                                    // current user is just spectating which means there is not customer's token, just get the biggest front number
                                    // regardless of the current customer's token
                                    if (counter.getFrontNumber() >= biggestFrontNumber) {
                                        biggestFrontNumber = counter.getFrontNumber();
                                    }
                                }


                                // get the shortest Waiting Time
                                if (shortestWaitingTime == 0 || counter.getWaitingTime() <= shortestWaitingTime) {
                                    shortestWaitingTime = counter.getWaitingTime();
                                }

                                // get the shortest Service Time
                                if (shortestServiceTime == 0 || counter.getServiceTime() <= shortestServiceTime) {
                                    shortestServiceTime = counter.getServiceTime();
                                }
                            }
                        }// End loop
                    }


                    // Calculating the ahead users
                    int aheadCustomers;
                    if(mCurrentCustomer != null && null != mCurrentCustomer.getNumber()){
                        aheadCustomers = mCurrentCustomer.getNumber() - biggestFrontNumber;// - queue.getFrontNumber;
                        // ahead customers can't be greater or equal total customers
                        if(aheadCustomers >= mQueue.getTotalCustomers()){
                            aheadCustomers = mQueue.getTotalCustomers() - 1;
                        }
                    }else{
                        // If user book now his number will be the queue last number + 1 or queue total
                        //aheadCustomers = (mQueue.getLastNumber() + 1) - biggestFrontNumber;
                        aheadCustomers = (mQueue.getLastNumber() + 1) - biggestFrontNumber;
                        // ahead customers can't be greater than total customers
                        if(aheadCustomers > mQueue.getTotalCustomers()){
                            aheadCustomers = mQueue.getTotalCustomers();
                        }
                    }

                    if(aheadCustomers <= 1){
                        mBottomSheetBinding.aheadCustomers.setText(getString(R.string.queue_info_ahead_customers, aheadCustomers));
                    }else{
                        mBottomSheetBinding.aheadCustomers.setText(getString(R.string.queue_info_ahead_customers_more_less, aheadCustomers));
                    }

                    // Display expected waiting time
                    expectedWaitingTime = shortestServiceTime * aheadCustomers;
                    mBottomSheetBinding.expectedWaiting.setText(getString(R.string.queue_info_expected_waiting, getRelativeTime(expectedWaitingTime, mContext)));

                    // Display average waiting time
                    mBottomSheetBinding.averageWaiting.setText(getString(R.string.queue_info_average_waiting, getRelativeTime(shortestWaitingTime, mContext)));


                    // Display average service time
                    mBottomSheetBinding.serviceTime.setText(getString(R.string.queue_info_service_time, getRelativeTime(shortestServiceTime, mContext)));

                    // Display current customer number
                    Log.d(TAG, "mCurrentCustomer= "+ mCurrentCustomer );
                    if(mCurrentCustomer != null && null != mCurrentCustomer.getNumber()){
                        Log.d(TAG,  " token= "+mCurrentCustomer.getNumber());
                        mBottomSheetBinding.yourNumber.setText(getString(R.string.queue_info_your_number, mCurrentCustomer.getNumber()));
                        mBottomSheetBinding.yourNumber.setVisibility(View.VISIBLE);
                    }else{
                        mBottomSheetBinding.yourNumber.setVisibility(View.GONE);
                        Log.d(TAG,  " token= gone");
                    }

                    // Display front number
                    mBottomSheetBinding.servedNumber.setText(getString(R.string.queue_info_current_number, biggestFrontNumber));

                }
            }
        });


        mBinding.customersRecycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
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
                int visibleItemCount = mBinding.customersRecycler.getChildCount(); // items are shown on screen right now

                Log.d(TAG, "visibleItemCount = "+visibleItemCount +" totalItemCount= "+totalItemCount+" lastVisibleItem "+lastVisibleItem +" firstVisibleItem "+firstVisibleItem);

                //if(lastCompletelyVisibleItem >= (totalItemCount-1)){
                /*if(lastVisibleItem >= (totalItemCount-1)){
                    // The position of last displayed item = total items, witch means we are at the bottom
                    mScrollDirection = REACHED_THE_BOTTOM;
                    Log.i(TAG, "List reached the bottom");
                    // Set scrolling direction and and last visible item which is needed to know
                    // the initial key position weather it's above or below
                    mViewModel.setScrollDirection(mScrollDirection, lastVisibleItem);

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
                Log.d(TAG, "onSwiped: getItemCount="+ mAdapter.getItemCount());

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
                    final Customer deletedCustomer = mAdapter.getItem(position); // Get customer to be deleted, it is also useful if user undo
                    String shortenName; // To use user's first name instead of Customer word
                    if(deletedCustomer != null) {
                        // Check if we suppose to remove this user or he is not served yet
                        if(TextUtils.equals(deletedCustomer.getStatus(), CUSTOMER_STATUS_FRONT)
                                || TextUtils.equals(deletedCustomer.getKey(), mCurrentUserId)){
                            // proceed with removing the customer
                            if(!TextUtils.isEmpty(deletedCustomer.getName())){
                                shortenName = getFirstWord(deletedCustomer.getName());
                            }else{
                                shortenName = getString(R.string.customer_name_constant);
                            }
                            // If current user deleting himself we must change Snackbar message
                            String SnackMessage;
                            if(TextUtils.equals(deletedCustomer.getKey(), mCurrentUserId)){
                                SnackMessage = getString(R.string.alert_confirm_removing_booking);
                            }else{
                                SnackMessage = getString(R.string.alert_confirm_removing_customer, shortenName);
                            }
                            Snackbar.make(mBinding.customersRecycler, SnackMessage, Snackbar.LENGTH_LONG)
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
                                                Log.d(TAG, "onDismissed: event is not click undo deletedCustomer= "+deletedCustomer.getKey());
                                                mViewModel.removeCustomer(deletedCustomer);
                                                if(mAdapter.getItemCount() <= 1){
                                                    mAdapter.notifyItemRemoved(position);
                                                    mAdapter.submitList(null);
                                                }
                                            }
                                        }

                                        @Override
                                        public void onShown(Snackbar snackbar) {
                                            Log.d(TAG, "onShown: ");
                                        }
                                    })
                                    .show();
                        }else{
                            // Don't delete this customer
                            mAdapter.notifyItemChanged(position);
                            Toast.makeText(mContext, R.string.swipe_item_disabled_toast, Toast.LENGTH_SHORT).show();
                        }

                    }
                }
            }

            public void onChildDraw(Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                Log.d(TAG, "onChildDraw: isCurrentlyActive= "+isCurrentlyActive);
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && viewHolder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                    int position = viewHolder.getAdapterPosition(); // Get Position of to be deleted item
                    Customer deletedCustomer = mAdapter.getItem(position); // Get customer to be deleted, it is also useful if user undo
                    float translationX = 0;
                    // Check if we suppose to remove this user or he is not served yet
                    if(deletedCustomer != null && TextUtils.equals(deletedCustomer.getStatus(), CUSTOMER_STATUS_FRONT)
                        || deletedCustomer != null &&  TextUtils.equals(deletedCustomer.getKey(), mCurrentUserId)) {
                        // proceed with removing the customer
                        // Decorate swipe background
                        new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                                .addBackgroundColor(ContextCompat.getColor(mContext, R.color.colorError))
                                .addActionIcon(R.drawable.ic_delete_sweep_24dp)
                                .setActionIconTint(R.color.colorOnError)
                                .create()
                                .decorate();
                        super.onChildDraw(c, recyclerView, viewHolder, (dX -translationX), dY, actionState, isCurrentlyActive);
                    }else{
                        // Don't allow removing the customer he is not served yet
                        if (dX < 0) {
                            translationX = Math.min(-dX, viewHolder.itemView.getWidth() >> 2); // Math.min(-dX, viewHolder.itemView.getWidth() /4);
                        } else {
                            translationX = Math.max(-dX, (-1) * viewHolder.itemView.getWidth() >> 2); // Math.max(-dX, (-1) * viewHolder.itemView.getWidth() /4);
                        }
                        // Decorate swipe background
                        new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, -translationX, dY, actionState, isCurrentlyActive)
                                .addBackgroundColor(ContextCompat.getColor(mContext, R.color.disabled_button))
                                .addActionIcon(R.drawable.ic_delete_sweep_24dp)
                                .setActionIconTint(R.color.colorOnError)
                                .create()
                                .decorate();
                        viewHolder.itemView.setTranslationX(-translationX);
                    }
                }
            }
        });

        itemTouchHelper.attachToRecyclerView(mBinding.customersRecycler);

        return view;
    }


    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        // Update all broken avatars on fragment's stop
        /*if (mAdapter != null) {
            mViewModel.updateBrokenAvatars(mAdapter.getBrokenAvatarsList(), new FirebaseOnCompleteCallback() {
                @Override
                public void onCallback(@NonNull Task<Void> task) {
                    if(task.isSuccessful()){
                        // onSuccess clear the list to start all over
                        Log.d(TAG, "brokenAvatarsList url . onSuccess ");
                        mAdapter.clearBrokenAvatarsList();
                    }
                }
            });
        }*/
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView");
        super.onDestroyView();
        mBinding = null;
    }

    @Override
    public void onClick(View view, int position, boolean isLongClick) {
        Log.d(TAG, "onClick: item id="+mAdapter.getItemId(position));
        Customer customer = mAdapter.getItem(position);
        if(customer != null){
            if (view.getId() == R.id.customer_image) {
                // only avatar is clicked, go to profile
                NavDirections direction = CustomersFragmentDirections.actionCustomersToProfile(customer.getKey());
                navController.navigate(direction);
            } else {
                // entire row is clicked, chat with user
                NavDirections direction = CustomersFragmentDirections.actionCustomersToMessages(null, customer.getKey(), false);
                navController.navigate(direction);
            }
        }
    }

}
