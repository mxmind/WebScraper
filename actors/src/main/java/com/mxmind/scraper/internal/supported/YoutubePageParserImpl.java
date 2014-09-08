package com.mxmind.scraper.internal.supported;

import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.*;
import com.mxmind.scraper.api.PageContent;
import com.mxmind.scraper.api.PageParser;
import net.sourceforge.htmlunit.corejs.javascript.EcmaError;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;

/**
 * @author vzdomish
 * @version 1.0.0
 * @since 1.0.0
 */
public class YoutubePageParserImpl implements PageParser {

    private static final Logger LOG = LoggerFactory.getLogger(YoutubePageParserImpl.class);

    public static final int PAGE_JS_TIMEOUT = 1000;

    private final String baseUrl;

    public YoutubePageParserImpl(String url) {
        baseUrl = url;
    }

    @Override
    public PageContent fetchPageContent(String url) {
        final WebClient webClient = getWebClient();
        try {
            WebRequest request = new WebRequest(new URL(url));

            final HtmlPage page = webClient.getPage(request);
            int i = webClient.waitForBackgroundJavaScript(PAGE_JS_TIMEOUT);
            while (i > 0) {
                i = webClient.waitForBackgroundJavaScript(PAGE_JS_TIMEOUT);
                if (i == 0) {
                    break;
                }
                synchronized (page) {
                    page.wait(PAGE_JS_TIMEOUT / 2);
                }
            }
            webClient.getAjaxController().processSynchron(page, request, false);

            final DomNode node = page.querySelector("#pl-video-list > button");
            if (node != null && node instanceof HtmlButton) {
                final HtmlButton button = (HtmlButton) node;
                button.click();
            }

            final String title = page.getTitleText();
            final List<String> linksToVisit = new ArrayList<>();
            page.querySelectorAll("tr.pl-video td.pl-video-title a").forEach(input -> {
                assert input != null;
                //System.out.println(input.getTextContent().trim());
                if(input instanceof HtmlAnchor){
                    final Optional<HtmlAnchor> link = Optional.of((HtmlAnchor) node);
                    if(link.isPresent()){
                        final String href = link.get().getHrefAttribute();
                        System.out.println(href);
                    }
                }
            });

            return new PageContentImpl(url, linksToVisit, title, "");

        } catch (IOException | InterruptedException ex) {
            // should create page retireval actor to handle ex in friendly way.
            LOG.error(ex.getMessage(), ex);
        } catch (EcmaError | ScriptException ignored) {
            /* ignore */
        }

        return null;
    }

    private WebClient getWebClient() {
        LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");

        java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);
        java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);

        final WebClient webClient = new WebClient(BrowserVersion.FIREFOX_10);
        webClient.getOptions().setCssEnabled(true);
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.setAjaxController(new NicelyResynchronizingAjaxController());

        return webClient;
    }
}
