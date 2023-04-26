package com.tabourless.queue.models;

import android.text.TextUtils;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.tabourless.queue.App.CUSTOMER_STATUS_WAITING;

@IgnoreExtraProperties
public class UserQueue {

    // This is an object for queues map inside place object
    private String key;
    private String name;
    private String placeId;
    private String placeName;
    private String status;
    private Map<String, Counter> counters = new LinkedHashMap<>(); // To get the suitable counters (for current user) of the the selected chip item
    private int number;
    private Object joined;

    public UserQueue() {
        this.joined = ServerValue.TIMESTAMP;
    }

    public UserQueue(String key, String name, String placeId, String placeName) {
        this.key = key;
        this.name = name;
        this.placeId = placeId;
        this.placeName = placeName;
        this.joined = ServerValue.TIMESTAMP;
    }

    public UserQueue(String key, String name, String placeId, String placeName, String status, Map<String, Counter> counters) {
        this.key = key;
        this.name = name;
        this.placeId = placeId;
        this.placeName = placeName;
        this.status = status;
        this.counters = counters;
        this.joined = ServerValue.TIMESTAMP;
    }

    // [START post_to_map]
    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("name", name);
        result.put("joined", joined);
        result.put("placeId", placeId);
        result.put("placeName", placeName);
        result.put("status", status);
        result.put("counters", counters);
        result.put("number", number);

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

    public String getPlaceName() {
        return placeName;
    }

    public void setPlaceName(String placeName) {
        this.placeName = placeName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, Counter> getCounters() {
        return counters;
    }

    public void setCounters(Map<String, Counter> counters) {
        this.counters = counters;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
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
                TextUtils.equals(placeName, queue1.placeName)&&
                TextUtils.equals(status, queue1.status) &&
                number == queue1.number &&
                (joined == queue1.joined || (joined!=null && joined.equals(queue1.joined)));
    }

    @Override
    public int hashCode() {
        //return Objects.hash(key, avatar, name, read);
        int result = 1;
        result = 31 * result + (name == null ? 0 : name.hashCode());
        result = 31 * result + (placeId == null ? 0 : placeId.hashCode());
        result = 31 * result + (placeName == null ? 0 : placeName.hashCode());
        result = 31 * result + (status == null ? 0 : status.hashCode());
        result = 31 * result + (number == 0 ? 0 : 1);
        result = 31 * result + (joined == null ? 0 : joined.hashCode());
        return result;
    }
}
// [END blog_user_class]