package com.mxmind.scraper.internal.supported;

import com.gargoylesoftware.htmlunit.*;
import org.apache.commons.logging.LogFactory;

import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.System.out;

/**
 * The WebScraper solution.
 *
 * @author mxmind
 * @version 1.0-SNAPSHOT
 * @since 1.0-SNAPSHOT
 */
public abstract class WebClientFactory {

    static {
        LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");

        Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);
        Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);
    }

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
                    out.format("Scraper scriptable web client is configured \r");
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
                    out.format("Scraper base web client is configured \r");

                }
            }
        }
        return syncInstance;
    }

}
