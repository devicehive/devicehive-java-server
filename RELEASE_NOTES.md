FIXED ISSUES
============

* Internal server error (500) and no message return on Device: list, DeviceClass: list methods call with negative
values of parameters
* Optimization was added for many-to-many related entities

NEW FEATURES
============

* Java Client library
* OAuth 2.0 support
* Support AccessKey authorization
* Allow to subscribe on specific notification/command types
* Allow to get grouped historical notifications
* Websocket client endpoint extended to support device actions
* Equipment list made a part of DeviceClass
* String identifiers can be used for devices instead of UUID
* Multiple subscriptions added: every subscribe action create one new subscription, unsubscribe action is performed now using subscription's identifier

NOTES
=====

**The only supported version of Glassfish Application Server is 4.1 because of Glassfish 4 [bug](https://java.net/jira/browse/TYRUS-229)**

Do not forget to update database schema. It can be done using dh_dbtool.
