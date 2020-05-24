package com.tabourless.queue.interfaces;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.tabourless.queue.models.User;
// To get OnComplete listeners from database
public interface FirebaseOnCompleteCallback {
    void onCallback(@NonNull Task<Void> task);
}
