package com.tabourless.queue.adapters;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.tabourless.queue.R;
import com.tabourless.queue.databinding.AddCounterDialogBinding;
import com.tabourless.queue.databinding.EditCounterItemBinding;
import com.tabourless.queue.interfaces.CounterSaveListener;
import com.tabourless.queue.interfaces.ItemClickListener;
import com.tabourless.queue.models.Counter;
import com.tabourless.queue.models.Queue;
import com.tabourless.queue.ui.CountersDialogFragment;
import com.tabourless.queue.ui.addplace.AddPlaceViewModel;

import java.util.Map;

public class AddCounterAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements CounterSaveListener {

    private final static String TAG = AddCounterAdapter.class.getSimpleName();

    //Fragments tags
    private  static final String COUNTER_DIALOG_FRAGMENT_TAG = "CountersDialogFragment";

    // View binding
    private EditCounterItemBinding mBinding;
    private AddPlaceViewModel mViewModel; // To get and update places's queues map
    private Map<String, Counter> mQueueCountersMap; // A counters map for a specific queue
    private Map<String, Queue> mPlaceQueuesMap; // A Queues map to update view model's queues
    private Object[] mCountersMapKeys; // To get specific queue easily from queue map without the need to loop
    private Queue mCurrentQueue; // this queue that has the adapter's counters

    private Fragment mFragment;

    private ItemClickListener itemClickListener;

