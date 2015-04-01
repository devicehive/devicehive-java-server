package com.devicehive.controller;

import com.devicehive.auth.AllowedKeyAction;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.auth.HiveSecurityContext;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.controller.util.ResponseFactory;
import com.devicehive.controller.util.SimpleWaiter;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.json.strategies.JsonPolicyDef.Policy;
import com.devicehive.messages.handler.RestHandlerCreator;
import com.devicehive.messages.subscriptions.NotificationSubscription;
import com.devicehive.messages.subscriptions.NotificationSubscriptionStorage;
import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.wrappers.DeviceNotificationWrapper;
import com.devicehive.service.DeviceNotificationService;
import com.devicehive.service.DeviceService;
import com.devicehive.service.TimestampService;
import com.devicehive.util.LogExecutionTime;
import com.devicehive.util.ParseUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.CompletionCallback;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

import static com.devicehive.auth.AllowedKeyAction.Action.CREATE_DEVICE_NOTIFICATION;
import static com.devicehive.auth.AllowedKeyAction.Action.GET_DEVICE_NOTIFICATION;
import static com.devicehive.configuration.Constants.*;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NOTIFICATION_FROM_DEVICE;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NOTIFICATION_TO_DEVICE;
import static javax.ws.rs.core.Response.Status.*;

/**
 * REST controller for device notifications: <i>/device/{deviceGuid}/notification</i> and <i>/device/notification</i>.
 * See <a href="http://www.devicehive.com/restful#Reference/DeviceNotification">DeviceHive RESTful API:
 * DeviceNotification</a> for details.
 *
 * @author rroschin
 */
