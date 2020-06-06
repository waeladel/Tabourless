package com.tabourless.queue.ui.addplace;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.tabourless.queue.R;
import com.tabourless.queue.adapters.AddPlaceAdapter;
import com.tabourless.queue.databinding.ActivityMainBinding;
import com.tabourless.queue.databinding.FragmentAddPlaceBinding;
import com.tabourless.queue.interfaces.FirebaseOnCompleteCallback;
import com.tabourless.queue.interfaces.FirebasePlaceCallback;
import com.tabourless.queue.interfaces.ItemClickListener;
import com.tabourless.queue.models.Counter;
import com.tabourless.queue.models.Place;
import com.tabourless.queue.models.PlaceItem;
import com.tabourless.queue.models.Queue;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.tabourless.queue.App.DATABASE_REF_CUSTOMERS;
import static com.tabourless.queue.App.DATABASE_REF_PLACES;
import static com.tabourless.queue.App.DIRECTION_ARGUMENTS_KEY_PLACE_KEY;
import static com.tabourless.queue.App.DIRECTION_ARGUMENTS_KEY_POINT;


/**
 * A simple {@link Fragment} subclass.
 */
public class AddPlaceFragment extends Fragment implements ItemClickListener {
    private final static String TAG = AddPlaceFragment.class.getSimpleName();
    private FirebaseUser mFirebaseCurrentUser;
    private AddPlaceViewModel mViewModel;
    private FragmentAddPlaceBinding mBinding;
    private ActivityMainBinding mActivityBinding;
    private NavController navController;
    private String currentUserId;
    private String PlaceKey;
    private LatLng point;

    private AddPlaceAdapter mAddPlaceAdapter;
    private ArrayList<PlaceItem> placeItemsList = new ArrayList<>();
    private Map<String, Counter> mCountersMap = new LinkedHashMap<>();
    private Map<String, Queue> mQueuesMap = new LinkedHashMap<>();

    private DatabaseReference mDatabaseRef;
    private DatabaseReference mPlacesRef; // for all places which have services and counters within (like chats in basbes)
    private DatabaseReference mCustomersRef; // for all queues (like messages in basbes)



    private Context mContext;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.mContext = context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Get current logged in user
        mFirebaseCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = mFirebaseCurrentUser!= null ? mFirebaseCurrentUser.getUid() : null;

        if(getArguments() != null && getArguments().containsKey(DIRECTION_ARGUMENTS_KEY_POINT)) {
            // get latLng of this place
            point = AddPlaceFragmentArgs.fromBundle(getArguments()).getPoint();
            Log.d(TAG, "getArguments point: "+point);
        }

        if(getArguments() != null && getArguments().containsKey(DIRECTION_ARGUMENTS_KEY_PLACE_KEY)) {
            // get latLng of this place
            if(null != AddPlaceFragmentArgs.fromBundle(getArguments()).getPlaceKey()){
                PlaceKey = AddPlaceFragmentArgs.fromBundle(getArguments()).getPlaceKey();
                Log.d(TAG, "getArguments PlaceKey: "+PlaceKey);
            }
        }

        mViewModel = new ViewModelProvider(this).get(AddPlaceViewModel.class);

        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        mPlacesRef = mDatabaseRef.child(DATABASE_REF_PLACES);
        mCustomersRef = mDatabaseRef.child(DATABASE_REF_CUSTOMERS);

        // Create the adapter first then set it's data
        mAddPlaceAdapter = new AddPlaceAdapter(this,this);

        // We can't display fields unless there is a place object,
        // lets create a place to use it's fields to get dynamic methods working

