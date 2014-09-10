package com.mxmind.scraper.internal;

import akka.actor.*;
import akka.routing.RoundRobinPool;
import com.mxmind.scraper.api.*;
import com.mxmind.scraper.api.messages.IndexedMessage;
import com.mxmind.scraper.api.messages.IndexingMessage;
import com.mxmind.scraper.internal.actors.IndexerActor;
import com.mxmind.scraper.internal.actors.PageParserActor;
import com.mxmind.scraper.internal.stores.VisitedPageStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The WebScraper solution.
 *
 * @author mxmind
 * @version 1.0-SNAPSHOT
 * @since 1.0-SNAPSHOT
 */
@SuppressWarnings({"deprecation"})
public final class Supervisor extends UntypedActor {

    private static final Logger LOG = LoggerFactory.getLogger(Supervisor.class);

    private final VisitedPageStore visitedPageStore = new VisitedPageStore();

    public static final String WORKER_DISPATCHER = "worker-dispatcher";

    private final ActorRef indexerActor, parserActor;

    public Supervisor(Indexer indexer, PageParser parser) {
        final UntypedActorContext ctx = getContext();

        parserActor = ctx.actorOf(Props.create(PageParserActor.class, parser)
            .withRouter(new RoundRobinPool(10))
            .withDispatcher(WORKER_DISPATCHER));

        indexerActor = ctx.actorOf(Props.create(IndexerActor.class, indexer));
    }

    @Override
    public void onReceive(Object message) throws Exception {

        if (message instanceof String) {
            final String start = (String) message;
            visitedPageStore.add(start);
            getParserActor().tell(visitedPageStore.next(), getSelf());
        } else if (message instanceof PageContent) {
            final PageContent content = (PageContent) message;


            visitedPageStore.addAll(content.getVideoLinks());
            getIndexerActor().tell(content, getSelf());
            if (visitedPageStore.isFinished()) {
                LOG.info(visitedPageStore.toString());
                commit();
            } else {
                visitedPageStore
                    .nextBatch()
                    .parallelStream()
                    .forEach(page -> getParserActor().tell(page, getSelf()));
            }
        } else if (message instanceof IndexedMessage) {
            IndexedMessage indexedMessage = (IndexedMessage) message;
             visitedPageStore.finished(indexedMessage.path);
            LOG.info(visitedPageStore.toString());
            if (visitedPageStore.isFinished()) {
                commit();
            }
        } else if (message == IndexingMessage.COMMITTED_MESSAGE) {
            getContext().system().shutdown();
        }
    }

    private ActorRef getParserActor() {
        return parserActor;
    }

    private ActorRef getIndexerActor() {
        return indexerActor;
    }

    private void commit() {
        getIndexerActor().tell(IndexingMessage.COMMIT_MESSAGE, getSelf());
    }
}