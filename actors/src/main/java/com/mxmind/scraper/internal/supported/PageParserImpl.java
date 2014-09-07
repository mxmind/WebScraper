package com.mxmind.scraper.internal.supported;

import com.mxmind.scraper.api.PageContent;
import com.mxmind.scraper.api.PageParser;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The WebScraper solution.
 *
 * @author mxmind
 * @version 1.0-SNAPSHOT
 * @since 1.0-SNAPSHOT
 */
public final class PageParserImpl implements PageParser {

    private static final String USER_AGENT = "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.1; Trident/4.0)";

    private static final int TIME_OUT = 12000;

    private static final Logger LOG = LoggerFactory.getLogger(PageParserImpl.class);

    private final String baseUrl;

    public PageParserImpl(String url) {
        baseUrl = url;
    }

    @Override
    public PageContent fetchPageContent(String url) {

        LOG.debug("Fetching {}", url);
        final Connection conn = Jsoup.connect(url).timeout(TIME_OUT).userAgent(USER_AGENT);

        try {
            final Document doc = conn.get();
            final String title = doc.title();
            final List<String> linksToVisit = new ArrayList<>();

            doc.select("tr.pl-video > td:nth-child(4) > a:nth-child(1)")
               .forEach(link -> linksToVisit.add(link.absUrl("href")));


            if(linksToVisit.isEmpty()){
                Pattern pattern = Pattern.compile("youtube.com/watch?.*v=([^&]*)");
                Matcher matcher = pattern.matcher(url.toString());
                if (matcher.find()){
                    final Optional<String> optionalId = Optional.of(matcher.group(1));
                    if(optionalId.isPresent()){
                        final String id = optionalId.get();
                        System.out.println(id);
                    }
                }
            }


            return new PageContentImpl(url, linksToVisit, title, "");

        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
