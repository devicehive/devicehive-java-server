Riak data structure
===================
When deployed against Riak, DeviceHive data is stored with the following structure:

Counters bucket
---------------
The Counters bucket is used to retrieve unique IDs for other entities. Each entity that requires such ID has a 
separate counter within the Counters bucket, identified by a key.

**Bucket name:** _dh_counters_

**Keys:**
* accessKeyCounter
* deviceClassCounter
* deviceCounter
* deviceEquipmentCounter
* networkCounter
* oauthClientCounter
* oauthGrantCounter
* userCounter

AccessKey bucket
----------------
Stores the AccessKey entities.

**Bucket name:** _access_key_

**Secondary indexes:**
* _label_ - bin
* _userId_ - int
* _key_ - bin
* _expirationDate_ - int

**Expected model:**


Configuration bucket
--------------------
Stores the Configuration entities.

**Bucket name:** _configuration_

DeviceClass bucket
------------------
Stores the DeviceClass entities.

**Bucket name:** _device_class_

Device bucket
-------------
Stores the Device entities.

**Bucket name:** _device_

DeviceEquipment bucket
----------------------
Stores the DeviceEquipment entities.

**Bucket name:** _device_equipment_

IdentityProvider bucket
-----------------------
Stores the IdentityProvider entities.

**Bucket name:** _identity_provider_

Network bucket
--------------
Stores the Network entities.

**Bucket name:** _network_

NetworkDevice bucket
--------------------
Stores the relations between Network and Device entities.

**Bucket name:** _network_device_

OAuthClient bucket
------------------
Stores the OAuthClient entities.

**Bucket name:** _oauth_client_

OAuthGrant bucket
-----------------
Stores the OAuthGrant entities.

**Bucket name:** _oauth_grant_

User bucket
-----------
Stores the User entities.

**Bucket name:** _user_

UserNetwork bucket
------------------
Stores the relations between User and Network entities.

**Bucket name:** _user_network_
