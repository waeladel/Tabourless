package com.tabourless.queue.interfaces;

import com.tabourless.queue.models.Place;
// notify the View model that the place is found
public interface FirebasePlaceCallback {
    void onCallback(Place place);
}
