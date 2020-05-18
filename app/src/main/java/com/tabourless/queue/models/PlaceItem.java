package com.tabourless.queue.models;

import java.util.HashMap;
import java.util.Map;

public class PlaceItem {

    // This is an object for each item in add place recycler
    private String name;
    private String value;
    private String hint;
    private String helper;
    private Queue queue;
    private int viewType;

    public static final int VIEW_TYPE_TEXT_INPUT = 1;
    public static final int VIEW_TYPE_QUEUE = 2;
    public static final int VIEW_TYPE_BUTTON = 3;

    public PlaceItem() {
    }

    public PlaceItem(String name, String value,String hint, String helper, int viewType) {
        this.name = name;
        this.value = value;
        this.hint = hint;
        this.helper = helper;
        this.viewType = viewType;

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public String getHelper() {
        return helper;
    }

    public void setHelper(String helper) {
        this.helper = helper;
    }

    public int getViewType() {
        return viewType;
    }

    public void setViewType(int viewType) {
        this.viewType = viewType;
    }

    public Queue getQueue() {
        return queue;
    }

    public void setQueue(Queue queue) {
        this.queue = queue;
    }
}
