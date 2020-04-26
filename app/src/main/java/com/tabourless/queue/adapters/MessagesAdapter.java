package com.tabourless.queue.adapters;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.paging.PagedList;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;
import com.tabourless.queue.R;
import com.tabourless.queue.databinding.MessageSentItemBinding;
import com.tabourless.queue.interfaces.ItemClickListener;
import com.tabourless.queue.models.Chat;
import com.tabourless.queue.models.Message;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessagesAdapter extends PagedListAdapter<Message, RecyclerView.ViewHolder> {

    private final static String TAG = MessagesAdapter.class.getSimpleName();
    private FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    private String currentUserId = currentUser != null ? currentUser.getUid() : null;

    private MessageSentItemBinding mSentBinding;

    private static final String AVATAR_THUMBNAIL_NAME = "avatar.jpg";
    private static final String COVER_THUMBNAIL_NAME = "cover.jpg";
    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;

    private static final String Message_STATUS_SENDING = "Sending";
    private static final String Message_STATUS_SENT = "Sent";
    private static final String Message_STATUS_DELIVERED = "Delivered";

    private StorageReference mStorageRef;

    // [START declare_database_ref]
    private DatabaseReference mDatabaseRef;
    private DatabaseReference mMessagesRef;

    private String chatKey; // the chat key
    private Chat chat; // the chat object

    // A static array list for all messages status to update the database when fragment stops
    private static List<Message> totalStatusList;// = new ArrayList<>();
    //private PagedList<Message> itemsList;

    // A not static for all notifications sender avatar to update the broken avatars when fragment stops
    private List<Message> brokenAvatarsList;// = new ArrayList<>();

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public MessagesAdapter() {
        super(DIFF_CALLBACK);
        // [START create_storage_reference]
        mStorageRef = FirebaseStorage.getInstance().getReference();

        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        // use received chatKey to create a database ref
        mMessagesRef = mDatabaseRef.child("messages");

        // Only create the static list if it's null
        if(totalStatusList == null){
            totalStatusList = new ArrayList<>();
            Log.d(TAG, "totalStatusList is null. new ArrayList is created= " + totalStatusList.size());
        }else{
            Log.d(TAG, "totalStatusList is not null. size=  "+ totalStatusList.size());
            if(totalStatusList.size() >0){
                // Clear the list to start all over
                totalStatusList.clear();
                Log.d(TAG, "totalStatusList is cleared. size=  "+ totalStatusList.size());
            }
        }

        // Only create the static list if it's null
        if(brokenAvatarsList == null){
            brokenAvatarsList = new ArrayList<>();
            Log.d(TAG, "brokenAvatarsList is null. new ArrayList is created= " + brokenAvatarsList.size());
        }/*else{
            Log.d(TAG, "brokenAvatarsList is not null. size=  "+ brokenAvatarsList.size());
            if(brokenAvatarsList.size() >0){
                // Clear the list to start all over
                brokenAvatarsList.clear();
                Log.d(TAG, "brokenAvatarsList is cleared. size=  "+ brokenAvatarsList.size());
            }
        }*/

    }

    public List<Message> getStatusList(){
        return totalStatusList;
    }

    public void addSentToStatusList(final Message message){
        Log.d(TAG, "addSentToStatusList ... message is sent= "+message.getStatus());
         totalStatusList.add(message);

         /*if(null != getCurrentList()){
             for (Message submittedMessageItem : getCurrentList()) {
                 Log.d(TAG, "addSentToStatusList.  message= "+submittedMessageItem.getMessage() + " key= "+submittedMessageItem.getKey() +" isSent= "+ submittedMessageItem.getSent());
             }
         }
            itemsList = getCurrentList();
            for (Message sentMessageItem : totalStatusList) {
            int Position = updateSentItemStatus(sentMessageItem.getKey(), itemsList);
            if (Position != -2){
                itemsList.snapshot().get(Position).setSent(true);
            }*/
            //Log.d(TAG, "submitList. Position= "+ Position +" message= "+sentMessageItem.getMessage() + " key= "+sentMessageItem.getKey());
             //notifyItemRangeChanged(itemsList.size()-5, 10);
            //submitList(itemsList);
            //itemsList.snapshot().get(position).setSent(true);
            //notifyItemInserted(position);

        // Delay notifyItemChanged for 2 seconds until DIFF_CALLBACK is finished
        executor.schedule(new Runnable() {
            @Override
            public void run() {
                // Set message to sent and get it's position
                if(null != getCurrentList()){
                    int position = updateSentItemStatus(message.getKey(), getCurrentList());
                    if(position != -2){
                        notifyItemChanged(position);
                        Log.d(TAG, "addSentToStatusList. notifyItemChanged position = "+position);
                    }
                }
            }
        }, 2, TimeUnit.SECONDS);

            //submitList(itemsList);
            //notifyDataSetChanged();
    }

    // clear sent messages list after updating the database
    public void clearStatusList(){
        totalStatusList.clear();
    }

    // Get the position of the new sent message to notify the adapter
    private int updateSentItemStatus(String key, PagedList<Message> itemsList ){

        int Position = 0;

        if(itemsList != null){
            Log.d(TAG, "Getting updateSentItemStatus. itemsList size= "+itemsList.size());
            List<Message> messages = itemsList.snapshot();
            // Loop throw all messages array list to get the position of new sent message
            for (Message messageItem : messages) {
                if(messageItem.getKey().equals(key)){
                    if(TextUtils.equals(messageItem.getStatus(), Message_STATUS_DELIVERED)){
                        return -2;
                    }else{
                        Log.d(TAG, "updateSentItemStatus: key= "+ key+ " message= "+ messageItem.getMessage()+ " status= "+ messageItem.getStatus()+" Position= " +Position);
                        messageItem.setStatus(Message_STATUS_SENT); // set message to sent because it's was sent successfully
                    }
                    return Position;
                }else{
                    Position++;
                }
            }
            Log.d(TAG, "updateSentItemStatus. messageItem not found ");
            return -2;
        }
        Log.d(TAG, "updateSentItemStatus. itemsList is null= ");
        return -2;
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view;
        switch (viewType){
            case VIEW_TYPE_MESSAGE_SENT:
                // // If the current user is the sender of the message;
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_sent_item , parent, false);
                //mSentBinding = MessageSentItemBinding.inflate(LayoutInflater.from(parent.getContext()).inflate(R.layout.message_sent_item , parent, false));
                //mSentBinding = MessageSentItemBinding.bind(view);
                return new SentMessageHolder(view);
            default:
                //// If some other user sent the message
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_received_item, parent, false);
                return new ReceivedMessageHolder(view);

        }
        // default
        /*view = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_received_item, parent, false);
        return new SentMessageHolder(view);*/
    }



    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position) {

        final Message message = getItem(position);

        if (holder instanceof ReceivedMessageHolder){
            final ReceivedMessageHolder ReceivedHolder = (ReceivedMessageHolder) holder;

            if (message != null) {
                //holder.bindTo(user);
                //Log.d(TAG, "mama  onBindViewHolder. users key"  +  user.getCreatedLong()+ "name: "+user.getName());
                // click listener using interface
                // user name text value
                if (null != message.getMessage()) {
                    // Message can't be empty because scratch view width will crash the app if it's width is < 0
                    if(!message.getMessage().isEmpty()){
                        // Only display message text if it's not empty
                        ReceivedHolder.mMessage.setText(message.getMessage());
                    }else{
                        // if message is empty we must display empty space to have a width for scratch view
                        ReceivedHolder.mMessage.setText(" ");
                    }
                    //ReceivedHolder.mScratch.setText(message.getMessage()+ message.getKey());
                }else{
                    // if message is null (not set) we must display empty space to have a width for scratch view
                    ReceivedHolder.mMessage.setText(" ");
                    //ReceivedHolder.mScratch.setText(null);
                }

                if (null != message.getSenderId()) {
                    // [START create_storage_reference]
                    //ReceivedHolder.mAvatar.setImageResource(R.drawable.ic_user_account_grey_white);
                    //mStorageRef.child("images/"+message.getSenderId()+"/"+ AVATAR_THUMBNAIL_NAME).getFile()
                    Picasso.get()
                            .load(message.getSenderAvatar())
                            .placeholder(R.mipmap.ic_round_account_filled_72)
                            .error(R.drawable.ic_round_broken_image_72px)
                            .into(ReceivedHolder.mAvatar , new com.squareup.picasso.Callback() {
                                @Override
                                public void onSuccess() {
                                    // loading avatar succeeded, do nothing
                                }
                                @Override
                                public void onError(Exception e) {
                                    // loading avatar failed, lets try to get the avatar from storage instead of database link
                                    loadStorageImage(message, ReceivedHolder.mAvatar);
                                }
                            });
                }else{
                    // Handle if getSenderId() is null
                    ReceivedHolder.mAvatar.setImageResource(R.drawable.ic_round_account_filled_72);
                }

                if (null != message.getCreated()) {
                    Calendar c = Calendar.getInstance();
                    c.setTimeInMillis(message.getCreatedLong());
                    String sentTime = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(c.getTime());
                    ReceivedHolder.mSentTime.setText(sentTime);
                }else{
                    ReceivedHolder.mSentTime.setText(null);
                }

                /*ReceivedHolder.setItemClickListener(new ItemClickListener(){
                    @Override
                    public void onClick(View view, int position, boolean isLongClick) {
                        if(isLongClick){
                            Log.d(TAG, "Action ReceivedHolder  setItemClickListener"+ message.getKey());
                            ReceivedHolder.mResetView.setVisibility(View.GONE);

                        }
                    }
                });*/

            }
        }

        if (holder instanceof SentMessageHolder){
            SentMessageHolder SentHolder = (SentMessageHolder) holder;

            if (message != null) {
                //holder.bindTo(user);
                //Log.d(TAG, "mama  onBindViewHolder. users key"  +  user.getCreatedLong()+ "name: "+user.getName());
                // click listener using interface
                // user name text value
                if (null != message.getMessage()) {
                    SentHolder.mMessage.setText(message.getMessage());
                    //SentHolder.mMessage.setText(message.getMessage()+ message.getKey());
                }else{
                    SentHolder.mMessage.setText(null);
                }

                if (null != message.getCreated()) {
                    Calendar c = Calendar.getInstance();
                    c.setTimeInMillis(message.getCreatedLong());
                    String sentTime = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(c.getTime());
                    SentHolder.mSentTime.setText(sentTime);
                }else{
                    SentHolder.mSentTime.setText(null);
                }

                // update sent icon according to message's sent boolean
                Log.d(TAG, " message status ="+ message.getStatus());
                // if message is sent show send icon
                if(null != message.getStatus() && message.getStatus().equals(Message_STATUS_SENT)){
                    // Show seen sent icon
                    SentHolder.mSentIcon.setImageResource(R.drawable.ic_sent_message_thick);
                }else{
                    // Show sending sent icon
                    SentHolder.mSentIcon.setImageResource(R.drawable.ic_sending_message_thick);
                }
            }
        }

    }

    private void loadStorageImage(final Message message, final CircleImageView avatar) {

        mStorageRef.child("images/"+message.getSenderId() +"/"+ AVATAR_THUMBNAIL_NAME ).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                // Got the download URL for 'users/me/profile.png'
                Picasso.get()
                        .load(uri)
                        .placeholder(R.mipmap.ic_round_account_filled_72)
                        .error(R.drawable.ic_round_broken_image_72px)
                        .into(avatar);
                //updateAvatarUri(key, uri);
                // Add updated notification to brokenAvatarsList. the list will be used to update broken avatars when fragment stops
                message.setSenderAvatar(String.valueOf(uri));
                brokenAvatarsList.add(message);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
                avatar.setImageResource(R.drawable.ic_round_account_filled_72);
            }
        });
    }

    public List<Message> getBrokenAvatarsList(){
        return brokenAvatarsList;
    }

    // clear sent messages list after updating the database
    public void clearBrokenAvatarsList(){
        brokenAvatarsList.clear();
    }


    // CALLBACK to calculate the difference between the old item and the new item
    private static final DiffUtil.ItemCallback<Message> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Message>() {
                // User details may have changed if reloaded from the database,
                // but ID is fixed.

                // if the two items are the same
                @Override
                public boolean areItemsTheSame(Message oldMessage, Message newMessage) {
                    /*Log.d(TAG, " DIFF_CALLBACK areItemsTheSame " + (oldUser.getCreatedLong() == newUser.getCreatedLong()));
                    Log.d(TAG, " DIFF_CALLBACK areItemsTheSame keys= old: " + oldUser.getCreatedLong() +" new: "+ newUser.getCreatedLong());*/
                    Log.d(TAG, " DIFF_CALLBACK areItemsTheSame keys= old: " + oldMessage.getKey() +" name: "+ oldMessage.getMessage()+" new: "+ newMessage.getKey()+ " name: "+ newMessage.getMessage()+" areItemsTheSame: " +oldMessage.getKey().equals(newMessage.getKey()));

                    // If updated database has item that is not sent but it exist on the totalStatusList
                    /*for (int i = 0; i < totalStatusList.size(); i++) {
                        if(newMessage.getKey().equals(totalStatusList.get(i).getKey()) ){
                            // set the new message to sent because the user sent it successfully and was added to totalStatusList
                            // but we didn't update the database yet
                            newMessage.setSent(true);
                            Log.d(TAG, " DIFF_CALLBACK areItemsTheSame. set send to true. old name: " +oldMessage.getMessage()+" value: " + oldMessage.getSent() + " new name: "+ newMessage.getMessage()+" value: " +newMessage.getSent()+" areItemsTheSame: " +oldMessage.getKey().equals(newMessage.getKey()));
                        }

                        if(oldMessage.getKey().equals(totalStatusList.get(i).getKey()) ){
                            // set the new message to sent because the user sent it successfully and was added to totalStatusList
                            // but we didn't update the database yet
                            oldMessage.setSent(true);
                            Log.d(TAG, " DIFF_CALLBACK areItemsTheSame. set send to true. old name: " +oldMessage.getMessage()+" value: " + oldMessage.getSent() + " new name: "+ newMessage.getMessage()+" value: " +newMessage.getSent()+" areItemsTheSame: " +oldMessage.getKey().equals(newMessage.getKey()));

                        }
                    }*/

                    return oldMessage.getKey().equals(newMessage.getKey());
                    //return TextUtils.equals(oldMessage.getKey(), newMessage.getKey());
                    //return true;
                }

                // if the content of two items is the same
                @Override
                public boolean areContentsTheSame(Message oldMessage, Message newMessage) {
                   /* Log.d(TAG, "  DIFF_CALLBACK areContentsTheSame object " + (oldUser.equals(newUser)));
                    Log.d(TAG, "  DIFF_CALLBACK areContentsTheSame Names() " + (oldUser.getName().equals(newUser.getName())));
                    Log.d(TAG, "  DIFF_CALLBACK areContentsTheSame old name: " + oldUser.getName() + " new name: "+newUser.getName());
*/
                    // NOTE: if you use equals, your object must properly override Object#equals()
                    // Incorrectly returning false here will result in too many animations.
                    //Log.d(TAG, "  DIFF_CALLBACK areContentsTheSame old name: " + oldMessage.getMessage() + " new name: "+newMessage.getMessage()+ " areContentsTheSame= "+oldMessage.getMessage().equals(newMessage.getMessage()));
                    //return oldMessage.getMessage().equals(newMessage.getMessage());

                    /*Log.d(TAG, " DIFF_CALLBACK areContentsTheSame old name: " +oldMessage.getMessage()+" value: " + oldMessage.getRevealed() + " new name: "+ newMessage.getMessage()+" value: " +newMessage.getRevealed()+ " areContentsTheSame= "+(oldMessage.getRevealed() == newMessage.getRevealed()));
                    return oldMessage.getRevealed()== newMessage.getRevealed();*/

                    // If updated database has item that is not sent but it exist on the totalStatusList
                    for (int i = 0; i < totalStatusList.size(); i++) {
                        if(newMessage.getKey().equals(totalStatusList.get(i).getKey())){
                            // set the new message to sent because the user sent it successfully and was added to totalStatusList
                            // but we didn't update the database yet
                            if (TextUtils.equals(newMessage.getStatus(), Message_STATUS_SENDING)){
                                newMessage.setStatus(totalStatusList.get(i).getStatus());
                            }else {
                                // If new statues is not sending, remove items from totalStatusList
                                totalStatusList.remove(i);
                            }

                        }
                    }

                    Log.d(TAG, " DIFF_CALLBACK areContentsTheSame old name: " +oldMessage.getMessage()+" value: " + oldMessage.getStatus() + " new name: "+ newMessage.getMessage()+" value: " +newMessage.getStatus()+ " areContentsTheSame= " +oldMessage.equals(newMessage));
                    // Equals method id overridden
                    return oldMessage.equals(newMessage) ;
                    //return false;
                }


            };

    // Determines the appropriate ViewType according to the sender of the message.
    @Override
    public int getItemViewType(int position) {

        Message message = getItem(position);

        if(null!= (message != null ? message.getSenderId() : null) && null != currentUserId && currentUserId.equals(message.getSenderId())){
            // If the current user is the sender of the message
            return VIEW_TYPE_MESSAGE_SENT;
        }else{// it's a message from chat user
            // If some other user sent the message
            return VIEW_TYPE_MESSAGE_RECEIVED;
        }

    }

    @Nullable
    @Override
    public Message getItem(int position) {
        return super.getItem(position);
    }
    /*protected Message getItem(int position) {
        return super.getItem(position);
    }*/

    /*@Override
    public void submitList(PagedList<Message> pagedList) {
         Log.d(TAG, "submitList"+ pagedList.size());

        for (Message submittedItem : pagedList) {
            Log.d(TAG, "onSubmitList.  message= "+submittedItem.getMessage() + " key= "+submittedItem.getKey() +" isStatus= "+ submittedItem.getStatus());
            if(null != submittedItem.getSenderId() && !submittedItem.getSenderId().equals(currentUserId)){
                submittedItem.setStatus(Message_STATUS_SEEN);
                totalStatusList.add(submittedItem);
            }
        }

        super.submitList(pagedList);
    }*/

    /*@Override
    public void onCurrentListChanged(@Nullable PagedList<Message> currentList) {

        Log.d(TAG, "onCurrentListChanged. list size= "+ currentList.size());
        for (Message submittedMessageItem : currentList) {
            Log.d(TAG, "onCurrentListChanged.  message= "+submittedMessageItem.getMessage() + " key= "+submittedMessageItem.getKey() +" isSent= "+ submittedMessageItem.getSent());

        }
        for (Message sentMessageItem : totalStatusList) {
            int Position = updateSentItemStatus(sentMessageItem.getKey(), currentList);
            if (Position != -2){
                notifyItemChanged(Position);
            }
            Log.d(TAG, "notifyItemChanged. Position= "+ Position +" message= "+sentMessageItem.getMessage() + " key= "+sentMessageItem.getKey());
        }

        super.onCurrentListChanged(currentList);
    }*/

    /*@Nullable
    @Override
    public PagedList<Message> getCurrentList() {
        return super.getCurrentList();
    }*/

    /// ViewHolder for ReceivedMessages list /////
    public class ReceivedMessageHolder extends RecyclerView.ViewHolder {

        View row;
        private TextView mMessage, mSentTime, mResetView;
        private CircleImageView mAvatar;
        ItemClickListener itemClickListener;

        private ReceivedMessageHolder(View itemView) {
            super(itemView);
            //itemView = row;

            row = itemView;
            mMessage = row.findViewById(R.id.message_text);
            mAvatar = row.findViewById(R.id.user_image);
            mSentTime = row.findViewById(R.id.sent_time);

            /*itemClickListener= new ItemClickListener() {
                @Override
                public void onClick(View view, int position, boolean isLongClick) {

                }
            };*/
        }


       /* @Override
        public boolean onLongClick(View v) {
            Log.d(TAG, "onDrag ");
            //return false;
            //itemClickListener.onClick(v, getAdapterPosition(), true);
            return false;
        }*/

       /* @Override
        public boolean onHover(View v, MotionEvent event) {
            Log.d(TAG, "onDrag ");
            return true;
        }*/


        // needed only if i want the listener to be inside the adapter
        public void setItemClickListener(ItemClickListener itemClickListener) {
            this.itemClickListener = itemClickListener;
        }

    }

    /// ViewHolder for SentMessages list /////
    public class SentMessageHolder extends RecyclerView.ViewHolder {

        View row;
        private TextView mMessage, mSentTime;
        private CircleImageView mAvatar;
        private ImageView mSentIcon;


        public SentMessageHolder(View itemView) {
            super(itemView);
            //itemView = row;

            row = itemView;
            mMessage = row.findViewById(R.id.message_text);
            mAvatar = row.findViewById(R.id.user_image);
            mSentTime = row.findViewById(R.id.sent_time);
            mSentIcon = row.findViewById(R.id.sending_icon);
        }

    }

}

