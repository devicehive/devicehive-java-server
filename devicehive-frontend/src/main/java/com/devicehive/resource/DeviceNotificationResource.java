package com.devicehive.resource;

import com.devicehive.configuration.Constants;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.wrappers.DeviceNotificationWrapper;
import io.swagger.annotations.*;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NOTIFICATION_FROM_DEVICE;

/**
 * REST controller for device notifications: <i>/device/{deviceGuid}/notification</i> and <i>/device/notification</i>.
 * See <a href="http://www.devicehive.com/restful#Reference/DeviceNotification">DeviceHive RESTful API:
 * DeviceNotification</a> for details.
 *
 * @author rroschin
 */
@Path("/device")
@Api(tags = {"DeviceNotification"})
public interface DeviceNotificationResource {

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
     * .com/restful#Reference/DeviceNotification">DeviceNotification</a> resources in the response body. <table>
     * <tr> <td>Property Name</td> <td>Type</td> <td>Description</td> </tr> <tr> <td>id</td> <td>integer</td>
     * <td>Notification identifier</td> </tr> <tr> <td>timestamp</td> <td>datetime</td> <td>Notification
     * timestamp (UTC)</td> </tr> <tr> <td>notification</td> <td>string</td> <td>Notification name</td> </tr>
     * <tr> <td>parameters</td> <td>object</td> <td>Notification parameters, a JSON object with an arbitrary
     * structure</td> </tr> </table>
     */
    @GET
    @Path("/{deviceGuid}/notification")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'KEY') and hasPermission(null, 'GET_DEVICE_NOTIFICATION')")
    @ApiOperation(value = "Get notifications", notes = "Returns notifications by provided parameters",
            response = DeviceNotification.class)
    void query(
            @ApiParam(name = "deviceGuid", value = "Device GUID", required = true)
            @PathParam("deviceGuid")
            String guid,
            @ApiParam(name = "start", value = "Start timestamp")
            @QueryParam("start")
            String startTs,
            @ApiParam(name = "end", value = "End timestamp")
            @QueryParam("end")
            String endTs,
            @ApiParam(name = "notification", value = "Notification name")
            @QueryParam("notification")
            String notification,
            @ApiParam(name = "sortField", value = "Sort field")
            @QueryParam("sortField")
            @DefaultValue("timestamp")
            String sortField,
            @ApiParam(name = "sortOrder", value = "Sort order")
            @QueryParam("sortOrder")
            String sortOrderSt,
            @ApiParam(name = "take", value = "Limit param")
            @QueryParam("take")
            @DefaultValue(Constants.DEFAULT_TAKE_STR)
            Integer take,
            @ApiParam(name = "skip", value = "Skip param")
            @QueryParam("skip")
            @DefaultValue(Constants.DEFAULT_SKIP_STR)
            Integer skip,
            @Suspended
            AsyncResponse asyncResponse);

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceNotification/get">DeviceHive RESTful
     * API: DeviceNotification: get</a> Gets information about device notification.
     *
     * @param guid           Device unique identifier.
     * @param notificationId Notification identifier.
     * @return If successful, this method returns a <a href="http://www.devicehive .com/restful#Reference/DeviceNotification">DeviceNotification</a>
     * resource in the response body. <table> <tr> <td>Property Name</td> <td>Type</td> <td>Description</td>
     * </tr> <tr> <td>id</td> <td>integer</td> <td>Notification identifier</td> </tr> <tr> <td>timestamp</td>
     * <td>datetime</td> <td>Notification timestamp (UTC)</td> </tr> <tr> <td>notification</td> <td>string</td>
     * <td>Notification name</td> </tr> <tr> <td>parameters</td> <td>object</td> <td>Notification parameters, a
     * JSON object with an arbitrary structure</td> </tr> </table>
     */
    @GET
    @Path("/{deviceGuid}/notification/{id}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'KEY') and hasPermission(null, 'GET_DEVICE_NOTIFICATION')")
    @ApiOperation(value = "Get notification", notes = "Returns notification by device guid and notification id")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Returned notification by device guid and notification id",
                    response = DeviceNotification.class),
            @ApiResponse(code = 404, message = "If device or notification not found")
    })
    void get(
            @ApiParam(name = "deviceGuid", value = "Device GUID", required = true)
            @PathParam("deviceGuid")
            String guid,
            @ApiParam(name = "id", value = "Notification id", required = true)
            @PathParam("id")
            Long notificationId,
            @Suspended final AsyncResponse asyncResponse);

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceNotification/poll">DeviceHive
     * RESTful API: DeviceNotification: poll</a>
     *
     * @param deviceGuid Device unique identifier.
     * @param timestamp  Timestamp of the last received notification (UTC). If not specified, the server's timestamp is taken
     *                   instead.
     * @param timeout    Waiting timeout in seconds (default: 30 seconds, maximum: 60 seconds). Specify 0 to disable
     *                   waiting.
     */
    @GET
    @Path("/{deviceGuid}/notification/poll")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'KEY') and hasPermission(null, 'GET_DEVICE_NOTIFICATION')")
    @ApiOperation(value = "Poll for notifications ", notes = "Polls new device notifications for specified device Guid.\n" +
            "\n" +
            "This method returns all device notifications that were created after specified timestamp.\n" +
            "\n" +
            "In the case when no notifications were found, the method blocks until new notification is received. " +
            "If no notifications are received within the waitTimeout period, the server returns an empty response." +
            " In this case, to continue polling, the client should repeat the call with the same timestamp value."
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK",
                    response = DeviceNotification.class,
                    responseContainer = "List")

    })
    void poll(
            @ApiParam(name = "deviceGuid", value = "Device GUID", required = true)
            @PathParam("deviceGuid")
            String deviceGuid,
            @ApiParam(name = "names", value = "Notification names")
            @QueryParam("names")
            String namesString,
            @ApiParam(name = "timestamp", value = "Timestamp to start from")
            @QueryParam("timestamp")
            String timestamp,
            @ApiParam(name = "waitTimeout", value = "Wait timeout")
            @DefaultValue(Constants.DEFAULT_WAIT_TIMEOUT)
            @Min(0) @Max(Constants.MAX_WAIT_TIMEOUT)
            @QueryParam("waitTimeout")
            long timeout,
            @Suspended AsyncResponse asyncResponse) throws Exception;

    @GET
    @Path("/notification/poll")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'KEY') and hasPermission(null, 'GET_DEVICE_NOTIFICATION')")
    @ApiOperation(value = "Poll for notifications ", notes = "Polls new device notifications.\n" +
            "\n" +
            "This method returns all device notifications that were created after specified timestamp.\n" +
            "\n" +
            "In the case when no notifications were found, the method blocks until new notification is received." +
            " If no notifications are received within the waitTimeout period, the server returns an empty response." +
            " In this case, to continue polling, the client should repeat the call with the same timestamp value."
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK",
                    response = DeviceNotification.class,
                    responseContainer = "List")
    })
    void pollMany(
            @ApiParam(name = "waitTimeout", value = "Wait timeout")
            @DefaultValue(Constants.DEFAULT_WAIT_TIMEOUT)
            @Min(0) @Max(Constants.MAX_WAIT_TIMEOUT)
            @QueryParam("waitTimeout")
            long timeout,
            @ApiParam(name = "deviceGuids", value = "Device guids")
            @QueryParam("deviceGuids")
            String deviceGuidsString,
            @ApiParam(name = "names", value = "Notification names")
            @QueryParam("names")
            String namesString,
            @ApiParam(name = "timestamp", value = "Timestamp to start from")
            @QueryParam("timestamp")
            String timestamp,
            @Suspended AsyncResponse asyncResponse) throws Exception;

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceNotification/insert">DeviceHive
     * RESTful API: DeviceNotification: insert</a> Creates new device notification.
     *
     * @param guid               Device unique identifier.
     * @param notificationSubmit In the request body, supply a DeviceNotification resource. <table> <tr> <td>Property
     *                           Name</td> <td>Required</td> <td>Type</td> <td>Description</td> </tr> <tr>
     *                           <td>notification</td> <td>Yes</td> <td>string</td> <td>Notification name.</td> </tr> <tr>
     *                           <td>parameters</td> <td>No</td> <td>object</td> <td>Notification parameters, a JSON object
     *                           with an arbitrary structure.</td> </tr> </table>
     * @return If successful, this method returns a <a href="http://www.devicehive.com/restful#Reference/DeviceNotification">DeviceNotification</a>
     * resource in the response body. <table> <tr> <tr>Property Name</tr> <tr>Type</tr> <tr>Description</tr>
     * </tr> <tr> <td>id</td> <td>integer</td> <td>Notification identifier.</td> </tr> <tr> <td>timestamp</td>
     * <td>datetime</td> <td>Notification timestamp (UTC).</td> </tr> </table>
     */
    @POST
    @Path("/{deviceGuid}/notification")
    @Consumes(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'KEY') and hasPermission(null, 'CREATE_DEVICE_NOTIFICATION')")
    @ApiOperation(value = "Create notification", notes = "Creates notification")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "notification sent", response = DeviceNotification.class),
            @ApiResponse(code = 404, message = "If device not found"),
            @ApiResponse(code = 400, message = "If request is malformed"),
            @ApiResponse(code = 403, message = "If device is not connected to network")
    })
    void insert(
            @ApiParam(name = "deviceGuid", value = "Device GUID", required = true)
            @PathParam("deviceGuid")
            String guid,
            @ApiParam(value = "Notification body", required = true, defaultValue = "{}")
            @JsonPolicyDef(NOTIFICATION_FROM_DEVICE)
            DeviceNotificationWrapper notificationSubmit,
            @Suspended final AsyncResponse asyncResponse);
}
