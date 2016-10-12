package com.devicehive.auth.rest.providers;

import com.devicehive.auth.HiveAction;
import com.devicehive.auth.HiveAuthentication;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.model.Subnet;
import com.devicehive.model.enums.UserStatus;
import com.devicehive.service.AccessKeyService;
import com.devicehive.service.time.TimestampService;
import com.devicehive.vo.AccessKeyVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.util.HashSet;
import java.util.Set;

public class AccessTokenAuthenticationProvider implements AuthenticationProvider {
    private static final Logger logger = LoggerFactory.getLogger(AccessTokenAuthenticationProvider.class);

    @Autowired
    private AccessKeyService accessKeyService;

    @Autowired
    private TimestampService timestampService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String token = (String) authentication.getPrincipal();
        AccessKeyVO accessKey = accessKeyService.authenticate(token);
        if (accessKey == null
                || accessKey.getUser() == null || !accessKey.getUser().getStatus().equals(UserStatus.ACTIVE)
                || (accessKey.getExpirationDate() != null && accessKey.getExpirationDate().before(timestampService.getDate()))) {
            throw new BadCredentialsException("Unauthorized"); //"Wrong access key"
        }
        logger.debug("Access token authentication successful");

        HivePrincipal principal = new HivePrincipal();
        principal.setUser(accessKey.getUser());

        Set<Long> allowedNetworksIds = new HashSet<>();
        Set<String> allowedDeviceGuids = new HashSet<>();
        Set<HiveAction> actions = new HashSet<>();
        Set<String> subnets = new HashSet<>();
        Set<String> domains = new HashSet<>();
        accessKey.getPermissions().forEach(permission -> {
            if (permission.getNetworkIdsAsSet() != null) {
                allowedNetworksIds.addAll(permission.getNetworkIdsAsSet());
            }

            if (permission.getDeviceGuidsAsSet() != null) {
                allowedDeviceGuids.addAll(permission.getDeviceGuidsAsSet());
            }

            Set<String> allowedActions = permission.getActionsAsSet();
            if (permission.getActionsAsSet() != null)
                allowedActions.forEach(action -> actions.add(HiveAction.fromString(action)));

            Set<Subnet> allowedSubnets = permission.getSubnetsAsSet();
            if (permission.getSubnetsAsSet() != null)
                allowedSubnets.forEach(subnet -> subnets.add(subnet.getSubnet()));

            if (principal.getDomains() != null)
                domains.addAll(principal.getDomains());
        });

        principal.setNetworkIds(allowedNetworksIds);
        principal.setDeviceGuids(allowedDeviceGuids);
        principal.setActions(actions);
        principal.setSubnets(subnets);
        principal.setDomains(domains);

        return new HiveAuthentication(principal,
                AuthorityUtils.createAuthorityList(HiveRoles.KEY));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return PreAuthenticatedAuthenticationToken.class.equals(authentication);
    }
}
