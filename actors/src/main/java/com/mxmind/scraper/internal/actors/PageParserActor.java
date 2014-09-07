package com.mxmind.scraper.internal.actors;

import akka.actor.UntypedActor;
import com.mxmind.scraper.api.PageContent;
import com.mxmind.scraper.api.PageParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Option;

/**
 * The WebScraper solution
 *
 * @author mxmind
 * @version 1.0-SNAPSHOT
 * @since 1.0-SNAPSHOT
 */
public class PageParserActor extends UntypedActor {

    private static final Logger LOG = LoggerFactory.getLogger(PageParserActor.class);

    private final PageParser parser;

    public PageParserActor(PageParser parser) {
        this.parser = parser;
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof String) {
            final String url = (String) message;
            final PageContent content = parser.fetchPageContent(url);

            getSender().tell(content, getSelf());
        } else {
            unhandled(message);
        }
    }

    @Override
    public void preRestart(Throwable reason, Option<Object> message) throws Exception {
        LOG.info("Restarting PageParserActor because of {}", reason.getClass());
        super.preRestart(reason, message);
    }
}
