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
    private String biography;
    private int loveCounter;
    private int PickupCounter;
    private String relationship;
    private String interestedIn;
    private  int soundId;

    public String gender;
    public Long birthDate;
    public String horoscope;
    public String lives;
    public String hometown;
    public String nationality;
    public String religion;
    public String politics;
    public String work;
    public String college;
    public String school;

    public Boolean smoke;
    public Boolean shisha;
    public Boolean drugs;
    public Boolean drink;
    public Boolean gamer;
    public Boolean cook;
    public Boolean read;
    public Boolean athlete;
    public Boolean travel;


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
        result.put("biography", biography);
        result.put("loveCounter", loveCounter);
        result.put("PickupCounter", PickupCounter);
        result.put("relationship", relationship);
        result.put("interestedIn", interestedIn);

        result.put("gender", gender);
        result.put("birthDate", birthDate);
        result.put("horoscope", horoscope);
        result.put("lives", lives);
        result.put("hometown", hometown);
        result.put("nationality", nationality);
        result.put("religion", religion);
        result.put("politics", politics);
        result.put("work", work);
        result.put("college", college);
        result.put("school", school);

        result.put("smoke", smoke);
        result.put("shisha", shisha);
        result.put("drugs", drugs);
        result.put("drink", drink);
        result.put("gamer", gamer);
        result.put("cook", cook);
        result.put("read", read);
        result.put("athlete", athlete);
        result.put("travel", travel);

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

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    public int getLoveCounter() {
        return loveCounter;
    }

    public void setLoveCounter(int loveCounter) {
        this.loveCounter = loveCounter;
    }

    public int getPickupCounter() {
        return PickupCounter;
    }

    public void setPickupCounter(int pickupCounter) {
        PickupCounter = pickupCounter;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public String getInterestedIn() {
        return interestedIn;
    }

    public void setInterestedIn(String interestedIn) {
        this.interestedIn = interestedIn;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Long getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(Long birthDate) {
        this.birthDate = birthDate;
    }

    public String getHoroscope() {
        return horoscope;
    }

    public void setHoroscope(String horoscope) {
        this.horoscope = horoscope;
    }

    public String getLives() {
        return lives;
    }

    public void setLives(String lives) {
        this.lives = lives;
    }

    public String getHometown() {
        return hometown;
    }

    public void setHometown(String hometown) {
        this.hometown = hometown;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getReligion() {
        return religion;
    }

    public void setReligion(String religion) {
        this.religion = religion;
    }

    public String getPolitics() {
        return politics;
    }

    public void setPolitics(String politics) {
        this.politics = politics;
    }

    public String getWork() {
        return work;
    }

    public void setWork(String work) {
        this.work = work;
    }

    public String getCollege() {
        return college;
    }

    public void setCollege(String college) {
        this.college = college;
    }

    public String getSchool() {
        return school;
    }

    public void setSchool(String school) {
        this.school = school;
    }

    public Boolean getSmoke() {
        return smoke;
    }

    public void setSmoke(Boolean smoke) {
        this.smoke = smoke;
    }

    public Boolean getShisha() {
        return shisha;
    }

    public void setShisha(Boolean shisha) {
        this.shisha = shisha;
    }

    public Boolean getDrugs() {
        return drugs;
    }

    public void setDrugs(Boolean drugs) {
        this.drugs = drugs;
    }

    public Boolean getDrink() {
        return drink;
    }

    public void setDrink(Boolean drink) {
        this.drink = drink;
    }

    public Boolean getGamer() {
        return gamer;
    }

    public void setGamer(Boolean gamer) {
        this.gamer = gamer;
    }

    public Boolean getCook() {
        return cook;
    }

    public void setCook(Boolean cook) {
        this.cook = cook;
    }

    public Boolean getRead() {
        return read;
    }

    public void setRead(Boolean read) {
        this.read = read;
    }

    public Boolean getAthlete() {
        return athlete;
    }

    public void setAthlete(Boolean athlete) {
        this.athlete = athlete;
    }

    public Boolean getTravel() {
        return travel;
    }

    public void setTravel(Boolean travel) {
        this.travel = travel;
    }

    public Map<String, Boolean> getTokens() {
        return tokens;
    }

    public void setTokens(Map<String, Boolean> tokens) {
        this.tokens = tokens;
    }

    //@Exclude
    public int getSoundId() { return soundId; }

    @Exclude
    public void setSoundId(int soundId) { this.soundId = soundId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return
                TextUtils.equals(avatar, user.avatar) &&
                        TextUtils.equals(name, user.name) &&
                        TextUtils.equals(biography, user.biography) &&
                        TextUtils.equals(relationship, user.relationship) &&
                        TextUtils.equals(interestedIn, user.interestedIn) &&
                        TextUtils.equals(gender, user.gender) &&
                        TextUtils.equals(horoscope, user.horoscope) &&
                        (birthDate == user.birthDate || (birthDate!= null && birthDate.equals(user.birthDate))) &&
                        (created == user.created || (created!=null && created.equals(user.created)));
    }

    @Override
    public int hashCode() {
        //return Objects.hash(created, avatar, name, biography, relationship, interestedIn, gender, birthDate, horoscope);
        int result = 1;
        result = 31 * result + (avatar == null ? 0 : avatar.hashCode());
        result = 31 * result + (name == null ? 0 : name.hashCode());
        result = 31 * result + (biography == null ? 0 : biography.hashCode());
        result = 31 * result + (relationship == null ? 0 : relationship.hashCode());
        result = 31 * result + (interestedIn == null ? 0 : interestedIn.hashCode());
        result = 31 * result + (gender == null ? 0 : gender.hashCode());
        result = 31 * result + (horoscope == null ? 0 : horoscope.hashCode());
        result = 31 * result + (birthDate == null ? 0 : birthDate.hashCode());
        result = 31 * result + (created == null ? 0 : created.hashCode());
        return result;

    }
}
// [END blog_user_class]