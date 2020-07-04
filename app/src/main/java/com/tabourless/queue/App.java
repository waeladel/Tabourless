package com.tabourless.queue;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDexApplication;
import androidx.preference.PreferenceManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import static androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode;

import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created on 25/03/2017.
 */

public class App extends MultiDexApplication { // had to enable MultiDex after adding chirpsdk:3.9.2

    private final static String TAG = App.class.getSimpleName();
    private static Context sApplicationContext;

    // Since I can connect from multiple devices, we store each connection instance separately
    // any time that connectionsRef's value is null (i.e. has no children) I am offline

    private static FirebaseUser currentFirebaseUser;
    private static String currentUserId;

    public static final String MESSAGES_CHANNEL_ID = "Messages_id";
    public static final String QUEUES_CHANNEL_ID = "Queues_id";

    private static final String PREFERENCE_KEY_NIGHT = "night" ;

    private static final String NIGHT_VALUE_LIGHT = "light";
    private static final String NIGHT_VALUE_DARK = "dark";
    private static final String NIGHT_VALUE_BATTERY = "battery";
    private static final String NIGHT_VALUE_SYSTEM = "system";

    public static final String COUNTER_SPINNER_ANY = "any";
    public static final String COUNTER_SPINNER_BOTH = "both";
    public static final String COUNTER_SPINNER_GENDER_MALE = "male";
    public static final String COUNTER_SPINNER_GENDER_FEMALE = "female";
    public static final String COUNTER_SPINNER_AGE_YOUNG = "young";
    public static final String COUNTER_SPINNER_AGE_OLD = "old";
    public static final String COUNTER_SPINNER_DISABILITY_DISABLED = "disabled";
    public static final String COUNTER_SPINNER_DISABILITY_ABLED = "abled";

    public static final String USER_SPINNER_GENDER_MALE = "male";
    public static final String USER_SPINNER_GENDER_FEMALE = "female";

    public static final String DATABASE_REF_USERS = "users";
    public static final String DATABASE_REF_USER_AVATAR = "avatar";
    public static final String DATABASE_REF_USER_COVER = "coverImage";
    public static final String DATABASE_REF_USER_LAST_ONLINE = "lastOnline";
    public static final String DATABASE_REF_USER_QUEUES = "userQueues";
    public static final String DATABASE_REF_USER_CHATS = "userChats";
    public static final String DATABASE_REF_CHAT_LAST_SENT = "lastSent";
    public static final String DATABASE_REF_PLACES = "places";
    public static final String DATABASE_REF_NOTIFICATIONS = "notifications";
    public static final String DATABASE_REF_MESSAGES = "messages";
    public static final String DATABASE_REF_MESSAGE_STATUS = "status";
    public static final String DATABASE_REF_CUSTOMERS = "customers";
    public static final String DATABASE_REF_CUSTOMER_USER_ID = "userId";
    public static final String DATABASE_REF_QUEUE_JOINED = "joined";
    public static final String DATABASE_REF_CHATS = "chats";
    public static final String DATABASE_REF_CHAT_ACTIVE = "active";
    public static final String DATABASE_REF_CHATS_MEMBERS = "members";
    public static final String DATABASE_REF_CHATS_MEMBER_READ = "read";
    public static final String DATABASE_REF_COUNTERS = "counters";
    public static final String DATABASE_REF_QUEUES = "queues";
    public static final String DATABASE_REF_NOTIFICATIONS_ALERTS = "alerts";
    public static final String DATABASE_REF_NOTIFICATIONS_MESSAGES = "messages";
    public static final String DATABASE_REF_NOTIFICATIONS_SEEN = "seen";
    public static final String DATABASE_REF_USER_TOKENS = "tokens";
    public static final String DATABASE_REF_RELATIONS= "relations";
    public static final String DATABASE_REF_RELATION_STATUS= "status";


    public static final String STORAGE_REF_IMAGES = "images";
    public static final String AVATAR_THUMBNAIL_NAME = "avatar.jpg";
    public static final String COVER_THUMBNAIL_NAME = "cover.jpg";
    public static final String AVATAR_ORIGINAL_NAME = "original_avatar.jpg";
    public static final String COVER_ORIGINAL_NAME = "original_cover.jpg";

    public static final String DIRECTION_ARGUMENTS_KEY_POINT = "point";
    public static final String DIRECTION_ARGUMENTS_KEY_PLACE_KEY = "placeKey";
    public static final String DIRECTION_ARGUMENTS_KEY_IS_EDIT = "isEdit";
    public static final String DIRECTION_ARGUMENTS_KEY_PLACE_ID = "placeId";
    public static final String DIRECTION_ARGUMENTS_KEY_QUEUE_ID = "queueId";
    public static final String DIRECTION_ARGUMENTS_KEY_CHAT_ID = "chatId";
    public static final String DIRECTION_ARGUMENTS_KEY_CHAT_USER_ID = "chatUserId";
    public static final String DIRECTION_ARGUMENTS_KEY_IS_GROUP = "isGroup";
    public static final String DIRECTION_ARGUMENTS_KEY_USER_ID = "userId";
    public static final String DIRECTION_ARGUMENTS_KEY_IMAGE_NAME = "imageName";


