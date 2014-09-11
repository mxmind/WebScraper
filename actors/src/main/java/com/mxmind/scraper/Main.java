package com.mxmind.scraper;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.mxmind.scraper.api.Process;
import com.mxmind.scraper.internal.ProcessExecutor;
import com.mxmind.scraper.internal.Supervisor;
import com.mxmind.scraper.internal.supported.IndexerImpl;
import com.mxmind.scraper.internal.supported.YoutubePageParserImpl;
import com.mxmind.scraper.internal.utils.Props;
import org.apache.lucene.index.IndexWriter;

/**
 * The WebScraper solution.
 *
 * @author mxmind
 * @version 1.0-SNAPSHOT
 * @since 1.0-SNAPSHOT
 */
public class Main implements Process {

    public static final String ACTOR_SYSTEM = "web-scraper-actor-system";

    public static final Props props = new Props(Main.class.getResource("/scraper.properties"));

    @Override
    public void proc(String path, IndexWriter writer) {
        final ActorSystem actorSystem = ActorSystem.create(ACTOR_SYSTEM);
        final ActorRef supervisor = actorSystem.actorOf(
            akka.actor.Props.create(Supervisor.class, new IndexerImpl(writer), new YoutubePageParserImpl(path))
        );

        supervisor.tell(path, actorSystem.guardian());
        actorSystem.awaitTermination();
    }

    public static void main(String... args) throws Exception {
        final String playlist = Main.props.getString("scraper.playlist.url").toLowerCase();
        if(!playlist.contains("youtube") || !playlist.contains("playlist")){
            System.err.println("Scraper cannot recognize playlist url");
            System.exit(0);
        }

        new ProcessExecutor(new Main()).exec(Main.props.getString("scraper.playlist.url"));
    }
}
