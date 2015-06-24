package com.devicehive.model.oauth;

import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;

/**
 * Created by tmatvienko on 1/9/15.
 */
public enum IdentityProviderEnum {
    GOOGLE("Google"), FACEBOOK("Facebook"), GITHUB("Github"), PASSWORD("Password");

    private static final Logger logger = LoggerFactory.getLogger(IdentityProviderEnum.class);

    private String value;

    private IdentityProviderEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static IdentityProviderEnum forName(String value) {
        if (StringUtils.isBlank(value)) {
            logger.error(String.format(Messages.INVALID_REQUEST_PARAMETERS, value));
            throw new HiveException(Messages.INVALID_REQUEST_PARAMETERS, HttpServletResponse.SC_BAD_REQUEST);
        }
        for (IdentityProviderEnum type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        logger.error(String.format(Messages.IDENTITY_PROVIDER_NOT_FOUND, value));
        throw new HiveException("Illegal provider name was found: " + value, HttpServletResponse.SC_UNAUTHORIZED);
    }
}
