package com.devicehive.controller;

import com.devicehive.auth.AllowedKeyAction;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.auth.HiveSecurityContext;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.controller.converters.TimestampQueryParamParser;
import com.devicehive.controller.util.ResponseFactory;
import com.devicehive.controller.util.SimpleWaiter;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.messages.handler.RestHandlerCreator;
import com.devicehive.messages.subscriptions.NotificationSubscription;
import com.devicehive.messages.subscriptions.NotificationSubscriptionStorage;
import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.response.NotificationPollManyResponse;
import com.devicehive.model.wrappers.DeviceNotificationWrapper;
import com.devicehive.service.DeviceNotificationService;
import com.devicehive.service.DeviceService;
import com.devicehive.service.TimestampService;
import com.devicehive.util.LogExecutionTime;
import com.devicehive.util.ParseUtil;
import com.google.common.util.concurrent.Runnables;
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
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.FutureTask;

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
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceNotification/query">DeviceHive
     * RESTful API: DeviceNotification: query</a> Queries device notifications.
     *
     * @param guid         Device unique identifier.
     * @param startTs      Filter by notification start timestamp (UTC).
     * @param endTs        Filter by notification end timestamp (UTC).
     * @param notification Filter by notification name.
     * @param sortField    Result list sort field. Available values are Timestamp (default) and Notification.
     * @param sortOrderSt  Result list sort order. Available values are ASC and DESC.
     * @param take         Number of records to take from the result list (default is 1000).
     * @param skip         Number of records to skip from the result list.
     * @return If successful, this method returns array of <a href="http://www.devicehive
     *         .com/restful#Reference/DeviceNotification">DeviceNotification</a> resources in the response body. <table>
     *         <tr> <td>Property Name</td> <td>Type</td> <td>Description</td> </tr> <tr> <td>id</td> <td>integer</td>
     *         <td>Notification identifier</td> </tr> <tr> <td>timestamp</td> <td>datetime</td> <td>Notification
     *         timestamp (UTC)</td> </tr> <tr> <td>notification</td> <td>string</td> <td>Notification name</td> </tr>
     *         <tr> <td>parameters</td> <td>object</td> <td>Notification parameters, a JSON object with an arbitrary
     *         structure</td> </tr> </table>
     */
    @GET
    @Path("/{deviceGuid}/notification")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.KEY})
    @AllowedKeyAction(action = GET_DEVICE_NOTIFICATION)
    public Response query(@PathParam(DEVICE_GUID) String guid,
                          @QueryParam(START) String startTs,
                          @QueryParam(END) String endTs,
                          @QueryParam(NOTIFICATION) String notification,
                          @QueryParam(SORT_FIELD) @DefaultValue(TIMESTAMP) String sortField,
                          @QueryParam(SORT_ORDER) String sortOrderSt,
                          @QueryParam(TAKE) Integer take,
                          @QueryParam(SKIP) Integer skip,
                          @QueryParam(GRID_INTERVAL) Integer gridInterval) {

        LOGGER.debug("Device notification query requested for device {}", guid);
        Timestamp timestamp = TimestampQueryParamParser.parse(startTs);

        HivePrincipal principal = hiveSecurityContext.getHivePrincipal();
        Device device = deviceService.getDeviceWithNetworkAndDeviceClass(guid, principal);

        List<DeviceNotification> result = notificationService.getDeviceNotificationsList(Arrays.asList(device.getGuid()),
                StringUtils.isNoneEmpty(notification) ? Arrays.asList(notification) : null, timestamp, principal);

        LOGGER.debug("Device notification query request proceed successfully for device {}", guid);

        return ResponseFactory.response(Response.Status.OK, result, JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT);
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceNotification/get">DeviceHive RESTful
     * API: DeviceNotification: get</a> Gets information about device notification.
     *
     * @param guid           Device unique identifier.
     * @param notificationId Notification identifier.
     * @return If successful, this method returns a <a href="http://www.devicehive .com/restful#Reference/DeviceNotification">DeviceNotification</a>
     *         resource in the response body. <table> <tr> <td>Property Name</td> <td>Type</td> <td>Description</td>
     *         </tr> <tr> <td>id</td> <td>integer</td> <td>Notification identifier</td> </tr> <tr> <td>timestamp</td>
     *         <td>datetime</td> <td>Notification timestamp (UTC)</td> </tr> <tr> <td>notification</td> <td>string</td>
     *         <td>Notification name</td> </tr> <tr> <td>parameters</td> <td>object</td> <td>Notification parameters, a
     *         JSON object with an arbitrary structure</td> </tr> </table>
     */
    @GET
    @Path("/{deviceGuid}/notification/{id}")
    @AllowedKeyAction(action = GET_DEVICE_NOTIFICATION)
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.KEY})
    public Response get(@PathParam(DEVICE_GUID) String guid, @PathParam(ID) Long notificationId) {
        LOGGER.debug("Device notification requested. Guid {}, notification id {}", guid, notificationId);

        final HivePrincipal principal = hiveSecurityContext.getHivePrincipal();

        Device device = deviceService.findByGuidWithPermissionsCheck(guid, principal);
        if (device == null) {
            return ResponseFactory.response(NOT_FOUND, new ErrorResponse(NOT_FOUND.getStatusCode(),
                    String.format(Messages.DEVICE_NOT_FOUND, guid)));
        }

        DeviceNotification notification = notificationService.findByIdAndGuid(notificationId, guid);

        if (notification == null) {
            LOGGER.warn("Device command get failed. No command with id = {} found for device with guid = {}", notificationId, guid);
            return ResponseFactory.response(NOT_FOUND, new ErrorResponse(NOT_FOUND.getStatusCode(),
                    String.format(Messages.COMMAND_NOT_FOUND, notificationId)));
        }

        if (!notification.getDeviceGuid().equals(guid)) {
            LOGGER.debug("DeviceCommand wait request failed. Command with id = {} was not sent for device with guid = {}",
                    notificationId, guid);
            return ResponseFactory.response(BAD_REQUEST, new ErrorResponse(BAD_REQUEST.getStatusCode(),
                    String.format(Messages.COMMAND_NOT_FOUND, notificationId)));
        }

        LOGGER.debug("Device notification proceed successfully");

        return ResponseFactory.response(Response.Status.OK, notification, JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT);
    }

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

        poll(timeout, deviceGuid, namesString, timestamp, asyncResponse, false);
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

        poll(timeout, deviceGuidsString, namesString, timestamp, asyncResponse, true);
    }

    private void poll(final long timeout,
                      final String deviceGuidsString,
                      final String namesString,
                      final String timestamp,
                      final AsyncResponse asyncResponse,
                      final boolean isMany) {
        final HivePrincipal principal = hiveSecurityContext.getHivePrincipal();

        final Timestamp ts = TimestampQueryParamParser.parse(timestamp);
        final String devices = StringUtils.isNoneBlank(deviceGuidsString) ? deviceGuidsString : null;
        final String names = StringUtils.isNoneBlank(namesString) ? namesString : null;

        mes.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    getOrWaitForNotifications(principal, devices, names, ts, timeout, asyncResponse, isMany);
                } catch (Exception e) {
                    asyncResponse.resume(e);
                }
            }
        });
    }

    private void getOrWaitForNotifications(final HivePrincipal principal, final String devices,
                                                               final String names, final Timestamp timestamp, long timeout,
                                                               final AsyncResponse asyncResponse, final boolean isMany) {
        LOGGER.debug("Device notification pollMany requested for : {}, {}.  Timeout = {}", devices, names, timeout);
        if (timeout <= 0) {
            notificationService.submitEmptyResponse(asyncResponse);
        }

        final List<String> deviceGuids = ParseUtil.getList(devices);
        final List<String> commandNames = ParseUtil.getList(names);
        List<DeviceNotification> list = new ArrayList<>();

        if (timestamp != null) {
            list = notificationService.getDeviceNotificationsList(deviceGuids, commandNames, timestamp, principal);
        }

        if (!list.isEmpty()) {
            Response response;
            LOGGER.warn("Messages present in Redis: {}, {}", list.size(), list.get(0));
            if (isMany) {
                List<NotificationPollManyResponse> resultList = new ArrayList<>(list.size());
                for (DeviceNotification notification : list) {
                    resultList.add(new NotificationPollManyResponse(notification, notification.getDeviceGuid()));
                }
                response = ResponseFactory.response(Response.Status.OK, resultList, JsonPolicyDef.Policy.COMMAND_LISTED);
            } else {
                response = ResponseFactory.response(Response.Status.OK, list, JsonPolicyDef.Policy.COMMAND_LISTED);
            }
            asyncResponse.resume(response);
        } else {
            final UUID reqId = UUID.randomUUID();
            NotificationSubscriptionStorage storage = subscriptionManager.getNotificationSubscriptionStorage();
            Set<NotificationSubscription> subscriptionSet = new HashSet<>();

            if (StringUtils.isNotEmpty(devices)) {
                List<String> availableDevices = deviceService.findGuidsWithPermissionsCheck(ParseUtil.getList(devices), principal);
                for (String guid : availableDevices) {
                    subscriptionSet.add(new NotificationSubscription(principal, guid, reqId, names,
                            RestHandlerCreator.createNotificationInsert(asyncResponse, isMany)));
                }
            } else {
                subscriptionSet.add(new NotificationSubscription(principal, Constants.NULL_SUBSTITUTE, reqId, names,
                        RestHandlerCreator.createNotificationInsert(asyncResponse, isMany)));
            }

            if (!SimpleWaiter.subscribeAndWait(storage, subscriptionSet, new FutureTask<Void>(Runnables.doNothing(), null), timeout)) {
                notificationService.submitEmptyResponse(asyncResponse);
            }
        }

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