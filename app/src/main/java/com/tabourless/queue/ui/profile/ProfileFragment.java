package com.tabourless.queue.ui.profile;

import android.content.Context;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.tabourless.queue.GlideApp;
import com.tabourless.queue.R;
import com.tabourless.queue.databinding.FragmentProfileBinding;
import com.tabourless.queue.interfaces.FirebaseUserCallback;
import com.tabourless.queue.interfaces.ItemClickListener;
import com.tabourless.queue.models.Relation;
import com.tabourless.queue.models.User;
import com.tabourless.queue.ui.BlockAlertFragment;
import com.tabourless.queue.ui.BlockDeleteAlertFragment;
import java.util.Calendar;

import static com.tabourless.queue.App.AVATAR_ORIGINAL_NAME;
import static com.tabourless.queue.App.AVATAR_THUMBNAIL_NAME;
import static com.tabourless.queue.App.COVER_ORIGINAL_NAME;
import static com.tabourless.queue.App.COVER_THUMBNAIL_NAME;
import static com.tabourless.queue.App.DIRECTION_ARGUMENTS_KEY_USER_ID;
import static com.tabourless.queue.App.RELATION_STATUS_BLOCKED;
import static com.tabourless.queue.App.RELATION_STATUS_BLOCKING;
import static com.tabourless.queue.App.RELATION_STATUS_NOT_FRIEND;
import static com.tabourless.queue.App.STORAGE_REF_IMAGES;
import static com.tabourless.queue.App.USER_SPINNER_GENDER_FEMALE;
import static com.tabourless.queue.App.USER_SPINNER_GENDER_MALE;

public class ProfileFragment extends Fragment implements ItemClickListener {

    private final static String TAG = ProfileFragment.class.getSimpleName();

    private ProfileViewModel mViewModel;
    private FragmentProfileBinding mBinding;

    //Fragments tags
    private  static final String REQUEST_FRAGMENT = "RequestFragment";
    private  static final String EDIT_UNREVEAL_FRAGMENT = "EditFragment";
    private  static final String CONFIRM_DELETE_RELATION_ALERT_FRAGMENT = "DeleteRelationAlertFragment";
    private  static final String CONFIRM_BLOCK_ALERT_FRAGMENT = "BlockFragment"; // Tag for confirm block alert fragment
    private  static final String CONFIRM_BLOCK_DELETE_ALERT_FRAGMENT = "BlockDeleteFragment"; // Tag for confirm block and delete alert fragment

    private String notificationType;
    private String mRelationStatus;

    private String mCurrentUserId, mUserId;
    private User mUser, mCurrentUser;
    private FirebaseUser mFirebaseCurrentUser;
    private ColorStateList mFabDefaultColor; // To rest FAB's default color
    private ColorStateList mFabDefaultTextColor; // To rest the hint's default color of FAB buttons

    // [START declare_database_ref]
    private DatabaseReference mDatabaseRef;
    private DatabaseReference mUserRef;
    private StorageReference mStorageRef;

