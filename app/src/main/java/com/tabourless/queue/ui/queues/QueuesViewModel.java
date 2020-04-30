package com.tabourless.queue.ui.queues;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class QueuesViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public QueuesViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is home fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}