        // Check if we have a place key, it means we should edit this existing place, not adding a new one
        // Get place from database if it we have place key
        if(mViewModel.getPlace() == null){
            if(PlaceKey != null){
                mViewModel.getPlaceOnce(PlaceKey, new FirebasePlaceCallback() {
                    @Override
                    public void onCallback(Place place) {
                        if(place != null){
                            Log.d(TAG,  "FirebasePlaceCallback onCallback. name= " + place.getName()+ " key= "+place.getKey());
                            mViewModel.setPlace(place);

                            if(point != null && mViewModel.getPlace().getLatitude() == 0.0){
                                // Set lat and lng of this place
                                mViewModel.getPlace().setLatitude(point.latitude);
                                mViewModel.getPlace().setLongitude(point.longitude);
                            }
                            // Display the fetched place
                            showPlace(mViewModel.getPlace());
                        }else{
                            // couldn't get place from database, toast error message
                            Toast.makeText(mContext, R.string.fetch_place_error, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }else{
                // Place key in null, We must create a temp place because we are adding a new place
                // let's create a temp place as we couldn't get it form database
                Place tempPlace = new Place(point.latitude, point.longitude);
                mViewModel.setPlace(tempPlace);

                // Only create place's key if it's the first time to add this place
                if(null == mViewModel.getPlace().getKey()){
                    String placeKey =  mPlacesRef.push().getKey(); // The key of place to be added
                    mViewModel.getPlace().setKey(placeKey); // set the key to place object in the view model
                }

                if(point != null && mViewModel.getPlace().getLatitude() == 0.0){
                    // Set lat and lng of this place
                    mViewModel.getPlace().setLatitude(point.latitude);
                    mViewModel.getPlace().setLongitude(point.longitude);
                }

                showPlace(mViewModel.getPlace());
            }

        }else{
            // mViewModel.getPlace() is not null, we don't need to fetch data again
            Log.d(TAG,  "getPlace is not null. no need to get user from database "+mViewModel.getPlace().getKey());

            if(point != null && mViewModel.getPlace().getLatitude() == 0.0){
                // Set lat and lng of this place
                mViewModel.getPlace().setLatitude(point.latitude);
                mViewModel.getPlace().setLongitude(point.longitude);
            }

            // Display the fetched place
            showPlace(mViewModel.getPlace());
        }

        // Display the temp empty place that has only key, or complete place object if it's not the first time to create it
        //showPlace(mViewModel.getPlace());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mBinding = FragmentAddPlaceBinding.inflate(inflater, container, false);
        View view = mBinding.getRoot();

        navController = NavHostFragment.findNavController(this);

        // Initiate the RecyclerView
        mBinding.addPlaceRecycler.setHasFixedSize(false);
        //mBinding.addPlaceRecycler.setLayoutManager(new LinearLayoutManager(mContext)); // LinearLayoutManager is added in the xml
        mBinding.addPlaceRecycler.setAdapter(mAddPlaceAdapter);

        /*DividerItemDecoration divider = new DividerItemDecoration(mBinding.addPlaceRecycler.getContext(), DividerItemDecoration.VERTICAL);
        mBinding.addPlaceRecycler.addItemDecoration(divider);*/

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if((getActivity())!= null) {
            // To save place when save fab in the activity is clicked
            getActivity().findViewById(R.id.save_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Save place is clicked");
                    savePlace();
                }
            });
        }
    }


    // To split place object to place's  items for the recycler. each field in a single item, and item for each queue
    private void showPlace(Place place) {
        if (null!= place) {
            Field[] fields = place.getClass().getDeclaredFields();
            for (Field f : fields) {
                //Log.d(TAG, "fields=" + f.getName());
                //Log.d(TAG, "fields="+f.get);
                getDynamicMethod(f.getName(), place);
            }
            //Update adapter's data and notify changes
            mAddPlaceAdapter.setPlaceItemsList(placeItemsList);
            mAddPlaceAdapter.setPlaceQueuesMap(mViewModel.getPlace().getQueues());
            mAddPlaceAdapter.notifyDataSetChanged();
        }
    }

    private void getDynamicMethod(String fieldName, Place place) {

        Method[] methods = place.getClass().getMethods();

        for (Method method : methods) {
            if ((method.getName().startsWith("get")) && (method.getName().length() == (fieldName.length() + 3))) {
                if (method.getName().toLowerCase().endsWith(fieldName.toLowerCase())) {
                    // Method found, run it
                    try {
                        String value; // To get the value of each field
                        // check if method is null
                        if (method.invoke(place) != null){
                            value = String.valueOf(method.invoke(place));
                            Log.d(TAG, "DynamicMethod value= " +method.getName()+" = "+ value);
                            //Log.d(TAG, "DynamicMethod ReturnType= " +method.getReturnType().getSimpleName());
                        }else{
                            value = null;
                        }

                        if(fieldName.equals("name")){
                            // To and item for name field
                            placeItemsList.add(new PlaceItem(getString(R.string.add_place_name_title)+"*", value, getString(R.string.add_place_name_hint),getString(R.string.add_place_name_helper), PlaceItem.VIEW_TYPE_TEXT_INPUT));
                        }

                        if(fieldName.equals("parent")){
                            // To and item for parent field
                            placeItemsList.add(new PlaceItem(getString(R.string.add_place_parent_title), value, getString(R.string.add_place_parent_hint),getString(R.string.add_place_parent_helper), PlaceItem.VIEW_TYPE_TEXT_INPUT));
                        }

                        if(fieldName.equals("queues")){
                            // To items for each queue. If first created, just add one empty queue
                            Log.d(TAG, "getDynamicMethod: queues size= "+place.getQueues().size());
                            if(0 == place.getQueues().size()){
                                // Crete temp queue
                                showQueue(null,2);
                            }else{
                                // show queues data, each queue in a separate item
                                // loop to get all queues HashMap
                                int QueuesStartPosition = 2;
                                for (Object o : place.getQueues().entrySet()) {
                                    Map.Entry pair = (Map.Entry) o;
                                    Log.d(TAG, "queues objects = " + pair.getKey() + " = " + pair.getValue());

                                    Queue queue = place.getQueues().get(String.valueOf(pair.getKey()));
                                    if (queue != null) {
                                        queue.setKey(String.valueOf(pair.getKey()));
                                        int position = QueuesStartPosition ++; // position = 2 now, but it will increment to 3 next loop
                                        Log.d(TAG, "added queue position"+ position);
                                        showQueue(queue, position);
                                    }
                                }
                            }

                            // Add button at the end of recycler
                            placeItemsList.add(new PlaceItem(null, value, null,null, PlaceItem.VIEW_TYPE_BUTTON));
                        }


                    } catch (IllegalAccessException e) {
                        Log.e(TAG, "Could not determine method: " + method.getName());

                    } catch (InvocationTargetException e) {

                        Log.e(TAG, "Could not determine method:" + method.getName());
                    }

                }
            }
        }
    }

    private void showQueue(Queue queue, int position) {

        // check if queue is null it means we had to add a new temp queue
        if(queue == null){
            // Create keys for queues inside queue child (places/placeId/queues/queueKey1,2,3). it must be the same key in customers and userQueues nods
            String queueKey = mCustomersRef.push().getKey();
            queue = new Queue();  // create new temp queue object
            queue.setKey(queueKey); //  set Queue key, it will help us saving queue data to queue map when saving
        }

        // add temp queue to queues hashMap to generate recycler view for queues
        //mQueuesMap.put(queueKey, tempQueue);

        /*// TreeMap keeps all entries in sorted order by key
        TreeMap<String, Queue> sortedQueuesMAp = new TreeMap<>(mQueuesMap);*/

        // Add queue and it's counters to place item
        PlaceItem placeItem = new PlaceItem(getString(R.string.add_place_queue_title)+"*", queue.getName(), getString(R.string.add_place_queue_hint),getString(R.string.add_place_queue_helper), PlaceItem.VIEW_TYPE_QUEUE);
        placeItem.setQueue(queue);
        placeItemsList.add(position, placeItem);

        // Add queue to view model, so we keep the date even if device is rotated, and view model object will be used in saving place to database
        mViewModel.getPlace().getQueues().put(queue.getKey(), queue);

        // loop to get all chat members HashMap
        /*final List<Queue> queuesList = new ArrayList<>();
        for (Object o : place.getQueues().entrySet()) {
            Map.Entry pair = (Map.Entry) o;
            Log.d(TAG, "mama Chats getMember = " + pair.getKey() + " = " + pair.getValue());

            if (!mFirebaseCurrentUser.getUid().equals(pair.getKey())) {
                Queue queue = place.getQueues().get(String.valueOf(pair.getKey()));
                if (queue != null) {
                    queue.setKey(String.valueOf(pair.getKey()));
                    queuesList.add(queue);
                    Log.d(TAG, "queue membersListSize=" + queuesList.size());
                    Log.d(TAG, "queue name=" + queue.getName());
                }
            }
        }*/
    }

    /*private void showCounter(int queuePosition) {
        // Get queue that needs to be updated
        PlaceItem placeItem = placeItemsList.get(queuePosition);
        Queue queue = placeItem.getQueue();
        // Create keys for counters inside counter child (places/placeId/counters/counterKey1,2,3)
        String counterKey = mPlacesRef.child(mViewModel.getPlace().getKey()).child("queues").child(queue.getKey()).child("counters").push().getKey();

        // create temp queue and counter just to display empty service and counter
        Counter tempCounter = new Counter();
        tempCounter.setKey(counterKey); // set key for the empty new counter

        // add temp counter to counters hashMap to generate child recycler view for counters
        queue.getCounters().put(counterKey, tempCounter);

        // Update child recycler
        mAddPlaceAdapter.notifyItemChanged(queuePosition);
    }*/

    private void savePlace() {

        // Check if place name is not empty
        if(TextUtils.isEmpty(mViewModel.getPlace().getName())){
            Toast.makeText(getActivity(), R.string.add_place_name_error, Toast.LENGTH_LONG).show();
            return;
        }

        // return if queues size is 0
        if(mViewModel.getPlace().getQueues().size() == 0){
            Toast.makeText(getActivity(), R.string.add_place_service_empty_error, Toast.LENGTH_LONG).show();
            return;
        }
        // Check if service name is not empty
        // loop to get all queues HashMap
        for (Object q : mViewModel.getPlace().getQueues().entrySet()) {
            Map.Entry pair = (Map.Entry) q;
            Log.d(TAG, "loop throw all queues: key= " + pair.getKey() + " value = " + pair.getValue());

            Queue queue = mViewModel.getPlace().getQueues().get(String.valueOf(pair.getKey()));
            if (queue != null) {
                // return if service name is empty
                if(TextUtils.isEmpty(queue.getName())){
                    Toast.makeText(getActivity(), R.string.add_place_service_error, Toast.LENGTH_LONG).show();
                    return;
                }

                // return if counters size is 0
                if(queue.getCounters().size() == 0){
                    Toast.makeText(getActivity(), R.string.add_place_counter_empty_error, Toast.LENGTH_LONG).show();
                    return;
                }

                // Loop throw all counters of this specific queue
                for (Object c : queue.getCounters().entrySet()) {
                    Map.Entry counterPair = (Map.Entry) c;
                    Log.d(TAG, "loop throw all queues: key= " + counterPair.getKey() + " value = " + counterPair.getValue());

                    Counter counter = queue.getCounters().get(String.valueOf(counterPair.getKey()));
                    if (counter != null) {
                        // return if service name is empty
                        if(TextUtils.isEmpty(counter.getName())){
                            Toast.makeText(getActivity(), R.string.add_place_counter_error, Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                }
            }
        }

        // Let's save place to database
        mViewModel.addPlace(new FirebaseOnCompleteCallback() {
            @Override
            public void onCallback(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    // Go to customers recycler
                    Log.d(TAG, "FirebaseOnCompleteCallback onCallback: "+task.isSuccessful());
                }else{
                    Toast.makeText(mContext, R.string.save_place_error, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    // get Item click from add place adapter
    @Override
    public void onClick(View view, int position, boolean isLongClick) {

        // Check which button is clicked
        if(view.getId() == R.id.add_more_services){
            // Show new added queue
            showQueue(null, position); // queue should be null to create new temp queue
            mAddPlaceAdapter.notifyItemInserted(position);

            // Delay the scroll until the new queue item is added
            Handler handler = new Handler();
            Runnable r = new Runnable() {
                public void run() {
                    // Scroll to lase position
                    mBinding.addPlaceRecycler.smoothScrollToPosition(mAddPlaceAdapter.getItemCount()-1);
                }
            };
            handler.postDelayed(r, 500);

        }else if(view.getId() == R.id.delete_service_button){
            // Delete the selected queue
            Log.d(TAG, "onClick: view= "+view.getId()+ " position = "+ position + " item count= "+ mAddPlaceAdapter.getItemCount());

            // remove queue from view model
            PlaceItem placeItem = placeItemsList.get(position);
            if(placeItem != null && null != placeItem.getQueue().getKey()){
                mViewModel.getPlace().getQueues().remove(placeItem.getQueue().getKey());
            }

            // update adapter
            placeItemsList.remove(position);
            mAddPlaceAdapter.notifyItemRemoved(position);
        }

    }
}
