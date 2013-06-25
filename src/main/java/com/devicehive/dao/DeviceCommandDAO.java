package com.devicehive.dao;

import com.devicehive.exceptions.HiveWebsocketException;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.User;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: ssidorenko
 * Date: 20.06.13
 * Time: 15:36
 * To change this template use File | Settings | File Templates.
 */
public class DeviceCommandDAO {

    @PersistenceContext(unitName = "devicehive")
    private EntityManager em;


    @Transactional
    public void saveCommand(DeviceCommand deviceCommand) {
        em.persist(deviceCommand);
    }


    @Transactional
    public DeviceCommand updateCommand(DeviceCommand update, Device expectedDevice) {
        DeviceCommand cmd = em.find(DeviceCommand.class, update.getId(), LockModeType.WRITE);
        if (!cmd.getDevice().getId().equals(expectedDevice.getId())) {
            throw new HiveWebsocketException("Device tries to update incorrect command");
        }
        cmd.setCommand(update.getCommand());
        cmd.setParameters(update.getParameters());
        cmd.setLifetime(update.getLifetime());
        cmd.setFlags(update.getFlags());
        cmd.setStatus(update.getStatus());
        cmd.setResult(update.getResult());
        return em.merge(cmd);
    }


    @Transactional
    public DeviceCommand findById(Long id) {
        return em.find(DeviceCommand.class, id);
    }


    @Transactional
    public List<DeviceCommand> getNewerThan(Device device, Date timestamp) {
        TypedQuery<DeviceCommand> query = em.createNamedQuery("DeviceCommand.getNewerThan", DeviceCommand.class);
        query.setParameter("timestamp", timestamp);
        query.setParameter("device", device);
        return query.getResultList();
    }
}