@Path("/device")
@LogExecutionTime
public class DeviceNotificationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceNotificationController.class);
    @EJB
    private DeviceNotificationService notificationService;
    @EJB
    private SubscriptionManager subscriptionManager;
    @EJB
    private DeviceService deviceService;
    @EJB
    private TimestampService timestampService;
    @Resource(name = "concurrent/DeviceHiveWaitService")
    private ManagedExecutorService mes;
    @Inject
    private HiveSecurityContext hiveSecurityContext;

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceNotification/poll">DeviceHive
     * RESTful API: DeviceNotification: poll</a>
     *
     * @param deviceGuid Device unique identifier.
     * @param timestamp  Timestamp of the last received command (UTC). If not specified, the server's timestamp is taken
     *                   instead.
     * @param timeout    Waiting timeout in seconds (default: 30 seconds, maximum: 60 seconds). Specify 0 to disable
     *                   waiting.
     */
    @GET
    @Path("/{deviceGuid}/notification/poll")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.KEY})
    @AllowedKeyAction(action = GET_DEVICE_NOTIFICATION)
    public void poll(
        @PathParam(DEVICE_GUID) final String deviceGuid,
        @QueryParam(NAMES) final String namesString,
        @QueryParam(TIMESTAMP) final String timestamp,
        @DefaultValue(Constants.DEFAULT_WAIT_TIMEOUT) @Min(0) @Max(Constants.MAX_WAIT_TIMEOUT)
        @QueryParam(WAIT_TIMEOUT) final long timeout,
        @Suspended final AsyncResponse asyncResponse) {

        poll(timeout, deviceGuid, namesString, asyncResponse, false);
    }

    @GET
    @Path("/notification/poll")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.KEY})
    @AllowedKeyAction(action = GET_DEVICE_NOTIFICATION)
    public void pollMany(
        @DefaultValue(Constants.DEFAULT_WAIT_TIMEOUT) @Min(0) @Max(Constants.MAX_WAIT_TIMEOUT)
        @QueryParam(WAIT_TIMEOUT) final long timeout,
        @QueryParam(DEVICE_GUIDS) String deviceGuidsString,
        @QueryParam(NAMES) final String namesString,
        @QueryParam(TIMESTAMP) final String timestamp,
        @Suspended final AsyncResponse asyncResponse) {

        poll(timeout, deviceGuidsString, namesString, asyncResponse, true);
    }

    private void poll(final long timeout,
                      final String deviceGuidsString,
                      final String namesString,
                      final AsyncResponse asyncResponse,
                      final boolean isMany) {
        final HivePrincipal principal = hiveSecurityContext.getHivePrincipal();

        asyncResponse.register(new CompletionCallback() {
            @Override
            public void onComplete(Throwable throwable) {
            }
        });

        mes.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    getOrWaitForNotifications(principal, deviceGuidsString, namesString, timeout, asyncResponse, isMany);
                } catch (Exception e) {
                    asyncResponse.resume(e);
                }
            }
        });
    }

    private void getOrWaitForNotifications(final HivePrincipal principal, final String deviceGuids,
                                                               final String names, long timeout,
                                                               final AsyncResponse asyncResponse, final boolean isMany) {
        LOGGER.debug("Device notification pollMany requested for : {}, {}.  Timeout = {}", deviceGuids, names, timeout);
        if (timeout <= 0) {
            asyncResponse.resume(ResponseFactory.response(Response.Status.OK, Collections.emptyList(),
                    Policy.NOTIFICATION_TO_CLIENT));
        }
        final List<String> availableDeviceGuids = deviceService.findGuidsWithPermissionsCheck(ParseUtil.getList(deviceGuids), principal);

        NotificationSubscriptionStorage storage = subscriptionManager.getNotificationSubscriptionStorage();
        UUID reqId = UUID.randomUUID();
        Set<NotificationSubscription> subscriptionSet = new HashSet<>();
        if (StringUtils.isNotEmpty(deviceGuids)) {
            for (String guid : availableDeviceGuids) {
                subscriptionSet.add(new NotificationSubscription(principal, guid, reqId, names,
                        RestHandlerCreator.createNotificationInsert(asyncResponse, isMany)));
            }
        } else {
            subscriptionSet.add(new NotificationSubscription(principal, Constants.NULL_SUBSTITUTE, reqId, names,
                    RestHandlerCreator.createNotificationInsert(asyncResponse, isMany)));
        }

        SimpleWaiter.subscribeAndWait(storage, subscriptionSet, RestHandlerCreator.getFutureTask(), timeout);
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceNotification/insert">DeviceHive
     * RESTful API: DeviceNotification: insert</a> Creates new device notification.
     *
     * @param guid         Device unique identifier.
     * @param notificationSubmit In the request body, supply a DeviceNotification resource. <table> <tr> <td>Property
     *                     Name</td> <td>Required</td> <td>Type</td> <td>Description</td> </tr> <tr>
     *                     <td>notification</td> <td>Yes</td> <td>string</td> <td>Notification name.</td> </tr> <tr>
     *                     <td>parameters</td> <td>No</td> <td>object</td> <td>Notification parameters, a JSON object
     *                     with an arbitrary structure.</td> </tr> </table>
     * @return If successful, this method returns a <a href="http://www.devicehive.com/restful#Reference/DeviceNotification">DeviceNotification</a>
     *         resource in the response body. <table> <tr> <tr>Property Name</tr> <tr>Type</tr> <tr>Description</tr>
     *         </tr> <tr> <td>id</td> <td>integer</td> <td>Notification identifier.</td> </tr> <tr> <td>timestamp</td>
     *         <td>datetime</td> <td>Notification timestamp (UTC).</td> </tr> </table>
     */
    @POST
    @Path("/{deviceGuid}/notification")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({HiveRoles.DEVICE, HiveRoles.ADMIN, HiveRoles.CLIENT, HiveRoles.KEY})
    @AllowedKeyAction(action = CREATE_DEVICE_NOTIFICATION)
    public Response insert(@PathParam(DEVICE_GUID) String guid,
                           @JsonPolicyDef(NOTIFICATION_FROM_DEVICE)
                           DeviceNotificationWrapper notificationSubmit) {
        LOGGER.debug("DeviceNotification insertAll requested");

        HivePrincipal principal = hiveSecurityContext.getHivePrincipal();
        if (notificationSubmit == null || notificationSubmit.getNotification() == null){
            LOGGER.debug("DeviceNotification insertAll proceed with error. Bad notification: notification is required.");
            return ResponseFactory.response(BAD_REQUEST,
                                            new ErrorResponse(BAD_REQUEST.getStatusCode(),
                                                              Messages.INVALID_REQUEST_PARAMETERS));
        }
        Device device = deviceService.findByGuidWithPermissionsCheck(guid, principal);
        if (device == null) {
            return ResponseFactory.response(NOT_FOUND,
                                            new ErrorResponse(NOT_FOUND.getStatusCode(),
                                                              String.format(Messages.DEVICE_NOT_FOUND, guid)));
        }
        if (device.getNetwork() == null) {
            return ResponseFactory.response(FORBIDDEN,
                                            new ErrorResponse(FORBIDDEN.getStatusCode(),
                                                              String.format(Messages.DEVICE_IS_NOT_CONNECTED_TO_NETWORK,
                                                                            guid)));
        }
        DeviceNotification message = notificationService.convertToMessage(notificationSubmit, device);
        notificationService.submitDeviceNotification(message, device);

        LOGGER.debug("DeviceNotification insertAll proceed successfully");
        return ResponseFactory.response(CREATED, message, NOTIFICATION_TO_DEVICE);
    }

}