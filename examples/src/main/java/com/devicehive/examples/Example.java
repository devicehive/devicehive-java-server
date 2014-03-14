package com.devicehive.examples;


import com.devicehive.client.model.exceptions.HiveException;
import com.devicehive.exceptions.ExampleException;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;

import static com.devicehive.constants.Constants.*;

public abstract class Example {
    private final HelpFormatter HELP_FORMATTER = new HelpFormatter();
    private final Options options;
    private final PrintStream err;
    private final CommandLine commandLine;
    private final PrintStream out;
    private final URI serverUrl;

    protected Example(PrintStream err,
                      PrintStream out,
                      String... args)
            throws HiveException {
        this.err = err;
        this.out = out;
        options = makeOptionsSet();
        options.addOption(USE_SOCKETS, false, USE_SOCKETS_DESCRIPTION);
        Option url = new Option(URL, true, URL_DESCRIPTION);
        url.setRequired(true);
        options.addOption(url);
        commandLine = parse(args);
        try {
            serverUrl = new URI(commandLine.getOptionValue(URL));
        } catch (URISyntaxException e) {
            help();
            throw new HiveException("Incorrect server URI", e);
        }
    }

    protected final URI getServerUrl() {
        return serverUrl;
    }

    private CommandLine parse(String... args) {
        CommandLineParser parser = new BasicParser();
        try {
            return parser.parse(options, args);
        } catch (ParseException e) {
            err.print(PARSE_EXCEPTION_MESSAGE);
            help();
        }
        return null;
    }

    public final void help() {
        HELP_FORMATTER.printHelp(NAME, options);
    }


    public abstract Options makeOptionsSet();

    public final CommandLine getCommandLine() {
        return commandLine;
    }

    public final void print(String msg, Object... objects) {
        String resultMessage;
        if (objects != null && objects.length > 0) {
            String[] replaceList = new String[objects.length];
            for (String str : replaceList) {
                str = "{}";
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

    public abstract void run() throws HiveException, ExampleException, IOException;
}
