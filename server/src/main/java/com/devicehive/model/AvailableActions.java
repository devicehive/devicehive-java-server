package com.devicehive.model;


import java.util.HashSet;
import java.util.Set;

public class AvailableActions {
    private static Set knownActions = new HashSet() {{
        add("GETNETWORK");
        add("GETDEVICE");
        add("GETDEVICESTATE");
        add("GETDEVICENOTIFICATION");
        add("GETDEVICECOMMAND");
        add("REGISTERDEVICE");
        add("CREATEDEVICENOTIFICATION");
        add("CREATEDEVICECOMMAND");
        add("UPDATEDEVICECOMMAND");
    }};

    public static boolean isAvailable(String action){
        String actionUpper = action.toUpperCase();
        return knownActions.contains(actionUpper);
    }

    public static boolean validate(Set<String> actions){
        for(String current : actions){
            String actionUpper = current.toUpperCase();
            if (!knownActions.contains(actionUpper)){
                return false;
            }
        }
        return true;
    }

}
