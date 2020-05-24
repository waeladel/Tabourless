package com.tabourless.queue.models;

import android.text.TextUtils;

import com.google.android.gms.maps.model.Marker;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@IgnoreExtraProperties
public class PlaceMarker {

    // This is PlaceMarker object that contains maps that contains places and it's markers together
    private String key;
    private Place place;
    private Marker marker;

    public PlaceMarker() {
    }

    public PlaceMarker(String key, Place place, Marker marker) {
        this.key = key;
        this.place = place;
        this.marker = marker;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Place getPlace() {
        return place;
    }

    public void setPlace(Place place) {
        this.place = place;
    }

    public Marker getMarker() {
        return marker;
    }

    public void setMarker(Marker marker) {
        this.marker = marker;
    }
}
