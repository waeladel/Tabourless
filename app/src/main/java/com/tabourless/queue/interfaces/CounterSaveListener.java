package com.tabourless.queue.interfaces;

import android.view.View;

import com.tabourless.queue.models.Counter;
// Listen to save counter click on counter dialog
public interface CounterSaveListener {
    void onSave(Counter counter, int position);
}
