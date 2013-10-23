/*
* DeviceHive 1.1
* (c) 2012 DataArt Apps
* MIT license
*
* Client library to access DeviceHive service.
*/

(function ($, window) {
"use strict";

if (typeof($) !== "function") {
    throw new Error("DeviceHive: jQuery is not found");
}

if (!window.JSON) {
    throw new Error("DeviceHive: JSON parser is not found");
}

// private functions
var
    parseDate = function (date) {
        return new Date(date.substring(0, 4), parseInt(date.substring(5, 7), 10) - 1, date.substring(8, 10),
            date.substring(11, 13), date.substring(14, 16), date.substring(17, 19), date.substring(20, 23));
    },

    formatDate = function (date) {
        if (Object.prototype.toString.call(date) === '[object String]')
            return date; // already formatted string - do not modify

        if (Object.prototype.toString.call(date) !== '[object Date]')
            throw SyntaxError("Invalid object type");

        var pad = function (value, length) {
            value = String(value);
            length = length || 2;
            while (value.length < length)
                value = "0" + value;
            return value;
        };

        return date.getFullYear() + "-" + pad(date.getMonth() + 1) + "-" + pad(date.getDate()) + "T" +
            pad(date.getHours()) + ":" + pad(date.getMinutes()) + ":" + pad(date.getSeconds()) + "." + pad(date.getMilliseconds(), 3);
    },

    encodeBase64 = function (data) {
        var b64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
        var o1, o2, o3, h1, h2, h3, h4, bits, i = 0, ac = 0, enc = "", tmp_arr = []; 
        if (!data) {
            return data;
        }
        do { // pack three octets into four hexets
            o1 = data.charCodeAt(i++);
            o2 = data.charCodeAt(i++);
            o3 = data.charCodeAt(i++);
            bits = o1 << 16 | o2 << 8 | o3;
            h1 = bits >> 18 & 0x3f;
            h2 = bits >> 12 & 0x3f;
            h3 = bits >> 6 & 0x3f;
            h4 = bits & 0x3f;

            // use hexets to index into b64, and append result to encoded string
            tmp_arr[ac++] = b64.charAt(h1) + b64.charAt(h2) + b64.charAt(h3) + b64.charAt(h4);
        } while (i < data.length);
        enc = tmp_arr.join('');
        var r = data.length % 3;
        return (r ? enc.slice(0, r - 3) : enc) + '==='.slice(r || 3);
    },

    ajax = function (self, method, url, params) {
        var settings = {
            type: method,
            url: self.serviceUrl + url,
            dataType: "json",
            data: params,
            context: self,
            xhrFields: { withCredentials: true },
            beforeSend: function (jqXHR, settings) {
                jqXHR.setRequestHeader("Authorization", "Bearer " + self.accessKey)
            }
        };
        if (params && (method == "POST" || method == "PUT")) {
            settings.contentType = "application/json";
            settings.data = JSON.stringify(params);
        }
        return $.ajax(settings);
    },

    query = function (self, method, url, params) { // ajax then parse error response
        return ajax(self, method, url, params).pipe(null, function (xhr) {
            var message = "DeviceHive server error";
            if (xhr.responseText) {
                try { message += " - " + $.parseJSON(xhr.responseText).message; }
                catch (e) { message += " - " + xhr.responseText; }
            }
            return $.Deferred().reject(message, xhr);
        });
    },

    ensureConnectedState = function (state) {
        if (state === deviceHive.channelState.disconnected) {
            throw new Error("DeviceHive: Channel is not opened, call the .openChannel() method first");
        }
        if (state === deviceHive.channelState.connecting) {
            throw new Error("DeviceHive: Channel has not been initialized, use .openChannel().done() to run logic after the channel is initialized");
        }
    },

    changeChannelState = function (self, newState, oldState) {
        oldState = oldState || self.channelState;
        if (oldState === self.channelState) {
            self.channelState = newState;
            $(self).triggerHandler("onChannelStateChanged", [{ oldState: oldState, newState: newState }]);
            return true;
        }
        return false;
    };

// DeviceHive object constructor
var deviceHive = function (serviceUrl, accessKey) {
    this.serviceUrl = serviceUrl;
    this.accessKey = accessKey;
};

// DeviceHive channel states
deviceHive.channelState = {
    disconnected: 0, // channel is not connected
    connecting: 1,   // channel is being connected
    connected: 2     // channel is connected
};

deviceHive.prototype = {

    // current channel state
    channelState: deviceHive.channelState.disconnected,

    // gets a list of networks
    // filter object may include the following properties:
    //    - name: filter by network name
    //    - namePattern: filter by network name pattern
    //    - sortField: result list sort field: ID or Name
    //    - sortOrder: result list sort order: ASC or DESC
    //    - take: number of records to take from the result list
    //    - skip: number of records to skip from the result list
    getNetworks: function (filter) {
        return query(this, "GET", "/network", filter);
    },

    // gets information about a network and associated devices
    getNetwork: function (networkId) {
        return query(this, "GET", "/network/" + networkId);
    },

    // gets a list of devices
    // filter object may include the following properties:
    //    - name: filter by device name
    //    - namePattern: filter by device name pattern
    //    - status: filter by device status
    //    - networkId: filter by associated network identifier
    //    - networkName: filter by associated network name
    //    - deviceClassId: filter by associated device class identifier
    //    - deviceClassName: filter by associated device class name
    //    - deviceClassVersion: filter by associated device class version
    //    - sortField: result list sort field: Name, Status, Network or DeviceClass
    //    - sortOrder: result list sort order: ASC or DESC
    //    - take: number of records to take from the result list
    //    - skip: number of records to skip from the result list
    getDevices: function(filter) {
        return query(this, "GET", "/device", filter);
    },

    // gets information about a device
    getDevice: function (deviceId) {
        return query(this, "GET", "/device/" + deviceId);
    },

    // gets information about a device class and associated equipment
    getDeviceClass: function (deviceClassId) {
        return query(this, "GET", "/device/class/" + deviceClassId);
    },

    // gets a list of device equipment states (current state of device equipment)
    getEquipmentState: function (deviceId) {
        return query(this, "GET", "/device/" + deviceId + "/equipment");
    },

    // gets a list of notifications generated by the device
    // filter object may include the following properties:
    //    - start: filter by notification start timestamp (inclusive, UTC)
    //    - end: filter by notification end timestamp (inclusive, UTC)
    //    - notification: filter by notification name
    //    - sortField: result list sort field: Timestamp (default) or Notification
    //    - sortOrder: result list sort order: ASC or DESC
    //    - take: number of records to take from the result list
    //    - skip: number of records to skip from the result list
    getNotifications: function (deviceId, filter) {
        if (filter && filter.start) { filter.start = formatDate(filter.start); }
        if (filter && filter.end) { filter.end = formatDate(filter.end); }
        return query(this, "GET", "/device/" + deviceId + "/notification", filter);
    },

    // gets information about a device notification
    getNotification: function (deviceId, notificationId) {
        return query(this, "GET", "/device/" + deviceId + "/notification/" + notificationId);
    },

    // gets a list of commands previously sent to the device
    // filter object may include the following properties:
    //    - start: filter by command start timestamp (inclusive, UTC)
    //    - end: filter by command end timestamp (inclusive, UTC)
    //    - command: filter by command name
    //    - status: filter by command status
    //    - sortField: result list sort field: Timestamp (default), Command or Status
    //    - sortOrder: result list sort order: ASC or DESC
    //    - take: number of records to take from the result list
    //    - skip: number of records to skip from the result list
    getCommands: function (deviceId, filter) {
        if (filter && filter.start) { filter.start = formatDate(filter.start); }
        if (filter && filter.end) { filter.end = formatDate(filter.end); }
        return query(this, "GET", "/device/" + deviceId + "/command", filter);
    },

    // gets information about a device command
    getCommand: function (deviceId, commandId) {
        return query(this, "GET", "/device/" + deviceId + "/command/" + commandId);
    },

    // gets information about the logged-in user and associated networks
    getCurrentUser: function () {
        return query(this, "GET", "/user/current");
    },

    // updates information about the logged-in user
    updateCurrentUser: function (user) {
        return query(this, "PUT", "/user/current/", user);
    },

    // opens a communication channel to the server
    // supported channels: webSockets, longPolling
    openChannel: function (channels) {
        if (!changeChannelState(this, deviceHive.channelState.connecting, deviceHive.channelState.disconnected)) {
            return $.Deferred().resolve().promise(); // connection already opened
        }

        var self = this;
        var infoQuery = this.serverInfo ? $.Deferred().resolve(this.serverInfo) : query(this, "GET", "/info");
        var deferred = infoQuery.pipe(function (info) {
            self.serverInfo = info;

            if (!channels) {
                channels = [];
                $.each(deviceHive.channels, function (t) { channels.push(t); });
            }
            else if (!$.isArray(channels)) {
                channels = [channels];
            }

            var deferred = $.Deferred().reject("DeviceHive: None of the specified channels are supported");
            $.each(channels, function () { // enumerate all channels in order
                var channel = this;
                if (deviceHive.channels[channel]) {
                    deferred = deferred.pipe(null, function () { // in case of failure - try next channel
                        self.channel = new deviceHive.channels[channel](self);
                        return self.channel.open();
                    });
                }
            });
            return deferred;
        });

        return deferred
            .done(function () { changeChannelState(self, deviceHive.channelState.connected); })
            .fail(function () { changeChannelState(self, deviceHive.channelState.disconnected); })
            .promise();
    },

    // closes the communications channel to the server
    closeChannel: function () {
        if (this.channelState === deviceHive.channelState.disconnected)
            return;

        if (this.channel) {
            this.channel.close();
            this.channel = null;
        }
        changeChannelState(this, deviceHive.channelState.disconnected);
    },

    // subscribes to device notifications
    // deviceIds - single device identifier, array of identifiers or null (subscribe to all devices)
    subscribe: function (deviceIds) {
        if (deviceIds && !$.isArray(deviceIds)) {  deviceIds = [deviceIds]; }
        ensureConnectedState(this.channelState);
        return this.channel.subscribe(deviceIds);
    },

    // unsubscribes from device notifications
    // deviceIds - single device identifier, array of identifiers or null (unsubscribe from all devices)
    unsubscribe: function (deviceIds) {
        if (deviceIds && !$.isArray(deviceIds)) { deviceIds = [deviceIds]; }
        ensureConnectedState(this.channelState);
        return this.channel.unsubscribe(deviceIds);
    },

    // sends new command to the device
    // use sendCommand().result(callback) to specify a callback to be invoken when the command is executed
    sendCommand: function (deviceId, command, parameters) {
        ensureConnectedState(this.channelState);
        return this.channel.sendCommand(deviceId, command, parameters);
    },

    // adds a callback that will be invoked when a device notification is received
    notification: function (callback) {
        var self = this;
        $(this).bind("onNotification", function (e, deviceId, notification) {
            callback.call(self, deviceId, notification);
        });
        return this;
    },

    // adds a callback that will be invoked when the communication channel state is changed
    channelStateChanged: function (callback) {
        var self = this;
        $(this).bind("onChannelStateChanged", function (e, data) {
            callback.call(self, data);
        });
        return this;
    }
};

deviceHive.channels = {};

deviceHive.channels.webSocket = function (hive) {
    this.hive = hive;
};

deviceHive.channels.webSocket.requestTimeout = 10000;

deviceHive.channels.webSocket.prototype = {
    requestId: 0,
    requests: {},
    commandRequests: {},

    open: function () {
        var deferred = $.Deferred();
        if (!this.hive.serverInfo.webSocketServerUrl) {
            return deferred.reject("DeviceHive: The server does not support WebSocket API").promise();
        }
        if (!window.WebSocket) {
            return deferred.reject("DeviceHive: The browser does not support WebSocket").promise();
        }

        var self = this;
        var opened = false;
        this.socket = new window.WebSocket(this.hive.serverInfo.webSocketServerUrl + "/client");
        this.socket.onopen = function (e) {
            opened = true;
            deferred.resolve();
        };

        this.socket.onmessage = function (e) {
            var response = window.JSON.parse(e.data);
            
            if (response.requestId) {
                var request = self.requests[response.requestId];
                if (request) {
                    window.clearTimeout(request.timeout);
                    if (response.status && response.status == "success") {
                        request.resolve(response);
                    }
                    else {
                        request.reject(response.error);
                    }
                    delete self.requests[response.requestId];
                }
            }
            
            if (response.action == "command/update" && response.command && response.command.id) {
                var commandRequest = self.commandRequests[response.command.id];
                if (commandRequest) {
                    $(commandRequest).triggerHandler("onResult", [response]);
                    delete self.commandRequests[response.command.id];
                }
            }
            
            if (response.action == "notification/insert" && response.deviceGuid && response.notification) {
                $(self.hive).triggerHandler("onNotification", [response.deviceGuid, response.notification]);
            }
        };

        this.socket.onclose = function (e) {
            if (!opened) {
                deferred.reject("DeviceHive: WebSocket connection has failed to open", e);
            }
            else {
                changeChannelState(self.hive, deviceHive.channelState.disconnected);
            }
        };

        return deferred.pipe(function () {
            return self.send("authenticate", { login: self.hive.login, password: self.hive.password });
        });
    },

    subscribe: function (deviceIds) {
        return this.send("notification/subscribe", { deviceGuids: deviceIds });
    },

    unsubscribe: function (deviceIds) {
        return this.send("notification/unsubscribe", { deviceGuids: deviceIds });
    },

    sendCommand: function (deviceId, command, parameters) {
        var self = this;
        var data = { deviceGuid: deviceId, command: { command: command, parameters: parameters }};
        var request = this.send("command/insert", data);

        request.done(function (response) {
            if (response && response.command && response.command.id) {
                self.commandRequests[response.command.id] = request;
            }
        });
        request.result = function (callback) {
            $(request).bind("onResult", function (e, command) {
                callback.call(self.hive, command.command);
            });
            return request;
        };
        return request;
    },

    close: function () {
        this.socket.close();
    },

    // private methods
    send: function (action, data) {
        var self = this;
        var request = $.Deferred();
        request.id = ++this.requestId;
        request.timeout = window.setTimeout(function () {
            request.reject("DeviceHive: Operation timeout");
            delete self.requests[request.id];
        }, deviceHive.channels.webSocket.requestTimeout);
        this.requests[request.id] = request;

        data.requestId = request.id;
        data.action = action;
        this.socket.send(JSON.stringify(data));
        
        return request.promise();
    }
};

deviceHive.channels.longPolling = function (hive) {
    this.hive = hive;
};

deviceHive.channels.longPolling.prototype = {
    deviceIds: [],

    open: function () { // do nothing
        return $.Deferred().resolve().promise();
    },

    subscribe: function (deviceIds) {
        var self = this;
        if (this.deviceIds) {
            if (!deviceIds) { // subscribe to all devices
                this.deviceIds = null;
            }
            else { // merge subscriptions
                $.each(deviceIds, function () {
                    if ($.inArray(this.toLowerCase(), self.deviceIds) < 0) {
                        self.deviceIds.push(this.toLowerCase());
                    }
                });
            }
        }
        return this.startPolling().promise();
    },

    unsubscribe: function (deviceIds) {
        var self = this;
        if (!deviceIds) { // unsubscribe from all devices
            this.deviceIds = [];
        }
        else if (this.deviceIds) { // except subscriptions
            $.each(deviceIds, function () {
                var index = $.inArray(this.toLowerCase(), self.deviceIds);
                if (index >= 0) {
                    self.deviceIds.splice(index, 1);
                }
            });
        }
        return this.startPolling().promise();
    },

    sendCommand: function (deviceId, command, parameters) {
        var self = this;
        var data = { command: command, parameters: parameters };
        var request = query(this.hive, "POST", "/device/" + deviceId + "/command", data);
        request.result = function (callback) {
            request.done(function (response) {
                if (response && response.id) {
                    self.waitCommandResult(deviceId, response.id).done(function (result) {
                        if (result) {
                            callback.call(self.hive, result);
                        }
                    });
                }
            });
            return request;
        };
        return request;
    },

    close: function () {
        this.stopPolling();
    },

    // private methods
    startPolling: function () {
        this.stopPolling();
        if (this.deviceIds && this.deviceIds.length == 0) {
            return $.Deferred().resolve().promise();
        }
        
        var self = this;
        return query(this.hive, "GET", "/info").done(function (info) {
            self.poll(info.serverTimestamp);
        });
    },

    stopPolling: function () {
        if (this.pollXhr) {
            this.pollXhr.abort("The client stopped polling");
        }
    },

    poll: function (timestamp) {
        var self = this;
        var params = { timestamp: timestamp };
        if (this.deviceIds) { params.deviceGuids = this.deviceIds.join(); }
        this.pollXhr = ajax(this.hive, "GET", "/device/notification/poll", params)
            .done(function (data, jqXhr) {
                var lastTimestamp = null;
                if (data != null && data != "") {
                    $.each(data, function () {
                        if (!lastTimestamp || this.notification.timestamp > lastTimestamp) {
                            lastTimestamp = this.notification.timestamp;
                        }
                        $(self.hive).triggerHandler("onNotification", [this.deviceGuid, this.notification]);
                    });
                }
                self.poll(lastTimestamp || timestamp);
            }).fail(function (response) {
                if (response.status != 0 && response.statusText != "The client stopped polling") {
                    setTimeout(function () { self.poll(timestamp); }, 1000);
                }
            });
    },

    waitCommandResult: function (deviceId, commandId) {
        return ajax(this.hive, "GET", "/device/" + deviceId + "/command/" + commandId + "/poll");
    }
};

window.DeviceHive = $.DeviceHive = deviceHive;

}(window.jQuery, window));