package com.mxmind.scraper.internal.actors;

import akka.actor.UntypedActor;
import com.mxmind.scraper.api.Indexer;
import com.mxmind.scraper.api.PageContent;
import com.mxmind.scraper.api.messages.IndexedMessage;
import com.mxmind.scraper.api.messages.IndexingMessage;

/**
 * The WebScraper solution
 *
 * @author mxmind
 * @version 1.0-SNAPSHOT
 * @since 1.0-SNAPSHOT
 */
public final class IndexerActor extends UntypedActor {

    private final Indexer indexer;

    public IndexerActor(Indexer indexer) {
        this.indexer = indexer;
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof PageContent) {
            final PageContent content = (PageContent) message;
            indexer.index(content);
            getSender().tell(new IndexedMessage(content.getPath()), getSelf());
        } else if (message.equals(IndexingMessage.COMMIT_MESSAGE)) {

            indexer.commit();
            getSender().tell(IndexingMessage.COMMITTED_MESSAGE, getSelf());
        } else {
            unhandled(message);
        }
    }
}