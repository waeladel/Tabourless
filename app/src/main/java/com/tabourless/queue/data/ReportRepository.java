package com.tabourless.queue.data;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.tabourless.queue.models.Message;
import com.tabourless.queue.models.User;

import java.util.HashMap;
import java.util.Map;

public class ReportRepository {

    private final static String TAG = ReportRepository.class.getSimpleName();

    // [START declare_database_ref]
    private DatabaseReference mDatabaseRef, mReportsRef;

    public ReportRepository(){
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        mReportsRef = mDatabaseRef.child("reports");
    }

    public void sendReport(String userID, String currentUserID, User user, User currentUser, String issue) {
        Map<String, Object> childUpdates = new HashMap<>();

        // create a map for reported user to add to childUpdates
        Map<String, Object> reportedUserUpdates = new HashMap<>();
        reportedUserUpdates.put("name", user.getName());
        reportedUserUpdates.put("avatar", user.getAvatar());
        reportedUserUpdates.put("coverImage", user.getCoverImage());
        reportedUserUpdates.put("gender", user.getGender());
        reportedUserUpdates.put("created", user.getCreated());


        // create a map for reporter user to add to childUpdates
        Map<String, Object> reporterUserUpdates = new HashMap<>();
        reporterUserUpdates.put("name", currentUser.getName());
        reporterUserUpdates.put("avatar", currentUser.getAvatar());
        reporterUserUpdates.put("gender", currentUser.getGender());
        reporterUserUpdates.put("created", currentUser.getCreated());
        reporterUserUpdates.put("issue", issue);
        reporterUserUpdates.put("reportTime", ServerValue.TIMESTAMP);

        //map reported user to hash to we can add it to childUpdates under reported profile nod
        Map<String, Object> reportedProfileUpdates = user.toMap();
        reportedProfileUpdates.put("issue", issue);
        reportedProfileUpdates.put("reportTime", ServerValue.TIMESTAMP);

        childUpdates.put("/reported/" + userID , reportedUserUpdates);
        childUpdates.put("/reporters/" + userID + "/" + currentUserID, reporterUserUpdates);
        childUpdates.put("/content/" + userID + "/" + currentUserID + "/profile", reportedProfileUpdates);

        mReportsRef.updateChildren(childUpdates).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // Write was successful!
                Log.i(TAG, "report sent onSuccess");
                // ...
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "report sent onFailure: "+ e);
            }
        });
    }

    // For reporting messages
    public void sendReport(String userID, String currentUserID, User user, User currentUser, String issue, Message message) {
        Map<String, Object> childUpdates = new HashMap<>();

        // create a map for reported user to add to childUpdates
        Map<String, Object> reportedUserUpdates = new HashMap<>();
        reportedUserUpdates.put("name", user.getName());
        reportedUserUpdates.put("avatar", user.getAvatar());
        reportedUserUpdates.put("coverImage", user.getCoverImage());
        reportedUserUpdates.put("gender", user.getGender());
        reportedUserUpdates.put("created", user.getCreated());


        // create a map for reporter user to add to childUpdates
        Map<String, Object> reporterUserUpdates = new HashMap<>();
        reporterUserUpdates.put("name", currentUser.getName());
        reporterUserUpdates.put("avatar", currentUser.getAvatar());
        reporterUserUpdates.put("gender", currentUser.getGender());
        reporterUserUpdates.put("created", currentUser.getCreated());
        reporterUserUpdates.put("issue", issue);
        reporterUserUpdates.put("reportTime", ServerValue.TIMESTAMP);

        //map reported user to hash to we can add it to childUpdates under reported profile nod
        Map<String, Object> reportedMessageUpdates = message.toMap();
        reportedMessageUpdates.put("issue", issue);
        reportedMessageUpdates.put("reportTime", ServerValue.TIMESTAMP);

        childUpdates.put("/reported/" + userID , reportedUserUpdates);
        childUpdates.put("/reporters/" + userID + "/" + currentUserID, reporterUserUpdates);
        childUpdates.put("/content/" + message.getSenderId() + "/" + currentUserID + "/messages/" + message.getKey(), reportedMessageUpdates);

        mReportsRef.updateChildren(childUpdates).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // Write was successful!
                Log.i(TAG, "report sent onSuccess");
                // ...
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "report sent onFailure: "+ e);
            }
        });
    }

}

