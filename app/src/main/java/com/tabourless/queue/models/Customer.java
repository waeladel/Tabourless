package com.tabourless.queue.models;

import android.text.TextUtils;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@IgnoreExtraProperties
public class Customer {

    // This is a customer object which is added to customers column that holds all customers waiting in a queue
    private String key;
    private String userId;
    private Object joined;
    private Long lastHere;
    private Long started;
    private String status;
    private String counter;
    private Map<String, Boolean> counters = new LinkedHashMap<>();
    private int number;

    private String avatar;
    private String name;
    private String gender;
    private int age;
    private boolean disabled;

    // startedAt: firebase.database.ServerValue.TIMESTAMP
    //private Date joined;// anotation to put server timestamp

    public Customer() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
        this.joined = ServerValue.TIMESTAMP;
    }

    public Customer(String userId, String avatar, String name, String gender, int age, boolean disabled, String status) {
        this.joined = ServerValue.TIMESTAMP;
        this.userId = userId;
        this.avatar = avatar;
        this.name = name;
        this.gender = gender;
        this.age = age;
        this.disabled = disabled;
        this.status = status;
        /*this.lastHere;
        this.counter;
        this.number;*/
    }


    // [START post_to_map]
    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("joined", ServerValue.TIMESTAMP);
        result.put("lastHere", lastHere);
        result.put("started", started);
        result.put("status", status);
        result.put("counter", counter);
        result.put("counters", counters);
        result.put("number", number);
        result.put("avatar", avatar);
        result.put("name", name);
        result.put("gender", gender);
        result.put("age", age);
        result.put("disabled", disabled);

        return result;
    }
    // [END post_to_map]

    @Exclude
    public String getKey() { return key; }
    @Exclude
    public void setKey(String key) { this.key = key; }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Object getJoined() {
        return joined;
    }

    @Exclude
    public long getJoinedLong() {
        return (long) joined;
    }

    public void setJoined(Object joined) {
        this.joined = joined;
    }

    public Long getLastHere() {
        return lastHere;
    }

    public void setLastHere(Long lastHere) {
        this.lastHere = lastHere;
    }

    public Long getStarted() {
        return started;
    }

    public void setStarted(Long started) {
        this.started = started;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCounter() {
        return counter;
    }

    public void setCounter(String counter) {
        this.counter = counter;
    }

    public Map<String, Boolean> getCounters() {
        return counters;
    }

    public void setCounters(Map<String, Boolean> counters) {
        this.counters = counters;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Customer customer = (Customer) o;
        return
                disabled == customer.disabled &&
                number == customer.number &&
                age == customer.age &&
                //TextUtils.equals(userId, customer.userId) &&
                TextUtils.equals(status, customer.status) &&
                TextUtils.equals(counter, customer.counter) &&
                TextUtils.equals(avatar, customer.avatar) &&
                TextUtils.equals(name, customer.name) &&
                TextUtils.equals(gender, customer.gender) &&
                (joined == customer.joined || (joined !=null && joined.equals(customer.joined))) &&
                (lastHere == customer.lastHere || (lastHere!=null && lastHere.equals(customer.lastHere)));
    }

    @Override
    public int hashCode() {
        //return Objects.hash(created, avatar, name, biography, relationship, interestedIn, gender, birthDate, horoscope);
        int result = 1;
        result = 31 * result + (disabled ? 1 : 0);
        result = 31 * result + (number == 0 ? 0 : 1);
        result = 31 * result + (age == 0 ? 0 : 1);
        //result = 31 * result + (userId == null ? 0 : userId.hashCode());
        result = 31 * result + (status == null ? 0 : status.hashCode());
        result = 31 * result + (counter == null ? 0 : counter.hashCode());
        result = 31 * result + (avatar == null ? 0 : avatar.hashCode());
        result = 31 * result + (name == null ? 0 : name.hashCode());
        result = 31 * result + (gender == null ? 0 : gender.hashCode());
        result = 31 * result + (joined == null ? 0 : joined.hashCode());
        result = 31 * result + (lastHere == null ? 0 : lastHere.hashCode());
        return result;

    }
}
// [END blog_user_class]