    private Context mContext;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
    }

    // This method will only be called once when the retained
    // Fragment is first created.
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Get current logged in user
        mFirebaseCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mCurrentUserId = mFirebaseCurrentUser != null ? mFirebaseCurrentUser.getUid() : null;

        Log.d(TAG, "getArguments: "+ getArguments());

        if(getArguments() != null && getArguments().containsKey(DIRECTION_ARGUMENTS_KEY_USER_ID)) {
            // Check if current logged in user is the selected user
            mUserId = ProfileFragmentArgs.fromBundle(getArguments()).getUserId(); // any user
            Log.d(TAG, "mCurrentUserId= " + mCurrentUserId + " mUserId= " + mUserId );
        }

        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        // [START create_storage_reference]
        mStorageRef = FirebaseStorage.getInstance().getReference();

        mViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

    }


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        mBinding = FragmentProfileBinding.inflate(inflater, container, false);
        View view = mBinding.getRoot();

        mFabDefaultColor = mBinding.blockEditButton.getBackgroundTintList(); // get default FAB's color
        mFabDefaultTextColor = mBinding.blockEditText.getTextColors();

        // display user data as it's not null
        if (null != mUserId && !mUserId.equals(mCurrentUserId)) { // it's not logged in user. It's another user

            mViewModel.getUser(mUserId).observe(getViewLifecycleOwner(), new Observer<User>() {
                @Override
                public void onChanged(User user) {
                    if(user != null){
                        mUser = user;
                        mUser.setKey(mUserId);
                        Log.d(TAG,  "onChanged user name= " + user.getName() + " hashcode= "+ hashCode()+ " userId= "+mUser.getKey());
                        showCurrentUser();
                    }
                }
            });
        }else{
            // get current logged in user
            mViewModel.getUser(mCurrentUserId).observe(getViewLifecycleOwner(), new Observer<User>() {
                @Override
                public void onChanged(User user) {
                    if(user != null){
                        mUser = user;
                        mUser.setKey(mCurrentUserId);
                        Log.d(TAG,  "onChanged user name= " + user.getName() + " hashcode= "+ hashCode()+ " userId= "+mUser.getKey());
                        showCurrentUser();
                    }else{
                        // if user is not exist. if new user managed to open profile fragment without having a profile yet
                        Log.e(TAG,  "User is null");
                        // When user is null, avatar image will be empty, must display the place holder
                        mBinding.userImage.setImageResource(R.drawable.ic_round_account_filled_72);

                        // Disable edit profile button
                        //mBlockEditButton.setClickable(false);
                        mBinding.blockEditButton.setEnabled(false);
                        mBinding.blockEditButton.setBackgroundTintList(ColorStateList.valueOf
                                (getResources().getColor(R.color.disabled_button)));
                        mBinding.blockEditText.setEnabled(false);

                        // Disable avatar and cover clicks
                        mBinding.userImage.setClickable(false);
                        mBinding.userImage.setEnabled(false);

                        mBinding.coverImage.setClickable(false);
                        mBinding.coverImage.setEnabled(false);
                    }
                }
            });
        }

        // Get current user once, to get currentUser's name and avatar for notifications
        mViewModel.getUserOnce(mCurrentUserId, new FirebaseUserCallback() {
            @Override
            public void onCallback(User user) {
                if(user != null){
                    Log.d(TAG,  "FirebaseUserCallback onCallback. name= " + user.getName() + " hashcode= "+ hashCode());
                    mCurrentUser = user;
                }
            }
        });

        // toggle Buttons
        if (null != mUserId && !mUserId.equals(mCurrentUserId)) { // it's not logged in user. It's another user
            mBinding.blockEditButton.setImageResource(R.drawable.ic_block_24dp);
            mBinding.blockEditText.setText(R.string.block_button);

            // update the reveal request
            mRelationStatus = RELATION_STATUS_NOT_FRIEND;

            // get relations with selected user if any
            mViewModel.getRelation(mCurrentUserId, mUserId).observe(getViewLifecycleOwner(), new Observer<Relation>() {
                @Override
                public void onChanged(Relation relation) {
                    if (relation != null){
                        Log.i(TAG, "onChanged mProfileViewModel getRelation = " +relation.getStatus() + " hashcode= "+ hashCode());
                        // Relation exist
                        switch (relation.getStatus()){
                            case RELATION_STATUS_BLOCKING:
                                // If this selected user has blocked me
                                //current user can't do anything about it
                                mRelationStatus = RELATION_STATUS_BLOCKING;

                                mBinding.blockEditButton.setEnabled(false);
                                mBinding.blockEditButton.setClickable(false);
                                mBinding.blockEditButton.setBackgroundTintList(ColorStateList.valueOf
                                        (getResources().getColor(R.color.disabled_button)));

                                mBinding.messageButton.setEnabled(false);
                                mBinding.messageButton.setClickable(false);
                                mBinding.messageButton.setBackgroundTintList(ColorStateList.valueOf
                                        (getResources().getColor(R.color.disabled_button)));

                                //disable all FAB's hints
                                mBinding.messageButtonText.setEnabled(false);
                                mBinding.blockEditText.setEnabled(false);
                                break;
                            case RELATION_STATUS_BLOCKED:
                                // If this selected user is blocked by me (current user)
                                // the only option is to unblock him
                                mRelationStatus = RELATION_STATUS_BLOCKED;

                                // change block hint to Unblock, and change color to red
                                mBinding.blockEditText.setText(R.string.unblock_button);
                                mBinding.blockEditText.setTextColor(getResources().getColor(R.color.colorError));

                                mBinding.messageButton.setEnabled(false);
                                mBinding.messageButton.setClickable(false);
                                mBinding.messageButton.setBackgroundTintList(ColorStateList.valueOf
                                        (getResources().getColor(R.color.disabled_button)));

                                ////disable all FAB's except block button
                                mBinding.messageButtonText.setEnabled(false);

                                break;
                        }
                        Log.d(TAG, "onChanged relation Status= " + relation.getStatus());

                    }else{
                        // Relation doesn't exist, use default user settings
                        Log.i(TAG, "onChanged relation Status= Relation doesn't exist. mRelationStatus= "+ mRelationStatus);
                        // Check if it's null because it was blocked before or not
                        // if it was blocked it means current user is unblocking this user, we need to force default buttons colors
                        if (TextUtils.equals(mRelationStatus, RELATION_STATUS_BLOCKED)) {
                            // Return to default text color when user unblock
                            mBinding.blockEditText.setTextColor(mFabDefaultTextColor);
                        }

                        mRelationStatus = RELATION_STATUS_NOT_FRIEND;

                        // Enable all buttons. In case they were disabled by previous block
                        mBinding.blockEditText.setText(R.string.block_button); // in case it was set to unblock from previous block
                        mBinding.blockEditButton.setEnabled(true);
                        mBinding.blockEditButton.setClickable(true);
                        mBinding.blockEditButton.setBackgroundTintList(mFabDefaultColor);

                        mBinding.messageButton.setEnabled(true);
                        mBinding.messageButton.setClickable(true);
                        mBinding.messageButton.setBackgroundTintList(mFabDefaultColor);

                        mBinding.messageButtonText.setEnabled(true);
                        mBinding.blockEditText.setEnabled(true);
                    }
                }
            });
        } else {
            // it's logged in user profile
            Log.d(TAG, "it's logged in user profile= " + mUserId);
            mBinding.blockEditButton.setImageResource(R.drawable.ic_user_edit_profile);
            mBinding.blockEditText.setText(R.string.edit_button_title);

            mBinding.messageButton.setEnabled(false);
            mBinding.messageButton.setClickable(false);
            mBinding.messageButton.setBackgroundTintList(ColorStateList.valueOf
                    (getResources().getColor(R.color.disabled_button)));

            mBinding.messageButtonText.setEnabled(false);
        }

        mBinding.blockEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (null != mUserId && !mUserId.equals(mCurrentUserId)) { // it's not logged in user. It's another user
                    Log.d(TAG, "blockUser clicked. mRelationStatus= "+mRelationStatus);
                    switch (mRelationStatus) {
                        case RELATION_STATUS_BLOCKING:
                            Log.d(TAG, "mRelationStatus = RELATION_STATUS_BLOCKING");
                            // If this selected user has blocked me
                            //current user can't do anything about it
                            break;
                        case RELATION_STATUS_BLOCKED:
                            Log.d(TAG, "mRelationStatus = RELATION_STATUS_BLOCKED");
                            // If this selected user is blocked by me (current user)
                            // the only option is to unblock him
                            mViewModel.unblockUser(mCurrentUserId, mUserId);
                            break;
                        default:
                            // There is no blocking relation, proceed with blocking
                            // There is no blocking relation
                            //showBlockDialog(); // To confirm blocking or cancel
                            // Create a popup Menu if null. To show when block is clicked
                            PopupMenu popupBlockMenu = new PopupMenu(mContext, view);
                            popupBlockMenu.getMenu().add(Menu.NONE, 0, 0, R.string.popup_menu_block);
                            popupBlockMenu.getMenu().add(Menu.NONE, 1, 1, R.string.popup_menu_block_delete);

                            popupBlockMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    switch (item.getItemId()) {
                                        case 0:
                                            Log.i(TAG, "onMenuItemClick. item block clicked ");
                                            //blockUser();
                                            showBlockDialog();
                                            return true;
                                        case 1:
                                            Log.i(TAG, "onMenuItemClick. item block and delete conversation clicked ");
                                            //blockDelete();
                                            showBlockDeleteDialog();
                                            return true;
                                        default:
                                            return false;
                                    }
                                }
                            });
                            popupBlockMenu.show();
                            break;
                    }
                } else {
                    Log.i(TAG, "going to edit profile fragment= ");
                    NavDirections direction = ProfileFragmentDirections.actionProfileToCompleteProfile(true);
                    NavController navController = Navigation.findNavController(view);
                    navController.navigate(direction);
                }
            }
        });

        mBinding.messageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (null != mUserId && !mUserId.equals(mCurrentUserId)) { // it's not logged in user. It's another user
                    Log.d(TAG, "send message to user");
                    NavDirections MessageDirection = ProfileFragmentDirections.actionProfileToMessages(null, mUserId, false);
                    //NavController navController = Navigation.findNavController(this, R.id.host_fragment);
                    Navigation.findNavController(view).navigate(MessageDirection);
                } else {
                    Log.i(TAG, "don't send message to current logged in user ");
                }

            }
        });

        mBinding.coverImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (null != mUserId && !mUserId.equals(mCurrentUserId)) { // it's not logged in user. It's another user
                    Log.i(TAG, "going to Cover image view");
                    NavDirections direction = ProfileFragmentDirections.actionProfileToPhoto(mUserId, COVER_ORIGINAL_NAME);
                    NavController navController = Navigation.findNavController(view);
                    navController.navigate(direction);
                } else {
                    Log.i(TAG, "going to Cover image view");
                    NavDirections direction = ProfileFragmentDirections.actionProfileToPhoto(mCurrentUserId, COVER_ORIGINAL_NAME);
                    NavController navController = Navigation.findNavController(view);
                    navController.navigate(direction);
                }
            }
        });

        mBinding.userImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (null != mUserId && !mUserId.equals(mCurrentUserId)) { // it's not logged in user. It's another user
                    Log.i(TAG, "going to Avatar image view");
                    NavDirections direction = ProfileFragmentDirections.actionProfileToPhoto(mUserId, AVATAR_ORIGINAL_NAME);
                    NavController navController = Navigation.findNavController(view);
                    navController.navigate(direction);
                } else {
                    Log.i(TAG, "going to Avatar image view");
                    NavDirections direction = ProfileFragmentDirections.actionProfileToPhoto(mCurrentUserId, AVATAR_ORIGINAL_NAME);
                    NavController navController = Navigation.findNavController(view);
                    navController.navigate(direction);
                }
            }
        });

        return view;
    }

    // To listen to dialog clicked buttons
    @Override
    public void onClick(View view, int position, boolean isLongClick) {

        switch (position) {
              case 6:
                // block is clicked
                Log.i(TAG, "block is clicked, we must start blocking function");
                  mViewModel.blockUser(mCurrentUserId, mUserId);
                break;
            case 7:
                // block and delete is clicked
                Log.i(TAG, "block and delete  is clicked, we must start blocking function");
                mViewModel.blockDelete(mCurrentUserId, mUserId);
                break;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }

    private void showCurrentUser() {
        // Get user values
        if (mUser != null) {
            // Lets get cover
            if(!TextUtils.isEmpty(mUser.getCoverImage())){
                StorageReference userCoverStorageRef = mStorageRef.child(STORAGE_REF_IMAGES +"/"+ mUser.getKey() +"/"+ COVER_THUMBNAIL_NAME);
                // Download directly from StorageReference using Glide
                GlideApp.with(mContext)
                        .load(userCoverStorageRef)
                        //.placeholder(R.mipmap.ic_picture_gallery_white_512px)
                        .placeholder(R.drawable.ic_picture_gallery)
                        .error(R.drawable.ic_broken_image_512px)
                        .into(mBinding.coverImage);
            }else{
                mBinding.coverImage.setImageResource(R.drawable.ic_picture_gallery);
            }

            // Lets get avatar
            if(!TextUtils.isEmpty(mUser.getAvatar())){
                StorageReference userAvatarStorageRef = mStorageRef.child(STORAGE_REF_IMAGES +"/"+ mUser.getKey() +"/"+ AVATAR_THUMBNAIL_NAME);
                // Download directly from StorageReference using Glide
                GlideApp.with(mContext)
                        .load(userAvatarStorageRef)
                        //.placeholder(R.mipmap.account_circle_72dp)
                        .placeholder(R.drawable.ic_round_account_filled_72)
                        .error(R.drawable.ic_round_broken_image_72px)
                        .into(mBinding.userImage);
            }else{
                mBinding.userImage.setImageResource(R.drawable.ic_round_account_filled_72);
            }

            // Show user name
            if (null != mUser.getName()) {
                mBinding.userNameText.setText(mUser.getName());
            }else{
                mBinding.userNameText.setText(null);
            }

            //mLovedByValue.setText(getString(R.string.user_loved_by, mUser.getLoveCounter()));
            //mPickUpValue.setText(getString(R.string.user_pickedup_by, mUser.getPickupCounter()));

            //mRelationship.setText(getString(R.string.user_relationship_value, user.getRelationship()));

            if (null != mUser.getGender()) {
                switch (mUser.getGender()) {
                    case USER_SPINNER_GENDER_MALE:
                        mBinding.userGenderValue.setText(R.string.male);
                        mBinding.userGenderIcon.setImageResource(R.drawable.ic_business_man);
                        mBinding.userGenderIcon.setVisibility(View.VISIBLE);
                        break;
                    case USER_SPINNER_GENDER_FEMALE:
                        mBinding.userGenderValue.setText(R.string.female);
                        mBinding.userGenderIcon.setImageResource(R.drawable.ic_business_woman);
                        mBinding.userGenderIcon.setVisibility(View.VISIBLE);
                        break;
                    default:
                        mBinding.userGenderValue.setText(R.string.not_specified);
                        mBinding.userGenderIcon.setVisibility(View.INVISIBLE);
                        break;
                }
            }

            if (mUser.getBirthYear() != 0) {
                int thisYear = Calendar.getInstance().get(Calendar.YEAR);
                mBinding.userAgeValue.setText(String.valueOf(thisYear - mUser.getBirthYear()));
                mBinding.userAgeIcon.setVisibility(View.VISIBLE);
            }

            if (mUser.getDisabled()) {
                mBinding.userDisabilityValue.setText(R.string.yes);
                mBinding.userDisabilityIcon.setImageResource(R.drawable.ic_wheelchair_accessible);
                mBinding.userDisabilityIcon.setVisibility(View.VISIBLE);
            } else if (!mUser.getDisabled()) {
                mBinding.userDisabilityValue.setText(R.string.no);
                mBinding.userDisabilityIcon.setImageResource(R.drawable.ic_fit_person_stretching_exercises);
                mBinding.userDisabilityIcon.setVisibility(View.VISIBLE);
            } else {
                mBinding.userDisabilityValue.setText(R.string.not_specified);
                mBinding.userDisabilityIcon.setVisibility(View.INVISIBLE);
            }

            // End of display parcelable data]
        }
    }

   /* // Get image from storage if the link in database is broken
    private void loadStorageImage(final String userId, final boolean isAvatar) {
        Log.d(TAG, "loadStorageImage: userId= "+ userId);
        if(isAvatar){
            // Lets get avatar
            *//*mStorageRef.child("images/"+userId +"/"+ AVATAR_THUMBNAIL_NAME).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    Log.d(TAG, "onSuccess: uri= "+ uri);
                    if(null != mBinding.userImage){
                        Picasso.get()
                                .load(uri)
                                .placeholder(R.mipmap.account_circle_72dp)
                                .error(R.drawable.ic_round_broken_image_72px)
                                .into(mBinding.userImage);
                    }

                    // Update broken images in the database (Only if it's current user, due to security rules)
                    if(TextUtils.equals(userId, mCurrentUserId)){
                        mViewModel.updateBrokenImage(userId, uri, isAvatar);
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle any errors
                    mBinding.userImage.setImageResource(R.drawable.ic_round_account_filled_72);
                }
            });*//*

        }else{
            // Lets get Cover
            mStorageRef.child("images/"+userId +"/"+ COVER_THUMBNAIL_NAME).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    Picasso.get()
                            .load(uri)
                            .placeholder(R.mipmap.ic_picture_gallery_white_512px)
                            .error(R.drawable.ic_broken_image_512px)
                            .into(mBinding.coverImage);

                    // Update broken images in the database (Only if it's current user, due to security rules)
                    if(TextUtils.equals(userId, mCurrentUserId)){
                        mViewModel.updateBrokenImage(userId, uri, isAvatar);
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle any errors
                    mBinding.coverImage.setImageResource(R.drawable.ic_picture_gallery);
                }
            });

        }

    }*/

    //Show a dialog to confirm blocking user
    private void showBlockDialog() {
        BlockAlertFragment blockFragment = BlockAlertFragment.newInstance(mContext, this);
        if(getParentFragmentManager() != null) {
            blockFragment.show(getParentFragmentManager(), CONFIRM_BLOCK_ALERT_FRAGMENT);
            Log.i(TAG, "blockFragment show clicked ");
        }

    }

    //Show a dialog to confirm blocking user and delete his conversation with us (current user)
    private void showBlockDeleteDialog() {
        BlockDeleteAlertFragment blockDeleteFragment = BlockDeleteAlertFragment.newInstance(mContext, this);
        if(getParentFragmentManager() != null) {
            blockDeleteFragment.show(getParentFragmentManager(), CONFIRM_BLOCK_DELETE_ALERT_FRAGMENT);
            Log.i(TAG, "blockDeleteFragment show clicked ");
        }
    }
}
