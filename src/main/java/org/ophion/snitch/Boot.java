package org.ophion.snitch;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.commons.cli.*;
import org.ophion.snitch.handlers.SNSEventHandler;
import org.ophion.snitch.util.Log;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: raferrei
 * Date: Nov 6, 2011
 * Time: 9:12:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class Boot {

    private static final Log LOG = new Log();

    public static void main(String[] args) throws RuntimeException, ParseException, InterruptedException {


        // CLI
        Options options = new Options();
        options.addOption("c", "config", true, "config file");
        options.addOption("v", "verbose", false, "spews more to the logs");
        options.addOption("h", "help", false, "shows help");


        CommandLineParser parser = new PosixParser();
        CommandLine c = parser.parse(options, args);

        if (args.length < 2) {
            HelpFormatter hp = new HelpFormatter();
            hp.printHelp("java -jar snitch.jar [options] --config FILE", "\n\n Options:", options, "\n");
            System.exit(1);

        }

        if (!c.hasOption("c")) {
            System.out.println("You must pass a config file!");
            System.exit(1);
        }

        LOG.info("using config file: %s", c.getOptionValue("c"));

        if (c.hasOption("v")) {
            Logger logger = Logger.getLogger("org.ophion");
            logger.setLevel(Level.FINE);
            LOG.fine("running in verbose mode");
        }


        final Injector injector = Guice.createInjector(new SnitchModule(c.getOptionValue("c")));
        EventManager em = injector.getInstance(EventManager.class);

        // wiring handlers
        em.onInstanceEvent.appendHandler( injector.getInstance(SNSEventHandler.class));
        
        em.start();

    }
}