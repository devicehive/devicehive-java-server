package com.devicehive.examples;


import com.devicehive.client.impl.context.connection.HiveConnectionEventHandler;
import com.devicehive.client.model.exceptions.HiveException;
import com.devicehive.exceptions.ExampleException;
import com.devicehive.impl.ConnectionEventHandlerImpl;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;

import static com.devicehive.constants.Constants.NAME;
import static com.devicehive.constants.Constants.PARSE_EXCEPTION_MESSAGE;
import static com.devicehive.constants.Constants.URL;
import static com.devicehive.constants.Constants.URL_DESCRIPTION;
import static com.devicehive.constants.Constants.USE_SOCKETS;
import static com.devicehive.constants.Constants.USE_SOCKETS_DESCRIPTION;

/**
 * Base class for full examples set.
 */
public abstract class Example {
    public static final ConnectionEventHandlerImpl impl = new ConnectionEventHandlerImpl();

    private final HelpFormatter HELP_FORMATTER = new HelpFormatter();
    private final Options options;
    private final CommandLine commandLine;
    private final PrintStream out;
    private final URI serverUrl;

    /**
     * Constructor. Initialize global parameter such as server URL and defines if sockets should be used. Creates
     * handler for commands and notifications
     *
     * @param out  out print stream
     * @param args command line arguments
     * @throws ExampleException if server URL cannot be parsed
     */
    protected Example(PrintStream out, String... args) throws ExampleException {
        this.out = out;
        options = makeOptionsSet();
        options.addOption(USE_SOCKETS, false, USE_SOCKETS_DESCRIPTION);
        Option url = new Option(URL, true, URL_DESCRIPTION);
        url.setRequired(true);
        options.addOption(url);
        commandLine = parse(args);
        try {
            String urlValue = commandLine.getOptionValue(URL);
            serverUrl = new URI(urlValue);
        } catch (URISyntaxException e) {
            help();
            throw new ExampleException("Incorrect server URI", e);
        }
    }


    /**
     * @return REST service URL
     */
    final URI getServerUrl() {
        return serverUrl;
    }

    /**
     * Parses command line according to provided options
     *
     * @param args command line arguments
     * @return parsed command line
     * @throws ExampleException if unable to parse command line (in case of incorrect use of commands)
     */
    private CommandLine parse(String... args) throws ExampleException {
        CommandLineParser parser = new BasicParser();
        try {
            return parser.parse(options, args);
        } catch (ParseException e) {
            help();
            throw new ExampleException(PARSE_EXCEPTION_MESSAGE);
        }
    }

    /**
     * Prints help
     */
    public final synchronized void help() {
        HELP_FORMATTER.printHelp(NAME, options);
    }

    /**
     * Make additional options set
     *
     * @return additional options
     */
    public abstract Options makeOptionsSet();

    /**
     * @return command line
     */
    public final CommandLine getCommandLine() {
        return commandLine;
    }

    /**
     * Prints message. Some objects will be placed instead of "{}" substring
     *
     * @param msg     message
     * @param objects objects to replace "{}"
     */
    public final void print(String msg, Object... objects) {
        String resultMessage;
        if (objects != null && objects.length > 0) {
            String[] replaceList = new String[objects.length];
            for (int i = 0; i < objects.length; i++) {
                replaceList[i] = "{}";
            }
            String[] replacement = new String[objects.length];
            for (int i = 0; i < objects.length; i++) {
                replacement[i] = objects[i].toString();
            }
            resultMessage = StringUtils.replaceEach(msg, replaceList, replacement);
        } else
            resultMessage = msg;
        out.println(resultMessage);

    }

    /**
     * Main method. Represents example's logic
     *
     * @throws HiveException
     * @throws ExampleException
     * @throws IOException
     */
    public abstract void run() throws HiveException, ExampleException, IOException;
}
