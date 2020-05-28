package com.tabourless.queue.models;

import android.text.TextUtils;
import android.util.Log;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class Chat {

    private String key;
    private String lastMessage;
    private String sender;
    private Object lastSent ;
    private Long active;
    //private List<String> members = new ArrayList<>();
    private Map<String, ChatMember> members = new HashMap<>();


    public Chat() {
        this.lastSent = ServerValue.TIMESTAMP;
    }

    public Chat(String lastMessage, String sender, Map<String, ChatMember> members) {
        this.lastMessage = lastMessage;
        //this.members = members;
        this.members = members;
        this.sender = sender;
        this.lastSent = ServerValue.TIMESTAMP;
    }

    // [START post_to_map]
    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("lastMessage", lastMessage);
        //result.put("members", members);
        result.put("members", members);
        result.put("active", active);
        result.put("sender", sender);
        result.put("lastSent", ServerValue.TIMESTAMP);

        return result;
    }
// [END post_to_map]

    @Exclude
    public String getKey() {
        return key;
    }

    @Exclude
    public void setKey(String key) {
        this.key = key;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Object getLastSent() {
        return lastSent;
    }

    @Exclude
    public long getLastSentLong() {
        return (long) lastSent;
    }

    public void setLastSent(Object lastSent) {
        this.lastSent = lastSent;
    }

    public Long getActive() {
        return active;
    }

    public void setActive(Long active) {
        this.active = active;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    /* public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;

    }*/

    public Map<String, ChatMember> getMembers() {
        return members;
    }

    public void setMembers(Map<String, ChatMember> members) {
        this.members = members;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Chat chat1 = (Chat) o;
        Log.d("chats are", "chats are . lastMessage ="+lastMessage +" chat1.lastMessage= "+chat1.lastMessage );
        //Log.d("chats are", "chats are . lastSent ="+getLastSentLong() +" chat1.lastSent= "+chat1.getLastSentLong() );
        //Long lastSentLong = getLastSentLong();
        //Long chat1LastSentLong = chat1.getLastSentLong();

        //Log.d("chats are", "chats are . lastSentLong ="+lastSentLong +" chat1LastSentLong= "+chat1LastSentLong);


            //members.get("Hcs4JY1zMJgF1cZsTY9R4xI670R2").isSaw();
        return TextUtils.equals(lastMessage, chat1.lastMessage) &&
                (members == chat1.members || (members!=null && members.equals(chat1.members)))&&
                (lastSent == chat1.lastSent || (lastSent!=null && lastSent.equals(chat1.lastSent)));
                //getLastSent() == null ? null == chat1.getLastSent()  : (long) lastSent == (long) chat1.lastSent;
                //lastSentLong.compareTo(chat1LastSentLong) == 0;
                //getLastSentLong()== chat1.getLastSentLong();
                //getLastSentLong() == 0 ? chat1.getLastSentLong() == 0 :

                //(active == chat.active) &&
                //members == chat1.members || (members!=null && members.equals(chat1.members)));
                //members.equals(chat.members);
    }

    @Override
    public int hashCode() {
        //return Objects.hash(lastMessage, lastSent, active, members);
        int result = 1;
        result = 31 * result + (lastMessage == null ? 0 : lastMessage.hashCode());
        result = 31 * result + (lastSent == null ? 0 : lastSent.hashCode());
        result = 31 * result + (members == null ? 0 : members.hashCode());
        return result;
    }

    /*@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Chat chat = (Chat) o;
        return Objects.equals(getLastSent(), chat.getLastSent()) &&
                Objects.equals(getActive(), chat.getActive());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLastSent(), getActive());
    }*/
}
