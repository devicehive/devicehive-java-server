package com.devicehive.service;

import com.devicehive.configuration.Constants;
import com.devicehive.dao.rdbms.DeviceDaoImpl;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DeviceActivityServiceTest {
    private static final String DEFAULT_DEVICE_GUID = "Test_device_guid";
    private static final Integer OFFLINE_TIME = 50;
    private static final Integer OFFLINE_TIME_ZERO = 0;
    private static final Integer OFFLINE_TIME_NULL = null;
    private static final Integer PROCESS_DEVICES_BUFFER_SIZE = 100;


    @Mock
    private DeviceDaoImpl deviceDAO;

    @Mock
    private HazelcastInstance hzInstance;

    @Mock
    private IMap imap;

    @InjectMocks
    private DeviceActivityService activityService;

    private TreeMap<String, Integer> offlineTimeMap;
    private List<String> guids;
    private TreeMap<String, Long> imapProxy;
    private Long inputTime;

    private class CustomComparartor implements Comparator<String> {

        @Override
        public int compare(String o1, String o2) {
            return Integer.parseInt(o1.replace(DEFAULT_DEVICE_GUID, "")) - Integer.parseInt(o2.replace(DEFAULT_DEVICE_GUID, ""));
        }
    }

    @Before
    public void setUp() {
        offlineTimeMap = new TreeMap<>();
        imapProxy = new TreeMap<>();
        guids = new ArrayList<>();
        inputTime = System.currentTimeMillis();
    }

    @Test
    public void testStatusChangedWhileProcessOfflineDevicesWithOfflineTimeZero() {
        //Given
        inputTime -= 1L;
        imapProxy.put(DEFAULT_DEVICE_GUID, inputTime);
        offlineTimeMap.put(DEFAULT_DEVICE_GUID, OFFLINE_TIME_ZERO);
        when(imap.get(DEFAULT_DEVICE_GUID)).thenReturn(inputTime);
        when(imap.keySet()).thenReturn(imapProxy.keySet());
        when(imap.remove(DEFAULT_DEVICE_GUID, inputTime)).thenReturn(true);
        guids.addAll(imapProxy.keySet());
        when(deviceDAO.getOfflineTimeForDevices(guids)).thenReturn(offlineTimeMap);
        when(hzInstance.getMap(anyString())).thenReturn(imap);
        activityService.postConstruct();

        //When
        activityService.processOfflineDevices();

        //Then
        verify(imap, times(1)).remove(DEFAULT_DEVICE_GUID, inputTime);
        verify(deviceDAO, times(1)).changeStatusForDevices(Constants.DEVICE_OFFLINE_STATUS, guids);
    }

    @Test
    public void testStatusChangedWhileProcessOfflineDevicesWithOfflineTime() {
        //Given
        inputTime -= OFFLINE_TIME * 1000 + 1;
        imapProxy.put(DEFAULT_DEVICE_GUID, inputTime);
        offlineTimeMap.put(DEFAULT_DEVICE_GUID, OFFLINE_TIME);
        when(imap.get(DEFAULT_DEVICE_GUID)).thenReturn(inputTime);
        when(imap.keySet()).thenReturn(imapProxy.keySet());
        when(imap.remove(DEFAULT_DEVICE_GUID, inputTime)).thenReturn(true);
        guids.addAll(imapProxy.keySet());
        when(deviceDAO.getOfflineTimeForDevices(guids)).thenReturn(offlineTimeMap);
        when(hzInstance.getMap(anyString())).thenReturn(imap);
        activityService.postConstruct();

        //When
        activityService.processOfflineDevices();

        //Then
        verify(imap, times(1)).remove(DEFAULT_DEVICE_GUID, inputTime);
        verify(deviceDAO, times(1)).changeStatusForDevices(Constants.DEVICE_OFFLINE_STATUS, guids);
    }

    @Test
    public void testStatusChangedWhileProcessOfflineDevicesWithOfflineTimeNull() {
        //Given
        imapProxy.put(DEFAULT_DEVICE_GUID, inputTime);
        offlineTimeMap.put(DEFAULT_DEVICE_GUID, OFFLINE_TIME_NULL);
        when(imap.get(DEFAULT_DEVICE_GUID)).thenReturn(inputTime);
        when(imap.keySet()).thenReturn(imapProxy.keySet());
        when(imap.remove(DEFAULT_DEVICE_GUID, inputTime)).thenReturn(true);
        when(imap.remove(DEFAULT_DEVICE_GUID)).thenReturn(true);
        guids.addAll(imapProxy.keySet());
        when(deviceDAO.getOfflineTimeForDevices(guids)).thenReturn(offlineTimeMap);
        when(hzInstance.getMap(anyString())).thenReturn(imap);
        activityService.postConstruct();

        //When
        activityService.processOfflineDevices();

        //Then
        verify(imap, never()).remove(DEFAULT_DEVICE_GUID);
        verify(imap, never()).remove(DEFAULT_DEVICE_GUID, inputTime);
        verify(deviceDAO, never()).changeStatusForDevices(Constants.DEVICE_OFFLINE_STATUS, guids);
    }

    @Test
    public void testStatusNotChangedWhileProcessOfflineDevicesWithOfflineTime() {
        //Given
        inputTime += OFFLINE_TIME;
        imapProxy.put(DEFAULT_DEVICE_GUID, inputTime);
        offlineTimeMap.put(DEFAULT_DEVICE_GUID, OFFLINE_TIME);
        when(imap.get(DEFAULT_DEVICE_GUID)).thenReturn(inputTime);
        when(imap.keySet()).thenReturn(imapProxy.keySet());
        when(imap.remove(DEFAULT_DEVICE_GUID, inputTime)).thenReturn(true);
        when(imap.remove(DEFAULT_DEVICE_GUID)).thenReturn(true);
        guids.addAll(imapProxy.keySet());
        when(deviceDAO.getOfflineTimeForDevices(guids)).thenReturn(offlineTimeMap);
        when(hzInstance.getMap(anyString())).thenReturn(imap);
        activityService.postConstruct();

        //When
        activityService.processOfflineDevices();

        //Then
        verify(imap, never()).remove(DEFAULT_DEVICE_GUID);
        verify(imap, never()).remove(DEFAULT_DEVICE_GUID, inputTime);
        verify(deviceDAO, never()).changeStatusForDevices(Constants.DEVICE_OFFLINE_STATUS, guids);
    }

    @Test
    public void testStatusChangedWhileProcessOfflineDevicesWhenNoDeviceInDB() {
        //Given
        imapProxy.put(DEFAULT_DEVICE_GUID, inputTime);
        when(imap.get(DEFAULT_DEVICE_GUID)).thenReturn(inputTime);
        when(imap.keySet()).thenReturn(imapProxy.keySet());
        when(imap.remove(DEFAULT_DEVICE_GUID, inputTime)).thenReturn(true);
        when(imap.remove(DEFAULT_DEVICE_GUID)).thenReturn(true);
        guids.addAll(imapProxy.keySet());
        when(deviceDAO.getOfflineTimeForDevices(guids)).thenReturn(offlineTimeMap);
        when(hzInstance.getMap(anyString())).thenReturn(imap);
        activityService.postConstruct();

        //When
        activityService.processOfflineDevices();

        //Then
        verify(imap, times(1)).remove(DEFAULT_DEVICE_GUID);
        verify(imap, never()).remove(DEFAULT_DEVICE_GUID, inputTime);
        verify(deviceDAO, never()).changeStatusForDevices(Constants.DEVICE_OFFLINE_STATUS, guids);
    }

    @Test
    public void testStatusChangedWhileProcessOfflineDevicesWithOfflineTimeForProcessBufferQuantity() {
        //Given
        inputTime -= OFFLINE_TIME * 1000 + 1;
        for (int i = 0; i < PROCESS_DEVICES_BUFFER_SIZE; i++) {
            String deviceGuid = DEFAULT_DEVICE_GUID + i;
            imapProxy.put(deviceGuid, inputTime);
            offlineTimeMap.put(deviceGuid, OFFLINE_TIME);
            when(imap.get(deviceGuid)).thenReturn(inputTime);
            when(imap.remove(deviceGuid, inputTime)).thenReturn(true);
        }
        when(imap.keySet()).thenReturn(imapProxy.keySet());
        guids.addAll(imapProxy.keySet());
        when(deviceDAO.getOfflineTimeForDevices(guids)).thenReturn(offlineTimeMap);
        when(hzInstance.getMap(anyString())).thenReturn(imap);
        activityService.postConstruct();

        //When
        activityService.processOfflineDevices();

        //Then
        verify(imap, times(PROCESS_DEVICES_BUFFER_SIZE)).remove(anyObject(), anyObject());
        verify(deviceDAO, times(1)).changeStatusForDevices(Constants.DEVICE_OFFLINE_STATUS, guids);
    }

    @Test
    public void testStatusChangedWhileProcessOfflineDevicesWithOfflineTimeForProcessBufferQuantityPlusOne() {
        //Given
        Comparator customComparator = new CustomComparartor();
        offlineTimeMap = new TreeMap<>(customComparator);
        imapProxy = new TreeMap<>(customComparator);
        inputTime -= OFFLINE_TIME * 1000 + 1;
        int buffer = PROCESS_DEVICES_BUFFER_SIZE + 1;
        for (int i = 0; i < buffer; i++) {
            String deviceGuid = DEFAULT_DEVICE_GUID + i;
            imapProxy.put(deviceGuid, inputTime);
            offlineTimeMap.put(deviceGuid, OFFLINE_TIME);
            when(imap.get(deviceGuid)).thenReturn(inputTime);
            when(imap.remove(deviceGuid, inputTime)).thenReturn(true);
        }
        when(imap.keySet()).thenReturn(imapProxy.keySet());
        guids.addAll(imapProxy.keySet());

        String keyFrom = DEFAULT_DEVICE_GUID + 0;
        String keyTo = DEFAULT_DEVICE_GUID + PROCESS_DEVICES_BUFFER_SIZE;
        int indexFrom = 0;
        int indexTo = PROCESS_DEVICES_BUFFER_SIZE;
        when(deviceDAO.getOfflineTimeForDevices(guids.subList(indexFrom, indexTo))).thenReturn(offlineTimeMap.subMap(keyFrom, keyTo));

        keyFrom = keyTo;
        keyTo = DEFAULT_DEVICE_GUID + buffer;
        indexFrom = indexTo;
        indexTo = guids.size();
        when(deviceDAO.getOfflineTimeForDevices(guids.subList(indexFrom, indexTo))).thenReturn(offlineTimeMap.subMap(keyFrom, keyTo));

        when(hzInstance.getMap(anyString())).thenReturn(imap);
        activityService.postConstruct();

        //When
        activityService.processOfflineDevices();

        //Then
        verify(imap, times(buffer)).remove(anyObject(), anyObject());
        indexFrom = 0;
        indexTo = PROCESS_DEVICES_BUFFER_SIZE;
        verify(deviceDAO, times(1)).changeStatusForDevices(Constants.DEVICE_OFFLINE_STATUS, guids.subList(indexFrom, indexTo));
        indexFrom = indexTo;
        indexTo = guids.size();
        verify(deviceDAO, times(1)).changeStatusForDevices(Constants.DEVICE_OFFLINE_STATUS, guids.subList(indexFrom, indexTo));
    }

    @Test
    public void testStatusChangedWhileProcessOfflineDevicesWithOfflineTimeForProcessBufferQuantityMinusOne() {
        //Given
        inputTime -= OFFLINE_TIME * 1000 + 1;
        int buffer = PROCESS_DEVICES_BUFFER_SIZE - 1;
        for (int i = 0; i < buffer; i++) {
            String deviceGuid = DEFAULT_DEVICE_GUID + i;
            imapProxy.put(deviceGuid, inputTime);
            offlineTimeMap.put(deviceGuid, OFFLINE_TIME);
            when(imap.get(deviceGuid)).thenReturn(inputTime);
            when(imap.remove(deviceGuid, inputTime)).thenReturn(true);
        }
        when(imap.keySet()).thenReturn(imapProxy.keySet());
        guids.addAll(imapProxy.keySet());
        when(deviceDAO.getOfflineTimeForDevices(guids)).thenReturn(offlineTimeMap);
        when(hzInstance.getMap(anyString())).thenReturn(imap);
        activityService.postConstruct();

        //When
        activityService.processOfflineDevices();

        //Then
        verify(imap, times(buffer)).remove(anyObject(), anyObject());
        verify(deviceDAO, times(1)).changeStatusForDevices(Constants.DEVICE_OFFLINE_STATUS, guids);
    }

}
