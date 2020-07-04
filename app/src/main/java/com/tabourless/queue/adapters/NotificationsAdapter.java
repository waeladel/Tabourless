package com.tabourless.queue.adapters;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.tabourless.queue.R;
import com.tabourless.queue.databinding.NotificationItemBinding;
import com.tabourless.queue.interfaces.ItemClickListener;
import com.tabourless.queue.models.DatabaseNotification;
import com.tabourless.queue.models.UserQueue;

import java.util.List;

import static android.graphics.Typeface.BOLD;
import static com.tabourless.queue.App.CUSTOMER_STATUS_AWAY;
import static com.tabourless.queue.App.CUSTOMER_STATUS_FRONT;
import static com.tabourless.queue.App.CUSTOMER_STATUS_NEXT;

public class NotificationsAdapter extends PagedListAdapter<DatabaseNotification, NotificationsAdapter.ViewHolder> {

    private final static String TAG = NotificationsAdapter.class.getSimpleName();

    private NotificationItemBinding mBinding;
    private ItemClickListener itemClickListener;

    public Context context;

    private static final String AVATAR_ORIGINAL_NAME = "original_avatar.jpg";
    private static final String COVER_ORIGINAL_NAME = "original_cover.jpg";
    private static final String AVATAR_THUMBNAIL_NAME = "avatar.jpg";
    private static final String COVER_THUMBNAIL_NAME = "cover.jpg";
    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;

    private static final String NOTIFICATION_TYPE_MESSAGE = "message";
    private static final String NOTIFICATION_TYPE_QUEUE_FRONT = "front"; // to inform the user about his or her position
    private static final String NOTIFICATION_TYPE_QUEUE_NEXT= "next"; // to inform the user about his or her position


    // A not static array list for all notifications sender avatar to update the broken avatars when fragment stops
    private List<DatabaseNotification> brokenAvatarsList;// = new ArrayList<>();

