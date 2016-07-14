package com.devicehive.dao.riak;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.commands.indexes.BinIndexQuery;
import com.basho.riak.client.api.commands.kv.DeleteValue;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.api.commands.mapreduce.BucketMapReduce;
import com.basho.riak.client.api.commands.mapreduce.MapReduce;
import com.basho.riak.client.api.commands.mapreduce.filters.MatchFilter;
import com.basho.riak.client.core.RiakFuture;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.basho.riak.client.core.query.functions.Function;
import com.basho.riak.client.core.util.BinaryValue;
import com.devicehive.dao.NetworkDao;
import com.devicehive.dao.UserDao;
import com.devicehive.model.Device;
import com.devicehive.model.Network;
import com.devicehive.model.User;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.enums.UserStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Profile({"riak"})
@Repository
public class UserDaoImpl implements UserDao {

    private static final Namespace USER_NS = new Namespace("user");

    @Autowired
    private RiakClient client;

    @Autowired
    private UserNetworkDaoImpl userNetworkDao;

    @Autowired
    private NetworkDao networkDao;

    private final Map<String, String> sortMap = new HashMap<>();

    public UserDaoImpl() {
        sortMap.put("login", "function(a,b){ return a.login %s b.login; }");
        sortMap.put("role", "function(a,b){ return a.role %s b.role; }");
        sortMap.put("status", "function(a,b){ return a.status %s b.status; }");
        sortMap.put("lastLogin", "function(a,b){ return a.lastLogin %s b.lastLogin; }");
        sortMap.put("googleLogin", "function(a,b){ return a.googleLogin %s b.googleLogin; }");
        sortMap.put("facebookLogin", "function(a,b){ return a.facebookLogin %s b.facebookLogin; }");
        sortMap.put("githubLogin", "function(a,b){ return a.githubLogin %s b.githubLogin; }");
        sortMap.put("entityVersion", "function(a,b){ return a.entityVersion %s b.entityVersion; }");
    }

    @Override
    public Optional<User> findByName(String name) {
        User user = findBySomeIdentityName(name, "login");
        return Optional.ofNullable(user);
    }

    @Override
    public User findByGoogleName(String name) {
        return findBySomeIdentityName(name, "googleLogin");
    }

    @Override
    public User findByFacebookName(String name) {
        return findBySomeIdentityName(name, "facebookLogin");
    }

    @Override
    public User findByGithubName(String name) {
        return findBySomeIdentityName(name, "githubLogin");
    }

