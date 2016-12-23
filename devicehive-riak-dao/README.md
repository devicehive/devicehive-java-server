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
* Long id - key
* String label
* String key
* RiakUser user
* Date expirationDate
* AccessKeyType type
* Set<RiakAccessKeyPermission> permissions
* long entityVersion

Configuration bucket
--------------------
Stores the Configuration entities.

**Bucket name:** _configuration_

**Secondary indexes:**
none

**Expected model:**
* String name - key
* long entityVersion
* String value

DeviceClass bucket
------------------
Stores the DeviceClass entities.

**Bucket name:** _device_class_

**Secondary indexes:**
* _name_ - bin

**Expected model:**
* Long id - key
* String name
* Boolean isPermanent
* Integer offlineTimeout
* JsonStringWrapper data
* Set<RiakDeviceClassEquipment> equipment

Device bucket
-------------
Stores the Device entities.

**Bucket name:** _device_

**Secondary indexes:**
* _guid_ - bin

**Expected model:**
* Long id - key
* String guid
* String name
* String status
* JsonStringWrapper data
* RiakNetwork network
* RiakDeviceClass deviceClass
* Boolean blocked

DeviceEquipment bucket
----------------------
Stores the DeviceEquipment entities.

**Bucket name:** _device_equipment_

**Secondary indexes:**
* _device_ - bin

**Expected model:**
* Long id - key
* String code
* Date timestamp
* JsonStringWrapper parameters
* String deviceGuid
* long entityVersion

IdentityProvider bucket
-----------------------
Stores the IdentityProvider entities.

**Bucket name:** _identity_provider_

**Secondary indexes:**
none

**Expected model:**
* String name - key
* String apiEndpoint
* String verificationEndpoint
* String tokenEndpoint

Network bucket
--------------
Stores the Network entities.

**Bucket name:** _network_

**Secondary indexes:**
* _name_ - bin

**Expected model:**
* Long id - key
* String key
* String name
* String description
* Long entityVersion

NetworkDevice bucket
--------------------
Stores the relations between Network and Device entities.

**Bucket name:** _network_device_

**Secondary indexes:**
* _networkId_ - int
* _deviceUuid_ - bin

**Expected model:**
* String id - key
* Long networkId
* String deviceUuid

OAuthClient bucket
------------------
Stores the OAuthClient entities.

**Bucket name:** _oauth_client_

**Secondary indexes:**
* _name_ - bin
* _oauthId_ - bin

**Expected model:**
* Long id - key
* String name
* String domain
* String subnet
* String redirectUri
* String oauthId
* String oauthSecret

OAuthGrant bucket
-----------------
Stores the OAuthGrant entities.

**Bucket name:** _oauth_grant_

**Secondary indexes:**
* _user_ - int
* _authCode_ - bin

**Expected model:**
* Long id - key
* Date timestamp
* String authCode
* OAuthClientVO client
* Type type
* AccessType accessType
* String redirectUri
* String scope
* JsonStringWrapper networkIds
* long entityVersion
* long userId
* long accessKeyId

User bucket
-----------
Stores the User entities.

**Bucket name:** _user_

**Secondary indexes:**
* _login_ - bin
* _googleLogin_ - bin
* _facebookLogin_ - bin
* _githubLogin_ - bin

**Expected model:**
* Long id - key
* String login
* String passwordHash
* String passwordSalt
* Integer loginAttempts
* UserRole role
* UserStatus status
* Date lastLogin
* String googleLogin
* String facebookLogin
* String githubLogin
* long entityVersion
* JsonStringWrapper data

UserNetwork bucket
------------------
Stores the relations between User and Network entities.

**Bucket name:** _user_network_

**Secondary indexes:**
* _userId_ - int
* _networkId_ - int

**Expected model:**
* String id
* Long userId
* Long networkId

