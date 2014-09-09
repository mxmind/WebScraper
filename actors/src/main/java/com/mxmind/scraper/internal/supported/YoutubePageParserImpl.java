package com.mxmind.scraper.internal.supported;

import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.util.UrlUtils;
import com.mxmind.scraper.api.PageContent;
import com.mxmind.scraper.api.PageParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.*;

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
        WebClient webClient;
        String title = "video link";
        final List<String> videoLinks = new ArrayList<>();
        final boolean isVideoLink = url.contains("youtube.com") && url.contains("watch");

        if(isVideoLink){
            webClient = WebClientSingleton.getBaseInstance();
        } else {
            webClient = WebClientSingleton.getScriptableInstance();
        }
        try {
            WebRequest request = new WebRequest(new URL(url));
            if(!isVideoLink){
                final Page result = webClient.getPage(request);
                if(result instanceof UnexpectedPage){
                    return new PageContentImpl(url, Collections.emptyList(), "UnexpectedPage", "");
                }

                final HtmlPage page = (HtmlPage) result;
                webClient.waitForBackgroundJavaScript(PAGE_JS_TIMEOUT);

                // 0) sync page val, cannot present page as field, due of "final modificator" restriction;
                synchronized (page) {
                    page.wait(PAGE_JS_TIMEOUT);
                }
                webClient.getAjaxController().processSynchron(page, request, false);

                // 1) max playlist.size <= 200,
                // 2) playlist splitted on 2 chunks,
                // 3) if button present, load yet another items.
                final DomNode node = page.querySelector("#pl-video-list > button");
                if (node != null && node instanceof HtmlButton) {
                    final HtmlButton button = (HtmlButton) node;
                    button.click();
                }

                // 4) get the title of page;
                title = page.getTitleText();

                // 5) collect video- and nav- links;
                page.querySelectorAll("tr.pl-video td.pl-video-title a").forEach(input -> {
                    if(input instanceof HtmlAnchor){
                        final Optional<HtmlAnchor> link = Optional.of((HtmlAnchor) input);
                        if(link.isPresent()){
                            final String href = link.get().getHrefAttribute();
                            videoLinks.add(UrlUtils.resolveUrl(baseUrl, href));
                        }
                    }
                });
                page.cleanUp();
            }
            return new PageContentImpl(url, videoLinks, title, "");

        } catch (Exception ex) {
            // e) should create page retireval actor to handle ex.
            LOG.error(ex.getMessage(), ex);
        }
        return new PageContentImpl(url, Collections.emptyList(), "Exception", "");
    }
}
