package com.devicehive.model.enums;

public enum SearchableField {
    ID("id"),
    GUID("deviceGuid"),
    TIMESTAMP("timestamp"),
    DEVICE_GUID("deviceGuid"),  //need this duplication to separate cases of single and multiple deviceGuid usage
    NOTIFICATION("notification"),
    COMMAND("command"),
    STATUS("status"),
    IS_UPDATED("isUpdated");

    private String field;

    SearchableField(String field) {
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
