package org.codelibs.fess.exec;

import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.Constants;
import org.codelibs.fess.crawler.client.EsClient;
import org.codelibs.fess.es.client.FessEsClient;
import org.codelibs.fess.helper.SuggestHelper;
import org.codelibs.fess.util.ComponentUtil;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.lastaflute.di.core.SingletonLaContainer;
import org.lastaflute.di.core.factory.SingletonLaContainerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class SuggestCreater implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Logger logger = LoggerFactory.getLogger(Crawler.class);

    @Resource
    public FessEsClient fessEsClient;

    protected static class Options {
        @Option(name = "-s", aliases = "--sessionId", metaVar = "sessionId", usage = "Session ID")
        protected String sessionId;

        @Option(name = "-n", aliases = "--name", metaVar = "name", usage = "Name")
        protected String name;

        protected Options() {
            // noghing
        }
    }

    public static void main(final String[] args) {
        final Options options = new Options();

        final CmdLineParser parser = new CmdLineParser(options);
        try {
            parser.parseArgument(args);
        } catch (final CmdLineException e) {
            System.err.println(e.getMessage()); // NOPMD
            System.err.println("java " + Crawler.class.getCanonicalName() // NOPMD
                    + " [options...] arguments...");
            parser.printUsage(System.err);
            return;
        }

        final String transportAddresses = System.getProperty(Constants.FESS_ES_TRANSPORT_ADDRESSES);
        if (StringUtil.isNotBlank(transportAddresses)) {
            System.setProperty(EsClient.TRANSPORT_ADDRESSES, transportAddresses);
        }
        final String clusterName = System.getProperty(Constants.FESS_ES_CLUSTER_NAME);
        if (StringUtil.isNotBlank(clusterName)) {
            System.setProperty(EsClient.CLUSTER_NAME, clusterName);
        }

        int exitCode;
        try {
            SingletonLaContainerFactory.setConfigPath("app.xml");
            SingletonLaContainerFactory.init();

            final Thread shutdownCallback = new Thread("ShutdownHook") {
                @Override
                public void run() {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Destroying LaContainer..");
                    }
                    SingletonLaContainerFactory.destroy();
                }
            };
            Runtime.getRuntime().addShutdownHook(shutdownCallback);
            exitCode = process(options);
        } catch (final Throwable t) { // NOPMD
            logger.error("Suggest creater does not work correctly.", t);
            exitCode = Constants.EXIT_FAIL;
        } finally {
            SingletonLaContainerFactory.destroy();
        }

        logger.info("Finished suggestCreater.");
        System.exit(exitCode);
    }

    private static int process(final Options options) {
        final SuggestCreater creater = SingletonLaContainer.getComponent(SuggestCreater.class);
        final LocalDateTime startTime = LocalDateTime.now();
        int ret = creater.create();
        if (ret == 0) {
            ret = creater.purge(startTime);
        }
        return ret;
    }

    private int create() {
        logger.info("Start create suggest document.");

        final AtomicInteger result = new AtomicInteger(1);
        final CountDownLatch latch = new CountDownLatch(1);

        final SuggestHelper suggestHelper = ComponentUtil.getSuggestHelper();
        suggestHelper.indexFromDocuments(ret -> {
            logger.info("Success index from documents.");
            result.set(0);
            latch.countDown();
        }, t -> {
            logger.error("Failed to update suggest index.", t);
            latch.countDown();
        });

        try {
            latch.await();
        } catch (InterruptedException ignore) {}

        return result.get();
    }

    private int purge(LocalDateTime time) {
        final SuggestHelper suggestHelper = ComponentUtil.getSuggestHelper();
        try {
            suggestHelper.purge(time);
            return 0;
        } catch (Exception e) {
            logger.info("Purge error.", e);
            return 1;
        }
    }

}