    public AddCounterAdapter(Map<String, Counter> counter, Queue queue, Fragment fragment, ItemClickListener itemClickListener) {
        this.mQueueCountersMap = counter;
        this.itemClickListener = itemClickListener;
        this.mFragment = fragment; // To use it as observer
        this.mCurrentQueue = queue;

        // initiate active fragment's view model
        mViewModel = new ViewModelProvider(mFragment).get(AddPlaceViewModel.class);

        // get a Map for queues inside the place
        mPlaceQueuesMap = mViewModel.getPlace().getQueues();

        mCountersMapKeys = mQueueCountersMap.keySet().toArray(); // To get counter from hash mao by it's index
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        mBinding = EditCounterItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(mBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        final Counter counterItem = mQueueCountersMap.get(mCountersMapKeys[position]); // get shown counter item from the counter's map
        Log.d(TAG, "onSave: counter name= "+counterItem.getName()+ " counter gender= "
                + counterItem.getGender()+ " counter age= "+ counterItem.getAge()+ " counter disability= "+ counterItem.getDisability());

        if (holder instanceof ViewHolder) {
            final ViewHolder counterHolder = (ViewHolder) holder;
            if (counterItem != null) {

                // Don't display more than 30 character
                String shortenString = counterItem.getName().substring(0, Math.min(counterItem.getName().length(), 30));
                counterHolder.mBinding.counterName.setText(shortenString); // set the name of counter's item
                //counterHolder.mBinding.counterName.setMaxWidth(700);

                /*counterHolder.mBinding.valueText.setText(counterItem.getName());

                // set selected value in gender spinner
                if(null != counterItem.getGender()) {
                    switch (counterItem.getGender()) { // display sorting option selected from shared preference
                        case "Any":
                            counterHolder.mBinding.spinnerGenderValue.setSelection(0);
                            Log.d(TAG, "display 0 option on sorting spinner");
                            break;
                        case "Females":
                            counterHolder.mBinding.spinnerGenderValue.setSelection(1);
                            Log.d(TAG, "display 1 option on sorting spinner");
                            break;
                        case "Males":
                            counterHolder.mBinding.spinnerGenderValue.setSelection(2);
                            Log.d(TAG, "display 2 option on sorting spinner");
                            break;
                        case "Both":
                            counterHolder.mBinding.spinnerGenderValue.setSelection(3);
                            Log.d(TAG, "display 3 option on sorting spinner");
                            break;
                    }
                }

                // set on item selected listener
                counterHolder.mBinding.spinnerGenderValue.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        // your code here for onItemSelected
                        switch (position) { // display sorting option selected from shared preference
                            case 0:
                                counterItem.setGender("Any"); // Update current counter gender value
                                mQueueCountersMap.put(counterItem.getKey(), counterItem); // update counters map by the updated current counter
                                mCurrentQueue.setCounters(mQueueCountersMap); // Update current queue by the updated counters map
                                mPlaceQueuesMap.put(mCurrentQueue.getKey(), mCurrentQueue); // update queue map by the updated queue
                                mViewModel.getPlace().setQueues(mPlaceQueuesMap); // update the view model
                                Log.d(TAG, "spinner item 0 is selected= ");
                                break;
                            case 1:
                                counterItem.setGender("Females"); // Update current counter gender value
                                mQueueCountersMap.put(counterItem.getKey(), counterItem); // update counters map by the updated current counter
                                mCurrentQueue.setCounters(mQueueCountersMap); // Update current queue by the updated counters map
                                mPlaceQueuesMap.put(mCurrentQueue.getKey(), mCurrentQueue); // update queue map by the updated queue
                                mViewModel.getPlace().setQueues(mPlaceQueuesMap); // update the view model
                                Log.d(TAG, "spinner item 1 is selected= ");
                                break;
                            case 2:
                                counterItem.setGender("Males"); // Update current counter gender value
                                mQueueCountersMap.put(counterItem.getKey(), counterItem); // update counters map by the updated current counter
                                mCurrentQueue.setCounters(mQueueCountersMap); // Update current queue by the updated counters map
                                mPlaceQueuesMap.put(mCurrentQueue.getKey(), mCurrentQueue); // update queue map by the updated queue
                                mViewModel.getPlace().setQueues(mPlaceQueuesMap); // update the view model
                                Log.d(TAG, "spinner item 2 is selected= ");
                                break;
                            case 3:
                                counterItem.setGender("Both"); // Update current counter gender value
                                mQueueCountersMap.put(counterItem.getKey(), counterItem); // update counters map by the updated current counter
                                mCurrentQueue.setCounters(mQueueCountersMap); // Update current queue by the updated counters map
                                mPlaceQueuesMap.put(mCurrentQueue.getKey(), mCurrentQueue); // update queue map by the updated queue
                                mViewModel.getPlace().setQueues(mPlaceQueuesMap); // update the view model
                                Log.d(TAG, "spinner item 3 is selected= ");
                                break;
                        }

                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });

                // set selected value in age spinner
                if(null != counterItem.getAge()) {
                    switch (counterItem.getAge()) { // display sorting option selected from shared preference
                        case "Any":
                            counterHolder.mBinding.spinnerAgeValue.setSelection(0);
                            Log.d(TAG, "display 0 option on sorting spinner");
                            break;
                        case "Old":
                            counterHolder.mBinding.spinnerAgeValue.setSelection(1);
                            Log.d(TAG, "display 1 option on sorting spinner");
                            break;
                        case "Young":
                            counterHolder.mBinding.spinnerAgeValue.setSelection(2);
                            Log.d(TAG, "display 2 option on sorting spinner");
                            break;
                        case "Both":
                            counterHolder.mBinding.spinnerAgeValue.setSelection(3);
                            Log.d(TAG, "display 3 option on sorting spinner");
                            break;
                    }
                }

                // set on item selected listener
                counterHolder.mBinding.spinnerAgeValue.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        // your code here for onItemSelected
                        switch (position) { // display sorting option selected from shared preference
                            case 0:
                                counterItem.setAge("Any"); // Update current counter gender value
                                mQueueCountersMap.put(counterItem.getKey(), counterItem); // update counters map by the updated current counter
                                mCurrentQueue.setCounters(mQueueCountersMap); // Update current queue by the updated counters map
                                mPlaceQueuesMap.put(mCurrentQueue.getKey(), mCurrentQueue); // update queue map by the updated queue
                                mViewModel.getPlace().setQueues(mPlaceQueuesMap); // update the view model
                                Log.d(TAG, "spinner item 0 is selected= ");
                                break;
                            case 1:
                                counterItem.setAge("Old"); // Update current counter gender value
                                mQueueCountersMap.put(counterItem.getKey(), counterItem); // update counters map by the updated current counter
                                mCurrentQueue.setCounters(mQueueCountersMap); // Update current queue by the updated counters map
                                mPlaceQueuesMap.put(mCurrentQueue.getKey(), mCurrentQueue); // update queue map by the updated queue
                                mViewModel.getPlace().setQueues(mPlaceQueuesMap); // update the view model
                                Log.d(TAG, "spinner item 1 is selected= ");
                                break;
                            case 2:
                                counterItem.setAge("Young"); // Update current counter gender value
                                mQueueCountersMap.put(counterItem.getKey(), counterItem); // update counters map by the updated current counter
                                mCurrentQueue.setCounters(mQueueCountersMap); // Update current queue by the updated counters map
                                mPlaceQueuesMap.put(mCurrentQueue.getKey(), mCurrentQueue); // update queue map by the updated queue
                                mViewModel.getPlace().setQueues(mPlaceQueuesMap); // update the view model
                                Log.d(TAG, "spinner item 2 is selected= ");
                                break;
                            case 3:
                                counterItem.setAge("Both"); // Update current counter gender value
                                mQueueCountersMap.put(counterItem.getKey(), counterItem); // update counters map by the updated current counter
                                mCurrentQueue.setCounters(mQueueCountersMap); // Update current queue by the updated counters map
                                mPlaceQueuesMap.put(mCurrentQueue.getKey(), mCurrentQueue); // update queue map by the updated queue
                                mViewModel.getPlace().setQueues(mPlaceQueuesMap); // update the view model
                                Log.d(TAG, "spinner item 3 is selected= ");
                                break;
                        }

                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });

                // set selected value in Disability spinner
                if(null != counterItem.getDisability()) {
                    switch (counterItem.getDisability()) { // display sorting option selected from shared preference
                        case "Any":
                            counterHolder.mBinding.spinnerDisabilityValue.setSelection(0);
                            Log.d(TAG, "display 0 option on sorting spinner");
                            break;
                        case "Disabled":
                            counterHolder.mBinding.spinnerDisabilityValue.setSelection(1);
                            Log.d(TAG, "display 1 option on sorting spinner");
                            break;
                        case "Abled":
                            counterHolder.mBinding.spinnerDisabilityValue.setSelection(2);
                            Log.d(TAG, "display 2 option on sorting spinner");
                            break;
                        case "Both":
                            counterHolder.mBinding.spinnerDisabilityValue.setSelection(3);
                            Log.d(TAG, "display 3 option on sorting spinner");
                            break;
                    }
                }

                // set on item selected listener
                counterHolder.mBinding.spinnerDisabilityValue.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        // your code here for onItemSelected
                        switch (position) { // display sorting option selected from shared preference
                            case 0:
                                counterItem.setDisability("Any"); // Update current counter gender value
                                mQueueCountersMap.put(counterItem.getKey(), counterItem); // update counters map by the updated current counter
                                mCurrentQueue.setCounters(mQueueCountersMap); // Update current queue by the updated counters map
                                mPlaceQueuesMap.put(mCurrentQueue.getKey(), mCurrentQueue); // update queue map by the updated queue
                                mViewModel.getPlace().setQueues(mPlaceQueuesMap); // update the view model
                                Log.d(TAG, "spinner item 0 is selected= ");
                                break;
                            case 1:
                                counterItem.setDisability("Disabled"); // Update current counter gender value
                                mQueueCountersMap.put(counterItem.getKey(), counterItem); // update counters map by the updated current counter
                                mCurrentQueue.setCounters(mQueueCountersMap); // Update current queue by the updated counters map
                                mPlaceQueuesMap.put(mCurrentQueue.getKey(), mCurrentQueue); // update queue map by the updated queue
                                mViewModel.getPlace().setQueues(mPlaceQueuesMap); // update the view model
                                Log.d(TAG, "spinner item 1 is selected= ");
                                break;
                            case 2:
                                counterItem.setDisability("Abled"); // Update current counter gender value
                                mQueueCountersMap.put(counterItem.getKey(), counterItem); // update counters map by the updated current counter
                                mCurrentQueue.setCounters(mQueueCountersMap); // Update current queue by the updated counters map
                                mPlaceQueuesMap.put(mCurrentQueue.getKey(), mCurrentQueue); // update queue map by the updated queue
                                mViewModel.getPlace().setQueues(mPlaceQueuesMap); // update the view model
                                Log.d(TAG, "spinner item 2 is selected= ");
                                break;
                            case 3:
                                counterItem.setDisability("Both"); // Update current counter gender value
                                mQueueCountersMap.put(counterItem.getKey(), counterItem); // update counters map by the updated current counter
                                mCurrentQueue.setCounters(mQueueCountersMap); // Update current queue by the updated counters map
                                mPlaceQueuesMap.put(mCurrentQueue.getKey(), mCurrentQueue); // update queue map by the updated queue
                                mViewModel.getPlace().setQueues(mPlaceQueuesMap); // update the view model
                                Log.d(TAG, "spinner item 3 is selected= ");
                                break;
                        }

                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });*/

            }
        }
    }


    @Override
    public int getItemCount() {
        Log.d(TAG, "getItemCount: "+ mQueueCountersMap.size());
        return mQueueCountersMap.size();
    }


    /// ViewHolder for SentMessages list /////
    public class ViewHolder extends RecyclerView.ViewHolder {

        // View binding
        private EditCounterItemBinding mBinding;

        public ViewHolder (EditCounterItemBinding binding) {
            super(binding.getRoot());
            this.mBinding = binding;

            // To delete counter item when close icon is clicked
            mBinding.counterName.setOnCloseIconClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Counter counterItem = mQueueCountersMap.get(mCountersMapKeys[getAdapterPosition()]);
                    mQueueCountersMap.remove(mCountersMapKeys[getAdapterPosition()]);
                    notifyItemRemoved(getAdapterPosition());
                }
            });

