package com.tabourless.queue;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDexApplication;
import androidx.preference.PreferenceManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import static androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode;

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

    private static final String PREFERENCE_KEY_NIGHT = "night" ;

    private static final String NIGHT_VALUE_LIGHT = "light";
    private static final String NIGHT_VALUE_DARK = "dark";
    private static final String NIGHT_VALUE_BATTERY = "battery";
    private static final String NIGHT_VALUE_SYSTEM = "system";

    private SharedPreferences sharedPreferences;

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
        // Initialize the SDK before executing any other operations,
        //FacebookSdk.sdkInitialize(sApplicationContext);

        // [START Firebase Database enable persistence]
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        // [END rtdb_enable_persistence]
        currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = currentFirebaseUser != null ? currentFirebaseUser.getUid() : null;
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
}

