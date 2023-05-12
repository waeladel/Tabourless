package com.tabourless.queue.adapters;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
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
import com.tabourless.queue.GlideApp;
import com.tabourless.queue.R;
import com.tabourless.queue.databinding.MessageReceivedItemBinding;
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

import static com.tabourless.queue.App.AVATAR_THUMBNAIL_NAME;
import static com.tabourless.queue.App.DATABASE_REF_MESSAGES;
import static com.tabourless.queue.App.Message_STATUS_DELIVERED;
import static com.tabourless.queue.App.Message_STATUS_SENDING;
import static com.tabourless.queue.App.Message_STATUS_SENT;
import static com.tabourless.queue.App.STORAGE_REF_IMAGES;

public class MessagesAdapter extends PagedListAdapter<Message, RecyclerView.ViewHolder> {

    private final static String TAG = MessagesAdapter.class.getSimpleName();
    private FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    private String currentUserId = currentUser != null ? currentUser.getUid() : null;

    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;

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

    // View binding
    private MessageSentItemBinding mSentBinding;
    private MessageReceivedItemBinding mReceivedBinding;

    // to copy message text to clipboard
    private ClipboardManager mClipboard;
    private ClipData mClip;
    private ItemClickListener itemClickListener;

    private Context mContext;
    public MessagesAdapter(Context context, ItemClickListener itemClickListener) {
        super(DIFF_CALLBACK);
        // [START create_storage_reference]
        this.mContext = context;
        this.itemClickListener = itemClickListener;

        mStorageRef = FirebaseStorage.getInstance().getReference();
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        // use received chatKey to create a database ref
        mMessagesRef = mDatabaseRef.child(DATABASE_REF_MESSAGES);

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
                mSentBinding = MessageSentItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
                return new SentMessageHolder(mSentBinding);
            default:
                //// If some other user sent the message
                mReceivedBinding = MessageReceivedItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
                return new ReceivedMessageHolder(mReceivedBinding);
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
                if (!TextUtils.isEmpty(message.getMessage())) {
                    // Message can't be empty because scratch view width will crash the app if it's width is < 0
                    // Only display message text if it's not empty
                    ReceivedHolder.mReceivedBinding.messageText.setText(message.getMessage());
                }else{
                    // if message is empty or null (not set) we must display empty space to have a width for scratch view
                    ReceivedHolder.mReceivedBinding.messageText.setText(" ");
                    //ReceivedHolder.mScratch.setText(null);
                }

                // Lets get avatar
                if(!TextUtils.isEmpty(message.getSenderAvatar())){
                    StorageReference userAvatarStorageRef = mStorageRef.child(STORAGE_REF_IMAGES +"/"+ message.getSenderId() +"/"+ AVATAR_THUMBNAIL_NAME);
                    // Download directly from StorageReference using Glide
                    GlideApp.with(mContext)
                            .load(userAvatarStorageRef)
                            //.placeholder(R.mipmap.account_circle_72dp)
                            .placeholder(R.drawable.ic_round_account_filled_72)
                            .error(R.drawable.ic_round_broken_image_72px)
                            .into(ReceivedHolder.mReceivedBinding.userImage);
                }else{
                    ReceivedHolder.mReceivedBinding.userImage.setImageResource(R.drawable.ic_round_account_filled_72);
                }



                if (null != message.getCreated()) {
                    Calendar c = Calendar.getInstance();
                    c.setTimeInMillis(message.getCreatedLong());
                    String sentTime = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(c.getTime());
                    ReceivedHolder.mReceivedBinding.sentTime.setText(sentTime);
                }else{
                    ReceivedHolder.mReceivedBinding.sentTime.setText(null);
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
                    SentHolder.mSentBinding.messageText.setText(message.getMessage());
                    //SentHolder.mMessage.setText(message.getMessage()+ message.getKey());
                }else{
                    SentHolder.mSentBinding.messageText.setText(null);
                }

                if (null != message.getCreated()) {
                    Calendar c = Calendar.getInstance();
                    c.setTimeInMillis(message.getCreatedLong());
                    String sentTime = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(c.getTime());
                    SentHolder.mSentBinding.sentTime.setText(sentTime);
                }else{
                    SentHolder.mSentBinding.sentTime.setText(null);
                }

                // update sent icon according to message's sent boolean
                Log.d(TAG, " message status ="+ message.getStatus());
                // if message is sent show send icon
                if(null != message.getStatus() && message.getStatus().equals(Message_STATUS_SENT)){
                    // Show seen sent icon
                    SentHolder.mSentBinding.sendingIcon.setImageResource(R.drawable.ic_sent_message_thick);
                }else{
                    // Show sending sent icon
                    SentHolder.mSentBinding.sendingIcon.setImageResource(R.drawable.ic_sending_message_thick);
                }
            }
        }

    }

    /*private void loadStorageImage(final Message message, final CircleImageView avatar) {
        mStorageRef.child("images/"+message.getSenderId() +"/"+ AVATAR_THUMBNAIL_NAME ).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
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
    }*/

    /*public List<Message> getBrokenAvatarsList(){
        return brokenAvatarsList;
    }

    // clear sent messages list after updating the database
    public void clearBrokenAvatarsList(){
        brokenAvatarsList.clear();
    }*/


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

        private MessageReceivedItemBinding mReceivedBinding;

        private ReceivedMessageHolder(MessageReceivedItemBinding binding) {
            super(binding.getRoot());
            this.mReceivedBinding = binding;
            mReceivedBinding.messageText.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    Log.d(TAG, "onLongClick: ");

                    if(itemClickListener != null && getBindingAdapterPosition() != RecyclerView.NO_POSITION) {
                        itemClickListener.onClick(view, getBindingAdapterPosition(), true);
                    }
                    // Create a popup Menu if null. To show when block is clicked
                    PopupMenu popupBlockMenu = new PopupMenu(mReceivedBinding.messageText.getContext(), view);
                    popupBlockMenu.getMenu().add(Menu.NONE, 0, 0, R.string.popup_menu_copy_text);
                    //popupBlockMenu.getMenu().add(Menu.NONE, 1, 1, R.string.popup_menu_delete_message);
                    //popupBlockMenu.getMenu().add(Menu.NONE, 2, 2, R.string.popup_menu_delete_message_all);
                    popupBlockMenu.getMenu().add(Menu.NONE, 1, 1, R.string.popup_menu_report_message);

                    popupBlockMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case 0:
                                    Log.i(TAG, "onMenuItemClick. copy test to clipboard is clicked");
                                    //copy selected text to clipboard
                                    copyToClipboard(mReceivedBinding.messageText.getContext(), mReceivedBinding.messageText.getText());
                                    return true;
                                case 1:
                                    Log.i(TAG, "onMenuItemClick. report message clicked");
                                    // show dialog For reporting messages
                                    if(itemClickListener != null && getBindingAdapterPosition() != RecyclerView.NO_POSITION) {
                                        itemClickListener.onClick(view, item.getItemId(), false);
                                    }
                                    return true;
                                /*case 2:
                                    Log.i(TAG, "onMenuItemClick. item delete message for all clicked ");
                                    return true;*/
                                default:
                                    return false;
                            }
                        }
                    });

                    popupBlockMenu.show();
                    return false;
                }
            });

        }
    }

    /// ViewHolder for SentMessages list /////
    public class SentMessageHolder extends RecyclerView.ViewHolder {

        // View binding
        private MessageSentItemBinding mSentBinding;

        public SentMessageHolder(MessageSentItemBinding binding) {
            super(binding.getRoot());
            this.mSentBinding = binding;

            mSentBinding.messageText.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    Log.d(TAG, "onLongClick: ");

                    // Create a popup Menu if null. To show when block is clicked
                    PopupMenu popupBlockMenu = new PopupMenu(mReceivedBinding.messageText.getContext(), view);
                    popupBlockMenu.getMenu().add(Menu.NONE, 0, 0, R.string.popup_menu_copy_text);
                    //popupBlockMenu.getMenu().add(Menu.NONE, 1, 1, R.string.popup_menu_delete_message);
                    //popupBlockMenu.getMenu().add(Menu.NONE, 2, 2, R.string.popup_menu_delete_message_all);

                    popupBlockMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case 0:
                                    Log.i(TAG, "onMenuItemClick. item block clicked ");
                                    //copy selected text to clipboard
                                    copyToClipboard(mReceivedBinding.messageText.getContext(), mSentBinding.messageText.getText());
                                    return true;
                                /*case 1:
                                    Log.i(TAG, "onMenuItemClick. item delete message for you clicked ");
                                    return true;
                                case 2:
                                    Log.i(TAG, "onMenuItemClick. item delete message for all clicked ");
                                    return true;*/
                                default:
                                    return false;
                            }
                        }
                    });

                    popupBlockMenu.show();
                    return false;
                }
            });

        }

    }

    private void copyToClipboard(Context context, CharSequence text) {
        mClipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        mClip = ClipData.newPlainText("Copied Text", text);
        mClipboard.setPrimaryClip(mClip);
    }

}

