package com.devicehive.websockets.handlers;

import com.devicehive.auth.HiveRoles;
import com.devicehive.auth.HiveSecurityContext;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.dao.DeviceDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.GsonFactory;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.model.Device;
import com.devicehive.model.Equipment;
import com.devicehive.model.NullableWrapper;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.service.DeviceService;
import com.devicehive.websockets.converters.WebSocketResponse;
import com.devicehive.websockets.handlers.annotations.Action;
import com.devicehive.websockets.handlers.annotations.WsParam;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.websocket.Session;
import java.util.HashSet;
import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.DEVICE_PUBLISHED_DEVICE_AUTH;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.DEVICE_SUBMITTED;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

public class DeviceHandlers extends WebsocketHandlers {

    private static final Logger logger = LoggerFactory.getLogger(DeviceHandlers.class);
    @EJB
    private DeviceDAO deviceDAO;
    @EJB
    private DeviceService deviceService;

    @Inject
    private HiveSecurityContext hiveSecurityContext;

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#WsReference/Device/deviceget">WebSocketAPI: Device:
     * device/get</a> Gets information about the current device.
     *
     * @return Json object with the following structure
     *         <pre>
     *                                                                         {
     *                                                                           "action": {string},
     *                                                                           "status": {string},
     *                                                                           "requestId": {object},
     *                                                                           "device": {
     *                                                                             "id": {guid},
     *                                                                             "name": {string},
     *                                                                             "status": {string},
     *                                                                             "data": {object},
     *                                                                             "network": {
     *                                                                               "id": {integer},
     *                                                                               "name": {string},
     *                                                                               "description": {string}
     *                                                                             },
     *                                                                             "deviceClass": {
     *                                                                               "id": {integer},
     *                                                                               "name": {string},
     *                                                                               "version": {string},
     *                                                                               "isPermanent": {boolean},
     *                                                                               "offlineTimeout": {integer},
     *                                                                               "data": {object}
     *                                                                              }
     *                                                                            }
     *                                                                         }
     *                                                                         </pre>
     */
    @Action(value = "device/get")
    @RolesAllowed({HiveRoles.DEVICE})
    public WebSocketResponse processDeviceGet() {
        Device device = hiveSecurityContext.getHivePrincipal().getDevice();
        Device toResponse = device == null ? null : deviceDAO.findByUUIDWithNetworkAndDeviceClass(device.getGuid());
        WebSocketResponse response = new WebSocketResponse();
        response.addValue(Constants.DEVICE, toResponse, DEVICE_PUBLISHED_DEVICE_AUTH);
        return response;
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#WsReference/Device/devicesave">WebSocketAPI: Device:
     * device/save</a> Registers or updates a device. A valid device key is required in the deviceKey parameter in order
     * to update an existing device.
     *
     * @param message Json object with the following structure
     *                <pre>
     *
     *                                     {
     *
     *                                       "action": {string},
     *
     *                                       "requestId": {object},
     *
     *                                       "deviceId": {guid},
     *
     *                                       "deviceKey": {string},
     *
     *                                       "device": {
     *
     *                                         "key": {string},
     *
     *                                         "name": {string},
     *
     *                                         "status": {string},
     *
     *                                         "data": {object},
     *
     *                                         "network": {integer or object},
     *
     *                                         "deviceClass": {integer or object},
     *
     *                                         "equipment": [
     *
     *                                         {
     *
     *                                          "name": {string},
     *
     *                                          "code": {string},
     *
     *                                          "type": {string},
     *
     *                                          "data": {object}
     *
     *                                         }
     *
     *                                         ]
     *
     *                                       }
     *
     *                                     }
     *
     *                                     </pre>
     * @param session Current session
     * @return Json object with the following structure
     *         <pre>
     *                                                                         {
     *                                                                           "action": {string},
     *                                                                           "status": {string},
     *                                                                           "requestId": {object}
     *                                                                         }
     *                                                                         </pre>
     */
    @Action(value = "device/save")
    @PermitAll
    public WebSocketResponse processDeviceSave(@WsParam(Constants.DEVICE_ID) String deviceId,
                                               @WsParam(Constants.DEVICE_KEY) String deviceKey,
                                               @WsParam(Constants.DEVICE) @JsonPolicyApply(DEVICE_SUBMITTED)
                                               DeviceUpdate device,
                                               JsonObject message,
                                               Session session) {
        logger.debug("device/save process started for session {}", session.getId());
        if (deviceId == null) {
            throw new HiveException(Messages.DEVICE_GUID_REQUIRED, SC_BAD_REQUEST);
        }
        if (deviceKey == null) {
            throw new HiveException(Messages.EMPTY_DEVICE_KEY, SC_BAD_REQUEST);
        }
        device.setGuid(new NullableWrapper<>(deviceId));
        Gson gsonForEquipment = GsonFactory.createGson();
        Set<Equipment> equipmentSet = gsonForEquipment.fromJson(
            message.get(Constants.EQUIPMENT),
            new TypeToken<HashSet<Equipment>>() {
            }.getType());

        if (equipmentSet != null) {
            equipmentSet.remove(null);
        }
        deviceService.deviceSaveAndNotify(device, equipmentSet, hiveSecurityContext.getHivePrincipal());
        logger.debug("device/save process ended for session  {}", session.getId());
        return new WebSocketResponse();
    }

}
