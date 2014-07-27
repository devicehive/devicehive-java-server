package com.devicehive.context;


import com.devicehive.auth.HivePrincipal;

@HiveRequestScoped
public class HiveRequestAttributes {

    private HivePrincipal hivePrincipal;

    public HivePrincipal getHivePrincipal() {
        return hivePrincipal;
    }

    public void setHivePrincipal(HivePrincipal hivePrincipal) {
        this.hivePrincipal = hivePrincipal;
    }
}
