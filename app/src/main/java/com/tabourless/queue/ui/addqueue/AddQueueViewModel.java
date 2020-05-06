package com.tabourless.queue.ui.addqueue;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class AddQueueViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public AddQueueViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is home fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}