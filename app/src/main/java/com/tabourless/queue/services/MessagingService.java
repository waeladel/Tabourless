package com.tabourless.queue.services;

import android.app.PendingIntent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.navigation.NavDeepLinkBuilder;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.tabourless.queue.App;
import com.tabourless.queue.GlideApp;
import com.tabourless.queue.R;
import static com.tabourless.queue.App.DIRECTION_ARGUMENTS_KEY_CHAT_ID;
import static com.tabourless.queue.App.DIRECTION_ARGUMENTS_KEY_CHAT_USER_ID;
import static com.tabourless.queue.App.DIRECTION_ARGUMENTS_KEY_IS_GROUP;
import static com.tabourless.queue.App.DIRECTION_ARGUMENTS_KEY_PLACE_ID;
import static com.tabourless.queue.App.DIRECTION_ARGUMENTS_KEY_QUEUE_ID;
import static com.tabourless.queue.App.MESSAGES_CHANNEL_ID;
import static com.tabourless.queue.App.NOTIFICATION_TYPE_QUEUE_FRONT;
import static com.tabourless.queue.App.NOTIFICATION_TYPE_QUEUE_NEXT;
import static com.tabourless.queue.App.NOTIFICATION_TYPE_MESSAGE;
import static com.tabourless.queue.App.QUEUES_CHANNEL_ID;

public class MessagingService extends FirebaseMessagingService {

    private final static String TAG = MessagingService.class.getSimpleName();
    private NotificationManagerCompat notificationManager;

    private FirebaseUser mFirebaseCurrentUser;
    private String mCurrentUserId; //get current to get uid

    // [START declare_database_ref]
    private DatabaseReference mDatabaseRef;
    private DatabaseReference mUserRef;
    private StorageReference mStorageRef;

    private static final int QUEUES_NOTIFICATION_ID = 1;
    private static final int MESSAGES_NOTIFICATION_ID = 2;

    private static final String PREFERENCE_KEY_NIGHT = "night" ;
    private static final String PREFERENCE_KEY_RINGTONE = "notification";
    private static final String PREFERENCE_KEY_VERSION = "version";

    private ImageView mAvatarImageView;
    private PendingIntent pendingIntent;
    private Bundle bundle;

    private static final String AVATAR_THUMBNAIL_NAME = "avatar.jpg";
    private static final String COVER_THUMBNAIL_NAME = "cover.jpg";

    private SharedPreferences sharedPreferences;
    private Target mTarget;

