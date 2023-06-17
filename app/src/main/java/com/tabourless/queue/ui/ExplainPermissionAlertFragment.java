package com.tabourless.queue.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.tabourless.queue.R;
import com.tabourless.queue.interfaces.ItemClickListener;

public class ExplainPermissionAlertFragment extends DialogFragment {
    private final static String TAG = ExplainPermissionAlertFragment.class.getSimpleName();

    // click listener to pass click events to parent fragment
    private Activity activity;

    private static final int MY_PERMISSION_REQUEST_CODE = 7000;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 7001;
    private static ItemClickListener itemClickListen;

    private ExplainPermissionAlertFragment(Activity activity) {
        // Empty constructor is required for DialogFragment
        // Make sure not to add arguments to the constructor
        // Use `newInstance` instead as shown below
        this.activity = activity;
    }

    public static ExplainPermissionAlertFragment newInstance(Activity activity, ItemClickListener itemClickListener) {

        itemClickListen = itemClickListener;
        ExplainPermissionAlertFragment fragment = new ExplainPermissionAlertFragment(activity);
        Bundle args = new Bundle();
        //args.putParcelableArrayList(PRIVET_CONTACTS_KEY, privateContacts);
        fragment.setArguments(args);
        return fragment;
    }


    // Alert dialog appears after user click block popup menu
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        //CharSequence options[] = new CharSequence[]{getString(R.string.alert_dialog_edit), getString(R.string.alert_dialog_unreveal)};
        // AlertDialog.Builder to create the dialog without custom xml layout
        //AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());
        MaterialAlertDialogBuilder alertDialogBuilder = new MaterialAlertDialogBuilder(activity);
        alertDialogBuilder.setTitle(getString(R.string.access_location));
        alertDialogBuilder.setMessage(R.string.location_access_required);

        // set click listener for Yes button
        alertDialogBuilder.setPositiveButton(R.string.user_confirm_dialog_enable,  new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // on success
                // Request the permission
                //ActivityCompat.requestPermissions(activity, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_REQUEST_CODE);
                if(itemClickListen != null){
                    // trigger click event when yes is selected
                    itemClickListen.onClick(null, 6, false);
                }
            }
        }).setNegativeButton(R.string.user_confirm_dialog_negative, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // on cancel
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        return alertDialogBuilder.create();
    }

}
