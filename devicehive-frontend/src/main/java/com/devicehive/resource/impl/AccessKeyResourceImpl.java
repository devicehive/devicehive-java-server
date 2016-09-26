package com.devicehive.resource.impl;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.enums.AccessKeyType;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.updates.AccessKeyUpdate;
import com.devicehive.resource.AccessKeyResource;
import com.devicehive.resource.converters.SortOrderQueryParamParser;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.service.AccessKeyService;
import com.devicehive.service.UserService;
import com.devicehive.vo.AccessKeyVO;
import com.devicehive.vo.UserVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;

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
    public void list(String userId, String label, String labelPattern, Integer type, String sortField, String sortOrderSt, Integer take, Integer skip, @Suspended final AsyncResponse asyncResponse) {
        logger.debug("Access key : list requested for userId : {}", userId);

        Long id = getUser(userId).getId();

        boolean sortOrder = SortOrderQueryParamParser.parse(sortOrderSt);

        if (sortField != null && !ID.equalsIgnoreCase(sortField) && !LABEL.equalsIgnoreCase(sortField)) {
            final Response response = ResponseFactory.response(BAD_REQUEST,
                    new ErrorResponse(BAD_REQUEST.getStatusCode(),
                            Messages.INVALID_REQUEST_PARAMETERS));
            asyncResponse.resume(response);
        } else {
            if (sortField != null) {
                sortField = sortField.toLowerCase();
            }

            accessKeyService.list(id, label, labelPattern, type, sortField, sortOrder, take, skip)
                    .thenApply(accessKeys -> {
                        logger.debug("Access key : insert proceed successfully for userId : {}", userId);
                        return ResponseFactory.response(OK, accessKeys, ACCESS_KEY_LISTED);
                    }).thenAccept(asyncResponse::resume);
        }
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
        UserVO user = getUser(userId);
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
    public void delete(String userId, Long accessKeyId, @Suspended final AsyncResponse asyncResponse) {
        logger.debug("Access key : delete requested for userId : {}", userId);

        Long id = getUser(userId).getId();

        accessKeyService.list(id, null, null, AccessKeyType.DEFAULT.getValue(), null, null, 2, 0)
                .thenApply(accessKeys -> {
                    if(accessKeys.size() < 2){
                        logger.debug("Rejected removing the last default access key");
                        return ResponseFactory.response(FORBIDDEN, new ErrorResponse(FORBIDDEN.getStatusCode(), Messages.CANT_DELETE_LAST_DEFAULT_ACCESS_KEY));
                    } else {
                        accessKeyService.delete(id, accessKeyId);
                        logger.debug("Access key : delete proceed successfully for userId : {} and access key id : {}", userId,
                                accessKeyId);
                        return ResponseFactory.response(NO_CONTENT);
                    }
                }).thenAccept(asyncResponse::resume);

    }

    private UserVO getUser(String userId) {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        //todo add check for permission to see all users?
        if (principal.getUser() == null) {
            throw new HiveException(Messages.UNAUTHORIZED_REASON_PHRASE, UNAUTHORIZED.getStatusCode());
        }
        UserVO currentUser = principal.getUser();

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

        UserVO result;
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
