package com.tabourless.queue.models;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

/**
 * Created by hp on 27/03/2018.
 */

public class FirebaseListeners {

    private DatabaseReference reference;
    private ValueEventListener listener;
    private Query query;

    public FirebaseListeners() {
    }

    public FirebaseListeners(DatabaseReference reference, ValueEventListener listener) {
        this.reference = reference;
        this.listener = listener;
    }

    public FirebaseListeners(Query query, ValueEventListener listener) {
        this.query = query;
        this.listener = listener;
    }

    public DatabaseReference getReference() {
        return reference;
    }

    public void setReference(DatabaseReference reference) {
        this.reference = reference;
    }

    public ValueEventListener getListener() {
        return listener;
    }

    public void setListener(ValueEventListener listener) {
        this.listener = listener;
    }

    public Query getQuery() {
        return query;
    }

    public void setQuery(Query query) {
        this.query = query;
    }

    public Query getQueryOrRef() {
        if (query != null){
            return query;
        }else{
            return getReference();
        }
    }
}
