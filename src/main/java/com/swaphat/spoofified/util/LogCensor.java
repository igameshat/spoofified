package com.swaphat.spoofified.util;

import com.swaphat.spoofified.ClientSpooferOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.rewrite.RewriteAppender;
import org.apache.logging.log4j.core.appender.rewrite.RewritePolicy;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class LogCensor implements RewritePolicy {

    public static void register() {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration config = context.getConfiguration();

        LoggerConfig rootConfig = config.getRootLogger();

        // 1. Grab the exact original references (THIS PRESERVES THE DEBUG/INFO FILTERS!)
        List<AppenderRef> originalRefs = new ArrayList<>(rootConfig.getAppenderRefs());
        AppenderRef[] appenderRefsArray = originalRefs.toArray(new AppenderRef[0]);

        // 2. Create our Censor Appender
        RewriteAppender rewriteAppender = RewriteAppender.createAppender(
                "SpoofifiedCensor",
                "true",
                appenderRefsArray,
                config,
                new LogCensor(),
                null
        );
        rewriteAppender.start();
        config.addAppender(rewriteAppender);

        // 3. Unplug the original, unfiltered connections
        for (AppenderRef ref : originalRefs) {
            rootConfig.removeAppender(ref.getRef());
        }

        // 4. Plug our Censor in, carefully preserving the Root Logger's original Level
        rootConfig.addAppender(rewriteAppender, rootConfig.getLevel(), rootConfig.getFilter());

        context.updateLoggers();
    }

    // Generates a random string of 5 to 12 asterisks
    private String getRandomStars() {
        int length = ThreadLocalRandom.current().nextInt(5, 13);
        return "*".repeat(length);
    }

    @Override
    public LogEvent rewrite(LogEvent source) {
        if (!ClientSpooferOptions.ENABLED || ClientSpooferOptions.HIDDEN_MODS.isEmpty()) {
            return source;
        }

        String originalMsg = source.getMessage() != null ? source.getMessage().getFormattedMessage() : "";

        if (originalMsg.contains("[CHAT]")) {
            return source;
        }

        String originalLogger = source.getLoggerName() != null ? source.getLoggerName() : "";

        String censoredMsg = originalMsg;
        String censoredLogger = originalLogger;
        boolean wasModified = false;

        for (String hiddenMod : ClientSpooferOptions.HIDDEN_MODS) {
            String sanitizedId = hiddenMod.toLowerCase().replace("-", "").replace("_", "");

            if (censoredMsg.toLowerCase().contains(sanitizedId)) {
                censoredMsg = censoredMsg.replaceAll("(?i)" + sanitizedId, getRandomStars());
                wasModified = true;
            }

            // Replace in the logger name with random stars
            if (censoredLogger.toLowerCase().contains(sanitizedId)) {
                censoredLogger = censoredLogger.replaceAll("(?i)" + sanitizedId, getRandomStars());
                wasModified = true;
            }
        }

        if (!wasModified) {
            return source; // If clean, return the original event untouched
        }

        // Return a cloned event with the censored text
        return new Log4jLogEvent.Builder(source)
                .setLoggerName(censoredLogger)
                .setMessage(new SimpleMessage(censoredMsg))
                .build();
    }
}