    public NotificationsAdapter( Context context, ItemClickListener itemClickListener) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.itemClickListener = itemClickListener;
    }

    @NonNull
    @Override
    public NotificationsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        mBinding = NotificationItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(mBinding);
    }



    @Override
    public void onBindViewHolder(@NonNull final NotificationsAdapter.ViewHolder holder, final int position) {

        final DatabaseNotification notification = getItem(position);
        if (notification != null) {
            // Notification body value
            if (null != notification.getType()) {
                switch (notification.getType()){
                    case NOTIFICATION_TYPE_QUEUE_FRONT:
                        if(!TextUtils.isEmpty(notification.getCounterName()) || !TextUtils.isEmpty(notification.getQueueName())){
                            String queueName = notification.getQueueName();
                            String counterName = notification.getCounterName();
                            String wholeText = context.getString(R.string.notification_item_queue_body_front, queueName, counterName);
                            Log.i(TAG, "getSenderName= "+notification.getSenderName());
                            setTextWithSpan(holder.mBinding.notificationText, wholeText, queueName, counterName, new StyleSpan(BOLD));
                        }else{
                            holder.mBinding.notificationText.setText(R.string.notification_default_queue_body_front);
                        }
                        // switch icons according to notification type
                        //holder.mIcon.setImageResource(R.drawable.ic_circle_favorite_24);
                        break;
                    case NOTIFICATION_TYPE_QUEUE_NEXT:
                        if(!TextUtils.isEmpty(notification.getQueueName())){
                            String queueName = notification.getQueueName();
                            String wholeText = context.getString(R.string.notification_item_queue_body_next, queueName);
                            Log.i(TAG, "getSenderName= "+notification.getSenderName());
                            setTextWithSpan(holder.mBinding.notificationText, wholeText, queueName, new StyleSpan(BOLD));
                        }else{
                            holder.mBinding.notificationText.setText(R.string.notification_default_queue_body_next);
                        }
                        // switch icons according to notification type
                        //holder.mIcon.setImageResource(R.drawable.ic_circle_favorite_24);
                        break;
                    case NOTIFICATION_TYPE_MESSAGE:
                        if(null != notification.getSenderName()){
                            String name = notification.getSenderName();
                            String wholeText = (context.getString(R.string.notification_message_body, name));
                            setTextWithSpan(holder.mBinding.notificationText, wholeText, name, new StyleSpan(BOLD));
                        }else{
                            holder.mBinding.notificationText.setText(R.string.notification_default_message_body);
                        }
                        // switch icons according to notification type
                        //holder.mIcon.setImageResource(R.drawable.ic_circle_chat_24);
                        break;
                }
            }else{
                holder.mBinding.notificationText.setText(null);

                // Use default notification icon
                //holder.mIcon.setImageResource(R.drawable.ic_circle_notification_24);
            }

            // LastSentTime text value
            if (null != notification.getSent()) {
                long now = System.currentTimeMillis();
                CharSequence ago =
                        DateUtils.getRelativeTimeSpanString(notification.getSentLong(), now, DateUtils.MINUTE_IN_MILLIS);
                holder.mBinding.sentTime.setText(ago);
            }else{
                holder.mBinding.sentTime.setText(null);
            }

            // Display user number
            if (notification.getNumber() != 0) {
                holder.mBinding.numberValue.setText(String.valueOf(notification.getNumber()));
            }else{
                holder.mBinding.numberValue.setText(null);
            }

            // Status
            if(!TextUtils.isEmpty(notification.getType())){
                switch (notification.getType()){
                    case CUSTOMER_STATUS_NEXT:
                        holder.mBinding.numberValue.setBackgroundResource(R.drawable.text_rounded_background_next);
                        //holder.mBinding.numberValue.setTextColor(ContextCompat.getColor(mContext, R.color.color_on_surface_emphasis_medium));
                        holder.mBinding.numberValue.setTextColor(R.drawable.my_color_on_surface_emphasis_medium_trype);
                        break;
                    case CUSTOMER_STATUS_FRONT:
                        holder.mBinding.numberValue.setBackgroundResource(R.drawable.text_rounded_background_front);
                        holder.mBinding.numberValue.setTextColor(R.drawable.my_color_on_surface_emphasis_medium_trype);
                        break;
                    case CUSTOMER_STATUS_AWAY:
                        holder.mBinding.numberValue.setBackgroundResource(R.drawable.text_rounded_background_away);
                        holder.mBinding.numberValue.setTextColor(R.drawable.my_color_on_surface_emphasis_disabled_trype);
                        break;
                    default:
                        // default is waiting
                        holder.mBinding.numberValue.setBackgroundResource(R.drawable.text_rounded_background_waiting);
                        holder.mBinding.numberValue.setTextColor(R.drawable.my_color_on_surface_emphasis_high_type);
                        break;
                }
            }else{
                // Status is not set, lets display away background
                holder.mBinding.numberValue.setBackgroundResource(R.drawable.text_rounded_background_away);
            }



            // background color
            if (notification.isClicked()) {
                // If item was clicked, normal background
                holder.mBinding.getRoot().setBackgroundColor(android.R.attr.colorBackground);
            }else{
                // item is not clicked, display colored background
                //holder.row.setBackgroundColor(App.getContext().getResources().getColor(R.color.transparent_read_items));
                holder.mBinding.getRoot().setBackgroundResource(R.color.color_highlighted_item);
            }

         }

    }


    /*private void updateAvatarUri(String key, Uri uri) {
        if(mNotificationsRef != null){
            //DatabaseReference mDatabaseRef = FirebaseDatabase.getInstance().getReference();
            //mNotificationsRef = mDatabaseRef.child("notifications").child("alerts").child(currentUserId);
            mNotificationsRef.child(key).child("senderAvatar").setValue(String.valueOf(uri)); // update senderAvatar to the new uri
        }

    }*/

    private void setTextWithSpan(TextView textView, String wholeText, String QueueSpanText, String counterSpanText, StyleSpan style) {

        SpannableStringBuilder sb = new SpannableStringBuilder(wholeText);
        //int color = ContextCompat.getColor(context.getContext(), R.drawable.my_color_on_surface_emphasis_high_type);//fragment.getResources().getColor(R.drawable.my_color_on_surface_emphasis_high_type);
        int start1 = wholeText.indexOf(QueueSpanText);
        int end1 = start1 + QueueSpanText.length();
        sb.setSpan(new StyleSpan(BOLD), start1, end1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        //sb.setSpan(new ForegroundColorSpan(color), start1, end1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        int start2 = wholeText.indexOf(counterSpanText);
        int end2 = start2 + QueueSpanText.length();
        sb.setSpan(new StyleSpan(BOLD), start2, end2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        //sb.setSpan(new ForegroundColorSpan(color), start2, end2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        textView.setText(sb);
    }

    private void setTextWithSpan(TextView textView, String wholeText, String spanText, StyleSpan style) {

        SpannableStringBuilder sb = new SpannableStringBuilder(wholeText);
        int start = wholeText.indexOf(spanText);
        int end = start + spanText.length();
        sb.setSpan(new StyleSpan(BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        //int color = context.getResources().getColor(R.color.color_on_surface_emphasis_high);
        //sb.setSpan(new ForegroundColorSpan(color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(sb);
    }

    // CALLBACK to calculate the difference between the old item and the new item
    private static final DiffUtil.ItemCallback<DatabaseNotification> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<DatabaseNotification>() {
                // User details may have changed if reloaded from the database,
                // but ID is fixed.
                // if the two items are the same
                @Override
                public boolean areItemsTheSame(DatabaseNotification oldItem, DatabaseNotification newItem) {

                    //Log.d(TAG, " DIFF_CALLBACK areItemsTheSame " + newChat);
                    Log.d(TAG, " DIFF_CALLBACK areItemsTheSame keys= old: " + oldItem.getSenderName() +" new: "+ newItem.getSenderName()+ " value= "+oldItem.getKey().equals(newItem.getKey()));
                    return oldItem.getKey().equals(newItem.getKey());
                    //return oldChat.getLastSentLong() == (newChat.getLastSentLong());
                    //return true;
                }

                // if the content of two items is the same
                @Override
                public boolean areContentsTheSame(DatabaseNotification oldItem, DatabaseNotification newItem) {

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
                    Log.d(TAG, "notifications query DIFF_CALLBACK areContentsTheSame old name: " + oldItem.getSenderName() + " new name: "+newItem.getSenderName()+ " value= "+(oldItem.equals(newItem)));
                    return oldItem.equals(newItem);
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
    public DatabaseNotification getItem(int position) {
        return super.getItem(position);
    }


        /// ViewHolder for ReceivedMessages list /////
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        private NotificationItemBinding mBinding;


        public ViewHolder(NotificationItemBinding binding) {
            super(binding.getRoot());
            this.mBinding = binding;
            mBinding.getRoot().setOnClickListener(this);
        }


        @Override
        public void onClick(View view) {
            if(itemClickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION){
                itemClickListener.onClick(view, getAdapterPosition(), false);
            }

            /*if(getAdapterPosition() != RecyclerView.NO_POSITION){
                Log.i(TAG, "user row clicked= "+view.getId()+ " Position= "+ getAdapterPosition());
                // get clicked notification
                DatabaseNotification notification = getItem(getAdapterPosition());
                if(notification != null){
                    notification.setClicked(true); // set clicked notification to true
                    mNotificationsRef.child(notification.getKey()).child("clicked").setValue(true);// update clicked field on database

                    // to open customers when notification type is not a message
                    NavDirections customersDirection = NotificationsFragmentDirections.actionNotificationsFragToProfileFrag(notification.getSenderId());
                    // to open chat room when notification type is a message
                    NavDirections MessageDirection = NotificationsFragmentDirections.actionNotificationsFragToMessagesFrag(notification.getChatId(), notification.getSenderId(), false);

                    if (null != notification.getType()) {
                        switch (notification.getType()){
                            case NOTIFICATION_TYPE_QUEUE_FRONT:
                                Navigation.findNavController(view).navigate(customersDirection);
                                break;
                            case NOTIFICATION_TYPE_QUEUE_NEXT:
                                Navigation.findNavController(view).navigate(customersDirection);
                                break;
                            case NOTIFICATION_TYPE_MESSAGE:
                                Navigation.findNavController(view).navigate(MessageDirection);
                                break;
                            default:
                                Navigation.findNavController(view).navigate(customersDirection);
                                break;
                        }

                    }
                }

            }*/

        }

    }

}

