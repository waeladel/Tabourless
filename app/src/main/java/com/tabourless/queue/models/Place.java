package com.tabourless.queue.models;

import android.text.TextUtils;
import android.util.Log;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@IgnoreExtraProperties
public class Place {

    // This is place object that contains queues maps that contains counters map
    private String key;
    private String name;
    private String parent;
    private String parentId;
    private double latitude, longitude;

    private Map<String, Queue> queues = new LinkedHashMap<>();


    public Place() {
    }

    public Place(String name, double latitude, double longitude ,String parent, String parentId, Map<String, Queue> queues) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.parent = parent;
        this.parent = parent;
        this.parentId = parentId;
    }

    // [START post_to_map]
    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("name", name);
        result.put("latitude", latitude);
        result.put("longitude", longitude);
        result.put("parent", parent);
        result.put("parentId", parentId);
        result.put("queues", queues);

        return result;
    }
// [END post_to_map]


    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public Map<String, Queue> getQueues() {
        return queues;
    }

    public void setQueues(Map<String, Queue> queues) {
        this.queues = queues;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Place place1 = (Place) o;

            //members.get("Hcs4JY1zMJgF1cZsTY9R4xI670R2").isSaw();
        return TextUtils.equals(name, place1.name) &&
                TextUtils.equals(parent, place1.parent) &&
                TextUtils.equals(parentId, place1.parentId) &&
                (queues == place1.queues || (queues!=null && queues.equals(place1.queues)));
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
        result = 31 * result + (name == null ? 0 : name.hashCode());
        result = 31 * result + (parent == null ? 0 : parent.hashCode());
        result = 31 * result + (parentId == null ? 0 : parentId.hashCode());
        result = 31 * result + (queues == null ? 0 : queues.hashCode());
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
