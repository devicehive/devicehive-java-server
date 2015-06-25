package com.devicehive.resource;

import com.devicehive.configuration.Constants;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.wrappers.DeviceNotificationWrapper;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static com.devicehive.configuration.Constants.*;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NOTIFICATION_FROM_DEVICE;

/**
 * REST controller for device notifications: <i>/device/{deviceGuid}/notification</i> and <i>/device/notification</i>.
 * See <a href="http://www.devicehive.com/restful#Reference/DeviceNotification">DeviceHive RESTful API:
 * DeviceNotification</a> for details.
 *
 * @author rroschin
 */
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
     *         .com/restful#Reference/DeviceNotification">DeviceNotification</a> resources in the response body. <table>
     *         <tr> <td>Property Name</td> <td>Type</td> <td>Description</td> </tr> <tr> <td>id</td> <td>integer</td>
     *         <td>Notification identifier</td> </tr> <tr> <td>timestamp</td> <td>datetime</td> <td>Notification
     *         timestamp (UTC)</td> </tr> <tr> <td>notification</td> <td>string</td> <td>Notification name</td> </tr>
     *         <tr> <td>parameters</td> <td>object</td> <td>Notification parameters, a JSON object with an arbitrary
     *         structure</td> </tr> </table>
     */
    @GET
    @Path("/{deviceGuid}/notification")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'KEY') and hasPermission(null, 'GET_DEVICE_NOTIFICATION')")
    Response query(
            @PathParam(DEVICE_GUID) String guid,
            @QueryParam(START) String startTs,
            @QueryParam(END) String endTs,
            @QueryParam(NOTIFICATION) String notification,
            @QueryParam(SORT_FIELD) @DefaultValue(TIMESTAMP) String sortField,
            @QueryParam(SORT_ORDER) String sortOrderSt,
            @QueryParam(TAKE) @DefaultValue(Constants.DEFAULT_TAKE_STR) Integer take,
            @QueryParam(SKIP) Integer skip,
            @QueryParam(GRID_INTERVAL) Integer gridInterval);

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
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'KEY') and hasPermission(null, 'GET_DEVICE_NOTIFICATION')")
    Response get(
            @PathParam(DEVICE_GUID) String guid,
            @PathParam(ID) Long notificationId);

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
    void poll(
            @PathParam(DEVICE_GUID) String deviceGuid,
            @QueryParam(NAMES) String namesString,
            @QueryParam(TIMESTAMP) String timestamp,
            @DefaultValue(Constants.DEFAULT_WAIT_TIMEOUT) @Min(0) @Max(Constants.MAX_WAIT_TIMEOUT)
            @QueryParam(WAIT_TIMEOUT) long timeout,
            @Suspended AsyncResponse asyncResponse);

    @GET
    @Path("/notification/poll")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'KEY') and hasPermission(null, 'GET_DEVICE_NOTIFICATION')")
    void pollMany(
            @DefaultValue(Constants.DEFAULT_WAIT_TIMEOUT) @Min(0) @Max(Constants.MAX_WAIT_TIMEOUT)
            @QueryParam(WAIT_TIMEOUT) long timeout,
            @QueryParam(DEVICE_GUIDS) String deviceGuidsString,
            @QueryParam(NAMES) String namesString,
            @QueryParam(TIMESTAMP) String timestamp,
            @Suspended AsyncResponse asyncResponse);

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
    @PreAuthorize("hasAnyRole('DEVICE', 'CLIENT', 'ADMIN', 'KEY') and hasPermission(null, 'CREATE_DEVICE_NOTIFICATION')")
    Response insert(
            @PathParam(DEVICE_GUID)
            String guid,
            @JsonPolicyDef(NOTIFICATION_FROM_DEVICE)
            DeviceNotificationWrapper notificationSubmit);
}
