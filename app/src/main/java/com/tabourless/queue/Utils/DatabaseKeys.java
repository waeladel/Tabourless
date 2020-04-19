package com.tabourless.queue.Utils;

public class DatabaseKeys {

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
}
