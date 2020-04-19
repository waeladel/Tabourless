package com.tabourless.queue.ui.completeprofile;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.tabourless.queue.R;
import com.tabourless.queue.databinding.FragmentCompleteProfileBinding;
import com.tabourless.queue.databinding.FragmentPlacesBinding;
import com.tabourless.queue.interfaces.FirebaseUserCallback;
import com.tabourless.queue.models.User;
import com.tabourless.queue.ui.main.MainActivity;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import com.yanzhenjie.album.Action;
import com.yanzhenjie.album.Album;
import com.yanzhenjie.album.AlbumFile;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;

import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;

import static android.app.Activity.RESULT_OK;

public class CompleteProfileFragment extends Fragment {

    private CompleteProfileViewModel mViewModel;
    private FragmentCompleteProfileBinding mBinding;
    private ArrayList<Integer> mBirthYears;
    private ArrayAdapter<Integer> spinnerAdapter;
    private Context mContext;
    private String currentUserId;
    private FirebaseUser mFirebaseCurrentUser;
    private StorageReference mStorageRef;
    private StorageReference mImagesRef;
    private FloatingActionButton mSaveFab;
    private NavController navController ;
    private DatabaseReference mDatabaseRef;
    private DatabaseReference mUserRef;
    private ArrayList<AlbumFile> mMediaFiles;

    // for image cropping and compressing
    private Uri mAvatarOriginalUri;
    private Uri mCoverOriginalUri;
    private Uri mAvatarUri;
    private Uri mCoverUri;

    // names of uploaded images
    private static final String AVATAR_THUMBNAIL_NAME = "avatar.jpg";
    private static final String COVER_THUMBNAIL_NAME = "cover.jpg";
    private static final String AVATAR_ORIGINAL_NAME = "original_avatar.jpg";
    private static final String COVER_ORIGINAL_NAME = "original_cover.jpg";


    private static final int SELECT_IMAGE_REQUEST_CODE = 200;
    private static final int CROP_IMAGE_AVATAR_REQUEST_CODE = 103;
    private static final int CROP_IMAGE_COVER_REQUEST_CODE = 104;

    private final static String TAG = CompleteProfileFragment.class.getSimpleName();

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
        currentUserId = mFirebaseCurrentUser!= null ? mFirebaseCurrentUser.getUid() : null;

        // [START database reference]
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        mUserRef = mDatabaseRef.child("users").child(currentUserId);

        // [START create_storage_reference]
        mStorageRef = FirebaseStorage.getInstance().getReference();
        mImagesRef = mStorageRef.child("images");

    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        mViewModel = new ViewModelProvider(this).get(CompleteProfileViewModel.class);

        mBinding = FragmentCompleteProfileBinding.inflate(inflater, container, false);
        View view = mBinding.getRoot();

        // set years for year of birth spinner
        spinnerAdapter = new ArrayAdapter<Integer>(mContext, android.R.layout.simple_spinner_item, mViewModel.getYears());
        mBinding.spinnerBirthValue.setAdapter(spinnerAdapter);

