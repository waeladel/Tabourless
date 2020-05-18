package com.tabourless.queue.models;

import android.text.TextUtils;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class Counter {
    // This is an object for counters map inside queues map
    private String key;
    private String name;
    private boolean open;
    private String gender;
    private String age;
    private String disability;
    private String nationality;

    public Counter() {
    }

    public Counter(String name, boolean open, String gender, String age, String disability, String nationality) {
        this.name = name;
        this.open = open;
        this.gender = gender;
        this.age = age;
        this.disability = disability;
        this.nationality = nationality;
    }

    // [START post_to_map]
    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("name", name);
        result.put("open", open);
        result.put("gender", gender);
        result.put("age", age);
        result.put("disability", disability);
        result.put("nationality", nationality);

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

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getDisability() {
        return disability;
    }

    public void setDisability(String disability) {
        this.disability = disability;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Counter that = (Counter) o;
        return  open == that.open &&
                TextUtils.equals(name, that.name) &&
                TextUtils.equals(gender, that.gender) &&
                TextUtils.equals(age, that.age) &&
                TextUtils.equals(disability, that.disability) &&
                TextUtils.equals(nationality, that.nationality);
    }

    @Override
    public int hashCode() {
        //return Objects.hash(key, avatar, name, read);
        int result = 1;
        result = 31 * result + (open ? 1 : 0);
        result = 31 * result + (name == null ? 0 : name.hashCode());
        result = 31 * result + (gender == null ? 0 : gender.hashCode());
        result = 31 * result + (age == null ? 0 : age.hashCode());
        result = 31 * result + (disability == null ? 0 : disability.hashCode());
        result = 31 * result + (nationality == null ? 0 : nationality.hashCode());

        return result;
    }
}
// [END blog_user_class]