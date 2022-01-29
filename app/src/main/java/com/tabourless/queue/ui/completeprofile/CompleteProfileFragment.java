package com.tabourless.queue.ui.completeprofile;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.iceteck.silicompressorr.SiliCompressor;
import com.tabourless.queue.BuildConfig;
import com.tabourless.queue.GlideApp;
import com.tabourless.queue.R;
import com.tabourless.queue.Utils.MyGlideEngine;
import com.tabourless.queue.databinding.FragmentCompleteProfileBinding;
import com.tabourless.queue.interfaces.FirebaseUserCallback;
import com.tabourless.queue.interfaces.ItemClickListener;
import com.tabourless.queue.models.User;
import com.tabourless.queue.ui.CameraPermissionAlertFragment;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.internal.entity.CaptureStrategy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static android.app.Activity.RESULT_OK;
import static com.tabourless.queue.App.AVATAR_ORIGINAL_NAME;
import static com.tabourless.queue.App.AVATAR_THUMBNAIL_NAME;
import static com.tabourless.queue.App.COVER_ORIGINAL_NAME;
import static com.tabourless.queue.App.COVER_THUMBNAIL_NAME;
import static com.tabourless.queue.App.DATABASE_REF_USERS;
import static com.tabourless.queue.App.DATABASE_REF_USER_TOKENS;
import static com.tabourless.queue.App.DIRECTION_ARGUMENTS_KEY_IS_EDIT;
import static com.tabourless.queue.App.STORAGE_REF_IMAGES;
import static com.tabourless.queue.App.USER_SPINNER_GENDER_FEMALE;
import static com.tabourless.queue.App.USER_SPINNER_GENDER_MALE;

public class CompleteProfileFragment extends Fragment implements ItemClickListener {

    private CompleteProfileViewModel mViewModel;
    private FragmentCompleteProfileBinding mBinding;
    private ArrayList<Integer> mBirthYears;
    private ArrayAdapter<Integer> spinnerAdapter;
    private Context mContext;
    private Activity mActivity;
    private FragmentManager mFragmentManager;
    private String currentUserId;
    private boolean isEdit;
    private FirebaseUser mFirebaseCurrentUser;
    private StorageReference mStorageRef;
    private StorageReference mImagesRef;
    private FloatingActionButton mSaveFab;
    private NavController navController ;
    private DatabaseReference mDatabaseRef;
    private DatabaseReference mUserRef;

    // for image cropping and compressing
    private Uri mOriginalAvatarUri;
    private Uri mOriginalCoverUri;
    private Uri mThumbnailAvatarUri;
    private Uri mThumbnailCoverUri;

    private static final int SELECT_AVATAR_REQUEST_CODE = 102;
    private static final int SELECT_COVER_REQUEST_CODE = 103;
    private static final int CROP_IMAGE_REQUEST_CODE = 104;
    private static final int REQUEST_STORAGE_PERMISSIONS_CODE = 124;

    private  final static String IMAGE_HOLDER_POSITION = "position";
    private static final String APP_AUTHORITY = BuildConfig.APPLICATION_ID +".fileprovider";
    private  static final String PERMISSION_RATIONALE_FRAGMENT = "storagePermissionFragment";

