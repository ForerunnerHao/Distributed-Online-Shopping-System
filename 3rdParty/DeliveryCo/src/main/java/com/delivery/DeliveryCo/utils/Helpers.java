package com.delivery.DeliveryCo.utils;

import java.util.UUID;

import lombok.experimental.UtilityClass;

/**Helper class, contain all helper methods. */
@UtilityClass
public class Helpers {
    /**Convert UUID to String, return null if there is IllegalArgException */
    public String UUIDtoString(UUID uuid){
        String re_str;
        try{
            re_str = uuid.toString();
        }
        catch (IllegalArgumentException e){
            return null;
        }
        return re_str;
    }
    /**Convert String to UUID, return null if there is IllegalArgException */
    public UUID StringtoUUID(String string){
        UUID uuid;
        try{
            uuid = UUID.fromString(string);
        }
        catch (IllegalArgumentException e){
            return null;
        }
        return uuid;
    }
}
