package com.devicehive.examples;


import com.devicehive.client.impl.context.HiveContext;
import com.devicehive.client.impl.context.HiveRestClient;
import com.devicehive.client.impl.context.HiveWebSocketClient;
import com.devicehive.client.model.Role;
import com.devicehive.client.model.exceptions.HiveException;
import org.apache.commons.cli.*;

import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;

import static com.devicehive.constants.Constants.*;

public abstract class Example {
    private final HelpFormatter HELP_FORMATTER = new HelpFormatter();
    private final Options options = new Options();
    private final HiveRestClient restClient;
    private final HiveWebSocketClient wsClient;
    private final PrintStream err;
    private final PrintStream out;

    protected Example(PrintStream err,
                      PrintStream out,
                      Role role,
                      String... args)
            throws HiveException, URISyntaxException {
        this.err = err;
        this.out = out;
        options.addOption(USE_SOCKETS, false, USE_SOCKETS_DESCRIPTION);
        options.addOption(URL, true, URL_DESCRIPTION);
//        addExtraOptions();
        CommandLine commandLine = parse(err, args);
        URI serverUrl = new URI(commandLine.getOptionValue(URL));
        HiveContext context;
        context = commandLine.hasOption(USE_SOCKETS)
                ? new HiveContext(true, serverUrl, role, null, null)
                : new HiveContext(false, serverUrl, role, null, null);
        wsClient = new HiveWebSocketClient(serverUrl, context);
        restClient = new HiveRestClient(serverUrl, context);
    }

    private CommandLine parse(PrintStream err, String... args) {
        CommandLineParser parser = new BasicParser();
        try {
            return parser.parse(options, args);
        } catch (ParseException e) {
            err.print(PARSE_EXCEPTION_MESSAGE);
            help();
        }
        return null;
    }

    private void help() {
        HELP_FORMATTER.printHelp(NAME, options);
    }

    public void addExtraOptions(){
        //nothing to add;
    }

    public abstract void run();
}
