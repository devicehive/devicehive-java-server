package com.devicehive.service;

import static javax.ejb.ConcurrencyManagementType.BEAN;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

@Singleton
@ConcurrencyManagement(BEAN)
@Startup
public class GCMCccService {

	static final long senderId = 945094773652L; // your GCM sender id
    private static final String password = "AIzaSyDTwgaB5kZiTr1qWzlJ72KcIVxKmDj0vAs";

    private static SmackCcsClient ccsClient = new SmackCcsClient();
	
    @PostConstruct
    protected void postConstruct() {
    	try {
			ccsClient.connect(senderId, password);
		} catch (XMPPException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SmackException e) {
			e.printStackTrace();
		}
	}
	
	@PreDestroy
    protected void preDestroy() {
		
	}
	
	public SmackCcsClient getGCMCcsClient() {
		return ccsClient;
	}
}
