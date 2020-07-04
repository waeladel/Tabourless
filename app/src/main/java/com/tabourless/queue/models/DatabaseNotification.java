package com.tabourless.queue.models;

import android.text.TextUtils;
import android.util.Log;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class DatabaseNotification {

    private String key;
    //private String title;
    //private String message;
    private String type;
    private String senderId;
    private String chatId;
    private String senderName;
    private String senderAvatar;
    private boolean seen; // to reset notification counter when user see the notifications
    private boolean clicked; // to change item background when clicked
    private Object sent; // sent time
    private String placeId;
    private String queueId;
    private String queueName;
    private String counterName;
    private int number;

    public DatabaseNotification() {
    }

    /*public DatabaseNotification(String title, String message, String type, String senderId, String senderName, String senderAvatar) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderAvatar = senderAvatar;
        //this.sent = sent;
    }*/

    public DatabaseNotification(String type, String senderId, String senderName, String senderAvatar) {
        //this.title = title;
        //this.message = message;
        this.senderName = senderName;
        this.senderAvatar = senderAvatar;
        this.type = type;
        this.senderId = senderId;
        this.sent = ServerValue.TIMESTAMP;
    }

    public DatabaseNotification(String type, String senderId, String senderName, String senderAvatar, String chatId) {
        //this.title = title;
        //this.message = message;
        this.senderName = senderName;
        this.senderAvatar = senderAvatar;
        this.type = type;
        this.senderId = senderId;
        this.chatId = chatId;
        this.sent = ServerValue.TIMESTAMP;
    }

    // [START post_to_map]
    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        //result.put("title", title);
        //result.put("message", message);
        result.put("type", type);
        result.put("senderId", senderId);
        result.put("chatId", chatId);
        result.put("senderName", senderName);
        result.put("senderAvatar", senderAvatar);
        result.put("seen", seen);
        result.put("sent", sent);
        result.put("placeId", placeId);
        result.put("queueId", queueId);
        result.put("queueName", queueName);
        result.put("counterName", counterName);
        result.put("number", number);

        return result;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    /*public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }*/

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }


    public Object getSent() {
        return sent;
    }

    @Exclude
    public long getSentLong() {
        return (long) sent;
    }

    public void setSent(Object sent) {
        this.sent = sent;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderAvatar() {
        return senderAvatar;
    }

    public void setSenderAvatar(String senderAvatar) {
        this.senderAvatar = senderAvatar;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }

    public boolean isClicked() {
        return clicked;
    }

    public void setClicked(boolean clicked) {
        this.clicked = clicked;
    }

    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    public String getQueueId() {
        return queueId;
    }

    public void setQueueId(String queueId) {
        this.queueId = queueId;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public String getCounterName() {
        return counterName;
    }

    public void setCounterName(String counterName) {
        this.counterName = counterName;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DatabaseNotification notification1 = (DatabaseNotification) o;

        Log.d("Notifications equals", "Notifications equals . lastMessage ="+senderName +" chat1.lastMessage= "+notification1.senderName + " value=" + TextUtils.equals(senderName, notification1.senderName) );
        Log.d("Notifications equals", "Notifications equals . lastMessage ="+sent +" chat1.lastMessage= "+notification1.sent + " value=" + sent.equals(notification1.sent) );

        return (seen == notification1.seen) &&
                (clicked == notification1.clicked) &&
                (TextUtils.equals(senderName, notification1.senderName)) &&
                (TextUtils.equals(senderAvatar, notification1.senderAvatar)) &&
                (sent == notification1.sent || (sent!=null && sent.equals(notification1.sent)));

        //sent == notification1.sent || (sent!=null && sent.equals(notification1.sent)
    }

    @Override
    public int hashCode() {
        //return Objects.hash(senderName, senderAvatar, seen, sent);
        int result = 1;
        result = 31 * result + (seen ? 1 : 0);
        result = 31 * result + (clicked ? 1 : 0);
        result = 31 * result + (senderName == null ? 0 : senderName.hashCode());
        result = 31 * result + (senderAvatar == null ? 0 : senderAvatar.hashCode());
        result = 31 * result + (sent == null ? 0 : sent.hashCode());
        return result;
    }

    // [END post_to_map]
}