    private User findBySomeIdentityName(String name, String identityName) {
        if (name == null) {
            return null;
        }
        BinIndexQuery biq = new BinIndexQuery.Builder(USER_NS, identityName, name).build();
        try {
            BinIndexQuery.Response response = client.execute(biq);
            List<BinIndexQuery.Response.Entry> entries = response.getEntries();
            if (entries.isEmpty()) {
                return null;
            }
            Location location = entries.get(0).getRiakObjectLocation();
            FetchValue fetchOp = new FetchValue.Builder(location)
                    .build();
            return client.execute(fetchOp).getValue(User.class);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Optional<User> findByIdentityName(String login, String googleLogin, String facebookLogin, String githubLogin) {
        User userToCheck;
        userToCheck = findByGoogleName(googleLogin);
        if (userToCheck != null) {
            if (doesUserAlreadyExist(userToCheck, login)) {
                return Optional.of(userToCheck);
            }
        }

        userToCheck = findByFacebookName(facebookLogin);
        if (userToCheck != null) {
            if (doesUserAlreadyExist(userToCheck, login)) {
                return Optional.of(userToCheck);
            }
        }

        userToCheck = findByGithubName(githubLogin);
        if (userToCheck != null) {
            if (doesUserAlreadyExist(userToCheck, login)) {
                return Optional.of(userToCheck);
            }
        }

        return Optional.empty();
    }

    private boolean doesUserAlreadyExist(User user, String login) {
        return (!user.getLogin().equals(login) && user.getStatus() != UserStatus.DELETED);
    }

    @Override
    public long hasAccessToNetwork(User user, Network network) {
        Set<Long> networks = userNetworkDao.findNetworksForUser(user.getId());
        if (networks != null && networks.contains(network.getId())) {
            return 1L;
        } else {
            return 0L;
        }
    }

    @Override
    public long hasAccessToDevice(User user, String deviceGuid) {
        Set<Long> networkIds = userNetworkDao.findNetworksForUser(user.getId());
        for (Long networkId : networkIds) {
            Network network = networkDao.find(networkId);
            long guidCount = network.getDevices()
                    .stream()
                    .map(Device::getGuid)
                    .filter(g -> g.equals(deviceGuid))
                    .count();
            if (guidCount > 0) {
                return guidCount;
            }
        }
        return 0L;
    }

    @Override
    public User getWithNetworksById(long id) {
        User user = find(id);

        Set<Long> networkIds = userNetworkDao.findNetworksForUser(id);
        if (networkIds == null) {
            return user;
        }
        Set<Network> networks = new HashSet<>();
        for (Long networkId : networkIds) {
            Network network = networkDao.find(networkId);
            networks.add(network);
        }
        user.setNetworks(networks);
        return user;
    }

    @Override
    public int deleteById(long id) {
        Location location = new Location(USER_NS, String.valueOf(id));
        DeleteValue deleteOp = new DeleteValue.Builder(location).build();
        try {
            client.execute(deleteOp);
            return 1;
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public User find(Long id) {
        try {
            Location location = new Location(USER_NS, String.valueOf(id));
            FetchValue fetchOp = new FetchValue.Builder(location).build();
            return client.execute(fetchOp).getValue(User.class);
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void persist(User user) {
        merge(user);
    }

    @Override
    public User merge(User user) {
        if (user.getId() == null) {
            user.setId(System.currentTimeMillis());
        }
        try {
            Location location = new Location(USER_NS, String.valueOf(user.getId()));
            StoreValue storeOp = new StoreValue.Builder(user).withLocation(location).build();
            client.execute(storeOp);
            return user;
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void unassignNetwork(@NotNull User existingUser, @NotNull long networkId) {
        userNetworkDao.delete(existingUser.getId(), networkId);
    }

    @Override
    public List<User> getList(String login, String loginPattern,
                              Integer role, Integer status,
                              String sortField, Boolean sortOrderAsc,
                              Integer take, Integer skip) {


        ArrayList<User> result = new ArrayList<>();
        if (login != null) {
            Optional<User> user = findByName(login);
            if (user.isPresent()) {
                result.add(user.get());
            }
        } else {
            try {
                String sortFunction = sortMap.get(sortField);
                if (sortFunction == null) {
                    sortFunction = sortMap.get("login");
                }
                if (sortOrderAsc == null) {
                    sortOrderAsc = true;
                }
                BucketMapReduce.Builder builder = new BucketMapReduce.Builder()
                        .withNamespace(USER_NS)
                        .withMapPhase(Function.newNamedJsFunction("Riak.mapValuesJson"));

                if (loginPattern != null) {
                    loginPattern = loginPattern.replace("%", "");
                    String functionString = String.format(
                        "function(values, arg) {" +
                            "return values.filter(function(v) {" +
                                "var login = v.login;" +
                                "var match = login.indexOf('%s');" +
                                "return match > -1;" +
                            "})" +
                        "}", loginPattern);
                    Function reduceFunction = Function.newAnonymousJsFunction(functionString);
                    builder.withReducePhase(reduceFunction);
                }

                if (role != null) {
                    String roleString = UserRole.getValueForIndex(role).name();
                    String functionString = String.format(
                            "function(values, arg) {" +
                                "return values.filter(function(v) {" +
                                    "var role = v.role;" +
                                    "return role == '%s';" +
                                "})" +
                            "}", roleString);
                    Function reduceFunction = Function.newAnonymousJsFunction(functionString);
                    builder.withReducePhase(reduceFunction);
                }

                if (status != null) {
                    String statusString = UserStatus.getValueForIndex(status).name();
                    String functionString = String.format(
                            "function(values, arg) {" +
                                "return values.filter(function(v) {" +
                                    "var status = v.status;" +
                                    "return status == '%s';" +
                                "})" +
                            "}", statusString);
                    Function reduceFunction = Function.newAnonymousJsFunction(functionString);
                    builder.withReducePhase(reduceFunction);
                }

                builder.withReducePhase(Function.newNamedJsFunction("Riak.reduceSort"),
                        String.format(sortFunction, sortOrderAsc ? ">" : "<"),
                        take == null);

                if (take != null) {
                    int[] args = new int[2];
                    args[0] = skip != null ? skip : 0;
                    args[1] = args[0] + take;
                    builder.withReducePhase(Function.newNamedJsFunction("Riak.reduceSlice"), args, true);
                }
                BucketMapReduce bmr = builder.build();
                RiakFuture<MapReduce.Response, BinaryValue> future = client.executeAsync(bmr);
                future.await();
                MapReduce.Response response = future.get();
                result.addAll(response.getResultsFromAllPhases(User.class));
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }
}
