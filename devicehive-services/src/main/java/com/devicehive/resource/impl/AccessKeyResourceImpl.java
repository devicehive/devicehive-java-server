package com.devicehive.resource.impl;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.AccessKey;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.User;
import com.devicehive.model.enums.AccessKeyType;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.updates.AccessKeyUpdate;
import com.devicehive.resource.AccessKeyResource;
import com.devicehive.resource.converters.SortOrderQueryParamParser;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.service.AccessKeyService;
import com.devicehive.service.UserService;
import com.devicehive.vo.AccessKeyVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import java.util.List;

import static com.devicehive.configuration.Constants.ID;
import static com.devicehive.configuration.Constants.LABEL;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.ACCESS_KEY_LISTED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.ACCESS_KEY_SUBMITTED;
import static javax.ws.rs.core.Response.Status.*;

/**
 * {@inheritDoc}
 */
@Service
public class AccessKeyResourceImpl implements AccessKeyResource {
    private static Logger logger = LoggerFactory.getLogger(AccessKeyResourceImpl.class);

    @Autowired
    private UserService userService;

    @Autowired
    private AccessKeyService accessKeyService;

    /**
     * {@inheritDoc}
     */
    @Override
    public Response list(String userId, String label, String labelPattern, Integer type, String sortField, String sortOrderSt, Integer take, Integer skip) {
        logger.debug("Access key : list requested for userId : {}", userId);

        Long id = getUser(userId).getId();

        boolean sortOrder = SortOrderQueryParamParser.parse(sortOrderSt);

        if (sortField != null && !ID.equalsIgnoreCase(sortField) && !LABEL.equalsIgnoreCase(sortField)) {
            return ResponseFactory.response(BAD_REQUEST,
                    new ErrorResponse(BAD_REQUEST.getStatusCode(),
                            Messages.INVALID_REQUEST_PARAMETERS));
        } else if (sortField != null) {
            sortField = sortField.toLowerCase();
        }
        List<AccessKeyVO> keyList = accessKeyService.list(id, label, labelPattern, type, sortField, sortOrder, take, skip);

        logger.debug("Access key : insert proceed successfully for userId : {}", userId);

        return ResponseFactory.response(OK, keyList, ACCESS_KEY_LISTED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response get(String userId, long accessKeyId) {

        logger.debug("Access key : get requested for userId : {} and accessKeyId", userId, accessKeyId);

        Long id = getUser(userId).getId();
        AccessKeyVO result = accessKeyService.find(accessKeyId, id);
        if (result == null) {
            logger.debug("Access key : list failed for userId : {} and accessKeyId : {}. Reason: No access key found" +
                    ".", userId, accessKeyId);
            return ResponseFactory
                    .response(NOT_FOUND,
                            new ErrorResponse(NOT_FOUND.getStatusCode(), "Access key not found."));
        }

        logger.debug("Access key : insert proceed successfully for userId : {} and accessKeyId : {}", userId,
                accessKeyId);

        return ResponseFactory.response(OK, result, ACCESS_KEY_LISTED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response insert(String userId, AccessKeyVO key) {

        logger.debug("Access key : insert requested for userId : {}", userId);
        User user = getUser(userId);
        AccessKeyVO generatedKey = accessKeyService.create(user, key);
        logger.debug("Access key : insert proceed successfully for userId : {}", userId);
        return ResponseFactory.response(CREATED, generatedKey, ACCESS_KEY_SUBMITTED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response update(String userId, Long accessKeyId, AccessKeyUpdate accessKeyUpdate) {
        logger.debug("Access key : update requested for userId : {}, access key id : {}, access key : {} ", userId,
                accessKeyId, accessKeyUpdate);

        Long id = getUser(userId).getId();
        if (!accessKeyService.update(id, accessKeyId, accessKeyUpdate)) {
            logger.debug("Access key : update failed for userId : {} and accessKeyId : {}. Reason: No access key " +
                    "found.", userId, accessKeyId);
            return ResponseFactory
                    .response(NOT_FOUND, new ErrorResponse(NOT_FOUND.getStatusCode(), Messages.ACCESS_KEY_NOT_FOUND));
        }

        logger.debug("Access key : update proceed successfully for userId : {}, access key id : {}, access key : {} ",
                userId, accessKeyId, accessKeyUpdate);
        return ResponseFactory.response(NO_CONTENT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response delete(String userId, Long accessKeyId) {
        logger.debug("Access key : delete requested for userId : {}", userId);

        Long id = getUser(userId).getId();
        List<AccessKeyVO> existingDefaultKeys = accessKeyService.list(id, null, null, AccessKeyType.DEFAULT.getValue(), null, null, 2, 0);
        if(existingDefaultKeys.size() < 2){
            logger.debug("Rejected removing the last default access key");
            throw new HiveException(Messages.CANT_DELETE_LAST_DEFAULT_ACCESS_KEY, FORBIDDEN.getStatusCode());
        }

        accessKeyService.delete(id, accessKeyId);

        logger.debug("Access key : delete proceed successfully for userId : {} and access key id : {}", userId,
                accessKeyId);
        return ResponseFactory.response(NO_CONTENT);

    }

    private User getUser(String userId) {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User currentUser = principal.getUser() != null ? principal.getUser() : principal.getKey().getUser();

        Long id;
        if (userId.equalsIgnoreCase(Constants.CURRENT_USER)) {
            return currentUser;
        } else {
            try {
                id = Long.parseLong(userId);
            } catch (NumberFormatException e) {
                throw new HiveException(String.format(Messages.BAD_USER_IDENTIFIER, userId), e,
                        BAD_REQUEST.getStatusCode());
            }
        }

        User result;
        if (!currentUser.getId().equals(id) && currentUser.isAdmin()) {
            result = userService.findById(id);
            if (result == null) {
                throw new HiveException(Messages.UNAUTHORIZED_REASON_PHRASE, UNAUTHORIZED.getStatusCode());
            }
            return result;

        }
        if (!currentUser.getId().equals(id) && currentUser.getRole().equals(UserRole.CLIENT)) {
            throw new HiveException(Messages.UNAUTHORIZED_REASON_PHRASE, UNAUTHORIZED.getStatusCode());
        }
        result = currentUser;
        return result;
    }
}
