package com.tabourless.queue.Utils;

import android.text.TextUtils;
import android.util.Log;

import com.tabourless.queue.models.Counter;
import com.tabourless.queue.models.Customer;

import java.util.HashMap;
import java.util.Map;

import static com.tabourless.queue.App.COUNTER_SPINNER_AGE_OLD;
import static com.tabourless.queue.App.COUNTER_SPINNER_AGE_YOUNG;
import static com.tabourless.queue.App.COUNTER_SPINNER_DISABILITY_ABLED;
import static com.tabourless.queue.App.COUNTER_SPINNER_DISABILITY_DISABLED;
import static com.tabourless.queue.App.COUNTER_SPINNER_GENDER_FEMALE;
import static com.tabourless.queue.App.COUNTER_SPINNER_GENDER_MALE;

public class DatabaseHelper {

    private final static String TAG = DatabaseHelper.class.getSimpleName();

    public static String getJoinedKeys(String uid1, String uid2) {

        //merge two string keys and make the smallest one at first
        int compare = uid1.compareTo(uid2);
        if (compare < 0){
            System.out.println(uid1+"user 1 is before user 2"+uid2);
            return uid1+"_"+uid2;
        } else if (compare > 0) {
            System.out.println(uid2+"user 2 is before user 1"+uid1);
            return uid2+"_"+uid1;
        }
        else {
            System.out.println(uid2+" is same as "+uid1);
            return uid2+"_"+uid1;
        }

    }

    public static Map<String, Boolean> isMatchedCountersExist(Map<String, Counter> counters, Customer customer) {
        // Create a HashMap to add suitable counters to it
        Map<String, Boolean> suitableCounters = new HashMap<>();// Map for all suitable counters

        // Loop throw all queue counters to match suitable ones for pushed customer
        for (Object o : counters.entrySet()) {
            Map.Entry pair = (Map.Entry) o;
            Log.d(TAG, "getMatchedCounters counters map key/val = " + pair.getKey() + " = " + pair.getValue());
            Counter counter = counters.get(String.valueOf(pair.getKey()));
            if (counter != null && counter.isOpen()) {
                counter.setKey(String.valueOf(pair.getKey()));

                // Checking each counter if it's suitable for the current user or not
                if ((TextUtils.equals(customer.getGender(), COUNTER_SPINNER_GENDER_MALE) && !TextUtils.equals(counter.getGender(), COUNTER_SPINNER_GENDER_FEMALE))
                        || (TextUtils.equals(customer.getGender(), COUNTER_SPINNER_GENDER_FEMALE) && !TextUtils.equals(counter.getGender(), COUNTER_SPINNER_GENDER_MALE))){
                    // user is male, Counter is not for females only, lets check age and disability
                    // user is female, Counter is not for males only, lets check age and disability
                    Log.d(TAG, "Matching. is customer gender= "+ customer.getGender() + ". counter is not for gender "+ counter.getGender());
                    if ((customer.getAge() <60 && !TextUtils.equals(counter.getAge(), COUNTER_SPINNER_AGE_OLD))
                            || (customer.getAge() >=60 && !TextUtils.equals(counter.getAge(), COUNTER_SPINNER_AGE_YOUNG))){
                        // user is young, Counter is not for old only, lets check disability
                        // user is old, Counter is not for young only, lets check disability
                        Log.d(TAG, "Matching. is customer age = "+ customer.getAge() + ". Counter is not for age "+ counter.getAge());
                        if ((customer.isDisabled() && !TextUtils.equals(counter.getDisability(), COUNTER_SPINNER_DISABILITY_ABLED))
                                || (!customer.isDisabled() && !TextUtils.equals(counter.getDisability(), COUNTER_SPINNER_DISABILITY_DISABLED))){
                            // user is disabled, Counter is not for fit people only
                            // user is fit, Counter is not for disabled only
                            Log.d(TAG, "Matching. is customer disabled? = "+ customer.isDisabled()+ ". counter is not for "+ counter.getDisability());

                            Log.d(TAG, "Matching. Add counter = "+ counter.getName());
                            suitableCounters.put(counter.getKey(), true); // could be counter.name, counter.key, true
                        } // End of isDisabled
                    }// End of Age
                }// End of gender
            }// End of  counter != null && counter.isOpen
        }// End of loop throw all queue counters
        return suitableCounters;
    }


    public static Map<String, Counter> getMatchedCounters(Map<String, Counter> counters, Customer customer) {
        // Create a HashMap to add suitable counters to it
        Map<String, Counter> suitableCounters = new HashMap<>();// Map for all suitable counters

        // Loop throw all queue counters to match suitable ones for pushed customer
        for (Object o : counters.entrySet()) {
            Map.Entry pair = (Map.Entry) o;
            Log.d(TAG, "getMatchedCounters counters map key/val = " + pair.getKey() + " = " + pair.getValue());
            Counter counter = counters.get(String.valueOf(pair.getKey()));
            if (counter != null && counter.isOpen()) {
                counter.setKey(String.valueOf(pair.getKey()));

                // Checking each counter if it's suitable for the current user or not
                if ((TextUtils.equals(customer.getGender(), COUNTER_SPINNER_GENDER_MALE) && !TextUtils.equals(counter.getGender(), COUNTER_SPINNER_GENDER_FEMALE))
                        || (TextUtils.equals(customer.getGender(), COUNTER_SPINNER_GENDER_FEMALE) && !TextUtils.equals(counter.getGender(), COUNTER_SPINNER_GENDER_MALE))){
                    // user is male, Counter is not for females only, lets check age and disability
                    // user is female, Counter is not for males only, lets check age and disability
                    Log.d(TAG, "Matching. is customer gender= "+ customer.getGender() + ". counter is not for gender "+ counter.getGender());
                    if ((customer.getAge() <60 && !TextUtils.equals(counter.getAge(), COUNTER_SPINNER_AGE_OLD))
                            || (customer.getAge() >=60 && !TextUtils.equals(counter.getAge(), COUNTER_SPINNER_AGE_YOUNG))){
                        // user is young, Counter is not for old only, lets check disability
                        // user is old, Counter is not for young only, lets check disability
                        Log.d(TAG, "Matching. is customer age = "+ customer.getAge() + ". Counter is not for age "+ counter.getAge());
                        if ((customer.isDisabled() && !TextUtils.equals(counter.getDisability(), COUNTER_SPINNER_DISABILITY_ABLED))
                                || (!customer.isDisabled() && !TextUtils.equals(counter.getDisability(), COUNTER_SPINNER_DISABILITY_DISABLED))){
                            // user is disabled, Counter is not for fit people only
                            // user is fit, Counter is not for disabled only
                            Log.d(TAG, "Matching. is customer disabled? = "+ customer.isDisabled()+ ". counter is not for "+ counter.getDisability());

                            Log.d(TAG, "Matching. Add counter = "+ counter.getName());
                            suitableCounters.put(counter.getKey(), counter); // could be counter.name, counter.key, true
                        } // End of isDisabled
                    }// End of Age
                }// End of gender
            }// End of  counter != null && counter.isOpen
        }// End of loop throw all queue counters
        return suitableCounters;
    }
}
