package com.devicehive.model.updates;

import com.devicehive.model.HiveEntity;
import com.devicehive.model.NullableWrapper;

public class UserPushInfoUpdate implements HiveEntity {

	private static final long serialVersionUID = -192980984468281274L;
	
	private NullableWrapper<String> osType;
    private NullableWrapper<String> version;
    private NullableWrapper<String> regId;
    
	public NullableWrapper<String> getOsType() {
		return osType;
	}
	public void setOsType(NullableWrapper<String> osType) {
		this.osType = osType;
	}
	public NullableWrapper<String> getVersion() {
		return version;
	}
	public void setVersion(NullableWrapper<String> version) {
		this.version = version;
	}
	public NullableWrapper<String> getRegId() {
		return regId;
	}
	public void setRegId(NullableWrapper<String> regId) {
		this.regId = regId;
	}
}
