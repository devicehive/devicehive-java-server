package com.devicehive.resource.util;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashSet;

public interface JsonTypes {
    Type STRING_SET_TYPE = new TypeToken<HashSet<String>>() {}.getType();
}
