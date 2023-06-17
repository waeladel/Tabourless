/****************************************************************************
 * Copyright 2010 kraigs.android@gmail.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ****************************************************************************/

package com.tabourless.queue.Utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.tabourless.queue.R;


/**
 * This container is to have one place that contains check permissions logic.
 */
public final class CheckPermissions {

    private static String TAG = CheckPermissions.class.getSimpleName();

    public static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 236;


    public static void checkNotificationPermission(Context context) {
        // Starting from Api 33 we must grant post notification permission at run time to be able to send notifications
        // lets check if permission is granted or if the user had disabled notifications for this app
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            if(!CheckPermissions.isNotificationPermissionGranted(context) || !CheckPermissions.isNotificationEnabled(context)){
                Log.d(TAG, "Notification permission is not granted or notification is disabled");
                CheckPermissions.requestNotificationPermission(context);
            }
        }
    }

    public static boolean isNotificationGrantedEnabled(Context context) {
        // Starting from Api 33 we must grant post notification permission at run time to be able to send notifications
        // lets check if permission is granted or if the user had disabled notifications for this app
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !CheckPermissions.isNotificationPermissionGranted(context)){
            return false;
        }else return CheckPermissions.isNotificationEnabled(context);
    }

    // Check If posting notifications is granted or not before creating new alarm because user won't be able to dismiss alarms if he can't receive notifications
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public static boolean isNotificationPermissionGranted(Context context) {
        Log.d(TAG, "is permission Granted= "+(ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED));
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isNotificationEnabled(Context context) {
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        notificationManagerCompat.areNotificationsEnabled();
        return notificationManagerCompat.areNotificationsEnabled();
    }

    public static void requestNotificationPermission(Context context) {
        // Should we show an explanation?
        if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.POST_NOTIFICATIONS)) {
            Log.i(TAG, "requestPermission: permission should show Rationale");
            // Show an explanation to the user *asynchronously* -- don't block
            // this thread waiting for the user's response! After the user
            // sees the explanation, try again to request the permission.
            showNotificationRationaleDialog(context);
        } else {
            // No explanation needed; request the permission
            Log.i(TAG, "requestPermission: No explanation needed; request the permission");
            // Use requestPermissions(new String[] to receive the result on fragment instead of the activity
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);}

    }

    //Show a dialog to select whether to edit or un-reveal
    public static void showNotificationRationaleDialog(Context activity) {

        Log.d(TAG, "showNotificationRationaleDialog: ");
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(activity);

        dialogBuilder
                .setTitle(R.string.allow_notification_title)
                .setMessage(R.string.allow_notification_message)
                .setPositiveButton(R.string.confirm_dialog_positive_button,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    // request the permission
                                    ActivityCompat.requestPermissions((Activity) activity, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
                                }
                            }
                        })

                .setNegativeButton(R.string.user_confirm_dialog_negative,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

        dialogBuilder.create().show();
    }

    //Show a dialog to take the user to the app section in android settings to enable notifications
    public static void showNotificationSettingsDialog(Context context) {

        Log.d(TAG, "showNotificationSettingsDialog: ");
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);

        dialogBuilder
                .setTitle(R.string.allow_notification_title)
                .setMessage(R.string.notification_settings_message)
                .setPositiveButton(R.string.confirm_dialog_positive_button,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    // intent to notifications settings
                                    Intent intent = new Intent();
                                    intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.putExtra("android.provider.extra.APP_PACKAGE", context.getPackageName());
                                    context.startActivity(intent);
                                }
                            }
                        })

                .setNegativeButton(R.string.user_confirm_dialog_negative,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();

    }

}
