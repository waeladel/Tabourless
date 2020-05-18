package com.tabourless.queue.models;

import android.text.TextUtils;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@IgnoreExtraProperties
public class Queue {

    // This is an object for queues map inside place object
    private String key;
    private String name;
    private Map<String, Counter> counters = new LinkedHashMap<>();


    public Queue() {
    }

    public Queue(String name, Map<String, Counter> counters) {
        this.name = name;
        this.counters = counters;
    }

    // [START post_to_map]
    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("name", name);
        result.put("counters", counters);

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

    public Map<String, Counter> getCounters() {
        return counters;
    }

    public void setCounters(Map<String, Counter> counters) {
        this.counters = counters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Queue queue1 = (Queue) o;
        return
                TextUtils.equals(name, queue1.name)&&
                (counters == queue1.counters || (counters!=null && counters.equals(queue1.counters)));
    }

    @Override
    public int hashCode() {
        //return Objects.hash(key, avatar, name, read);
        int result = 1;
        result = 31 * result + (name == null ? 0 : name.hashCode());
        result = 31 * result + (counters == null ? 0 : counters.hashCode());
        return result;
    }
}
// [END blog_user_class]