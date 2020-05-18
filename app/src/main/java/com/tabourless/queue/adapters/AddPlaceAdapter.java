package com.tabourless.queue.adapters;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.tabourless.queue.R;
import com.tabourless.queue.databinding.AddPlaceButtonItemBinding;
import com.tabourless.queue.databinding.AddPlaceItemBinding;
import com.tabourless.queue.databinding.AddPlaceQueueItemBinding;
import com.tabourless.queue.interfaces.CounterSaveListener;
import com.tabourless.queue.interfaces.ItemClickListener;
import com.tabourless.queue.models.Counter;
import com.tabourless.queue.models.PlaceItem;
import com.tabourless.queue.models.Queue;
import com.tabourless.queue.ui.CountersDialogFragment;
import com.tabourless.queue.ui.addplace.AddPlaceViewModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class AddPlaceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements CounterSaveListener {

    private final static String TAG = AddPlaceAdapter.class.getSimpleName();

    private static final int VIEW_TYPE_TEXT_INPUT = 1;
    private static final int VIEW_TYPE_QUEUE = 2;
    public static final int VIEW_TYPE_BUTTON = 3;

    //Fragments tags
    private  static final String COUNTER_DIALOG_FRAGMENT_TAG = "CountersDialogFragment";

    // View binding
    private AddPlaceItemBinding mTextInputBinding;
    private AddPlaceQueueItemBinding mQueueBinding;
    private AddPlaceButtonItemBinding mButtonBinding;

    private ArrayList<PlaceItem> placeItemsList;

    private ItemClickListener itemClickListener;
    private AddPlaceViewModel mViewModel;
    private Map<String, Counter> mQueueCountersMap;
    private Map<String, Queue> mPlaceQueuesMap;
    private Fragment mFragment;

    private DatabaseReference mDatabaseRef;
    private DatabaseReference mPlacesRef; // for all places which have queues and counters within

    public AddPlaceAdapter(ArrayList<PlaceItem> arrayList, Fragment fragment, ItemClickListener itemClickListener) {
        this.placeItemsList = arrayList;
        this.itemClickListener = itemClickListener;
        Log.d(TAG, "AddPlaceAdapter: constructor init");

        this.mFragment = fragment; // To use it as observer

        // initiate active fragment's view model
        mViewModel = new ViewModelProvider(mFragment).get(AddPlaceViewModel.class);

        // get Map of queues inside the place
        mPlaceQueuesMap = mViewModel.getPlace().getQueues();

        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        mPlacesRef = mDatabaseRef.child("places");
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        switch (viewType){
            case VIEW_TYPE_TEXT_INPUT:
                // It's text input item like name and parent place
                mTextInputBinding = AddPlaceItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
                return new TexInputHolder(mTextInputBinding);
            case VIEW_TYPE_QUEUE:
                // It's a service item
                mQueueBinding = AddPlaceQueueItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
                return new QueueHolder(mQueueBinding);
            default:
                // it's add more services button item
                mButtonBinding = AddPlaceButtonItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
                return new ButtonHolder(mButtonBinding);

        }
    }



    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position) {

        final PlaceItem placeItem = placeItemsList.get(position);

        if (holder instanceof QueueHolder){
            QueueHolder queuedHolder = (QueueHolder) holder;
            // It's a service item
            if (placeItem != null) {
                queuedHolder.mQueueBinding.titleText.setText(placeItem.getName()); // set the title
                queuedHolder.mQueueBinding.valueText.setText(placeItem.getValue()); // set the input text value
                queuedHolder.mQueueBinding.valueInputLayout.setHint(placeItem.getHint()); // set the input layout hint
                queuedHolder.mQueueBinding.valueInputLayout.setHelperText(placeItem.getHelper());  // set the input layout helper

                // Get all counters and pass it to child recycler view
                Queue queue = placeItem.getQueue();
                AddCounterAdapter addCounterAdapter = new AddCounterAdapter(queue.getCounters(), queue, mFragment, itemClickListener);
                queuedHolder.mQueueBinding.childRecycler.setAdapter(addCounterAdapter);

            }
        }

        if (holder instanceof TexInputHolder){
            TexInputHolder texInputHolder = (TexInputHolder) holder;
            // It's text input item like name and parent place

            if (placeItem != null) {
                texInputHolder.mTextInputBinding.titleText.setText(placeItem.getName()); // set the title
                texInputHolder.mTextInputBinding.valueText.setText(placeItem.getValue()); // set the input text value
                texInputHolder.mTextInputBinding.valueInputLayout.setHint(placeItem.getHint()); // set the input layout hint
                texInputHolder.mTextInputBinding.valueInputLayout.setHelperText(placeItem.getHelper()); // set the input layout helper
            }
        }

    }


    // Determines the appropriate ViewType according to the sender of the message.
    @Override
    public int getItemViewType(int position) {

        PlaceItem placeItem = placeItemsList.get(position);

        if(placeItem.getViewType() == PlaceItem.VIEW_TYPE_QUEUE){
            // it's a queue
            return VIEW_TYPE_QUEUE;
        }else if(placeItem.getViewType() == PlaceItem.VIEW_TYPE_TEXT_INPUT){
            // it's not a queue
            return VIEW_TYPE_TEXT_INPUT;
        }else{
            // it's a button
            return VIEW_TYPE_BUTTON;
        }

    }

    @Override
    public int getItemCount() {
        //Log.d(TAG, "getItemCount: "+ placeItemsList.size());
        return placeItemsList.size();
    }

    /// ViewHolder for services list /////
    public class QueueHolder extends RecyclerView.ViewHolder {

        private AddPlaceQueueItemBinding mQueueBinding;

        private QueueHolder(AddPlaceQueueItemBinding binding) {
            super(binding.getRoot());
            this.mQueueBinding = binding;

            // pass "add more counters" click listener to fragment
            mQueueBinding.addMoreCounters.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //itemClickListener.onClick(v, getAdapterPosition(), false);
                    //Queue mQueueItem = placeItemsList.get(getAdapterPosition()).getQueue();
                    CountersDialogFragment dialogFragment = CountersDialogFragment.newInstance(new Counter(), getAdapterPosition(), AddPlaceAdapter.this);
                    if (mFragment.getParentFragmentManager() != null) {
                        dialogFragment.show(mFragment.getParentFragmentManager(), COUNTER_DIALOG_FRAGMENT_TAG);
                    }

                }
            });

            // Add listener for deleting queue
            mQueueBinding.deleteServiceButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    itemClickListener.onClick(v, getAdapterPosition(), false);
                }
            });

            // To save updated text and spinners values
            mQueueBinding.valueText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable editable) {
                    Log.d(TAG, "Editable Name= "+ editable.toString()+ " position= "+getAdapterPosition()+ " key= "+placeItemsList.get(getAdapterPosition()).getName());

                    Queue mQueueItem = placeItemsList.get(getAdapterPosition()).getQueue();

                    if(TextUtils.isEmpty((String.valueOf(editable).trim()))){
                        // It's empty string, delete existing value
                        mQueueItem.setName(null);
                        placeItemsList.get(getAdapterPosition()).setQueue(mQueueItem);
                        placeItemsList.get(getAdapterPosition()).setValue(null); // to save service name input text

                        // set ViewModel queues values to use it in saving and when rotating the device
                        mPlaceQueuesMap.put(mQueueItem.getKey(), mQueueItem);
                        mViewModel.getPlace().setQueues(mPlaceQueuesMap);

                        mQueueBinding.valueText.setError(mFragment.getString(R.string.add_place_service_error));
                    }else{
                        // It's not empty string, save value
                        mQueueItem.setName(editable.toString());
                        placeItemsList.get(getAdapterPosition()).setQueue(mQueueItem);
                        placeItemsList.get(getAdapterPosition()).setValue(String.valueOf(editable).trim());// to save service name input text

                        // set ViewModel queues values to use it in saving and when rotating the device
                        mPlaceQueuesMap.put(mQueueItem.getKey(), mQueueItem);
                        mViewModel.getPlace().setQueues(mPlaceQueuesMap);

                    }
                }
            });
        }
    }

    /// ViewHolder for SentMessages list /////
    public class TexInputHolder extends RecyclerView.ViewHolder {

        // View binding
        private AddPlaceItemBinding mTextInputBinding;

        public TexInputHolder(AddPlaceItemBinding binding) {
            super(binding.getRoot());
            this.mTextInputBinding = binding;

            // To save updated text
            mTextInputBinding.valueText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable editable) {
                    Log.d(TAG, "Editable Name= "+ editable.toString()+ " position= "+getAdapterPosition()+ " key= "+placeItemsList.get(getAdapterPosition()).getName());

                    if(TextUtils.isEmpty((String.valueOf(editable).trim()))){
                        placeItemsList.get(getAdapterPosition()).setValue(null);
                        // set viewModel values
                        switch (placeItemsList.get(getAdapterPosition()).getName()){
                            case "Name*":
                                mViewModel.getPlace().setName(null);
                                mTextInputBinding.valueText.setError(mFragment.getString(R.string.add_place_name_error));
                                break;
                            case "Container place":
                                mViewModel.getPlace().setParent(null);
                                break;
                        }
                    }else{
                        placeItemsList.get(getAdapterPosition()).setValue(String.valueOf(editable).trim());

                        // set viewModel values
                        switch (placeItemsList.get(getAdapterPosition()).getName()){
                            case "Name*":
                                mViewModel.getPlace().setName(String.valueOf(editable).trim());
                                break;
                            case "Container place":
                                mViewModel.getPlace().setParent(String.valueOf(editable).trim());
                                break;
                        }
                    }

                }
            });

        }

    }

    /// ViewHolder for ReceivedMessages list /////
    public class ButtonHolder extends RecyclerView.ViewHolder {

        private AddPlaceButtonItemBinding mButtonBinding;

        private ButtonHolder(AddPlaceButtonItemBinding binding) {
            super(binding.getRoot());
            this.mButtonBinding = binding;

            // Add click listener for add more services
            mButtonBinding.addMoreServices.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    itemClickListener.onClick(v, getAdapterPosition(), false);
                }
            });
        }
    }

    private void insertCounter(Counter counter, int queueItemPosition) {
        // Get queue that needs to be updated
        PlaceItem placeItem = placeItemsList.get(queueItemPosition);
        Queue queue = placeItem.getQueue();
        // Create keys for counters inside counter child (places/placeId/queues/counters/counterKey1,2,3)
        String counterKey = mPlacesRef.child(mViewModel.getPlace().getKey()).child("queues").child(queue.getKey()).child("counters").push().getKey();

        counter.setKey(counterKey); // set key for the empty new counter

        // add temp counter to counters hashMap to generate child recycler view for counters
        queue.getCounters().put(counterKey, counter);

        // Save counter to view model
        //mQueueCountersMap = queue.getCounters(); // get previous counters of this queue
        //mQueueCountersMap.put(counter.getKey(), counter); // update counters map by the updated current counter
        //queue.setCounters(mQueueCountersMap); // Update current queue by the updated counters map
        mPlaceQueuesMap.put(queue.getKey(), queue); // update queue map by the updated queue
        mViewModel.getPlace().setQueues(mPlaceQueuesMap); // update the view model

        // Update child recycler
        notifyItemChanged(queueItemPosition);
    }


    // When save button is clicked in counter dialog
    @Override
    public void onSave(Counter counter, int position) {
        Log.d(TAG, "onSave: counter name= "+counter.getName()+ " counter gender= "
                + counter.getGender()+ " counter age= "+ counter.getAge()+ " counter disability= "+ counter.getDisability());
        insertCounter(counter, position);
    }

}

