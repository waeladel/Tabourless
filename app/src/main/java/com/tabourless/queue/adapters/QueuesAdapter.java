package com.tabourless.queue.adapters;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.tabourless.queue.R;
import com.tabourless.queue.databinding.QueueItemBinding;
import com.tabourless.queue.interfaces.ItemClickListener;
import com.tabourless.queue.models.UserQueue;
import java.text.DateFormat;
import java.util.Calendar;

import static com.tabourless.queue.App.CUSTOMER_STATUS_AWAY;
import static com.tabourless.queue.App.CUSTOMER_STATUS_FRONT;
import static com.tabourless.queue.App.CUSTOMER_STATUS_NEXT;

public class QueuesAdapter extends PagedListAdapter<UserQueue, QueuesAdapter.ViewHolder> {

    private final static String TAG = QueuesAdapter.class.getSimpleName();

    private FirebaseUser currentFirebaseUser ;
    private String currentUserId ;

    private QueueItemBinding mBinding;
    private ItemClickListener itemClickListener;

    public QueuesAdapter(ItemClickListener itemClickListener) {
        super(DIFF_CALLBACK);
        // [START create_storage_reference]
        this.itemClickListener = itemClickListener;

        currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if(currentFirebaseUser != null){
            currentUserId = currentFirebaseUser.getUid();
        }

    }

    @NonNull
    @Override
    public QueuesAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        mBinding = QueueItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(mBinding);
    }



    @Override
    public void onBindViewHolder(@NonNull final QueuesAdapter.ViewHolder holder, final int position) {

        final UserQueue userQueue = getItem(position);
        if (userQueue != null) {
            // Queue name text value
            if (!TextUtils.isEmpty(userQueue.getName())) {
                holder.mBinding.queueName.setText(userQueue.getName());
            }else{
                holder.mBinding.queueName.setText(null);
            }

            // Place name text value
            if (!TextUtils.isEmpty(userQueue.getName())) {
                holder.mBinding.placeName.setText(userQueue.getPlaceName());
            }else{
                holder.mBinding.placeName.setText(null);
            }

            // customer's number
            if (userQueue.getNumber() != 0) {
                holder.mBinding.numberValue.setText(String.valueOf(userQueue.getNumber()));
                holder.mBinding.numberValue.setTextColor(R.drawable.my_color_on_surface_emphasis_high_type);
            }else{
                holder.mBinding.placeName.setText(null);
            }


            // Joined Time text value
            Log.d(TAG, "onBindViewHolder: ");
            if (null != userQueue.getJoined()) {
                if(userQueue.getJoinedLong() != 0) {
                    Calendar c = Calendar.getInstance();
                    c.setTimeInMillis(userQueue.getJoinedLong());
                    String joinedTime = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(c.getTime());
                    holder.mBinding.joinedTimeValue.setText(joinedTime);
                    // check the user statues in the queue

                    switch (userQueue.getStatus()){
                        case CUSTOMER_STATUS_NEXT:
                            holder.mBinding.numberValue.setBackgroundResource(R.drawable.text_rounded_background_next);
                            //holder.mBinding.numberValue.setTextColor(ContextCompat.getColor(mContext, R.color.color_on_surface_emphasis_medium));
                            holder.mBinding.numberValue.setTextColor(R.drawable.my_color_on_surface_emphasis_medium_type);
                            break;
                        case CUSTOMER_STATUS_FRONT:
                            holder.mBinding.numberValue.setBackgroundResource(R.drawable.text_rounded_background_front);
                            holder.mBinding.numberValue.setTextColor(R.drawable.my_color_on_surface_emphasis_medium_type);
                            break;
                        case CUSTOMER_STATUS_AWAY:
                            holder.mBinding.numberValue.setBackgroundResource(R.drawable.text_rounded_background_away);
                            holder.mBinding.numberValue.setTextColor(R.drawable.my_color_on_surface_emphasis_disabled_type);
                            break;
                        default:
                            // default is waiting
                            holder.mBinding.numberValue.setBackgroundResource(R.drawable.text_rounded_background_waiting);
                            holder.mBinding.numberValue.setTextColor(R.drawable.my_color_on_surface_emphasis_high_type);
                            break;
                    }

                }else{
                    // booking ended
                    holder.mBinding.joinedTimeValue.setText(R.string.joined_time_ended);
                    holder.mBinding.numberValue.setBackgroundResource(R.drawable.text_rounded_background_away);
                    holder.mBinding.numberValue.setTextColor(R.drawable.my_color_on_surface_emphasis_disabled_type);
                }
            }else{
                holder.mBinding.joinedTimeValue.setText(null);
            }
        }
    }

    // CALLBACK to calculate the difference between the old item and the new item
    private static final DiffUtil.ItemCallback<UserQueue> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<UserQueue>() {
                // User details may have changed if reloaded from the database,
                // but ID is fixed.
                // if the two items are the same
                @Override
                public boolean areItemsTheSame(UserQueue oldUserQueue, UserQueue newUserQueue) {

                    //Log.d(TAG, " DIFF_CALLBACK areItemsTheSame " + newChat);
                    //Log.d(TAG, " DIFF_CALLBACK areItemsTheSame keys= old: " + oldChat.getLastMessage() +" new: "+ oldChat.getLastMessage());
                    return oldUserQueue.getKey().equals(newUserQueue.getKey());
                    //return oldChat.getLastSentLong() == (newChat.getLastSentLong());
                    //return true;
                }

                // if the content of two items is the same
                @Override
                public boolean areContentsTheSame(UserQueue oldUserQueue, UserQueue newUserQueue) {

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
                    return oldUserQueue.equals(newUserQueue);
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
    public UserQueue getItem(int position) {
        return super.getItem(position);
    }

    /// ViewHolder for ReceivedMessages list /////
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        private QueueItemBinding mBinding;

        public ViewHolder(QueueItemBinding binding) {
            super(binding.getRoot());
            this.mBinding = binding;

            mBinding.getRoot().setOnClickListener(this);
        }


        @Override
        public void onClick(View view) {
            if(itemClickListener != null && getBindingAdapterPosition() != RecyclerView.NO_POSITION){
                itemClickListener.onClick(view, getBindingAdapterPosition(), false);
            }
        }
    }

}

