package com.tabourless.queue.models;

import android.text.TextUtils;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@IgnoreExtraProperties
public class UserQueue {

    // This is an object for queues map inside place object
    private String key;
    private String name;
    private String placeId;
    private Object joined;

    public UserQueue() {
    }

    public UserQueue(String key, String name, String placeId) {
        this.key = key;
        this.name = name;
        this.placeId = placeId;
    }

    // [START post_to_map]
    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("name", name);
        result.put("joined", ServerValue.TIMESTAMP);
        result.put("placeId", placeId);

        return result;
    }
    // [END post_to_map]

    @Exclude
    public String getKey() { return key; }
    @Exclude
    public void setKey(String key) { this.key = key; }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    public Object getJoined() {
        return joined;
    }

    public long getJoinedLong() {
        return (long) joined;
    }

    public void setJoined(Object joined) {
        this.joined = joined;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserQueue queue1 = (UserQueue) o;
        return
                TextUtils.equals(name, queue1.name)&&
                TextUtils.equals(placeId, queue1.placeId)&&
                (joined == queue1.joined || (joined!=null && joined.equals(queue1.joined)));
    }

    @Override
    public int hashCode() {
        //return Objects.hash(key, avatar, name, read);
        int result = 1;
        result = 31 * result + (name == null ? 0 : name.hashCode());
        result = 31 * result + (placeId == null ? 0 : placeId.hashCode());
        result = 31 * result + (joined == null ? 0 : joined.hashCode());
        return result;
    }
}
// [END blog_user_class]