    private final static String TAG = CompleteProfileFragment.class.getSimpleName();

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
        if (context instanceof Activity){// check if fragmentContext is an activity
            mActivity =(Activity) context;
        }
    }

    // This method will only be called once when the retained
    // Fragment is first created.
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFragmentManager = getChildFragmentManager(); // Needed to open the rational dialog
        //Get current logged in user
        mFirebaseCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = mFirebaseCurrentUser!= null ? mFirebaseCurrentUser.getUid() : null;

        if(getArguments() != null && getArguments().containsKey(DIRECTION_ARGUMENTS_KEY_IS_EDIT)) {
            // Check if should display Edit profile
            isEdit = CompleteProfileFragmentArgs.fromBundle(getArguments()).getIsEdit();
        }

        // [START database reference]
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        mUserRef = mDatabaseRef.child(DATABASE_REF_USERS).child(currentUserId);

        // [START create_storage_reference]
        mStorageRef = FirebaseStorage.getInstance().getReference();
        mImagesRef = mStorageRef.child(STORAGE_REF_IMAGES);

    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        mViewModel = new ViewModelProvider(this).get(CompleteProfileViewModel.class);

        mBinding = FragmentCompleteProfileBinding.inflate(inflater, container, false);
        View view = mBinding.getRoot();

        // set years for year of birth spinner
        spinnerAdapter = new ArrayAdapter<Integer>(mContext, android.R.layout.simple_spinner_item, mViewModel.getYears());
        mBinding.spinnerBirthValue.setAdapter(spinnerAdapter);

        // Get User from database if it's null
        if(mViewModel.getUser() == null){
            mViewModel.getUserOnce(currentUserId, new FirebaseUserCallback() {
                @Override
                public void onCallback(User user) {
                    if(user != null){
                        Log.d(TAG,  "FirebaseUserCallback onCallback. name= " + user.getName()+ " key= "+user.getKey());
                        mViewModel.setUser(user);
                        mViewModel.getUser().setKey(user.getKey());
                        //currentUser = mEditProfileViewModel.getUser();
                        showCurrentUser(mViewModel.getUser());
                    }else{
                        // let's create a temp user to use it in saving data
                        User tempUser = new User();
                        tempUser.setKey(currentUserId);
                        mViewModel.setUser(tempUser);
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
                R.id.queues, R.id.inbox, R.id.notifications, R.id.complete_profile)
                //.setOpenableLayout(mBinding.drawerLayout)
                .build();
        navController = NavHostFragment.findNavController(this);

        // Setup toolbar
        //NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(mBinding.toolbarSave, navController, appBarConfiguration);

        // Setup Drawer Navigation
        //NavigationUI.setupWithNavController(mBinding.drawerNavView, navController);

        // Add black color filter to circle image
        int blackColor = (ContextCompat.getColor(mContext, R.color.transparent_edit_image));
        //ImageViewCompat.setImageTintList(mBinding.avatarImage, ColorStateList.valueOf(blackColor));
        ColorFilter colorFilter = new PorterDuffColorFilter(blackColor, PorterDuff.Mode.DARKEN);
        mBinding.avatarImage.setColorFilter(colorFilter);
        //mBinding.coverImage.setColorFilter(colorFilter);

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
                mViewModel.setSelectAvatarClicked(true);
                if (!isPermissionsGranted()) {
                    requestPermission();
                }else{
                    selectMedia(0);
                }
            }
        });

        // select image when cover is clicked
        mBinding.coverImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mViewModel.setSelectAvatarClicked(false);
                if (!isPermissionsGranted()) {
                    requestPermission();
                }else{
                    selectMedia(1);
                }
            }
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "requestCode ="+ requestCode);
        if (data != null) {
            int position = 0;
            if(null != data.getExtras()){
                position = data.getExtras().getInt(IMAGE_HOLDER_POSITION,0);
            }
            switch (requestCode){
                case SELECT_AVATAR_REQUEST_CODE:
                    // An avatar photo is selected
                    Log.d(TAG, "SELECT_AVATAR requestCode= "+ requestCode);
                    Log.d(TAG, "SELECT_AVATAR position= "+ position);
                    if (resultCode == RESULT_OK) {
                        List<Uri> selectedAvatars = Matisse.obtainResult(data);;
                        cropImage(selectedAvatars.get(0), 0);
                    }
                    break;
                case SELECT_COVER_REQUEST_CODE:
                    // A cover photo is selected
                    Log.d(TAG, "SELECT_COVER requestCode= "+ requestCode);
                    Log.d(TAG, "SELECT_COVER position= "+ position);
                    if (resultCode == RESULT_OK) {
                        List<Uri> selectedCovers = Matisse.obtainResult(data);
                        cropImage(selectedCovers.get(0), 1);
                    }
                    break;
                case CROP_IMAGE_REQUEST_CODE:
                    // A cropped photo is saved
                    Log.d(TAG, "CROP_PICTURE requestCode= "+ requestCode);
                    Log.d(TAG, "CROP_PICTURE position= "+ position);
                    CropImage.ActivityResult imageResult = CropImage.getActivityResult(data);
                    if (resultCode == RESULT_OK) {
                        if(position == 0){
                            // it's avatar image
                            mOriginalAvatarUri = imageResult.getOriginalUri();
                            mThumbnailAvatarUri = imageResult.getUri();
                            Log.d(TAG, "mOriginalAvatarUri = "+ mOriginalAvatarUri);
                            Log.d(TAG, "mThumbnailAvatarUri = "+ mThumbnailAvatarUri);
                            compressImage(mOriginalAvatarUri,"original avatar" ,position);
                            uploadImage(mThumbnailAvatarUri, "avatar");
                        }else{
                            // it's cover image
                            mOriginalCoverUri = imageResult.getOriginalUri();
                            mThumbnailCoverUri = imageResult.getUri();
                            Log.d(TAG, "mOriginalCoverUri = "+ mOriginalCoverUri);
                            Log.d(TAG, "mThumbnailCoverUri = "+ mThumbnailCoverUri);
                            //uploadImage(mCoverUri, "coverImage", position);
                            compressImage(mOriginalCoverUri,"original cover" ,position);
                            uploadImage(mThumbnailCoverUri, "coverImage");
                        }
                    } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                        Exception error = imageResult.getError();
                        Toast.makeText(mContext, error.toString(),
                                Toast.LENGTH_LONG).show();
                        Log.d(TAG, "mAvatarUri crop error= "+ error.toString());
                    }
                    break;
            }
        }

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // fragment title should be edit profile if we pass a user id
        Log.d(TAG,  "isEdit = "+isEdit);
        if(isEdit){
            mBinding.toolbarSave.setTitle(R.string.title_edit_profile);
        }else{
            mBinding.toolbarSave.setTitle(R.string.title_complete_profile);
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
        if(!TextUtils.isEmpty(user.getAvatar())){
            StorageReference userAvatarStorageRef = mStorageRef.child(STORAGE_REF_IMAGES +"/"+ user.getKey() +"/"+ AVATAR_THUMBNAIL_NAME);
            // Download directly from StorageReference using Glide
            GlideApp.with(mContext)
                    .load(userAvatarStorageRef)
                    //.placeholder(R.mipmap.account_circle_72dp)
                    .placeholder(R.drawable.ic_round_account_filled_72)
                    .error(R.drawable.ic_round_broken_image_72px)
                    .into(mBinding.avatarImage);
        }else{
            mBinding.avatarImage.setImageResource(R.drawable.ic_round_account_filled_72);
        }

        // Display cover
        if(!TextUtils.isEmpty(user.getCoverImage())){
            StorageReference userCoverStorageRef = mStorageRef.child(STORAGE_REF_IMAGES +"/"+ user.getKey() +"/"+ COVER_THUMBNAIL_NAME);
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


        //Set gender value
        if(TextUtils.equals(user.getGender(), USER_SPINNER_GENDER_MALE) ){
            mBinding.spinnerGenderValue.setSelection(0);
        }else{
            mBinding.spinnerGenderValue.setSelection(1);
        }

        // Set disability value
        if(user.getDisabled()){
            mBinding.spinnerDisabilityValue.setSelection(1);
        }else{
            mBinding.spinnerDisabilityValue.setSelection(0);
        }

        //Set year of birth value
        for (int year: mViewModel.getYears()) {
            if(year == user.getBirthYear()){
                mBinding.spinnerBirthValue.setSelection(mViewModel.getYears().indexOf(year));
                return;
            }
        }
    }

    private void selectMedia(final int position) {
        // Different ‎ request code for avatar than cover to know which is which in the onActivityResult
        int selectMediaRequestCode;
        if (position == 0) { // it's avatar
            selectMediaRequestCode = SELECT_AVATAR_REQUEST_CODE;
        } else {  // it's cover
            selectMediaRequestCode = SELECT_COVER_REQUEST_CODE;
        }

        //Don't enable capturing photos by camera without the permission
        if(ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
            // Enable camera
            Matisse.from(this)
                    .choose(MimeType.ofImage(), false)
                    .theme(R.style.Matisse_Dracula)
                    .countable(false)
                    .maxSelectable(1)
                    .capture(true)
                    //.captureStrategy(new CaptureStrategy(true, BuildConfig.APPLICATION_ID +".fileprovider", "Basbes"))
                    .captureStrategy(new CaptureStrategy(false, APP_AUTHORITY))
                    .showSingleMediaType(true)
                    //.addFilter(new GifSizeFilter(320, 320, 5 * Filter.K * Filter.K))
                    //.gridExpectedSize(getResources().getDimensionPixelSize(R.dimen.album_item_height))
                    .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                    .thumbnailScale(0.85f)
                    .imageEngine(new MyGlideEngine())
                    .showPreview(true) // Default is `true`
                    .autoHideToolbarOnSingleTap(true)
                    .forResult(selectMediaRequestCode);
        }else{
            // Disable camera
            Matisse.from(this)
                    .choose(MimeType.ofImage(), false)
                    .theme(R.style.Matisse_Dracula)
                    .countable(false)
                    .maxSelectable(1)
                    .showSingleMediaType(true)
                    //.addFilter(new GifSizeFilter(320, 320, 5 * Filter.K * Filter.K))
                    //.gridExpectedSize(getResources().getDimensionPixelSize(R.dimen.album_item_height))
                    .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                    .thumbnailScale(0.85f)
                    .imageEngine(new MyGlideEngine())
                    .showPreview(true) // Default is `true`
                    .autoHideToolbarOnSingleTap(true)
                    .forResult(selectMediaRequestCode);
        }
        /*Album.image(this) // Image and video mix options.
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
                .start();*/
    }

    private void cropImage(Uri mediaUri, int position) {
        Intent intent;
        if(position == 0){
            // Let's crop an avatar
            //Intent intent = CropImage.activity(Uri.fromFile(new File(mediaUri.toString())))
            intent = CropImage.activity(mediaUri) // Matisse albums users content not file url
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
            //.start(mActivityContext, this);

        }else{
            // Let's crop a cover
            //Intent intent = CropImage.activity(Uri.fromFile(new File(mediaUri.toString())))
            intent = CropImage.activity(mediaUri) // Matisse albums users content not file url
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
            //.start(mActivityContext, this);
        }

        Log.d(TAG, "cropImage starts" +mediaUri);
        //intent.putExtra(IMAGE_HOLDER_POSITION,position);
        Bundle mBundle = new Bundle();
        mBundle.putInt(IMAGE_HOLDER_POSITION,position);
        intent.putExtras(mBundle);

        this.startActivityForResult(intent, CROP_IMAGE_REQUEST_CODE);
    }

    private void compressImage(final Uri imageUri, final String type, final int position) {
        if (null != imageUri && null != imageUri.getPath()) {
            //File imageFile = new File(imageUri.getPath());
            String filePath = SiliCompressor.with(mContext).compress(imageUri.toString(), mContext.getCacheDir());
            //Uri compressedImageUri = FileProvider.getUriForFile(mActivityContext, APP_AUTHORITY, new File(filePath));
            Log.d(TAG, "compress: filePath = " +  filePath);

            if( filePath.startsWith("content://") || filePath.startsWith("file://") ) {
                Log.d(TAG, "compress: filePath starts with content or file: " +  filePath);
                uploadImage(Uri.parse(filePath), type);
            }else{
                Log.d(TAG, "compress: filePath doesn't starts with content or file" +  filePath);
                Uri compressedImageUri = Uri.fromFile(new File(filePath));
                Log.d(TAG, "compress: Uri.fromFile= " +  compressedImageUri);
                uploadImage(compressedImageUri, type);
            }

            /*Luban.get(getContext())
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
                    }).launch();    // Start compression*/
        }
    }

    private void uploadImage(Uri imageUri, final String type) {
        //Uri fileUri = Uri.fromFile(new File(imageUri.getPath()));
        StorageReference userRef ; //= mStorageRef.child("images/"+currentUserId+"avatar.jpg");

        switch (type){
            case "avatar":
                userRef = mStorageRef.child(STORAGE_REF_IMAGES +"/"+currentUserId +"/"+ AVATAR_THUMBNAIL_NAME);
                break;
            case "coverImage":
                userRef = mStorageRef.child(STORAGE_REF_IMAGES +"/"+currentUserId +"/"+ COVER_THUMBNAIL_NAME );
                break;
            case "original avatar":
                userRef = mStorageRef.child(STORAGE_REF_IMAGES +"/"+currentUserId +"/"+ AVATAR_ORIGINAL_NAME);
                break;
            case "original cover":
                userRef = mStorageRef.child(STORAGE_REF_IMAGES +"/"+currentUserId +"/"+ COVER_ORIGINAL_NAME);
                break;
            default:
                userRef = mStorageRef.child(STORAGE_REF_IMAGES +"/"+currentUserId+AVATAR_THUMBNAIL_NAME);
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
                    //System.out.println("Upload is " + progress + "% done");
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
                                mViewModel.getUser().setAvatar(String.valueOf(downloadUri));
                                //Display avatar
                                GlideApp.with(mContext)
                                        .load(mViewModel.getUser().getAvatar())
                                        //.placeholder(R.mipmap.account_circle_72dp)
                                        .placeholder(R.drawable.ic_round_account_filled_72)
                                        .error(R.drawable.ic_round_broken_image_72px)
                                        .into(mBinding.avatarImage);

                                /*mBinding.avatarImage.setImageResource(R.drawable.ic_round_account_filled_72);
                                Picasso.get()
                                        .load(mViewModel.getUser().getAvatar())
                                        .placeholder(R.mipmap.account_circle_72dp)
                                        .error(R.drawable.ic_round_broken_image_72px)
                                        .into(mBinding.avatarImage);*/
                                break;
                            case "coverImage":
                                mViewModel.getUser().setCoverImage(String.valueOf(downloadUri));
                                // Display cover
                                GlideApp.with(mContext)
                                        .load(mViewModel.getUser().getCoverImage())
                                        //.placeholder(R.mipmap.ic_picture_gallery_white_512px)
                                        .placeholder(R.drawable.ic_picture_gallery)
                                        .error(R.drawable.ic_broken_image_512px)
                                        .into(mBinding.coverImage);

                                /*mBinding.coverImage.setImageResource(R.drawable.ic_picture_gallery);
                                Picasso.get()
                                        .load(mViewModel.getUser().getCoverImage())
                                        .placeholder(R.mipmap.ic_picture_gallery_white_512px)
                                        .error(R.drawable.ic_broken_image_512px)
                                        .into(mBinding.coverImage);*/
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

        // check if name and avatar are not empty
        if (TextUtils.isEmpty(mBinding.nameValue.getText())) {
            Toast.makeText(getActivity(), R.string.empty_profile_name_error,
                    Toast.LENGTH_LONG).show();
            return;
        }

        if (TextUtils.isEmpty(mViewModel.getUser().getAvatar())) {
            Toast.makeText(getActivity(), R.string.empty_profile_avatar_error,
                    Toast.LENGTH_LONG).show();
            return;
        }

        if (!isEdit) {
            mViewModel.getUser().setCreated(ServerValue.TIMESTAMP);
        }
        mViewModel.getUser().setLastOnline(0L);
        mViewModel.getUser().setName(mBinding.nameValue.getText().toString().trim());
        mViewModel.getUser().setBirthYear(Integer.parseInt(mBinding.spinnerBirthValue.getSelectedItem().toString()));
        if (mBinding.spinnerGenderValue.getSelectedItemPosition() == 0) {
            mViewModel.getUser().setGender(USER_SPINNER_GENDER_MALE);
        } else {
            mViewModel.getUser().setGender(USER_SPINNER_GENDER_FEMALE);
        }

        if (mBinding.spinnerDisabilityValue.getSelectedItemPosition() == 0) {
            mViewModel.getUser().setDisabled(false);
        } else {
            mViewModel.getUser().setDisabled(true);
        }

        // update the database
        mUserRef.setValue(mViewModel.getUser()).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // Write was successful!
                Log.i(TAG, "mUserRef onSuccess");

                if (!isEdit) {
                    // Set user's notification tokens
                    // We have to use FirebaseInstallations because FirebaseInstanceId is deprecated
                    /*FirebaseInstanceId.getInstance().getInstanceId()
                            .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                                @Override
                                public void onComplete(@NonNull Task<InstanceIdResult> task) {
                                    if (!task.isSuccessful()) {
                                        Log.w(TAG, "getInstanceId failed", task.getException());
                                        return;
                                    }

                                    if (null != task.getResult()) {
                                        // Get new Instance ID token
                                        String token = task.getResult().getToken();
                                        //mTokensRef.child(mUserId).child(token).setValue(true);
                                        mUserRef.child(DATABASE_REF_USER_TOKENS).child(token).setValue(true);
                                    }
                                }
                            });*/
                    FirebaseMessaging.getInstance().getToken()
                            .addOnCompleteListener(new OnCompleteListener<String>() {
                                @Override
                                public void onComplete(@NonNull Task<String> task) {
                                    if (!task.isSuccessful()) {
                                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                                        return;
                                    }
                                    // Get new FCM registration token
                                    String token = task.getResult();
                                    //mTokensRef.child(mUserId).child(token).setValue(true);
                                    if (!TextUtils.isEmpty(token)) {
                                        Log.d(TAG, "getToken ="+ token);
                                        mUserRef.child(DATABASE_REF_USER_TOKENS).child(token).setValue(true);
                                    }
                                }
                            });
                }

                // Return to main fragment
                if (null != navController.getCurrentDestination() && R.id.queues != navController.getCurrentDestination().getId()) {
                    navController.navigateUp();
                }

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // Write failed
                Toast.makeText(getActivity(), R.string.update_profile_error,
                        Toast.LENGTH_LONG).show();
            }
        });


    }

    // If Storage and camera permissions are granted return true so that we stop asking for permissions
    private boolean isPermissionsGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Log.d(TAG, "is permission Granted= "+(ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED));

            return (ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED);
        }else{
            Log.d(TAG, "is permission Granted= "+(ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED));

            return (ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED);
        }

    }

    private void requestPermission() {
        // Permission is not granted
        // Should we show an explanation?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // API level 29 Android 10 and higher
            if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity, Manifest.permission.READ_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(mActivity, Manifest.permission.CAMERA)) {
                Log.i(TAG, "requestPermission: permission should show Rationale");
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                showPermissionRationaleDialog();
            } else {
                // No explanation needed; request the permission
                Log.i(TAG, "requestPermission: No explanation needed; request the permission");
                // using requestPermissions(new String[] instead of ActivityCompat.requestPermissions(this, new String[] to get onRequestPermissionsResult in the fragment
                requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE ,
                        Manifest.permission.CAMERA}, REQUEST_STORAGE_PERMISSIONS_CODE);
            }
        }else{
            if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity, Manifest.permission.READ_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(mActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(mActivity, Manifest.permission.CAMERA)) {
                Log.i(TAG, "requestPermission: permission should show Rationale");
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                showPermissionRationaleDialog();
            } else {
                // No explanation needed; request the permission
                Log.i(TAG, "requestPermission: No explanation needed; request the permission");
                // using requestPermissions(new String[] instead of ActivityCompat.requestPermissions(this, new String[] to get onRequestPermissionsResult in the fragment
                requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE ,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA}, REQUEST_STORAGE_PERMISSIONS_CODE);
            }
        }
    }

    private void showPermissionRationaleDialog() {
        CameraPermissionAlertFragment permissionRationaleDialog = CameraPermissionAlertFragment.newInstance(mContext, this);
        permissionRationaleDialog.show(mFragmentManager, PERMISSION_RATIONALE_FRAGMENT);
        Log.i(TAG, "showPermissionRationaleDialog: permission AlertFragment show clicked ");
    }

    @Override
    public void onClick(View view, int position, boolean isLongClick) {
        Log.d(TAG, "item clicked position= " + position + " View= "+view);
        if(view == null && position == 6){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // API level 29 Android 10 and higher
                // OK button of the permission dialog is clicked, lets ask for permissions
                requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE ,
                        Manifest.permission.CAMERA}, REQUEST_STORAGE_PERMISSIONS_CODE);
            }else{
                // OK button of the permission dialog is clicked, lets ask for permissions
                requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE ,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA}, REQUEST_STORAGE_PERMISSIONS_CODE);
            }

            return; // No need to check other clicks, it's the OK button of the permission dialog

        }
    }

    // Get Request Permissions Result
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult we got a permissions result");
        if (requestCode == REQUEST_STORAGE_PERMISSIONS_CODE) {
            // If request is cancelled, the result arrays are empty.
            // Camera permission is not a must, we can proceed with reading photos from gallery
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted, yay! Do the task you need to do.
                Log.i(TAG, "onRequestPermissionsResult permission was granted");
                if(mViewModel.isSelectAvatarClicked()){
                    selectMedia(0);
                }else{
                    selectMedia(1);
                }
            } else {
                // permission denied, boo! Disable the
                // functionality that depends on this permission.
                Log.i(TAG, "onRequestPermissionsResult permission denied");
            }
        }
    }
}