            // To open counter item when chip is clicked
            mBinding.counterName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Counter counterItem = mQueueCountersMap.get(mCountersMapKeys[getAdapterPosition()]);
                    //Log.d(TAG, "onClick: counterItem key= "+counterItem.getKey());

                    // Open edit counter dialog
                    CountersDialogFragment dialogFragment = CountersDialogFragment.newInstance(counterItem, getAdapterPosition(), AddCounterAdapter.this);
                    if (mFragment.getParentFragmentManager() != null) {
                        dialogFragment.show(mFragment.getParentFragmentManager(), COUNTER_DIALOG_FRAGMENT_TAG);
                    }
                }
            });

            // To save updated text
            /*mBinding.valueText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable editable) {
                    final Counter counterItem = mQueueCountersMap.get(mCountersMapKeys[getAdapterPosition()]);

                    Log.d(TAG, "Editable Name= "+ editable.toString()+ " position= "+getAdapterPosition() + " title= "+ counterItem.getName());
                    if(TextUtils.isEmpty((String.valueOf(editable).trim()))){
                        // It's empty string, delete existing value
                        counterItem.setName(null); // Update current counter gender value
                        mQueueCountersMap.put(counterItem.getKey(), counterItem);
                        *//*mQueueCountersMap.remove(counterItem.getKey()); // update counters map by the updated current counter
                        mCurrentQueue.setCounters(mQueueCountersMap); // Update current queue by the updated counters map
                        mPlaceQueuesMap.put(mCurrentQueue.getKey(), mCurrentQueue); // update queue map by the updated queue
                        mViewModel.getPlace().setQueues(mPlaceQueuesMap); // update the view model*//*

                        mBinding.valueText.setError(mFragment.getString(R.string.add_place_counter_error));

                    }else{
                        // It's not empty string, save value
                        counterItem.setName(editable.toString()); // Update current counter gender value
                        mQueueCountersMap.put(counterItem.getKey(), counterItem); // update counters map by the updated current counter
                        mCurrentQueue.setCounters(mQueueCountersMap); // Update current queue by the updated counters map
                        mPlaceQueuesMap.put(mCurrentQueue.getKey(), mCurrentQueue); // update queue map by the updated queue
                        mViewModel.getPlace().setQueues(mPlaceQueuesMap); // update the view model


                    }

                }
            });*/
        }

    }

    // When save button is clicked in counter dialog
    @Override
    public void onSave(Counter counter, int position) {
        Log.d(TAG, "onSave: counter name= "+counter.getName()+ " counter gender= "
                + counter.getGender()+ " counter age= "+ counter.getAge()+ " counter disability= "+ counter.getDisability());

        // add temp counter to counters hashMap to generate child recycler view for counters
        mCurrentQueue.getCounters().put(counter.getKey(), counter);

        // Save counter to view model
        //mQueueCountersMap = mCurrentQueue.getCounters(); // get previous counters of this queue
        //mQueueCountersMap.put(counter.getKey(), counter); // update counters map by the updated current counter
        //mCurrentQueue.setCounters(mQueueCountersMap); // Update current queue by the updated counters map
        mPlaceQueuesMap.put(mCurrentQueue.getKey(), mCurrentQueue); // update queue map by the updated queue
        mViewModel.getPlace().setQueues(mPlaceQueuesMap); // update the view model

        // Update child recycler
        notifyItemChanged(position);

    }

}

