package com.devicehive.auth.rest.providers;

import com.devicehive.auth.HiveAuthentication;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.model.Device;
import com.devicehive.service.DeviceActivityService;
import com.devicehive.service.DeviceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

public class DeviceAuthenticationProvider implements AuthenticationProvider {
    private static final Logger logger = LoggerFactory.getLogger(DeviceAuthenticationProvider.class);

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceActivityService deviceActivityService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String deviceId = (String) authentication.getPrincipal();
        String deviceKey = (String) authentication.getCredentials();
        logger.debug("Device authentication requested for device {}", deviceId);

        Device device = deviceService.authenticate(deviceId, deviceKey);
        if (device != null && !Boolean.TRUE.equals(device.getBlocked())) {
            logger.info("Device authentication for {} succeeded", device.getGuid());
            deviceActivityService.update(device.getGuid());
            return new HiveAuthentication(
                    new HivePrincipal(device),
                    Collections.singleton(new SimpleGrantedAuthority(HiveRoles.DEVICE)));
        }

        throw new BadCredentialsException("Device authentication failed");
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return DeviceAuthenticationToken.class.equals(authentication);
    }

}
