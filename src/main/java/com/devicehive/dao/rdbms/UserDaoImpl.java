package com.devicehive.dao.rdbms;

import com.devicehive.dao.CacheConfig;
import com.devicehive.dao.UserDao;
import com.devicehive.model.Network;
import com.devicehive.model.User;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.of;

@Repository
public class UserDaoImpl extends GenericDaoImpl implements UserDao {

    public Optional<User> findByName(String name) {
        return createNamedQuery(User.class, "User.findByName", of(CacheConfig.get()))
                .setParameter("login", name)
                .getResultList()
                .stream().findFirst();
    }

    public User findByGoogleName(String name) {
        return createNamedQuery(User.class, "User.findByGoogleName", empty())
                .setParameter("login", name)
                .getResultList()
                .stream().findFirst().orElse(null);
    }

    public User findByFacebookName(String name) {
        return createNamedQuery(User.class, "User.findByFacebookName", empty())
                .setParameter("login", name)
                .getResultList()
                .stream().findFirst().orElse(null);
    }

    public User findByGithubName(String name) {
        return createNamedQuery(User.class, "User.findByGithubName", empty())
                .setParameter("login", name)
                .getResultList()
                .stream().findFirst().orElse(null);
    }

    public Optional<User> findByIdentityName(String login, String googleLogin, String facebookLogin, String githubLogin) {
        return createNamedQuery(User.class, "User.findByIdentityName", of(CacheConfig.bypass()))
                .setParameter("login", login)
                .setParameter("googleLogin", googleLogin)
                .setParameter("facebookLogin", facebookLogin)
                .setParameter("githubLogin", githubLogin)
                .getResultList()
                .stream().findFirst();
    }

    public long hasAccessToNetwork(User user, Network network) {
        return createNamedQuery(Long.class, "User.hasAccessToNetwork", empty())
                .setParameter("user", user)
                .setParameter("network", network)
                .getSingleResult();
    }

    public long hasAccessToDevice(User user, String deviceGuid) {
        return createNamedQuery(Long.class, "User.hasAccessToDevice", empty())
                .setParameter("user", user)
                .setParameter("guid", deviceGuid)
                .getSingleResult();
    }

    public User getWithNetworksById(long id) {
        return createNamedQuery(User.class, "User.getWithNetworksById", of(CacheConfig.refresh()))
                .setParameter("id", id)
                .getResultList()
                .stream().findFirst().orElse(null);
    }

    @Override
    public int deleteById(long id) {
        return createNamedQuery("User.deleteById", of(CacheConfig.bypass()))
                .setParameter("id", id)
                .executeUpdate();
    }
}
