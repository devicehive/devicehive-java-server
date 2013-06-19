package com.devicehive.model;

import org.hibernate.validator.constraints.NotBlank;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
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
    @NotNull
    @NotBlank
    @Size(min = 1, max = 128)
    private String code;


    @Column
    private Date timestamp;


    @Column
    private String parameters;

    @ManyToOne
    @JoinColumn(name = "device_id")
    @NotNull
    private Device device;
}
