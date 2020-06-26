package com.tabourless.queue.interfaces;

import com.tabourless.queue.models.UserQueue;

public interface FirebaseUserQueueCallback {
    void onCallback(UserQueue userQueue);
}
