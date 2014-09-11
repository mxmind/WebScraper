package com.mxmind.scraper.internal;

import com.mxmind.scraper.Main;
import com.mxmind.scraper.api.Executor;
import com.mxmind.scraper.api.Process;
import com.mxmind.scraper.internal.supported.youtube.VideoDownloder;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static java.lang.System.out;

/**
 * The WebScraper solution.
 *
 * @author mxmind
 * @version 1.0-SNAPSHOT
 * @since 1.0-SNAPSHOT
 */
public class ProcessExecutor implements Executor {

    private static final Logger LOG = LoggerFactory.getLogger(Executor.class);

    private static final String TMP_DIR = System.getProperty("java.io.tmpdir");

    private static final int POOL_SIZE = Main.props.getInt("scraper.fixed.thread.pool.size", 10);

    private static final String DOWNLOAD_DIR = Main.props.getString("scraper.download.dir", System.getProperty("user.dir"));

    private static final String WEB_SCRAPER_INDEX = "web-scraper-index";

    private static final int CONNS_PER_DOWNLOAD = 8;

    private final boolean sublisted = Main.props.getBoolean("scraper.video.sublisting", false);

    private final int start = Main.props.getInt("scraper.video.sublisting.start", 0);

    private final int limit = Main.props.getInt("scraper.video.sublisting.limit", 100);

    private final Process process;

    public ProcessExecutor(Process currentProcess) {
        this.process = currentProcess;
    }

    @Override
    public void exec(String path) {
        ExecutorService executor = null;

        final int second = 1000;
        final File indexDir = Paths.get(TMP_DIR, WEB_SCRAPER_INDEX).toFile();
        final File downloadDir = Paths.get(DOWNLOAD_DIR).toFile();

        try (
            final IndexWriter writer = openWriter(indexDir)
        ) {
            final StopWatch stopWatch = new StopWatch();
            // 0) begin indexing;
            process.proc(path, writer);
            final IndexSearcher searcher = openSearcher(indexDir);
            final TopDocs result = searcher.search(new MatchAllDocsQuery(), Integer.MAX_VALUE - 1);

            // nothing needs from akka, just to build ordinal thread pool;
            // each task creates new nested threads accordingly to file parts amount;
            final long endScraping = stopWatch.getElapsedTime();
            out.format(
                "Scraper found %d videos with quality: %s, by time %.1fs \n\n",
                result.totalHits,
                Main.props.getString("scraper.video.quality", "p144"),
                Math.floor(endScraping / second)
            );

            // 1) create download tasks;
            List<Callable<Boolean>> tasks = getDownloadTasks(downloadDir, searcher, result);
            if(sublisted){
                int len = tasks.size() == 0 ? 0 : tasks.size() - 1;
                int begin = start > len ? len : Math.max(start, 0);
                int end = Math.min(tasks.size(), (begin + limit));
                tasks = tasks.subList(begin, end < start ? start : end);
            }

            // 2) execute these tasks;
            executor = Executors.newFixedThreadPool(POOL_SIZE);
            final List<Future<Boolean>> futures = executor.invokeAll(tasks);
            final long count = futures.stream().map(future -> {
                try {
                    return future.get();
                } catch (InterruptedException | ExecutionException ex) {
                    LOG.error(ex.getMessage(), ex);
                }
                return null;
            }).filter(input -> !!input).count();

            // 3) end downlouding;
            stopWatch.stop();
            out.format(
                "\nScraper downloaded %d videos, by time %.1fs \n",
                count,
                Math.floor((stopWatch.getElapsedTime() - endScraping) / second)
            );

        } catch (IOException | InterruptedException | RuntimeException ex) {
            LOG.error(ex.getMessage(), ex);
        } finally {
            // 4) shutdown system;
            if (executor != null) {
                executor.shutdown();
            }
        }
    }

    private IndexWriter openWriter(File indexDir) throws IOException {
        final Version matchVersion = Version.LUCENE_40;
        final Directory dir = FSDirectory.open(indexDir);
        final IndexWriterConfig config = new IndexWriterConfig(matchVersion, new StandardAnalyzer(matchVersion));

        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        return new IndexWriter(dir, config);
    }

    private IndexSearcher openSearcher(File indexDir) throws IOException {
        final Directory dir = FSDirectory.open(indexDir);
        final IndexReader reader = DirectoryReader.open(dir);

        return new IndexSearcher(reader);
    }

    private List<Callable<Boolean>> getDownloadTasks(File dir, IndexSearcher searcher, TopDocs result) throws IOException {
        final List<Callable<Boolean>> tasks = new ArrayList<>(result.totalHits);

        for (ScoreDoc scoreDoc : result.scoreDocs) {
            final Document doc = searcher.doc(scoreDoc.doc);

            final URL url = new URL(doc.get("link"));
            final File outputFile = new File(dir, doc.get("filename"));
            tasks.add(new VideoDownloder(url, outputFile, CONNS_PER_DOWNLOAD));
        }
        return tasks;
    }
}
