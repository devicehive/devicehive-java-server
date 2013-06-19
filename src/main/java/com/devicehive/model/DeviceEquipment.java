package com.devicehive.model;

import javax.persistence.*;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: ssidorenko
 * Date: 19.06.13
 * Time: 10:43
 * To change this template use File | Settings | File Templates.
 */
@Entity
@Table(name = "device_equipment")
public class DeviceEquipment {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column
    private String code;


    @Column
    private Date timestamp;


    @Column
    private String parameters;

    @ManyToOne
    @JoinColumn(name = "device_id")
    private Device device;
}
