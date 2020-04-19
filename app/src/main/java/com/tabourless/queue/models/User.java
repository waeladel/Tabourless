package com.tabourless.queue.models;

import android.text.TextUtils;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class User {


    private String key;
    private Object created;
    private Long lastOnline;

    private String avatar;
    private String coverImage;
    private String name;
    private String gender;
    private int birthYear;
    private boolean disability;



    public Map<String, Boolean> tokens = new HashMap<>();

    // startedAt: firebase.database.ServerValue.TIMESTAMP
    //private Date joined;// anotation to put server timestamp

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
        //this.created = ServerValue.TIMESTAMP;
    }


    // [START post_to_map]
    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("created", ServerValue.TIMESTAMP);
        result.put("lastOnline", lastOnline);
        result.put("avatar", avatar);
        result.put("coverImage", coverImage);
        result.put("name", name);
        result.put("gender", gender);
        result.put("birthDate", birthYear);
        result.put("horoscope", disability);

        return result;
    }
    // [END post_to_map]

    @Exclude
    public String getKey() { return key; }
    @Exclude
    public void setKey(String key) { this.key = key; }

    public Object getCreated() {
        return created;
    }

    @Exclude
    public long getCreatedLong() {
        return (long) created;
    }

    public void setCreated(Object created) {
        this.created = created;
    }

    public Long getLastOnline() {
        return lastOnline;
    }

    public void setLastOnline(Long lastOnline) {
        this.lastOnline = lastOnline;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
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

    public int getBirthYear() {
        return birthYear;
    }

    public void setBirthYear(int birthYear) {
        this.birthYear = birthYear;
    }

    public boolean isDisable() {
        return disability;
    }

    public void setDisability(boolean disability) {
        this.disability = disability;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return
                TextUtils.equals(avatar, user.avatar) &&
                        TextUtils.equals(name, user.name) &&
                        TextUtils.equals(gender, user.gender) &&
                        (created == user.created || (created!=null && created.equals(user.created)));
    }

    @Override
    public int hashCode() {
        //return Objects.hash(created, avatar, name, biography, relationship, interestedIn, gender, birthDate, horoscope);
        int result = 1;
        result = 31 * result + (avatar == null ? 0 : avatar.hashCode());
        result = 31 * result + (name == null ? 0 : name.hashCode());
        result = 31 * result + (gender == null ? 0 : gender.hashCode());
        result = 31 * result + (created == null ? 0 : created.hashCode());
        return result;

    }
}
// [END blog_user_class]