package com.tabourless.queue.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.google.firebase.database.annotations.NotNull;
import com.tabourless.queue.R;
import com.tabourless.queue.databinding.AddCounterDialogBinding;
import com.tabourless.queue.interfaces.CounterSaveListener;
import com.tabourless.queue.models.Counter;

import java.util.LinkedHashMap;
import java.util.Map;

public class CountersDialogFragment extends DialogFragment  {
    private final static String TAG = CountersDialogFragment.class.getSimpleName();

    private final static String QUEUE_POSITION_KEY = "queuePosition";
    private int mQueueItemPosition;
    private static Counter sCounter;

    private Context mContext;
    private Activity mActivity;
    private static Map<String, Counter> sCountersMap = new LinkedHashMap<>();
    private static CounterSaveListener sCounterSaveListener;
    private AddCounterDialogBinding mBinding;

    public CountersDialogFragment() {
        // Empty constructor is required for DialogFragment
        // Make sure not to add arguments to the constructor
        // Use `newInstance` instead as shown below
    }

    public static CountersDialogFragment newInstance(Counter counter, int queueItemPosition, CounterSaveListener counterSaveListener) {
        sCounterSaveListener = counterSaveListener; // a listener when save button is clicked inside counter's dialog
        sCounter = counter; // in case it's not new counter, and we need to display it's data

        CountersDialogFragment fragment = new CountersDialogFragment();
        Bundle args = new Bundle();
        args.putInt(QUEUE_POSITION_KEY, queueItemPosition); // position is needed to know which queue needs to update
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setStyle(DialogFragment.STYLE_NORMAL, R.style.colorPickerStyle);
        // this setStyle is VERY important.
        // STYLE_NO_FRAME means that I will provide my own layout and style for the whole dialog
        // so for example the size of the default dialog will not get in my way
        // the style extends the default one. see bellow.
        //mCounter = new Counter(); //we will get counter when calling dialog to make a different between adding/editing counter

        setStyle(STYLE_NORMAL, R.style.DialogMyTheme);

    }

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        mContext = context;
        if (context instanceof Activity){ // check if context is an activity
            mActivity =(Activity) context;
        }
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if(null != getDialog()){
            getDialog().setTitle(R.string.add_place_add_counter_title);
        }

        // get queueItemPosition  from arguments
        if (getArguments() != null) {
            mQueueItemPosition = getArguments().getInt(QUEUE_POSITION_KEY);
        }

        mBinding = AddCounterDialogBinding.inflate(inflater, container, false);

        // R.layout.dialog_color_picker is the custom layout of my dialog
        //WindowManager.LayoutParams windowManagerLayout = getDialog().getWindow().getAttributes();
        //windowManagerLayout.gravity = Gravity.LEFT;

        // if counter key is not null, display data
        if(sCounter != null && sCounter.getKey() != null){
            mBinding.valueText.setText(sCounter.getName());

            // set selected value in gender spinner
            if(null != sCounter.getGender()) {
                switch (sCounter.getGender()) { // display sorting option selected from shared preference
                    case "Any":
                        mBinding.spinnerGenderValue.setSelection(0);
                        Log.d(TAG, "display 0 option on sorting spinner");
                        break;
                    case "Females":
                        mBinding.spinnerGenderValue.setSelection(1);
                        Log.d(TAG, "display 1 option on sorting spinner");
                        break;
                    case "Males":
                        mBinding.spinnerGenderValue.setSelection(2);
                        Log.d(TAG, "display 2 option on sorting spinner");
                        break;
                    case "Both":
                        mBinding.spinnerGenderValue.setSelection(3);
                        Log.d(TAG, "display 3 option on sorting spinner");
                        break;
                }
            }

            // set selected value in age spinner
            if(null != sCounter.getAge()) {
                switch (sCounter.getAge()) { // display sorting option selected from shared preference
                    case "Any":
                        mBinding.spinnerAgeValue.setSelection(0);
                        Log.d(TAG, "display 0 option on sorting spinner");
                        break;
                    case "Old":
                        mBinding.spinnerAgeValue.setSelection(1);
                        Log.d(TAG, "display 1 option on sorting spinner");
                        break;
                    case "Young":
                        mBinding.spinnerAgeValue.setSelection(2);
                        Log.d(TAG, "display 2 option on sorting spinner");
                        break;
                    case "Both":
                        mBinding.spinnerAgeValue.setSelection(3);
                        Log.d(TAG, "display 3 option on sorting spinner");
                        break;
                }
            }

            // set selected value in Disability spinner
            if(null != sCounter.getDisability()) {
                switch (sCounter.getDisability()) { // display sorting option selected from shared preference
                    case "Any":
                        mBinding.spinnerDisabilityValue.setSelection(0);
                        Log.d(TAG, "display 0 option on sorting spinner");
                        break;
                    case "Disabled":
                        mBinding.spinnerDisabilityValue.setSelection(1);
                        Log.d(TAG, "display 1 option on sorting spinner");
                        break;
                    case "Abled":
                        mBinding.spinnerDisabilityValue.setSelection(2);
                        Log.d(TAG, "display 2 option on sorting spinner");
                        break;
                    case "Both":
                        mBinding.spinnerDisabilityValue.setSelection(3);
                        Log.d(TAG, "display 3 option on sorting spinner");
                        break;
                }
            }

        }

