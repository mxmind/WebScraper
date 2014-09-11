package com.mxmind.scraper.internal;

import akka.actor.*;
import akka.routing.RoundRobinPool;
import com.mxmind.scraper.Main;
import com.mxmind.scraper.api.*;
import com.mxmind.scraper.api.messages.IndexedMessage;
import com.mxmind.scraper.api.messages.IndexingMessage;
import com.mxmind.scraper.internal.actors.IndexerActor;
import com.mxmind.scraper.internal.actors.PageParserActor;
import com.mxmind.scraper.internal.stores.VisitedPageStore;

import static java.lang.System.out;

/**
 * The WebScraper solution.
 *
 * @author mxmind
 * @version 1.0-SNAPSHOT
 * @since 1.0-SNAPSHOT
 */
@SuppressWarnings({"deprecation"})
public final class Supervisor extends UntypedActor {

    public static final String WORKER_DISPATCHER = "worker-dispatcher";

    private static final int POOL_SIZE = Main.props.getInt("scraper.round.robin.pool.size", 10);

    private final VisitedPageStore visitedPageStore = new VisitedPageStore();

    private final ActorRef indexerActor, parserActor;

    public Supervisor(Indexer indexer, PageParser parser) {
        final UntypedActorContext ctx = getContext();

        parserActor = ctx.actorOf(Props.create(PageParserActor.class, parser)
            .withRouter(new RoundRobinPool(POOL_SIZE))
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
                out.format("Scraper indexing %s \r", visitedPageStore);
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
            out.format("Scraper indexing %s \r", visitedPageStore);
            if (visitedPageStore.isFinished()) {
                commit();
            }
        } else if (message == IndexingMessage.COMMITTED_MESSAGE) {
            getContext().system().shutdown();
            out.format("Scraper visited %d links\n", visitedPageStore.size());
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