package com.tabourless.queue.adapters;

import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.tabourless.queue.GlideApp;
import com.tabourless.queue.R;
import com.tabourless.queue.databinding.InboxItemBinding;
import com.tabourless.queue.interfaces.ItemClickListener;
import com.tabourless.queue.models.Chat;
import com.tabourless.queue.models.ChatMember;
import com.tabourless.queue.ui.inbox.InboxFragmentDirections;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import static com.tabourless.queue.App.AVATAR_THUMBNAIL_NAME;
import static com.tabourless.queue.App.STORAGE_REF_IMAGES;

public class InboxAdapter extends PagedListAdapter<Chat, InboxAdapter.ViewHolder> {

    private final static String TAG = InboxAdapter.class.getSimpleName();

    private FirebaseUser currentFirebaseUser ;
    private String currentUserId ;

    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;

    private StorageReference mStorageRef;

    // A not static array list for all notifications sender avatar to update the broken avatars when fragment stops
    private List<ChatMember> brokenAvatarsList;// = new ArrayList<>();

    private Fragment fragment;

    private InboxItemBinding mBinding;

    public InboxAdapter(Fragment fragment) {
        super(DIFF_CALLBACK);
        // [START create_storage_reference]
        this.fragment = fragment;
        mStorageRef = FirebaseStorage.getInstance().getReference();

        currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if(currentFirebaseUser != null){
            currentUserId = currentFirebaseUser.getUid();
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

    @NonNull
    @Override
    public InboxAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        mBinding = InboxItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(mBinding);
    }



    @Override
    public void onBindViewHolder(@NonNull final InboxAdapter.ViewHolder holder, final int position) {

        final Chat chat = getItem(position);
        if (chat != null) {
            // LastMessage text value
            if (null != chat.getLastMessage()) {
                holder.mBinding.lastMessage.setText(chat.getLastMessage());
            }else{
                holder.mBinding.lastMessage.setText(null);
            }

            // LastSentTime text value
            if (null != chat.getLastSent()) {
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(chat.getLastSentLong());
                String sentTime = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(c.getTime());
                holder.mBinding.lastSent.setText(sentTime);
            }else{
                holder.mBinding.lastSent.setText(null);
            }

            // participants' avatars and names
            if (null != chat.getMembers()) {
                // loop to get all chat members HashMap
                //String participantId;
                final List<ChatMember> membersList = new ArrayList<>();
                for (Object o : chat.getMembers().entrySet()) {
                    Map.Entry pair = (Map.Entry) o;
                    Log.d(TAG, "Chats getMember = " + pair.getKey() + " = " + pair.getValue() + currentFirebaseUser.getUid());

                    if (!currentFirebaseUser.getUid().equals(pair.getKey())) {
                        ChatMember user = chat.getMembers().get(String.valueOf(pair.getKey()));
                        if (user != null) {
                            user.setKey(String.valueOf(pair.getKey()));
                            membersList.add(user);
                            Log.d(TAG, "Chats membersListSize=" + membersList.size());
                            Log.d(TAG, "Chats getMember name=" + user.getName());
                        }
                    }else{
                        // this is the current user
                        ChatMember currentMember = chat.getMembers().get(String.valueOf(pair.getKey()));
                        if (currentMember != null) {
                            currentMember.setKey(String.valueOf(pair.getKey()));
                            // Check if current user read this chat or not
                            if(!currentMember.isRead()){
                                // Bold text
                                Log.d(TAG, "currentMember=" + currentMember.getName() + " isRead= "+ currentMember.isRead() + " message= "+ chat.getLastMessage() );
                                holder.mBinding.lastMessage.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
                                holder.mBinding.lastMessage.setTextColor(fragment.getResources().getColor(R.drawable.my_color_on_surface_emphasis_high_type));
                                /*holder.mLastMessage.setTextAppearance(App.getContext(), R.style.TextAppearance_MyTheme_Headline5);
                                holder.mLastMessage.setTextColor(R.drawable.my_on_surface_emphasis_high_type);*/
                                //holder.mLastMessage.setAlpha(0.78f);
                                // item is not clicked, display colored background
                                //holder.row.setBackgroundColor(App.getContext().getResources().getColor(R.color.transparent_read_items));
                                //holder.row.setBackgroundResource(R.color.color_highlighted_item);
                            }else{
                                // Normal text
                                Log.d(TAG, "currentMember=" + currentMember.getName() + " isRead= "+ currentMember.isRead() + " message= "+ chat.getLastMessage() );
                                holder.mBinding.lastMessage.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
                                holder.mBinding.lastMessage.setTextColor(fragment.getResources().getColor(R.drawable.my_color_on_surface_emphasis_medium_type));
                                //holder.mLastMessage.setTextAppearance(App.getContext(), R.style.TextAppearance_MyTheme_Body2);
                                //holder.mLastMessage.setTextColor(App.getContext().getResources().getColor(R.color.color_on_background));
                                //holder.mLastMessage.setAlpha(0.54f);
                                // If item was clicked, normal background
                                //holder.row.setBackgroundColor(App.getContext().getResources().getColor(R.color.color_background));
                                //holder.row.setBackgroundColor(android.R.attr.colorBackground);
                            }
                        }

                    }
                    //iterator.remove(); // avoids a ConcurrentModificationException
                }

                holder.setItemClickListener(new ItemClickListener(){
                    @Override
                    public void onClick(View view, int position, boolean isLongClick) {

                        if(membersList.size()== 1){
                            // it's private chat
                            //-1 entire row is clicked
                            if (view.getId() == R.id.user_image) { // only avatar is clicked
                                Log.i(TAG, "user avatar clicked= " + view.getId());
                                Log.i(TAG, "user avatar currentUserId= " + currentUserId + " userId " + membersList.get(0).getKey());
                                NavDirections ProfileDirection = InboxFragmentDirections.actionInboxToProfile(membersList.get(0).getKey());
                                Navigation.findNavController(view).navigate(ProfileDirection);
                            } else {
                                Log.i(TAG, "user row clicked= " + view.getId());
                                NavDirections MessageDirection = InboxFragmentDirections.actionInboxToMessages(chat.getKey(), membersList.get(0).getKey(), false);
                                Navigation.findNavController(view).navigate(MessageDirection);
                            }
                        }else{
                            // it's group chat
                            //-1 entire row is clicked
                            if (view.getId() == R.id.user_image) { // only avatar is clicked
                                    /*Log.i(TAG, "user avatar clicked= "+view.getId());
                                    NavDirections ProfileDirection = MainFragmentDirections.actionMainToProfile(currentUserId, membersList.get(0).getKey(), membersList.get(0));
                                    Navigation.findNavController(view).navigate(ProfileDirection);*/
                                Log.i(TAG, "chat avatar is clicked= " + view.getId());
                                NavDirections MessageDirection = InboxFragmentDirections.actionInboxToProfile(membersList.get(0).getKey());
                                Navigation.findNavController(view).navigate(MessageDirection);
                            } else {
                                Log.i(TAG, "user row clicked= " + view.getId());
                                NavDirections MessageDirection = InboxFragmentDirections.actionInboxToMessages(chat.getKey(), membersList.get(0).getKey(), true);
                                Navigation.findNavController(view).navigate(MessageDirection);
                            }
                        }

                    }
                });

                switch (membersList.size()){
                    case 1:// there is only one member other than current user
                        // names text value
                        if (null != membersList.get(0).getName()) {
                            holder.mBinding.userName.setText(membersList.get(0).getName());
                        }else{
                            holder.mBinding.userName.setText(null);
                        }

                        // Lets get avatar
                        if(!TextUtils.isEmpty(membersList.get(0).getAvatar())){
                            StorageReference userAvatarStorageRef = mStorageRef.child(STORAGE_REF_IMAGES +"/"+ membersList.get(0).getKey() +"/"+ AVATAR_THUMBNAIL_NAME);
                            // Download directly from StorageReference using Glide
                            GlideApp.with(fragment)
                                    .load(userAvatarStorageRef)
                                    //.placeholder(R.mipmap.account_circle_72dp)
                                    .placeholder(R.drawable.ic_round_account_filled_72)
                                    .error(R.drawable.ic_round_broken_image_72px)
                                    .into(holder.mBinding.userImage);
                        }else{
                            holder.mBinding.userImage.setImageResource(R.drawable.ic_round_account_filled_72);
                        }
                        break;
                    case 2:// there is 2 member other than current user
                        Log.d(TAG, " getItems getMember= "+membersList.get(0));
                        Log.d(TAG, " getItems getMember= "+membersList.get(1));
                        break;
                }


            }else{
                Log.d(TAG, "Chats= null");
                holder.mBinding.userImage.setImageResource(R.drawable.ic_round_account_filled_72);
            }

        }

    }

   /* private void loadStorageImage(final String key, final ChatMember chatMember, final CircleImageView avatar) {
        Log.d(TAG, "chatMember id= "+chatMember.getKey());
        mStorageRef.child("images/"+chatMember.getKey() +"/"+ AVATAR_THUMBNAIL_NAME ).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
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
                chatMember.setAvatar(String.valueOf(uri));
                chatMember.setName(key); // We use name as to store the chatId value. it's bad for logic but it's a quick fix
                brokenAvatarsList.add(chatMember);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
                avatar.setImageResource(R.drawable.ic_round_account_filled_72);
            }
        });

    }*/


    /*public List<ChatMember> getBrokenAvatarsList(){
        return brokenAvatarsList;
    }

    // clear sent messages list after updating the database
    public void clearBrokenAvatarsList(){
        brokenAvatarsList.clear();
    }*/

    // CALLBACK to calculate the difference between the old item and the new item
    private static final DiffUtil.ItemCallback<Chat> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Chat>() {
                // User details may have changed if reloaded from the database,
                // but ID is fixed.
                // if the two items are the same
                @Override
                public boolean areItemsTheSame(Chat oldChat, Chat newChat) {

                    //Log.d(TAG, " DIFF_CALLBACK areItemsTheSame " + newChat);
                    //Log.d(TAG, " DIFF_CALLBACK areItemsTheSame keys= old: " + oldChat.getLastMessage() +" new: "+ oldChat.getLastMessage());
                    return oldChat.getKey().equals(newChat.getKey());
                    //return oldChat.getLastSentLong() == (newChat.getLastSentLong());
                    //return true;
                }

                // if the content of two items is the same
                @Override
                public boolean areContentsTheSame(Chat oldChat, Chat newChat) {

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
                    return oldChat.equals(newChat);
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


    /// ViewHolder for ReceivedMessages list /////
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        ItemClickListener itemClickListener;
        private InboxItemBinding mBinding;

        public ViewHolder(InboxItemBinding binding) {
            super(binding.getRoot());
            this.mBinding = binding;

            mBinding.userImage.setOnClickListener(this);
            mBinding.getRoot().setOnClickListener(this);
        }


        @Override
        public void onClick(View view) {
            if(itemClickListener != null && getBindingAdapterPosition() != RecyclerView.NO_POSITION){
                itemClickListener.onClick(view, getBindingAdapterPosition(), false);
            }
        }

        // needed only if i want the listener to be inside the adapter
        public void setItemClickListener(ItemClickListener itemClickListener) {
            this.itemClickListener = itemClickListener;
        }
    }

}

