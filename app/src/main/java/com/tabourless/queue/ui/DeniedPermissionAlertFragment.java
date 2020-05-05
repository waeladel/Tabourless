package com.tabourless.queue.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.tabourless.queue.R;

public class DeniedPermissionAlertFragment extends DialogFragment {
    private final static String TAG = DeniedPermissionAlertFragment.class.getSimpleName();

    // click listener to pass click events to parent fragment
    private Activity activity;

    private static final int MY_PERMISSION_REQUEST_CODE = 7000;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 7001;

    private DeniedPermissionAlertFragment(Activity activity) {
        // Empty constructor is required for DialogFragment
        // Make sure not to add arguments to the constructor
        // Use `newInstance` instead as shown below
        this.activity = activity;
    }

    public static DeniedPermissionAlertFragment newInstance(Activity activity) {

        DeniedPermissionAlertFragment fragment = new DeniedPermissionAlertFragment(activity);
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
        alertDialogBuilder.setTitle(getString(R.string.permission_not_granted_title));
        alertDialogBuilder.setMessage(R.string.permission_not_granted);

        // set click listener for Yes button
        alertDialogBuilder.setPositiveButton(R.string.confirm_dialog_positive_button,  new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // on success
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        return alertDialogBuilder.create();
    }

}
