package com.devicehive.examples;

import com.devicehive.client.AccessKeyController;
import com.devicehive.client.HiveClient;
import com.devicehive.client.HiveFactory;
import com.devicehive.client.NetworkController;
import com.devicehive.client.UserController;
import com.devicehive.client.model.AccessKey;
import com.devicehive.client.model.AccessKeyPermission;
import com.devicehive.client.model.AllowedAction;
import com.devicehive.client.model.Network;
import com.devicehive.client.model.User;
import com.devicehive.client.model.UserRole;
import com.devicehive.client.model.UserStatus;
import com.devicehive.client.model.exceptions.HiveException;
import com.devicehive.exceptions.ExampleException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.devicehive.constants.Constants.USE_SOCKETS;

/**
 * Administrative example represents administrative client features such as creating networks,
 * users access keys and how to manage them.
 */
public class AdministrativeExample extends Example {
    private static final String LOGIN = "login";
    private static final String LOGIN_DESCRIPTION = "User login.";
    private static final String PASSWORD = "password";
    private static final String PASSWORD_DESCRIPTION = "User password.";
    private static final String ACCESS_KEY = "ak";
    private static final String ACCESS_KEY_DESCRIPTION = "Access key should be created. If used, " +
            "login and password should be provided";
    private static final String NETWORK = "nname";
    private static final String NETWORK_DESCRIPTION = "New network name.";
    private static final String NETWORK_KEY = "nk";
    private static final String NETWORK_KEY_DESCRIPTION = "New network key";
    private static final String DEFAULT_USER_NAME = "dhadmin";
    private static final String DEFAULT_USER_PASSWORD = "dhadmin_#911";
    private final HiveClient hiveClient;
    private final CommandLine commandLine;

    /**
     * Constructor. Creates hiveClient instance.
     *
     * @param out  out PrintStream
     * @param args commandLine arguments
     * @throws HiveException    if unable to create hiveClient instance
     * @throws ExampleException if server URL cannot be parsed
     */
    public AdministrativeExample(PrintStream out, String... args) throws ExampleException, HiveException {
        super(out, args);
        commandLine = getCommandLine();
        hiveClient = HiveFactory.createClient(getServerUrl(),
                commandLine.hasOption(USE_SOCKETS),
                Example.HIVE_CONNECTION_EVENT_HANDLER);
    }