    public static final String CUSTOMER_STATUS_WAITING = "waiting";
    public static final String CUSTOMER_STATUS_NEXT = "next";
    public static final String CUSTOMER_STATUS_FRONT = "front";
    public static final String CUSTOMER_STATUS_AWAY = "away";

    public static final String NOTIFICATION_TYPE_MESSAGE = "message";
    public static final String NOTIFICATION_TYPE_QUEUE_FRONT = "front"; // to inform the user about his or her position
    public static final String NOTIFICATION_TYPE_QUEUE_NEXT= "next"; // to inform the user about his or her position

    public static final String Message_STATUS_SENDING = "sending";
    public static final String Message_STATUS_SENT = "sent";
    public static final String Message_STATUS_DELIVERED = "delivered";

    public static final String RELATION_STATUS_NOT_FRIEND = "notFriend";
    public static final String RELATION_STATUS_BLOCKING = "blocking"; // the selected user is blocking me (current user)
    public static final String RELATION_STATUS_BLOCKED= "blocked"; // the selected user is blocked by me (current user)


    private SharedPreferences sharedPreferences;
    private AudioAttributes audioAttributes;

    public boolean isInForeground = false; // to check if app is open or not from alarm broadcast receiver

    @Override
    public void onCreate() {
        super.onCreate();

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        // Activate the saved theme in preferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this /* Activity context */);
        String darkModeValue = sharedPreferences.getString(PREFERENCE_KEY_NIGHT, "");

        Log.d(TAG, "darkMode = "+darkModeValue);

        switch (darkModeValue){
            case NIGHT_VALUE_LIGHT:
                setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case NIGHT_VALUE_DARK:
                setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case NIGHT_VALUE_BATTERY:
                setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
                break;
            case NIGHT_VALUE_SYSTEM:
                setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;

                default:
                    Log.i(TAG, "darkMode Value is not set yet");
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // Set the default value to FOLLOW_SYSTEM because it's API 29 and above
                        setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    }else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                        // Set the default value to AUTO_BATTERY because we are below api 29
                        setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
                    }else{
                        // Set the default value to NIGHT_NO because
                        // "system default" and "Battery Saver" not supported on api below 21
                        setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    }
                    break;
        }



        sApplicationContext = getApplicationContext();
        Log.i(TAG, "Application class onCreate");
        // Initialize the SDK before executing any other operations, observe how frequently users activate your app,
        // how much time they spend using it, and view other demographic information through Facebook Analytics for Apps.
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);

        // [START Firebase Database enable persistence]
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        // [END rtdb_enable_persistence]
        currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = currentFirebaseUser != null ? currentFirebaseUser.getUid() : null;

        //Only use if you need to know the key hash for facebook
        //printHashKey(getBaseContext());

        createNotificationsChannels();
    }

    public static Context getContext() {
        return sApplicationContext;
        //return instance.getApplicationContext();
    }

    public static String getCurrentUserId() {
        Log.i(TAG, "Application currentUserId= "+currentUserId);
        return currentUserId;
        //return instance.getApplicationContext();
    }

     /*public static void printHashKey(Context pContext) {
        try {
            PackageInfo info = pContext.getPackageManager().getPackageInfo(pContext.getPackageName(), PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                String hashKey = new String(Base64.encode(md.digest(), 0));
                Log.i(TAG, "printHashKey() Hash Key: " + hashKey);
            }
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "printHashKey()", e);
        } catch (Exception e) {
            Log.e(TAG, "printHashKey()", e);
        }
    }*/

    private void createNotificationsChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library

            // Create audioAttributes for notification's sound
            audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .build();

            NotificationChannel QueuesChannel = new NotificationChannel(
                    QUEUES_CHANNEL_ID,
                    getString(R.string.queue_notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
            );
            QueuesChannel.setDescription(getString(R.string.queue_notification_channel_description));
            //QueuesChannel.setSound(Uri.parse("android.resource://" + this.getPackageName() +"/raw/basbes"), audioAttributes);
            //LikesChannel.setSound(Uri.parse("content://media/internal/audio/media/23"), audioAttributes);
            /*if(mOutputFile.exists()){
                LikesChannel.setSound(Uri.parse(mOutputFile.getPath()), audioAttributes);
            }*/

            NotificationChannel MessagesChannel = new NotificationChannel(
                    MESSAGES_CHANNEL_ID,
                    getString(R.string.messages_notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            MessagesChannel.setDescription(getString(R.string.messages_notification_channel_description));
            //MessagesChannel.setSound(Uri.parse("android.resource://" + this.getPackageName() +"/raw/basbes"), audioAttributes);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(QueuesChannel);
                manager.createNotificationChannel(MessagesChannel);
            }
        }
    }
}

