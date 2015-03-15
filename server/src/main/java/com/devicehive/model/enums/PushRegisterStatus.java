package com.devicehive.model.enums;

public enum PushRegisterStatus {
	REGISTERED(0),
	UNREGISTERED(1);
	
	private int value;
	
	PushRegisterStatus(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return this.value;
	}
}
