package com.tabourless.queue.interfaces;


import com.tabourless.queue.models.Message;

// A call back triggered when database sends the required message result
public interface FirebaseMessageCallback {
    void onCallback(Message message);
}