    public MessagingService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "MessagingService onCreate");
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        // [START create_storage_reference]
        mStorageRef = FirebaseStorage.getInstance().getReference();
        //Get current logged in user
        mFirebaseCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mCurrentUserId = mFirebaseCurrentUser != null ? mFirebaseCurrentUser.getUid() : null;
        notificationManager = NotificationManagerCompat.from(this);

        mAvatarImageView = new ImageView(this);

        // Save the selected ringtone
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this /* Activity context */);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "onMessageReceived. remoteMessage= " + remoteMessage);

        //String notificationTitle = remoteMessage.getNotification().
        //RemoteMessage.DatabaseNotification notification =  remoteMessage.getNotification();
        Log.d(TAG, "onMessageReceived. remoteMessage From= " + remoteMessage.getFrom());
        Log.d(TAG, "onMessageReceived. remoteMessage MessageId= " + remoteMessage.getMessageId());
        Log.d(TAG, "onMessageReceived. remoteMessage MessageType= " + remoteMessage.getMessageType());
        Log.d(TAG, "onMessageReceived. remoteMessage To= " + remoteMessage.getTo());
        Log.d(TAG, "onMessageReceived. remoteMessage SentTime= " + remoteMessage.getSentTime());
        //Log.d(TAG, "onMessageReceived. remoteMessage ClickAction= " + notification.getClickAction());

        // Get notification data
        //Map<String, String> data = remoteMessage.getData();

        String type = remoteMessage.getData().get("type");
        String senderId = remoteMessage.getData().get("senderId");
        String notificationId = remoteMessage.getData().get("notificationId");
        String destinationId = remoteMessage.getData().get("destinationId");
        String name = remoteMessage.getData().get("name");
        String avatar = remoteMessage.getData().get("avatar");

        String placeId = remoteMessage.getData().get("placeId");
        String queueName = remoteMessage.getData().get("queueName");
        String counterName = remoteMessage.getData().get("counterName");

        Log.d(TAG, "onMessageReceived. remoteMessage type= " + type);
        Log.d(TAG, "onMessageReceived. remoteMessage senderId= " + senderId);
        Log.d(TAG, "onMessageReceived. remoteMessage notificationId= " + notificationId);
        Log.d(TAG, "onMessageReceived. remoteMessage destinationId= " + destinationId);
        Log.d(TAG, "onMessageReceived. remoteMessage name= " + name);
        Log.d(TAG, "onMessageReceived. remoteMessage avatar= " + avatar);
        Log.d(TAG, "onMessageReceived. remoteMessage placeId= " + placeId);
        Log.d(TAG, "onMessageReceived. remoteMessage queueName= " + queueName);
        Log.d(TAG, "onMessageReceived. remoteMessage counterName= " + counterName);

        bundle = new Bundle();

        if (remoteMessage.getData().size()>0 && type != null) {
            String messageTitle;
            String messageBody;
            switch (type){
                case NOTIFICATION_TYPE_QUEUE_FRONT:
                    Log.d(TAG, "Notification type= = " + NOTIFICATION_TYPE_QUEUE_FRONT);
                    if(TextUtils.isEmpty(queueName)){
                        messageTitle = getString(R.string.notification_default_queue_title);
                    }else{
                        messageTitle = getString(R.string.notification_queue_title, queueName);
                    }

                    if(TextUtils.isEmpty(counterName)){
                        messageBody = getString(R.string.notification_default_queue_body_front);
                    }else{
                        messageBody = getString(R.string.notification_queue_body_front, counterName);
                    }

                    bundle.clear();
                    bundle.putString(DIRECTION_ARGUMENTS_KEY_PLACE_ID, placeId);
                    bundle.putString(DIRECTION_ARGUMENTS_KEY_QUEUE_ID, destinationId);
                    pendingIntent = new NavDeepLinkBuilder(this)
                            .setGraph(R.navigation.mobile_navigation)
                            .setDestination(R.id.customers)
                            .setArguments(bundle)
                            .createPendingIntent();

                    sendNotification(messageTitle, messageBody, avatar, type ,notificationId, pendingIntent, QUEUES_CHANNEL_ID);
                    break;
                case NOTIFICATION_TYPE_QUEUE_NEXT:
                    Log.d(TAG, "Notification type= = " + NOTIFICATION_TYPE_QUEUE_NEXT);
                    if(TextUtils.isEmpty(queueName)){
                        messageTitle = getString(R.string.notification_default_queue_title);
                    }else{
                        messageTitle = getString(R.string.notification_queue_title, queueName);
                    }

                    messageBody = getString(R.string.notification_default_queue_body_next);

                    bundle.clear();
                    bundle.putString(DIRECTION_ARGUMENTS_KEY_PLACE_ID, placeId);
                    bundle.putString(DIRECTION_ARGUMENTS_KEY_QUEUE_ID, destinationId);
                    pendingIntent = new NavDeepLinkBuilder(this)
                            .setGraph(R.navigation.mobile_navigation)
                            .setDestination(R.id.customers)
                            .setArguments(bundle)
                            .createPendingIntent();

                    sendNotification(messageTitle, messageBody, avatar, type ,notificationId, pendingIntent, QUEUES_CHANNEL_ID);
                    break;
                case NOTIFICATION_TYPE_MESSAGE:
                    Log.d(TAG, "Notification type = " + NOTIFICATION_TYPE_MESSAGE);
                    messageTitle = getString(R.string.notification_message_title);
                    if(TextUtils.isEmpty(name)){
                        messageBody = getString(R.string.notification_default_message_body);
                    }else{
                        messageBody = getString(R.string.notification_message_body, name);
                    }

                    bundle.clear();
                    bundle.putString(DIRECTION_ARGUMENTS_KEY_CHAT_ID, destinationId);
                    bundle.putString(DIRECTION_ARGUMENTS_KEY_CHAT_USER_ID, senderId);
                    bundle.putBoolean(DIRECTION_ARGUMENTS_KEY_IS_GROUP, false);
                    pendingIntent = new NavDeepLinkBuilder(this)
                            .setGraph(R.navigation.mobile_navigation)
                            .setDestination(R.id.messages)
                            .setArguments(bundle)
                            .createPendingIntent();

                    sendNotification(messageTitle, messageBody, avatar, type, notificationId, pendingIntent, MESSAGES_CHANNEL_ID);
                    break;
            }

        }
    }

    private void sendNotification(String messageTitle, String messageBody, final String avatar, final String type, final String notificationId, PendingIntent pendingIntent, String channelId) {

        // Build notification
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(MessagingService.this, channelId)
                //.setLargeIcon(bitmap)
                //.setSmallIcon(R.mipmap.ic_launcher)
                .setSmallIcon(R.mipmap.ic_notification)
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setContentTitle(messageTitle)
                .setContentText(messageBody)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setSound(getCurrentRingtoneUri());
        }

        // if Avatar is not null or empty, set it as a large icon
        if(!TextUtils.isEmpty(avatar)){
            final Handler uiHandler = new Handler(Looper.getMainLooper());
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    // Call from here
                    mTarget = GlideApp.with(App.getContext())
                            .asBitmap()
                            .load(avatar)
                            .into(new CustomTarget<Bitmap>(100, 100) {
                                @Override
                                public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
                                    builder.setLargeIcon(bitmap); // set avatar as a large icon

                                    // change notification's tag and Id's according to notification's types
                                    if(TextUtils.equals(type, NOTIFICATION_TYPE_MESSAGE)) {
                                        Log.d(TAG, "onBitmapLoaded Notification type = " + NOTIFICATION_TYPE_MESSAGE);
                                        notificationManager.notify(notificationId, MESSAGES_NOTIFICATION_ID, builder.build());
                                    }else {
                                        Log.d(TAG, "onBitmapLoaded Notification type = " + NOTIFICATION_TYPE_QUEUE_FRONT + " or = "+ NOTIFICATION_TYPE_QUEUE_NEXT);
                                        notificationManager.notify(notificationId, QUEUES_NOTIFICATION_ID, builder.build());
                                    }
                                }

                                @Override
                                public void onLoadFailed(@Nullable Drawable errorDrawable) {
                                    super.onLoadFailed(errorDrawable);

                                    // change notification's tag and Id's according to notification's types
                                    if(TextUtils.equals(type, NOTIFICATION_TYPE_MESSAGE)) {
                                        Log.d(TAG, "onBitmapLoaded Notification type = " + NOTIFICATION_TYPE_MESSAGE);
                                        notificationManager.notify(notificationId, MESSAGES_NOTIFICATION_ID, builder.build());
                                    }else {
                                        Log.d(TAG, "onBitmapLoaded Notification type = " + NOTIFICATION_TYPE_QUEUE_FRONT + " or = "+ NOTIFICATION_TYPE_QUEUE_NEXT);
                                        notificationManager.notify(notificationId, QUEUES_NOTIFICATION_ID, builder.build());
                                    }
                                }

                                @Override
                                public void onLoadCleared(@Nullable Drawable placeholder) {
                                    // this is called when imageView is cleared on lifecycle call or for
                                    // some other reason.
                                    // if you are referencing the bitmap somewhere else too other than this imageView
                                    // clear it here as you can no longer have the bitmap
                                }

                            });
                }

            });
        }else{// avatar is null. create a notification with no large icon
            // change notification's tag and Id's according to notification's types
            if(TextUtils.equals(type, NOTIFICATION_TYPE_MESSAGE)) {
                Log.d(TAG, "onBitmapLoaded Notification type = " + NOTIFICATION_TYPE_MESSAGE);
                notificationManager.notify(notificationId, MESSAGES_NOTIFICATION_ID, builder.build());
            }else {
                Log.d(TAG, "onBitmapLoaded Notification type = " + NOTIFICATION_TYPE_QUEUE_FRONT + " or = "+ NOTIFICATION_TYPE_QUEUE_NEXT);
                notificationManager.notify(notificationId, QUEUES_NOTIFICATION_ID, builder.build());
            }
        }


        /*final long ONE_MEGABYTE = 1024 * 1024;
        mStorageRef.child("images/"+senderId+"/"+ AVATAR_THUMBNAIL_NAME).getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                // Data for "images/island.jpg" is returns, use this as needed
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0, bytes.length);
                // Bitmap is loaded, use image here
                NotificationCompat.Builder builder = new NotificationCompat.Builder(MessagingService.this, channelId)
                        .setLargeIcon(bitmap)
                        //.setSmallIcon(R.mipmap.ic_launcher)
                        .setSmallIcon(R.drawable.album_ic_back_white)
                        .setColor(getResources().getColor(R.color.album_ColorPrimary))
                        .setContentTitle(messageTitle)
                        .setContentText(messageBody)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

                //notificationId is a unique int for each notification that you must define
                notificationManager.notify(11, builder.build());

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
            }
        });*/

        // get person avatar bitmap
        /*mTarget = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                // Bitmap is loaded, use image here
                NotificationCompat.Builder builder = new NotificationCompat.Builder(MessagingService.this, channelId)
                        //.setSmallIcon(R.mipmap.ic_launcher)
                        .setSmallIcon(R.drawable.album_ic_back_white)
                        .setLargeIcon(bitmap)
                        .setColor(getResources().getColor(R.color.album_ColorPrimary))
                        .setContentTitle(messageTitle)
                        .setContentText(messageBody)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

            }

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                // Send Notification without a person avatar
                NotificationCompat.Builder builder = new NotificationCompat.Builder(MessagingService.this, channelId)
                        //.setSmallIcon(R.mipmap.ic_launcher)
                        .setSmallIcon(R.drawable.album_ic_back_white)
                        //.setLargeIcon(bitmap)
                        .setColor(getResources().getColor(R.color.album_ColorPrimary))
                        .setContentTitle(messageTitle)
                        .setContentText(messageBody)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

                // notificationId is a unique int for each notification that you must define
                notificationManager.notify(11, builder.build());

            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {

            }

        };

        Picasso.get().load(avatar).into(new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                Log.d(TAG, "onBitmapLoaded. bitmap From= " + from);

            }

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                Log.d(TAG, "onBitmapFailed. error= " + errorDrawable);
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {

            }
        });*/

    }

    // Returns ringtone url from settings property
    @Nullable
    private Uri getCurrentRingtoneUri() {
        return Uri.parse(sharedPreferences.getString(PREFERENCE_KEY_RINGTONE, String.valueOf(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))));
    }


    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);

        if(mCurrentUserId != null){
            //creates a new node of user's token and set its value to true.
            mUserRef = mDatabaseRef.child("users").child(mCurrentUserId);
            mUserRef.child("tokens").child(token).setValue(true);
        }

        /*FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "getInstanceId failed", task.getException());
                            return;
                        }
                        // Get new Instance ID token
                        String token = task.getResult().getToken();

                    }
                });*/
    }

}

