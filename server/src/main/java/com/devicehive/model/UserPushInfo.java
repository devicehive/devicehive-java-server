package com.devicehive.model;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Version;

import com.google.gson.annotations.SerializedName;
import com.devicehive.model.UserPushInfo.Queries.Names;
import com.devicehive.model.UserPushInfo.Queries.Values;
import com.devicehive.model.enums.PushRegisterStatus;

@Entity(name = "UserPushInfo")
@Table(name = "\"user_push_info\"")
@NamedQueries({
    @NamedQuery(name = Names.FIND_BY_USER_ID, query = Values.FIND_BY_USER_ID),
    @NamedQuery(name = Names.FIND_BY_USER_ID_REG_ID, query = Values.FIND_BY_USER_ID_REG_ID),
    @NamedQuery(name = Names.DELETE_BY_ID, query = Values.DELETE_BY_ID)
})
@Cacheable
public class UserPushInfo implements HiveEntity {

	public static final String ID_COLUMN = "id";
    public static final String USER_ID_COLUMN = "user_id";
    public static final String OS_TYPE_COLUMN = "os_type";
    public static final String VERSION_COLUMN = "version";
    public static final String REG_ID_COLUMN = "reg_id";
    public static final String STATUS_COLUMN = "status";
	private static final long serialVersionUID = -7960894312937630235L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SerializedName("id")
	private Long id;
    @Column(name = "user_id")
	private Long userId;
    @Column(name = "os_type")
	private String osType;
    @Column(name = "version")
	private String version;
    @Column(name = "reg_id")
	private String regId;
    @Column(name = "status")
    private PushRegisterStatus status;
    @Version
    @Column(name = "entity_version")
    private long entityVersion;
    
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Long getUserId() {
		return userId;
	}
	public void setUserId(Long userId) {
		this.userId = userId;
	}
	public String getOsType() {
		return osType;
	}
	public void setOsType(String osType) {
		this.osType = osType;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getRegId() {
		return regId;
	}
	public void setRegId(String regId) {
		this.regId = regId;
	}
	public PushRegisterStatus getStatus() {
		return status;
	}
	public void setStatus(PushRegisterStatus status) {
		this.status = status;
	}
	public long getEntityVersion() {
		return entityVersion;
	}
	public void setEntityVersion(long entityVersion) {
		this.entityVersion = entityVersion;
	}

	public static class Queries {

        public static interface Names {

            static final String FIND_BY_USER_ID = "UserPushInfo.findByUserId";
            static final String FIND_BY_USER_ID_REG_ID = "UserPushInfo.findByUserIdRegId";
            static final String DELETE_BY_ID = "UserPushInfo.deleteById";
        }

        public static interface Values {

            static final String FIND_BY_USER_ID = "select upi from UserPushInfo upi where upi.userId = :userId";
            static final String FIND_BY_USER_ID_REG_ID = 
            		"select upi from UserPushInfo upi where upi.userId = :userId and upi.regId = :regId";
            static final String DELETE_BY_ID = "delete from UserPushInfo upi where upi.id = :id";
        }

        public static interface Parameters {

            static final String USER_ID = "userId";
            static final String REG_ID = "regId";
            static final String ID = "id";
        }
    }
}
