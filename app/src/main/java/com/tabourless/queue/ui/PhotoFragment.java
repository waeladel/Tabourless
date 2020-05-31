package com.tabourless.queue.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.tabourless.queue.GlideApp;
import com.tabourless.queue.R;
import com.tabourless.queue.databinding.FragmentPhotoBinding;
import com.yanzhenjie.album.widget.photoview.PhotoViewAttacher;


/**
 * A simple {@link Fragment} subclass.
 */
public class PhotoFragment extends Fragment {

    private final static String TAG = PhotoFragment.class.getSimpleName();
    private String  mUserId, mImageName;
    private StorageReference mStorageRef,mImagesRef, mUserRef ;
    private Context mContext;
    private PhotoViewAttacher mAttacher;
    private FragmentPhotoBinding mBinding;
    private RequestListener mRequestListener;


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // [START create_storage_reference]
        mStorageRef = FirebaseStorage.getInstance().getReference();
        // A listener for finish loading, to stop progress animation and start the zoom attacher
        mRequestListener = new RequestListener() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target target, boolean isFirstResource) {
                // Handle any errors
                mBinding.loadingAnimation.setVisibility(View.GONE);
                Toast.makeText(mContext, R.string.download_image_error,
                        Toast.LENGTH_LONG).show();
                return false;
            }

            @Override
            public boolean onResourceReady(Object resource, Object model, Target target, DataSource dataSource, boolean isFirstResource) {
                //do Photo Attacher when picture is loaded successfully
                mBinding.loadingAnimation.setVisibility(View.GONE);
                if(mAttacher!=null){
                    mAttacher.update();
                }else{
                    mAttacher = new PhotoViewAttacher(mBinding.zoomedImage);
                }
                return false;
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mBinding = FragmentPhotoBinding.inflate(inflater, container, false);
        View view = mBinding.getRoot();

        if(null  != getArguments() && getArguments().containsKey("userId") && getArguments().containsKey("imageName")) {
            mUserId = PhotoFragmentArgs.fromBundle(getArguments()).getUserId(); // any user
            mImageName = PhotoFragmentArgs.fromBundle(getArguments()).getImageName();
            Log.d(TAG, "mCurrentUserId= " + mUserId + " mImageName= "+ mImageName);

            // Lets get avatar
            StorageReference userStorageRef = mStorageRef.child("images/"+ mUserId +"/"+ mImageName);
            // Download directly from StorageReference using Glide
            GlideApp.with(mContext)
                    .load(userStorageRef)
                    //.placeholder(R.mipmap.account_circle_72dp)
                    .placeholder(R.drawable.ic_picture_gallery)
                    .error(R.drawable.ic_broken_image_512px)
                    .listener(mRequestListener)
                    .into(mBinding.zoomedImage);

            /*mStorageRef.child("images/"+mUserId +"/"+ mImageName).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    // Got the download URL for 'users/me/profile.png'
                    Picasso.get()
                            .load(uri)
                            .placeholder(R.mipmap.ic_picture_gallery_white_512px)
                            .error(R.drawable.ic_broken_image_512px)
                            .into(mBinding.zoomedImage, new com.squareup.picasso.Callback() {
                                @Override
                                public void onSuccess() {
                                    //do Photo Attacher when picture is loaded successfully
                                    mBinding.loadingAnimation.setVisibility(View.GONE);
                                    if(mAttacher!=null){
                                        mAttacher.update();
                                    }else{
                                        mAttacher = new PhotoViewAttacher(mBinding.zoomedImage);
                                    }
                                }
                                @Override
                                public void onError(Exception e) {
                                    mBinding.loadingAnimation.setVisibility(View.GONE);
                                    Toast.makeText(mContext, R.string.download_image_error,
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle any errors
                    mBinding.loadingAnimation.setVisibility(View.GONE);
                    Toast.makeText(mContext, R.string.download_image_error,
                            Toast.LENGTH_LONG).show();
                }
            });*/

        }

        return view;
    }
}
