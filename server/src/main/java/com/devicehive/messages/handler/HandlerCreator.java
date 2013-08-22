package com.devicehive.messages.handler;

import com.google.gson.JsonElement;

public interface HandlerCreator {

    Runnable getHandler(JsonElement message);
}
