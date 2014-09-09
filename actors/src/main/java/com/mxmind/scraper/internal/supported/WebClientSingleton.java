package com.mxmind.scraper.internal.supported;

import com.gargoylesoftware.htmlunit.*;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.logging.Level;

/**
 * The WebScraper solution.
 *
 * @author mxmind
 * @version 1.0-SNAPSHOT
 * @since 1.0-SNAPSHOT
 */
public abstract class WebClientSingleton {

    static {
        LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");

        java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);
        java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);
    }

    private static final Logger LOG = LoggerFactory.getLogger(WebClientSingleton.class);

    private static volatile WebClient scriptableInstance;

    private static volatile WebClient baseInstance;

    public static WebClient getScriptableInstance() {
        WebClient syncInstance = scriptableInstance;
        if (syncInstance == null) {
            synchronized (WebClient.class) {
                syncInstance = scriptableInstance;
                if (syncInstance == null) {
                    syncInstance = new WebClient(BrowserVersion.FIREFOX_10);
                    syncInstance.getOptions().setCssEnabled(true);
                    syncInstance.getOptions().setJavaScriptEnabled(true);
                    syncInstance.getOptions().setThrowExceptionOnScriptError(false);
                    syncInstance.getOptions().setThrowExceptionOnFailingStatusCode(false);
                    syncInstance.setAjaxController(new NicelyResynchronizingAjaxController());

                    scriptableInstance = syncInstance;
                    if (LOG.isInfoEnabled()) {
                        LOG.info("The Scriptable Web Client is configured");
                    }
                }
            }
        }
        return syncInstance;
    }

    public static WebClient getBaseInstance() {
        WebClient syncInstance = baseInstance;
        if (syncInstance == null) {
            synchronized (WebClient.class) {
                syncInstance = baseInstance;
                if (syncInstance == null) {
                    syncInstance = new WebClient(BrowserVersion.FIREFOX_10);
                    syncInstance.getOptions().setCssEnabled(false);
                    syncInstance.getOptions().setJavaScriptEnabled(false);
                    baseInstance = syncInstance;
                    if (LOG.isInfoEnabled()) {
                        LOG.info("The Base Web Client is configured");
                    }
                }
            }
        }
        return syncInstance;
    }

}
