package com.tabourless.queue.adapters;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.tabourless.queue.GlideApp;
import com.tabourless.queue.R;
import com.tabourless.queue.databinding.CustomerItemBinding;
import com.tabourless.queue.interfaces.ItemClickListener;
import com.tabourless.queue.models.Customer;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class CustomersAdapter extends PagedListAdapter<Customer, CustomersAdapter.ViewHolder> {

    private final static String TAG = CustomersAdapter.class.getSimpleName();

    private FirebaseUser currentFirebaseUser ;
    private String currentUserId ;
    private Context mContext ;

    private CustomerItemBinding mBinding;
    private ItemClickListener itemClickListener;
    // A not static array list for all avatars to update the broken avatars when fragment stops
    private List<Customer> brokenAvatarsList;// = new ArrayList<>();
    private StorageReference mStorageRef;

    private static final String AVATAR_THUMBNAIL_NAME = "avatar.jpg";
    private static final String COVER_THUMBNAIL_NAME = "cover.jpg";

    private static final String CUSTOMER_STATUS_WAITING = "waiting";
    private static final String CUSTOMER_STATUS_NEXT = "next";
    private static final String CUSTOMER_STATUS_FRONT = "front";
    private static final String CUSTOMER_STATUS_AWAY = "away";

    public CustomersAdapter(Context context, ItemClickListener itemClickListener) {
        super(DIFF_CALLBACK);
        // [START create_storage_reference]
        this.itemClickListener = itemClickListener;
        this.mContext = context;
        mStorageRef = FirebaseStorage.getInstance().getReference();

        currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if(currentFirebaseUser != null){
            currentUserId = currentFirebaseUser.getUid();
        }

        // Only create the list if it's null
        if(brokenAvatarsList == null){
            brokenAvatarsList = new ArrayList<>();
            Log.d(TAG, "brokenAvatarsList is null. new ArrayList is created= " + brokenAvatarsList.size());
        }

    }

    @NonNull
    @Override
    public CustomersAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        mBinding = CustomerItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(mBinding);
    }



    @Override
    public void onBindViewHolder(@NonNull final CustomersAdapter.ViewHolder holder, final int position) {

        final Customer customer = getItem(position);
        if (customer != null) {
            // customer name text value
            if (null != customer.getName()) {
                holder.mBinding.customerName.setText(customer.getName());
            }else{
                holder.mBinding.customerName.setText(null);
            }

            // Lets get avatar
            if(!TextUtils.isEmpty(customer.getAvatar())){
                StorageReference userAvatarStorageRef = mStorageRef.child("images/"+ customer.getUserId() +"/"+ AVATAR_THUMBNAIL_NAME);
                // Download directly from StorageReference using Glide
                GlideApp.with(mContext)
                        .load(userAvatarStorageRef)
                        //.placeholder(R.mipmap.account_circle_72dp)
                        .placeholder(R.drawable.ic_round_account_filled_72)
                        .error(R.drawable.ic_round_broken_image_72px)
                        .into(holder.mBinding.customerImage);
            }else{
                holder.mBinding.customerImage.setImageResource(R.drawable.ic_round_account_filled_72);
            }// end of user avatar

            // Ticket number
            if (customer.getNumber() != 0) {
                holder.mBinding.numberValue.setText(String.valueOf(customer.getNumber()));
            }else{
                holder.mBinding.numberValue.setText(null);
            }

            // Status
            if(!TextUtils.isEmpty(customer.getStatus())){
                switch (customer.getStatus()){
                    case CUSTOMER_STATUS_WAITING:
                        holder.mBinding.numberValue.setBackgroundResource(R.drawable.text_rounded_background_waiting);
                        holder.mBinding.numberValue.setTextColor(ContextCompat.getColor(mContext, R.color.material_on_primary_emphasis_high_type));
                        break;
                    case CUSTOMER_STATUS_NEXT:
                        holder.mBinding.numberValue.setBackgroundResource(R.drawable.text_rounded_background_next);
                        holder.mBinding.numberValue.setTextColor(ContextCompat.getColor(mContext, R.color.color_on_surface_emphasis_medium));
                        break;
                    case CUSTOMER_STATUS_FRONT:
                        holder.mBinding.numberValue.setBackgroundResource(R.drawable.text_rounded_background_front);
                        holder.mBinding.numberValue.setTextColor(ContextCompat.getColor(mContext, R.color.color_on_surface_emphasis_high));
                        break;
                    case CUSTOMER_STATUS_AWAY:
                        holder.mBinding.numberValue.setBackgroundResource(R.drawable.text_rounded_background_away);
                        holder.mBinding.numberValue.setTextColor(ContextCompat.getColor(mContext, R.color.material_on_surface_disabled));
                        break;
                }
            }else{
                // Status is not set, lets display away background
                holder.mBinding.numberValue.setBackgroundResource(R.drawable.text_rounded_background_away);
            }


            // Joined Time text value
            if (null != customer.getJoined()) {
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(customer.getJoinedLong());
                String joinedTime = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(c.getTime());
                holder.mBinding.joinedTimeValue.setText(joinedTime);
            }else{
                holder.mBinding.joinedTimeValue.setText(null);
            }

            // Gender icon
            if (null != customer.getGender()) {
                switch (customer.getGender()) {
                    case "male":
                        holder.mBinding.genderIcon.setImageResource(R.drawable.ic_business_man);
                        holder.mBinding.genderIcon.setVisibility(View.VISIBLE);
                        break;
                    case "female":
                        holder.mBinding.genderIcon.setImageResource(R.drawable.ic_business_woman);
                        holder.mBinding.genderIcon.setVisibility(View.VISIBLE);
                        break;
                    default:
                        holder.mBinding.genderIcon.setVisibility(View.GONE);
                        break;
                }
            }else{
                holder.mBinding.genderIcon.setVisibility(View.GONE);
            }

            // Age icon
            if (customer.getAge() != 0) {
                if(customer.getAge()<60){
                    // customer is young
                    holder.mBinding.ageIcon.setImageResource(R.drawable.ic_not_old_man_with_cane);
                    holder.mBinding.ageIcon.setVisibility(View.VISIBLE);
                }else{
                    // customer is old
                    holder.mBinding.ageIcon.setImageResource(R.drawable.ic_old_man_with_cane);
                    holder.mBinding.ageIcon.setVisibility(View.VISIBLE);
                }
            }else{
                holder.mBinding.ageIcon.setVisibility(View.GONE);
            }

            // Disability icon
            if (customer.isDisabled()) {
                holder.mBinding.disabilityIcon.setImageResource(R.drawable.ic_wheelchair_accessible);
                holder.mBinding.disabilityIcon.setVisibility(View.VISIBLE);
            }else{
                holder.mBinding.disabilityIcon.setImageResource(R.drawable.ic_fit_person_stretching_exercises);
                holder.mBinding.disabilityIcon.setVisibility(View.VISIBLE);
            }

        }

    }

    /*private void loadStorageImage(final Customer customer, final CircleImageView avatar) {
        mStorageRef.child("images/"+customer.getUserId() +"/"+ AVATAR_THUMBNAIL_NAME ).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                // Got the download URL for 'users/me/profile.png'
                Picasso.get()
                        .load(uri)
                        .placeholder(R.mipmap.account_circle_72dp)
                        .error(R.drawable.ic_round_broken_image_72px)
                        .into(avatar);
                //updateAvatarUri(key, uri);
                // Add updated notification to brokenAvatarsList. the list will be used to update broken avatars when fragment stops
                customer.setAvatar(String.valueOf(uri));
                //customer.setKey(key); // We use name as to store the chatId value. it's bad for logic but it's a quick fix
                brokenAvatarsList.add(customer);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
                avatar.setImageResource(R.drawable.ic_round_account_filled_72);
            }
        });

    }*/


    /*public List<Customer> getBrokenAvatarsList(){
        return brokenAvatarsList;
    }

    // clear sent messages list after updating the database
    public void clearBrokenAvatarsList(){
        brokenAvatarsList.clear();
    }*/

    // CALLBACK to calculate the difference between the old item and the new item
    private static final DiffUtil.ItemCallback<Customer> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Customer>() {
                // User details may have changed if reloaded from the database,
                // but ID is fixed.
                // if the two items are the same
                @Override
                public boolean areItemsTheSame(Customer oldCustomer, Customer newCustomer) {

                    //Log.d(TAG, " DIFF_CALLBACK areItemsTheSame " + newChat);
                    //Log.d(TAG, " DIFF_CALLBACK areItemsTheSame keys= old: " + oldChat.getLastMessage() +" new: "+ oldChat.getLastMessage());
                    return oldCustomer.getKey().equals(newCustomer.getKey());
                    //return oldChat.getLastSentLong() == (newChat.getLastSentLong());
                    //return true;
                }

                // if the content of two items is the same
                @Override
                public boolean areContentsTheSame(Customer oldCustomer, Customer newCustomer) {

                   /* Log.d(TAG, "  DIFF_CALLBACK areContentsTheSame object " + (oldUser.equals(newUser)));
                    Log.d(TAG, "  DIFF_CALLBACK areContentsTheSame Names() " + (oldUser.getName().equals(newUser.getName())));
                    Log.d(TAG, "  DIFF_CALLBACK areContentsTheSame old name: " + oldUser.getName() + " new name: "+newUser.getName());
*/
                    // NOTE: if you use equals, your object must properly override Object#equals()
                    // Incorrectly returning false here will result in too many animations.
                    /*Log.d(TAG, " messages query DIFF_CALLBACK areContentsTheSame old name: " + oldChat.getLastMessage() + " new name: "+newChat.getLastMessage()+ " value= "+(oldChat.getLastMessage().equals(newChat.getLastMessage())
                            && (oldChat.getLastSentLong() == newChat.getLastSentLong())));*/

                    // compare old and new chat's sent time and last messages
                    /*return (oldChat.getLastMessage().equals(newChat.getLastMessage())
                            && (oldChat.getLastSentLong() == newChat.getLastSentLong()));*/
                    //Log.d(TAG, "messages query DIFF_CALLBACK areContentsTheSame old name: " + oldChat.getLastMessage() + " new name: "+newChat.getLastMessage()+ " value= "+(oldChat.equals(newChat)));
                    return oldCustomer.equals(newCustomer);
                    //return oldChat.getLastMessage().equals(newChat.getLastMessage());
                    //return false;
                }
            };


   /* @Override
    public void submitList(PagedList<Message> pagedList) {
        super.submitList(pagedList);
    }

    @Override
    public void onCurrentListChanged(@Nullable PagedList<Message> currentList) {
        super.onCurrentListChanged(currentList);
    }*/

    @Nullable
    @Override
    public Customer getItem(int position) {
        return super.getItem(position);
    }

    /// ViewHolder for ReceivedMessages list /////
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        private CustomerItemBinding mBinding;

        public ViewHolder(CustomerItemBinding binding) {
            super(binding.getRoot());
            this.mBinding = binding;

            mBinding.getRoot().setOnClickListener(this);
            mBinding.customerImage.setOnClickListener(this);
        }


        @Override
        public void onClick(View view) {
            if(itemClickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION){
                itemClickListener.onClick(view, getAdapterPosition(), false);
            }
        }
    }

}

