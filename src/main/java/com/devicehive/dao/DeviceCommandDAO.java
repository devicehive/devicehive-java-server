package com.devicehive.dao;

import com.devicehive.model.DeviceCommand;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

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
    public DeviceCommand updateCommand(DeviceCommand update) {
        DeviceCommand cmd = em.find(DeviceCommand.class, update.getId(), LockModeType.WRITE);
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
}
