package com.devicehive.websockets.handlers;




import com.devicehive.model.ApiInfo;
import com.devicehive.model.AuthLevel;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.Version;
import com.devicehive.websockets.json.ConvertorFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;


import javax.websocket.Session;
import java.text.DateFormat;
import java.util.Date;

public class ClientMessageHandlers implements HiveMessageHandlers {

    @Action(value = "authenticate", copyRequestId = true)
    public JsonObject processAuthenticate(JsonObject message, Session session) {
        String status = null;
        //TODO
        return JsonMessageFactory.createStatusResponce(status);
    }


    @Action(value = "command/insert", requredLevel = AuthLevel.USER, copyRequestId = true)
    public JsonObject processCommandInsert(JsonObject message, Session session) {
        //TODO
        String status = null;
        JsonObject jsonObject = JsonMessageFactory.createStatusResponce(status);
        return jsonObject;
    }

    @Action(value = "notification/subscribe", requredLevel = AuthLevel.USER, copyRequestId = true)
    public JsonObject processNotificationSubscribe(JsonObject message, Session session) {
        //TODO

        String status = null;
        JsonObject jsonObject = JsonMessageFactory.createStatusResponce(status);
        jsonObject.add("deviceGuids", new JsonObject());
        return jsonObject;

    }

    @Action(value = "notification/unsubscribe", requredLevel = AuthLevel.USER, copyRequestId = true)
    public JsonObject processNotificationUnsubscribe(JsonObject message, Session session) {
        //TODO

        String status = null;
        JsonObject jsonObject = JsonMessageFactory.createStatusResponce(status);
        jsonObject.add("deviceGuids", new JsonObject());
        return jsonObject;
    }

    /*
    @Action(value = "notification/insert")
    public JsonObject processNotificationInsert(JsonObject message, Session session) {
        return null;
    }
    @Action(value = "command/update")
    public JsonObject processCommandUpdate(JsonObject message, Session session) {
        return null;
    }*/


    @Action(value = "server/info", copyRequestId = true)
    public JsonObject processServerInfo(JsonObject message, Session session) {
        JsonObject jsonObject = JsonMessageFactory.createStatusResponce("success");
        ApiInfo apiInfo = new ApiInfo();
        apiInfo.setApiVersion(Version.VERSION);
        apiInfo.setServerTimestamp(new Date());
        apiInfo.setWebSocketServerUrl("fqfw");
        jsonObject.add("info", ConvertorFactory.createGson().toJsonTree(apiInfo));
        return jsonObject;
    }
}
