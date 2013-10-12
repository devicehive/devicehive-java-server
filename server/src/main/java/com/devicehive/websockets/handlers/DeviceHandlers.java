package com.devicehive.websockets.handlers;

import com.devicehive.auth.HiveRoles;
import com.devicehive.dao.DeviceDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.GsonFactory;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.model.*;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.service.*;
import com.devicehive.util.LogExecutionTime;
import com.devicehive.util.ThreadLocalVariablesKeeper;
import com.devicehive.websockets.handlers.annotations.Action;
import com.devicehive.websockets.handlers.annotations.WebsocketController;
import com.devicehive.websockets.handlers.annotations.WsParam;
import com.devicehive.websockets.converters.WebSocketResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.websocket.Session;
import java.util.HashSet;
import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

@LogExecutionTime
@WebsocketController
public class DeviceHandlers implements WebsocketHandlers {

    private static final Logger logger = LoggerFactory.getLogger(DeviceHandlers.class);
    @EJB
    private DeviceDAO deviceDAO;
    @EJB
    private DeviceService deviceService;


    /**
     * Implementation of <a href="http://www.devicehive.com/restful#WsReference/Device/deviceget">WebSocketAPI:
     * Device: device/get</a>
     * Gets information about the current device.
     *
     * @return Json object with the following structure
     *         <pre>
     *                                                 {
     *                                                   "action": {string},
     *                                                   "status": {string},
     *                                                   "requestId": {object},
     *                                                   "device": {
     *                                                     "id": {guid},
     *                                                     "name": {string},
     *                                                     "status": {string},
     *                                                     "data": {object},
     *                                                     "network": {
     *                                                       "id": {integer},
     *                                                       "name": {string},
     *                                                       "description": {string}
     *                                                     },
     *                                                     "deviceClass": {
     *                                                       "id": {integer},
     *                                                       "name": {string},
     *                                                       "version": {string},
     *                                                       "isPermanent": {boolean},
     *                                                       "offlineTimeout": {integer},
     *                                                       "data": {object}
     *                                                      }
     *                                                    }
     *                                                 }
     *                                                 </pre>
     */
    @Action(value = "device/get")
    @RolesAllowed({HiveRoles.DEVICE})
    public WebSocketResponse processDeviceGet() {
        Device device = ThreadLocalVariablesKeeper.getPrincipal().getDevice();
        Device toResponse = device == null ? null : deviceDAO.findByUUIDWithNetworkAndDeviceClass(device.getGuid());
        WebSocketResponse response = new WebSocketResponse();
        response.addValue("device", toResponse, DEVICE_PUBLISHED_DEVICE_AUTH);
        return response;
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#WsReference/Device/devicesave">WebSocketAPI:
     * Device: device/save</a>
     * Registers or updates a device. A valid device key is required in the deviceKey parameter in order to update an
     * existing device.
     *
     * @param message Json object with the following structure
     *                <pre>
     *                                                                                           {
     *                                                                                             "action": {string},
     *                                                                                             "requestId": {object},
     *                                                                                             "deviceId": {guid},
     *                                                                                             "deviceKey": {string},
     *                                                                                             "device": {
     *                                                                                               "key": {string},
     *                                                                                               "name": {string},
     *                                                                                               "status": {string},
     *                                                                                               "data": {object},
     *                                                                                               "network": {integer or object},
     *                                                                                               "deviceClass": {integer or object},
     *                                                                                               "equipment": [
     *                                                                                               {
     *                                                                                                "name": {string},
     *                                                                                                "code": {string},
     *                                                                                                "type": {string},
     *                                                                                                "data": {object}
     *                                                                                               }
     *                                                                                               ]
     *                                                                                             }
     *                                                                                           }
     *                                                                                           </pre>
     * @param session Current session
     * @return Json object with the following structure
     *         <pre>
     *                                                 {
     *                                                   "action": {string},
     *                                                   "status": {string},
     *                                                   "requestId": {object}
     *                                                 }
     *                                                 </pre>
     */
    @Action(value = "device/save")
    @PermitAll
    public WebSocketResponse processDeviceSave(@WsParam("deviceId") String deviceId,
                                               @WsParam("deviceKey") String deviceKey,
                                               @WsParam("device") @JsonPolicyApply(DEVICE_PUBLISHED) DeviceUpdate device,
                                               JsonObject message,
                                               Session session) {
        logger.debug("device/save process started for session {}", session.getId());
        if (deviceId == null) {
            throw new HiveException("Device ID is undefined!", SC_BAD_REQUEST);
        }
        if (deviceKey == null) {
            throw new HiveException("Device key is undefined!", SC_BAD_REQUEST);
        }
        device.setGuid(new NullableWrapper<>(deviceId));
        Gson gsonForEquipment = GsonFactory.createGson();
        boolean useExistingEquipment = message.get("equipment") == null;
        Set<Equipment> equipmentSet = gsonForEquipment.fromJson(
                message.get("equipment"),
                new TypeToken<HashSet<Equipment>>() {
                }.getType());

        if (equipmentSet != null) {
            equipmentSet.remove(null);
        }
        deviceService.deviceSaveAndNotify(device, equipmentSet, ThreadLocalVariablesKeeper.getPrincipal(),
                useExistingEquipment);
        logger.debug("device/save process ended for session  {}", session.getId());
        return new WebSocketResponse();
    }

}
