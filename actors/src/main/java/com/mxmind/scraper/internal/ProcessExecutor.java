package com.mxmind.scraper.internal;

import com.mxmind.scraper.api.Executor;
import com.mxmind.scraper.api.Process;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.perf4j.LoggingStopWatch;
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

/**
 * The WebScraper solution.
 *
 * @author mxmind
 * @version 1.0-SNAPSHOT
 * @since 1.0-SNAPSHOT
 */
public class ProcessExecutor implements Executor {

    private static final Logger LOG = LoggerFactory.getLogger(Executor.class);

    public static final String TMP_DIR = "java.io.tmpdir";

    public static final String WEB_SCRAPER_INDEX = "web-scraper-index";

    private final Process process;

    public ProcessExecutor(Process currentProcess) {
        this.process = currentProcess;
    }

    @Override
    public void exec(String path) {
        final File indexDir = Paths.get(System.getProperty(TMP_DIR), WEB_SCRAPER_INDEX).toFile();
        final File downloadDir = Paths.get("/Users/mxmind/Downloads/scraper").toFile();
        try (
            final IndexWriter writer = openWriter(indexDir)
        ) {

            final StopWatch stopWatch = new LoggingStopWatch();

            process.proc(path, writer);
            stopWatch.stop(process.getClass().getSimpleName());

            final IndexSearcher searcher = openSearcher(indexDir);
            final TopDocs result = searcher.search(new MatchAllDocsQuery(), Integer.MAX_VALUE);

            // nothing needs from akka, just to build ordinal thread pool;
            // but if we need to convert files we should to add 2 new actors,
            // for download and covert tasks accoringly.
            LOG.info("Found {} videos", result.totalHits);

            final List<Callable<String>> tasks = getDownloadTasks(downloadDir, searcher, result).subList(20, 24);

            final ExecutorService executor = Executors.newFixedThreadPool(10);
            final List<Future<String>> futures = executor.invokeAll(tasks);

            long count = futures.stream().map(future -> {
                try {
                    return future.get();
                } catch (InterruptedException | ExecutionException ex) {
                    LOG.error(ex.getMessage(), ex);
                }
                return null;
            }).filter(input -> input != null).count();

            LOG.info("Downloded {} videos", count);

        } catch (IOException | InterruptedException | RuntimeException ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

    private List<Callable<String>> getDownloadTasks(File dir, IndexSearcher searcher, TopDocs result) throws IOException {
        final List<Callable<String>> tasks = new ArrayList<>(result.totalHits);

        for (ScoreDoc scoreDoc : result.scoreDocs) {
            final Document doc = searcher.doc(scoreDoc.doc);

            final URL url = new URL(doc.get("link"));

            // add new Download Job;
            tasks.add(() -> {
                String success;
                try {
                    File destination = new File(dir.toString(), doc.get("filename"));
                    FileUtils.copyURLToFile(url, destination);

                    success = destination.toString();
                } catch (IOException ex) {
                    success = null;
                }
                return success;
            });
        }
        return tasks;
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
}
