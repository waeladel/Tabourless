package com.tabourless.queue.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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

import static com.tabourless.queue.App.COUNTER_SPINNER_ANY;
import static com.tabourless.queue.App.COUNTER_SPINNER_BOTH;
import static com.tabourless.queue.App.COUNTER_SPINNER_DISABILITY_ABLED;
import static com.tabourless.queue.App.COUNTER_SPINNER_DISABILITY_DISABLED;
import static com.tabourless.queue.App.COUNTER_SPINNER_GENDER_FEMALE;
import static com.tabourless.queue.App.COUNTER_SPINNER_GENDER_MALE;
import static com.tabourless.queue.App.COUNTER_SPINNER_AGE_OLD;
import static com.tabourless.queue.App.COUNTER_SPINNER_AGE_YOUNG;

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
            getDialog().setTitle(R.string.title_add_place_add_counter);
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
        Log.d(TAG, "onCreateView: counter key ="+ sCounter.getKey());
        if(sCounter != null && sCounter.getKey() != null){
            mBinding.nameValue.setText(sCounter.getName());

            // If counter is open, switch must be checked
            if(sCounter.isOpen()){
                mBinding.openValueSwitch.setChecked(true);
            }else{
                mBinding.openValueSwitch.setChecked(false);
            }

            // set selected value in gender spinner
            if(null != sCounter.getGender()) {
                switch (sCounter.getGender()) { // display sorting option selected from shared preference
                    case COUNTER_SPINNER_ANY:
                        mBinding.spinnerGenderValue.setSelection(0);
                        Log.d(TAG, "display 0 option on sorting spinner");
                        break;
                    case COUNTER_SPINNER_GENDER_FEMALE:
                        mBinding.spinnerGenderValue.setSelection(1);
                        Log.d(TAG, "display 1 option on sorting spinner");
                        break;
                    case COUNTER_SPINNER_GENDER_MALE:
                        mBinding.spinnerGenderValue.setSelection(2);
                        Log.d(TAG, "display 2 option on sorting spinner");
                        break;
                    case COUNTER_SPINNER_BOTH:
                        mBinding.spinnerGenderValue.setSelection(3);
                        Log.d(TAG, "display 3 option on sorting spinner");
                        break;
                }
            }

            // set selected value in age spinner
            if(null != sCounter.getAge()) {
                switch (sCounter.getAge()) { // display sorting option selected from shared preference
                    case COUNTER_SPINNER_ANY:
                        mBinding.spinnerAgeValue.setSelection(0);
                        Log.d(TAG, "display 0 option on sorting spinner");
                        break;
                    case COUNTER_SPINNER_AGE_OLD:
                        mBinding.spinnerAgeValue.setSelection(1);
                        Log.d(TAG, "display 1 option on sorting spinner");
                        break;
                    case COUNTER_SPINNER_AGE_YOUNG:
                        mBinding.spinnerAgeValue.setSelection(2);
                        Log.d(TAG, "display 2 option on sorting spinner");
                        break;
                    case COUNTER_SPINNER_BOTH:
                        mBinding.spinnerAgeValue.setSelection(3);
                        Log.d(TAG, "display 3 option on sorting spinner");
                        break;
                }
            }

            // set selected value in Disability spinner
            if(null != sCounter.getDisability()) {
                switch (sCounter.getDisability()) { // display sorting option selected from shared preference
                    case COUNTER_SPINNER_ANY:
                        mBinding.spinnerDisabilityValue.setSelection(0);
                        Log.d(TAG, "display 0 option on sorting spinner");
                        break;
                    case COUNTER_SPINNER_DISABILITY_DISABLED:
                        mBinding.spinnerDisabilityValue.setSelection(1);
                        Log.d(TAG, "display 1 option on sorting spinner");
                        break;
                    case COUNTER_SPINNER_DISABILITY_ABLED:
                        mBinding.spinnerDisabilityValue.setSelection(2);
                        Log.d(TAG, "display 2 option on sorting spinner");
                        break;
                    case COUNTER_SPINNER_BOTH:
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
                    if(TextUtils.isEmpty(String.valueOf(mBinding.nameValue.getText()))){
                        Toast.makeText(mContext, R.string.add_place_counter_error, Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Get values from dialog then set counter
                    sCounter.setName(String.valueOf(mBinding.nameValue.getText()));

                    // Get the value of open switch
                    if(mBinding.openValueSwitch.isChecked()){
                        sCounter.setOpen(true);
                    }else{
                        sCounter.setOpen(false);
                    }

                    // set gender value
                    switch (mBinding.spinnerGenderValue.getSelectedItemPosition()){
                        case 0:
                            sCounter.setGender(COUNTER_SPINNER_ANY); // Update current counter gender value
                            break;
                        case 1:
                            sCounter.setGender(COUNTER_SPINNER_GENDER_FEMALE); // Update current counter gender value
                            break;
                        case 2:
                            sCounter.setGender(COUNTER_SPINNER_GENDER_MALE); // Update current counter gender value
                            break;
                        case 3:
                            sCounter.setGender(COUNTER_SPINNER_BOTH); // Update current counter gender value
                            break;
                    }
                    Log.d(TAG, "onClick: selected gender= "+ mBinding.spinnerGenderValue.getSelectedItemPosition());

                    // set age value
                    switch (mBinding.spinnerAgeValue.getSelectedItemPosition()){
                        case 0:
                            sCounter.setAge(COUNTER_SPINNER_ANY); // Update current counter gender value
                            break;
                        case 1:
                            sCounter.setAge(COUNTER_SPINNER_AGE_OLD); // Update current counter gender value
                            break;
                        case 2:
                            sCounter.setAge(COUNTER_SPINNER_AGE_YOUNG); // Update current counter gender value
                            break;
                        case 3:
                            sCounter.setAge(COUNTER_SPINNER_BOTH); // Update current counter gender value
                            break;
                    }

                    Log.d(TAG, "onClick: selected age= "+ mBinding.spinnerAgeValue.getSelectedItemPosition());

                    // set disability value
                    switch (mBinding.spinnerDisabilityValue.getSelectedItemPosition()){
                        case 0:
                            sCounter.setDisability(COUNTER_SPINNER_ANY); // Update current counter gender value
                            break;
                        case 1:
                            sCounter.setDisability(COUNTER_SPINNER_DISABILITY_DISABLED); // Update current counter gender value
                            break;
                        case 2:
                            sCounter.setDisability(COUNTER_SPINNER_DISABILITY_ABLED); // Update current counter gender value
                            break;
                        case 3:
                            sCounter.setDisability(COUNTER_SPINNER_BOTH); // Update current counter gender value
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

        // listen to counter name value to set error when it's empty
        mBinding.nameValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable editable) {
                if(TextUtils.isEmpty((String.valueOf(editable).trim()))){
                    // It's empty string, set error icon
                    mBinding.nameValue.setError(getString(R.string.add_place_counter_error));
                }
            }
        });

        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NotNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if(null != getDialog()){
            getDialog().setTitle(R.string.title_add_place_add_counter);
        }
    }

    @Override
    public void onResume() {
        // Get existing layout params for the window
        ViewGroup.LayoutParams params = getDialog().getWindow().getAttributes();
        // Assign window properties to fill the parent
        params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        getDialog().getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
        // Call super onResume after sizing
        super.onResume();
    }

}