    /**
     * Entrance point.
     *
     * @param args command line arguments
     */
    public static void main(String... args) {
        try {
            Example clientExample = new AdministrativeExample(System.out, args);
            clientExample.run();
        } catch (HiveException | ExampleException | IOException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public Options makeOptionsSet() {
        Options options = new Options();
        options.addOption(LOGIN, true, LOGIN_DESCRIPTION);
        options.addOption(PASSWORD, true, PASSWORD_DESCRIPTION);
        options.addOption(ACCESS_KEY, false, ACCESS_KEY_DESCRIPTION);
        options.addOption(NETWORK, true, NETWORK_DESCRIPTION);
        options.addOption(NETWORK_KEY, true, NETWORK_KEY_DESCRIPTION);
        return options;
    }

    /**
     * Includes a set of operations: user, network and access key operations.
     *
     * @throws HiveException
     * @throws ExampleException
     * @throws IOException
     */
    @Override
    public void run() throws HiveException, ExampleException, IOException {
        try {
            hiveClient.authenticate(DEFAULT_USER_NAME, DEFAULT_USER_PASSWORD);
            Network network = networkOperationsSet();
            User user = usersOperationSet(network);
            accessKeyOperationsSet(user);
            cleanAll(user, network);
        } finally {
            hiveClient.close();
        }
    }

    /**
     * Performs set of network operations. Lists all available networks, creates a new network if there is no network
     * with such key provided or takes existing one. Updates network description. All these actions will be performed
     * if the network name and the network key are provided as command line arguments.
     *
     * @return created network, if any was created, null otherwise
     * @throws HiveException
     */
    private Network networkOperationsSet() throws HiveException {
        if (commandLine.hasOption(NETWORK)) {
            String name = commandLine.getOptionValue(NETWORK);
            String key = commandLine.getOptionValue(NETWORK_KEY);
            NetworkController nc = hiveClient.getNetworkController();
            List<Network> allCreatedWithName =
                    nc.listNetworks(name, null, null, null, null, null);
            Network network = null;
            if (allCreatedWithName.size() != 0) {
                for (Network n : allCreatedWithName) {
                    if (key.equals(n.getKey())) {
                        network = n;
                        break;
                    }
                }
                if (network != null)
                    print("There found {} networks with such name. Requested network found", allCreatedWithName.size());
            }
            if (network == null) {
                print("No networks with such name found. New network will be created");
                network = new Network();
                network.setName(name);
                network.setKey(key);
                long nId = nc.insertNetwork(network);
                network.setId(nId);
                network.setDescription("updated network");
                nc.updateNetwork(nId, network);
            }
            return network;
        }
        return null;
    }

    /**
     * performs set of user operations. Lists all available users. If there is found a user with provided name,
     *
     * @param network
     * @return
     * @throws HiveException
     */
    private User usersOperationSet(Network network) throws HiveException {
        if (commandLine.hasOption(LOGIN)) {
            String name = commandLine.getOptionValue(LOGIN);
            String password = commandLine.getOptionValue(PASSWORD);
            UserController uc = hiveClient.getUserController();
            List<User> usersWithSuchName = uc.listUsers(name, null, null, null, null, null, null, null);
            User user;
            if (usersWithSuchName.isEmpty()) {
                print("No users with such name found. New one will be created");
                user = new User();
                user.setLogin(name);
                user.setPassword("stubPassword");
                user.setRole(UserRole.CLIENT);
                user.setStatus(UserStatus.ACTIVE);
                User created = uc.insertUser(user);
                user.setId(created.getId());
                user.setLastLogin(created.getLastLogin());
            } else
                user = usersWithSuchName.get(0);
            user.setPassword(password);
            uc.updateUser(user.getId(), user);
            if (network != null)
                uc.assignNetwork(user.getId(), network.getId());
            return user;
        }
        return null;
    }

    private void accessKeyOperationsSet(User user) throws HiveException {
        if (commandLine.hasOption(ACCESS_KEY) && user != null) {
            AccessKeyController akc = hiveClient.getAccessKeyController();
            AccessKey accessKey = new AccessKey();
            accessKey.setLabel("example key");
            AccessKeyPermission akp = new AccessKeyPermission();
            Set<String> actions = new HashSet<>();
            actions.add(AllowedAction.GET_NETWORK.getValue());
            akp.setActions(actions);
            Set<Long> networks = new HashSet<>();
            List<Network> allAvailable =
                    hiveClient.getNetworkController().listNetworks(null, null, null, null, null, null);
            for (Network currentNetwork : allAvailable) {
                networks.add(currentNetwork.getId());
            }
            akp.setNetworks(networks);
            Set<AccessKeyPermission> permissions = new HashSet<>();
            permissions.add(akp);
            accessKey.setPermissions(permissions);
            AccessKey created = akc.insertKey(user.getId(), accessKey);
            accessKey.setId(created.getId());
            accessKey.setKey(created.getKey());
            HiveClient newUserHC = HiveFactory.createClient(getServerUrl(),
                    commandLine.hasOption(USE_SOCKETS),
                    Example.HIVE_CONNECTION_EVENT_HANDLER);
            newUserHC.authenticate(accessKey.getKey());
            List<Network> allowedNetworks = newUserHC.getNetworkController().listNetworks(null, null, null, null,
                    null, null);
            assert (allowedNetworks.size() == allAvailable.size());
            akc.deleteKey(accessKey.getId());
        }
    }

    private void cleanAll(User user, Network network) throws HiveException {
        if (user != null)
            hiveClient.getUserController().deleteUser(user.getId());
        if (network != null)
            hiveClient.getNetworkController().deleteNetwork(network.getId());
    }
}
