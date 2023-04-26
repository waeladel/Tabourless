package com.tabourless.queue.ui.report;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.tabourless.queue.R;
import com.tabourless.queue.models.Message;
import com.tabourless.queue.models.User;

import org.jetbrains.annotations.NotNull;

public class ReportFragment extends DialogFragment {
    private final static String TAG = ReportFragment.class.getSimpleName();

    private Button mSendButton, mCancelButton;

    private RadioGroup mRadioButtonsGroup;

    private ReportViewModel mViewModel;

    private static User sUser;
    private static User sCurrentUser;

    private static Message sMessage;


    private String mUserId, mCurrentUserId;

    private TextView mTitle;

    private static final String USER_ID_ARGS_KEY = "UserId";
    private static final String CURRENT_USER_ID_ARGS_KEY = "CurrentUserId";

    private static final String REPORTED_ISSUE_TYPE_HARASSMENT = "Harassment";
    private static final String REPORTED_ISSUE_TYPE_SUICIDE = "Suicide";
    private static final String REPORTED_ISSUE_TYPE_INAPPROPRIATE = "Inappropriate";
    private static final String REPORTED_ISSUE_TYPE_IMPERSONATE = "Impersonate";
    private static final String REPORTED_ISSUE_TYPE_HATE = "Hate";
    private static final String REPORTED_ISSUE_TYPE_CHILD_ABUSE = "ChildAbuse";
    private static final String REPORTED_ISSUE_TYPE_MISINFORMATION = "Misinformation";
    private static final String REPORTED_ISSUE_TYPE_VIOLENCE = "Violence";
    private static final String REPORTED_ISSUE_TYPE_TERRORISM = "Terrorism";
    private static final String REPORTED_ISSUE_TYPE_SCAMS = "Scams";
    private static final String REPORTED_ISSUE_TYPE_SPAM = "Spam";
    private static final String REPORTED_ISSUE_TYPE_OTHER = "Other";

    private String mIssueType;

    public ReportFragment() {
        // Empty constructor is required for DialogFragment
        // Make sure not to add arguments to the constructor
        // Use `newInstance` instead as shown below
    }

    public static ReportFragment newInstance(String userId, String currentUserId, User user, User currentUser) {

        ReportFragment fragment = new ReportFragment();
        Bundle args = new Bundle();
        args.putString(USER_ID_ARGS_KEY, userId); // Send relationStatus as arguments, it's important for the dialog title
        args.putString(CURRENT_USER_ID_ARGS_KEY, currentUserId); // Send relationStatus as arguments, it's important for the dialog title
        fragment.setArguments(args);
        sUser = user;
        sCurrentUser = currentUser;
        return fragment;
    }

    public static ReportFragment newInstance(String userId, String currentUserId, User user, User currentUser, Message message) {
        ReportFragment fragment = new ReportFragment();
        Bundle args = new Bundle();
        args.putString(USER_ID_ARGS_KEY, userId); // Send relationStatus as arguments, it's important for the dialog title
        args.putString(CURRENT_USER_ID_ARGS_KEY, currentUserId); // Send relationStatus as arguments, it's important for the dialog title
        fragment.setArguments(args);
        sUser = user;
        sCurrentUser = currentUser;
        sMessage = message;
        //sCurrentUser = currentUser;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setStyle(DialogFragment.STYLE_NORMAL, R.style.colorPickerStyle);
        // this setStyle is VERY important.
        // STYLE_NO_FRAME means that I will provide my own layout and style for the whole dialog
        // so for example the size of the default dialog will not get in my way
        // the style extends the default one. see bellow.
        setStyle(STYLE_NO_TITLE, R.style.DialogMyTheme);
        // Use a viewModel to preserve the new created contacts, We don't want to use the argument contacts because it changes the original contacts in ProfileFragment
        mViewModel = new ViewModelProvider(this).get(ReportViewModel.class);
    }


    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if(null != getDialog()){
            getDialog().setTitle(R.string.report_dialog_title);
        }

        View fragView = inflater.inflate(R.layout.report_dialog_fragment, container);

        mSendButton =  fragView.findViewById(R.id.send_button);
        mCancelButton =  fragView.findViewById(R.id.cancel_button);
        mTitle =  fragView.findViewById(R.id.reported_name);

        mRadioButtonsGroup = fragView.findViewById(R.id.report_issue_radio_group);

        // get userId and currentUserId from arguments
        if (getArguments() != null) {
            mUserId = getArguments().getString(USER_ID_ARGS_KEY);
            mCurrentUserId = getArguments().getString(CURRENT_USER_ID_ARGS_KEY);
        }

        mRadioButtonsGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {

                // User select an issue to report, now the send button should be enabled.
                mSendButton.setEnabled(true);

                switch (i){
                    case (R.id.harassment_radio_button):
                        mIssueType = REPORTED_ISSUE_TYPE_HARASSMENT;
                        break;
                    case (R.id.suicide_radio_button):
                        mIssueType = REPORTED_ISSUE_TYPE_SUICIDE;
                        break;
                    case (R.id.inappropriate_radio_button):
                        mIssueType = REPORTED_ISSUE_TYPE_INAPPROPRIATE;
                        break;
                    case (R.id.impersonate_radio_button):
                        mIssueType = REPORTED_ISSUE_TYPE_IMPERSONATE;
                        break;
                    case (R.id.Hate_radio_button):
                        mIssueType = REPORTED_ISSUE_TYPE_HATE;
                        break;
                    case (R.id.child_abuse_radio_button):
                        mIssueType = REPORTED_ISSUE_TYPE_CHILD_ABUSE;
                        break;
                    case (R.id.misinformation_radio_button):
                        mIssueType = REPORTED_ISSUE_TYPE_MISINFORMATION;
                        break;
                    case (R.id.violence_radio_button):
                        mIssueType = REPORTED_ISSUE_TYPE_VIOLENCE;
                        break;
                    case (R.id.terrorism_radio_button):
                        mIssueType = REPORTED_ISSUE_TYPE_TERRORISM;
                        break;
                    case (R.id.scams_radio_button):
                        mIssueType = REPORTED_ISSUE_TYPE_SCAMS;
                        break;
                    case (R.id.spam_radio_button):
                        mIssueType = REPORTED_ISSUE_TYPE_SPAM;
                        break;
                    case (R.id.other_radio_button):
                        mIssueType = REPORTED_ISSUE_TYPE_OTHER;
                        break;
                }
            }
        });

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(null == sMessage){
                    // It's a profile report
                    mViewModel.sendReport(mUserId, mCurrentUserId, sUser, sCurrentUser, mIssueType);
                }else{
                    // It's a message report
                    mViewModel.sendReport(mUserId, mCurrentUserId, sUser, sCurrentUser, mIssueType, sMessage);
                }
                Log.d(TAG, "onClick: selected user id="+ mUserId);
                Log.d(TAG, "onClick: current user id="+ mCurrentUserId);

                dismiss();
            }
        });

        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        return fragView;
    }

    @Override
    public void onViewCreated(@NotNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if(null != getDialog()){
            getDialog().setTitle(R.string.report_dialog_title);
        }
    }

    @Override
    public void onResume() {
        // Get existing layout params for the window
        ViewGroup.LayoutParams params = getDialog().getWindow().getAttributes();
        // Assign window properties to fill the parent
        params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        getDialog().getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
        // Call super onResume after sizing
        super.onResume();
    }


}