        // Get counter's data before saving
        mBinding.saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sCounter != null) {
                    // return if counter name is empty
                    if(TextUtils.isEmpty(String.valueOf(mBinding.valueText.getText()))){
                        Toast.makeText(mContext, R.string.add_place_counter_error, Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Get values from dialog then set counter
                    sCounter.setName(String.valueOf(mBinding.valueText.getText()));

                    // set gender value
                    switch (mBinding.spinnerGenderValue.getSelectedItemPosition()){
                        case 0:
                            sCounter.setGender("Any"); // Update current counter gender value
                            break;
                        case 1:
                            sCounter.setGender("Females"); // Update current counter gender value
                            break;
                        case 2:
                            sCounter.setGender("Males"); // Update current counter gender value
                            break;
                        case 3:
                            sCounter.setGender("Both"); // Update current counter gender value
                            break;
                    }
                    Log.d(TAG, "onClick: selected gender= "+ mBinding.spinnerGenderValue.getSelectedItemPosition());

                    // set age value
                    switch (mBinding.spinnerAgeValue.getSelectedItemPosition()){
                        case 0:
                            sCounter.setAge("Any"); // Update current counter gender value
                            break;
                        case 1:
                            sCounter.setAge("Old"); // Update current counter gender value
                            break;
                        case 2:
                            sCounter.setAge("Young"); // Update current counter gender value
                            break;
                        case 3:
                            sCounter.setAge("Both"); // Update current counter gender value
                            break;
                    }

                    Log.d(TAG, "onClick: selected age= "+ mBinding.spinnerAgeValue.getSelectedItemPosition());

                    // set disability value
                    switch (mBinding.spinnerDisabilityValue.getSelectedItemPosition()){
                        case 0:
                            sCounter.setDisability("Any"); // Update current counter gender value
                            break;
                        case 1:
                            sCounter.setDisability("Disabled"); // Update current counter gender value
                            break;
                        case 2:
                            sCounter.setDisability("Abled"); // Update current counter gender value
                            break;
                        case 3:
                            sCounter.setDisability("Both"); // Update current counter gender value
                            break;
                    }
                    Log.d(TAG, "onClick: selected Disability= "+ mBinding.spinnerDisabilityValue.getSelectedItemPosition());

                    // Notify counter save listener in the fragment to update recycler adapter and view model
                    sCounterSaveListener.onSave(sCounter, mQueueItemPosition);
                    dismiss();
                }
            }
        });


        mBinding.cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NotNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if(null != getDialog()){
            getDialog().setTitle(R.string.add_place_add_counter_title);
        }
    }

    @Override
    public void onResume() {
        // Get existing layout params for the window
        ViewGroup.LayoutParams params = getDialog().getWindow().getAttributes();
        // Assign window properties to fill the parent
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        getDialog().getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
        // Call super onResume after sizing
        super.onResume();
    }

}
