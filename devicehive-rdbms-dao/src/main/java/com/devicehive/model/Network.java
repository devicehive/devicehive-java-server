package com.devicehive.model;

/*
 * #%L
 * DeviceHive Dao RDBMS Implementation
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.vo.DeviceVO;
import com.devicehive.vo.NetworkVO;
import com.devicehive.vo.NetworkWithUsersAndDevicesVO;
import com.google.gson.annotations.SerializedName;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

/**
 * TODO JavaDoc
 */
@Entity
@Table(name = "network")
@NamedQueries({
        @NamedQuery(name = "Network.findByName", query = "select n from Network n where name = :name"),
        @NamedQuery(name = "Network.findWithUsers", query = "select n from Network n left join fetch n.users where n.id = :id"),
        @NamedQuery(name = "Network.findByUserOrderedById", query = "select n from Network n left join n.users u where u.id = :id order by n.id"),
        @NamedQuery(name = "Network.deleteById", query = "delete from Network n where n.id = :id"),
        @NamedQuery(name = "Network.getWithDevices", query = "select n from Network n left join fetch n.devices where n.id = :id"),
        @NamedQuery(name = "Network.getNetworksByIdsAndUsers", query = "select n from Network n left outer join n.users u left join fetch n.devices d " +
                "where n.id in :networkIds and (u.id = :userId or :userId is null) and (n.id in :permittedNetworks or :permittedNetworks is null)")
})
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Network implements HiveEntity {
    private static final long serialVersionUID = -4824259625517565076L;

    @SerializedName("id")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonPolicyDef({DEVICE_PUBLISHED, USER_PUBLISHED, NETWORKS_LISTED, NETWORK_PUBLISHED, NETWORK_SUBMITTED})
    private Long id;
    @SerializedName("name")
    @Column
    @NotNull(message = "name field cannot be null.")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of name should not be more than 128 " +
            "symbols.")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, USER_PUBLISHED, NETWORKS_LISTED, NETWORK_PUBLISHED})
    private String name;
    @SerializedName("description")
    @Column
    @Size(max = 128, message = "The length of description should not be more than 128 symbols.")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, USER_PUBLISHED,
            NETWORKS_LISTED, NETWORK_PUBLISHED})
    private String description;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_network", joinColumns = {@JoinColumn(name = "network_id", nullable = false,
            updatable = false)},
            inverseJoinColumns = {@JoinColumn(name = "user_id", nullable = false, updatable = false)})
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<User> users;
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "network")
    @JsonPolicyDef({NETWORK_PUBLISHED})
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<Device> devices;
    @Version
    @Column(name = "entity_version")
    private Long entityVersion;

    public Set<Device> getDevices() {
        return devices;
    }

    public void setDevices(Set<Device> devices) {
        this.devices = devices;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<User> getUsers() {
        return users;
    }

    public void setUsers(Set<User> users) {
        this.users = users;
    }

    public Long getEntityVersion() {
        return entityVersion;
    }

    public void setEntityVersion(Long entityVersion) {
        this.entityVersion = entityVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Network network = (Network) o;

        return !(id != null ? !id.equals(network.id) : network.id != null);

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Network{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }

    public static NetworkVO convertNetwork(Network network) {
        if (network != null) {
            NetworkVO vo = new NetworkVO();
            vo.setId(network.getId());
            vo.setName(network.getName());
            vo.setDescription(network.getDescription());
            vo.setEntityVersion(network.getEntityVersion());
            return vo;
        }
        return null;
    }

    public static NetworkWithUsersAndDevicesVO convertWithDevicesAndUsers(Network network) {
        if (network != null) {
            NetworkVO vo1 = convertNetwork(network);
            NetworkWithUsersAndDevicesVO vo = new NetworkWithUsersAndDevicesVO(vo1);
            if (network.getUsers() != null) {
                vo.setUsers(network.getUsers().stream().map(User::convertToVo).collect(Collectors.toSet()));
            } else {
                vo.setUsers(Collections.emptySet());
            }
            if (network.getDevices() != null) {
                Set<DeviceVO> deviceList = network.getDevices().stream().map(Device::convertToVo).collect(Collectors.toSet());
                vo.setDevices(deviceList);
            } else {
                vo.setDevices(Collections.emptySet());
            }
            return vo;
        }
        return null;
    }

    public static Network convert(NetworkVO vo) {
        if (vo != null) {
            Network network = new Network();
            network.setId(vo.getId());
            network.setName(vo.getName());
            network.setDescription(vo.getDescription());
            network.setEntityVersion(vo.getEntityVersion());
            return network;
        }
        return null;
    }
}