        // Get EditProfileViewModel.User from database if it's null
        if(mViewModel.getUser() == null){
            mViewModel.getUserOnce(currentUserId, new FirebaseUserCallback() {
                @Override
                public void onCallback(User user) {
                    if(user != null){
                        Log.d(TAG,  "FirebaseUserCallback onCallback. name= " + user.getName()+ " key= "+user.getKey());
                        mViewModel.setUser(user);
                        //currentUser = mEditProfileViewModel.getUser();
                        showCurrentUser(mViewModel.getUser());
                    }else{
                        // show default avatar place holder
                        mBinding.avatarImage.setImageResource(R.drawable.ic_round_account_filled_72);
                    }
                }
            });
        }else{
            Log.d(TAG,  "getUserOnce. user is not null. no need to get user from database "+mViewModel.getUser().getName());
            //currentUser = mEditProfileViewModel.getUser();
            showCurrentUser(mViewModel.getUser());
            //restoreLayoutManagerPosition();
        }

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.places, R.id.dashboard, R.id.notifications, R.id.complete_profile)
                //.setOpenableLayout(mBinding.drawerLayout)
                .build();
        navController = NavHostFragment.findNavController(this);

        // Setup toolbar
        //NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(mBinding.toolbarSave, navController, appBarConfiguration);

        // Setup Drawer Navigation
        //NavigationUI.setupWithNavController(mBinding.drawerNavView, navController);

        // Save profile when save button is clicked
        mBinding.saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProfile();
            }
        });

        // select image when avatar is clicked
        mBinding.avatarImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectMedia(true);
            }
        });

        // select image when cover is clicked
        mBinding.coverImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectMedia(false);
            }
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "requestCode ="+ requestCode);
        if (data != null) {
            switch (requestCode){
                case CROP_IMAGE_AVATAR_REQUEST_CODE:
                    Log.d(TAG, "AVATAR_CROP_PICTURE requestCode= "+ requestCode);
                    CropImage.ActivityResult avatarResult = CropImage.getActivityResult(data);
                    if (resultCode == RESULT_OK) {
                        mAvatarOriginalUri = avatarResult.getOriginalUri();
                        mAvatarUri = avatarResult.getUri();
                        compressImage(mAvatarOriginalUri,"original avatar");
                        uploadImage(mAvatarUri, "avatar");
                        Log.d(TAG, "mAvatarOriginalUri = "+ mAvatarOriginalUri);
                        Log.d(TAG, "mAvatarUri = "+ mAvatarUri);

                    } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                        Exception error = avatarResult.getError();
                        Toast.makeText(mContext, error.toString(),
                                Toast.LENGTH_LONG).show();
                    }
                    break;
                case CROP_IMAGE_COVER_REQUEST_CODE:
                    Log.d(TAG, "COVER CROP_PICTURE requestCode= "+ requestCode);
                    CropImage.ActivityResult coverResult = CropImage.getActivityResult(data);
                    if (resultCode == RESULT_OK) {
                        mCoverOriginalUri = coverResult.getOriginalUri();
                        mCoverUri = coverResult.getUri();
                        Log.d(TAG, "mCoverOriginalUri = "+ mCoverOriginalUri);
                        Log.d(TAG, "mCoverUri = "+ mCoverUri);
                        //uploadImage(mCoverUri, "coverImage", position);
                        compressImage(mCoverOriginalUri,"original cover");
                        uploadImage(mCoverUri, "coverImage");
                    } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                        Exception error = coverResult.getError();
                        Toast.makeText(mContext, error.toString(),
                                Toast.LENGTH_LONG).show();
                    }
                    break;
            }
        }

    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }

    private void showCurrentUser(User user) {
        mBinding.nameValue.setText(user.getName());

        //Display avatar
        if (null != user.getAvatar()) {
            mBinding.avatarImage.setImageResource(R.drawable.ic_round_account_filled_72);
            Picasso.get()
                    .load(user.getAvatar())
                    .placeholder(R.mipmap.ic_round_account_filled_72)
                    .error(R.drawable.ic_round_broken_image_72px)
                    .into(mBinding.avatarImage);
        }else{
            // end of user avatar
            mBinding.avatarImage.setImageResource(R.drawable.ic_round_account_filled_72);
        }

        // Display cover
        if (null != user.getCoverImage()) {
            mBinding.coverImage.setImageResource(R.drawable.ic_picture_gallery_white);
            Picasso.get()
                    .load(user.getCoverImage())
                    .placeholder(R.mipmap.ic_picture_gallery_white_512px)
                    .error(R.drawable.ic_broken_image_512px)
                    .into(mBinding.coverImage);
        }else{
            mBinding.coverImage.setImageResource(R.drawable.ic_picture_gallery_white);
        }

        //Set gender value
        if(TextUtils.equals(user.getGender(), "male") ){
            mBinding.spinnerGenderValue.setSelection(0);
        }else{
            mBinding.spinnerGenderValue.setSelection(1);
        }

        //Set year of birth value
        for (int year: mViewModel.getYears()) {
            if(year == user.getBirthYear()){
                mBinding.spinnerBirthValue.setSelection(mViewModel.getYears().indexOf(year));
                return;
            }
        }

        // Set disability value
        if(user.isDisable()){
            mBinding.spinnerDisabilityValue.setSelection(1);
        }else{
            mBinding.spinnerDisabilityValue.setSelection(0);
        }
    }

    private void selectMedia(final boolean isAvatar) {
        Album.image(this) // Image and video mix options.
                .singleChoice() // Multi-Mode, Single-Mode: singleChoice().
                .requestCode(SELECT_IMAGE_REQUEST_CODE) // The request code will be returned in the listener.
                .columnCount(2) // The number of columns in the page list.
                //.selectCount(1)  // Choose up to a few images.
                .camera(true) // Whether the camera appears in the Item.
                .onResult(new Action<ArrayList<AlbumFile>>() {
                    @Override
                    public void onAction(int requestCode, @NonNull ArrayList<AlbumFile> result) {
                        // accept the result.
                        mMediaFiles = result;
                        AlbumFile albumFile = mMediaFiles.get(0);
                        Uri MediaUri = Uri.parse(albumFile.getPath()) ;

                        Log.d(TAG, "MediaType" +albumFile.getMediaType());
                        Log.d(TAG, "MediaUri" +MediaUri);

                        cropImage(MediaUri, isAvatar);
                    }
                })
                .onCancel(new Action<String>() {
                    @Override
                    public void onAction(int requestCode, @NonNull String result) {
                        // The user canceled the operation.
                    }
                })
                .start();
    }

    private void cropImage(Uri mediaUri, boolean isAvatar) {
        if(isAvatar){
            Intent intent = CropImage.activity(Uri.fromFile(new File(mediaUri.toString())))
                    //.setGuidelines(CropImageView.Guidelines.ON)
                    .setAllowRotation(true)
                    .setAutoZoomEnabled(true)
                    .setCropShape(CropImageView.CropShape.OVAL)
                    .setActivityTitle(getString(R.string.crop_activity_title))
                    .setCropMenuCropButtonTitle(getString(R.string.upload_button))
                    //.setAspectRatio(1,1)
                    .setFixAspectRatio(true)
                    //.setMaxCropResultSize(600, 600)
                    .setMinCropResultSize(300,300)
                    .setRequestedSize(300,300)//resize
                    .getIntent(mContext);

            this.startActivityForResult(intent, CROP_IMAGE_AVATAR_REQUEST_CODE );

        }else{

            Intent intent = CropImage.activity(Uri.fromFile(new File(mediaUri.toString())))
                    //.setGuidelines(CropImageView.Guidelines.ON)
                    .setAllowRotation(true)
                    .setAutoZoomEnabled(true)
                    .setActivityTitle(getString(R.string.crop_activity_title))
                    .setCropMenuCropButtonTitle(getString(R.string.upload_button))
                    .setAspectRatio(2,1)
                    //.setMaxCropResultSize(600, 600)
                    .setMinCropResultSize(300,300)
                    .setRequestedSize(600,300) //resize
                    .getIntent(mContext);

            this.startActivityForResult(intent, CROP_IMAGE_COVER_REQUEST_CODE);
        }

        Log.d(TAG, "cropImage starts" +mediaUri);
    }

    private void compressImage(final Uri imageUri, final String type) {
        if (null != imageUri && null != imageUri.getPath()) {
            File imageFile = new File(imageUri.getPath());
            Luban.get(getContext())
                    .load(imageFile)                     // pass image to be compressed
                    .putGear(Luban.THIRD_GEAR)      // set compression level, defaults to 3
                    .setCompressListener(new OnCompressListener() { // Set up return

                        @Override
                        public void onStart() {
                            //Called when compression starts, display loading UI here
                            Log.d(TAG, "compress :onStart= ");
                        }
                        @Override
                        public void onSuccess(File file) {
                            //Called when compression finishes successfully, provides compressed image
                            Log.d(TAG, "compress :onSuccess= "+file.getPath());
                            Uri compressImageUri = Uri.fromFile(file);
                            uploadImage(compressImageUri, type);
                        }

                        @Override
                        public void onError(Throwable e) {
                            //Called if an error has been encountered while compressing
                            Log.d(TAG, "compress :onError= "+e);
                            uploadImage(imageUri, type);

                        }
                    }).launch();    // Start compression
        }
    }

    private void uploadImage(Uri imageUri, final String type) {
        //Uri fileUri = Uri.fromFile(new File(imageUri.getPath()));
        StorageReference userRef ; //= mStorageRef.child("images/"+currentUserId+"avatar.jpg");

        switch (type){
            case "avatar":
                userRef = mStorageRef.child("images/"+currentUserId +"/"+ AVATAR_THUMBNAIL_NAME);
                break;
            case "coverImage":
                userRef = mStorageRef.child("images/"+currentUserId +"/"+ COVER_THUMBNAIL_NAME );
                break;
            case "original avatar":
                userRef = mStorageRef.child("images/"+currentUserId +"/"+ AVATAR_ORIGINAL_NAME);
                break;
            case "original cover":
                userRef = mStorageRef.child("images/"+currentUserId +"/"+ COVER_ORIGINAL_NAME);
                break;
            default:
                userRef = mStorageRef.child("images/"+currentUserId+AVATAR_THUMBNAIL_NAME);
                break;
        }

        // Create the file metadata
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("image/jpg")
                .build();

        // Upload file and metadata to the path 'images/mountains.jpg'
        UploadTask uploadTask = userRef.putFile(imageUri, metadata);

        if(type.equals("avatar") || type.equals("coverImage")){
            // Listen for state changes, errors, and completion of the upload.
            uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {

                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {

                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    System.out.println("Upload is " + progress + "% done");
                    // Show progress loading animation
                    switch (type) {
                        case "avatar":
                            mBinding.avatarProgressIcon.setVisibility(View.VISIBLE);
                            break;
                        case "coverImage":
                            mBinding.coverProgressIcon.setVisibility(View.VISIBLE);
                            break;
                    }
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // Handle successful uploads on complete
                    Log.d(TAG, "uploads :onSuccess");
                }
            });// [END of Listen for state changes, errors, and completion of the upload]

            // [get DownloadUrl]
            final StorageReference finalUserRef = userRef;
            uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        if(null != task.getException()){
                            throw task.getException();
                        }
                    }
                    // Continue with the task to get the download URL
                    return finalUserRef.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();

                        // set ViewModel.user values
                        switch (type){
                            case "avatar":
                                //mProfileDataArrayList.set(position,new Profile(type, String.valueOf(downloadUri),SECTION_AVATAR, SECTION_AVATAR));
                                mViewModel.getUser().setAvatar(String.valueOf(downloadUri));
                                break;
                            case "coverImage":
                                //mProfileDataArrayList.set(position,new Profile(type, String.valueOf(downloadUri),SECTION_COVER, SECTION_COVER));
                                mViewModel.getUser().setCoverImage(String.valueOf(downloadUri));
                                break;
                        }

                        // Hide progress loading animation
                        switch (type) {
                            case "avatar":
                                mBinding.avatarProgressIcon.setVisibility(View.GONE);
                                break;
                            case "coverImage":
                                mBinding.coverProgressIcon.setVisibility(View.GONE);
                                break;
                        }

                    } else {
                        // Handle failures
                        Log.d(TAG, "uploads :FailureL");
                        Toast.makeText(mContext, R.string.upload_image_error,
                                Toast.LENGTH_LONG).show();
                    }
                }
            });

        }
    }

    private void saveProfile() {
    }

}
