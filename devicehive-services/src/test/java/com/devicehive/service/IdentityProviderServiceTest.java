package com.devicehive.service;

import com.devicehive.base.AbstractResourceTest;
import com.devicehive.configuration.Messages;
import com.devicehive.dao.IdentityProviderDao;
import com.devicehive.exceptions.IllegalParametersException;
import com.devicehive.vo.IdentityProviderVO;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class IdentityProviderServiceTest extends AbstractResourceTest {

    @Before
    public void beforeMethod() {
        org.junit.Assume.assumeTrue(txManager != null);
    }
    @Autowired(required = false)
    private PlatformTransactionManager txManager;

    @Autowired
    private IdentityProviderService identityProviderService;

    @Autowired
    private IdentityProviderDao providerDao;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void should_throw_IllegalParameterException_when_update_non_existing_identity_provider() throws Exception {
        expectedException.expect(IllegalParametersException.class);
        expectedException.expectMessage(String.format(Messages.IDENTITY_PROVIDER_NOT_FOUND, -1));

        IdentityProviderVO identityProvider = new IdentityProviderVO();
        identityProvider.setName("-1");
        identityProviderService.update("-1", identityProvider);
    }

    private IdentityProviderVO createIdentityProvider() {
        IdentityProviderVO identityProvider = new IdentityProviderVO();
        identityProvider.setName(RandomStringUtils.randomAlphabetic(10));
        identityProvider.setTokenEndpoint(RandomStringUtils.randomAlphabetic(10));
        identityProvider.setApiEndpoint(RandomStringUtils.randomAlphabetic(10));
        identityProvider.setVerificationEndpoint(RandomStringUtils.randomAlphabetic(10));

        DefaultTransactionDefinition tx = new DefaultTransactionDefinition();
        tx.setName("CreateTx");
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionStatus status = txManager.getTransaction(tx);
        try {
            providerDao.persist(identityProvider);
        } catch (Exception ex) {
            txManager.rollback(status);
            throw ex;
        } finally {
            try {
                txManager.commit(status);
            } catch (Exception ex) {
                txManager.rollback(status);
            }
        }

        return identityProvider;
    }

    @Test
    public void should_update_identity_provider() throws Exception {
        IdentityProviderVO identityProvider = createIdentityProvider();
        identityProvider.setApiEndpoint(RandomStringUtils.randomAlphabetic(10));
        identityProvider.setTokenEndpoint(RandomStringUtils.randomAlphabetic(10));
        identityProvider.setVerificationEndpoint(RandomStringUtils.randomAlphabetic(10));

        IdentityProviderVO updated = identityProviderService.update(identityProvider.getName(), identityProvider);
        assertThat(updated, notNullValue());
        assertThat(updated.getName(), equalTo(identityProvider.getName()));
        assertThat(updated.getApiEndpoint(), equalTo(identityProvider.getApiEndpoint()));
        assertThat(updated.getTokenEndpoint(), not(equalTo(identityProvider.getTokenEndpoint())));
        assertThat(updated.getVerificationEndpoint(), not(equalTo(identityProvider.getVerificationEndpoint())));
    }

    @Test
    public void should_update_identity_provider_name_change_not_allowed() throws Exception {
        IdentityProviderVO identityProvider = createIdentityProvider();
        String oldName = identityProvider.getName();
        identityProvider.setName(identityProvider.getName() + RandomStringUtils.randomAlphabetic(10));
        identityProvider.setApiEndpoint(RandomStringUtils.randomAlphabetic(10));
        identityProvider.setTokenEndpoint(RandomStringUtils.randomAlphabetic(10));
        identityProvider.setVerificationEndpoint(RandomStringUtils.randomAlphabetic(10));

        expectedException.expect(IllegalParametersException.class);
        expectedException.expectMessage(String.format(Messages.IDENTITY_PROVIDER_NAME_CHANGE_NOT_ALLOWED, oldName, identityProvider.getName()));

        identityProviderService.update(oldName, identityProvider);
    }

    @Test
    public void should_delete_identity_provider() throws Exception {
        IdentityProviderVO identityProvider = createIdentityProvider();
        assertNotNull(identityProviderService.find(identityProvider.getName()));
        identityProviderService.delete(identityProvider.getName());
        assertNull(identityProviderService.find(identityProvider.getName()));
    }

    @Test
    public void should_get_identity_provider_by_name() throws Exception {
        IdentityProviderVO identityProvider = createIdentityProvider();
        IdentityProviderVO returned = identityProviderService.find(identityProvider.getName());
        assertThat(returned.getName(), equalTo(identityProvider.getName()));
        assertThat(returned.getApiEndpoint(), equalTo(identityProvider.getApiEndpoint()));
        assertThat(returned.getTokenEndpoint(), equalTo(identityProvider.getTokenEndpoint()));
        assertThat(returned.getVerificationEndpoint(), equalTo(identityProvider.getVerificationEndpoint()));
    }

    @Test
    public void should_get_identity_provider_by_id() throws Exception {
        IdentityProviderVO identityProvider = createIdentityProvider();
        IdentityProviderVO returned = identityProviderService.find(identityProvider.getName());
        assertThat(returned.getName(), equalTo(identityProvider.getName()));
        assertThat(returned.getApiEndpoint(), equalTo(identityProvider.getApiEndpoint()));
        assertThat(returned.getTokenEndpoint(), equalTo(identityProvider.getTokenEndpoint()));
        assertThat(returned.getVerificationEndpoint(), equalTo(identityProvider.getVerificationEndpoint()));
    }
}
