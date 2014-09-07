package com.mxmind.scraper.internal;

import com.mxmind.scraper.api.Executor;
import com.mxmind.scraper.api.Process;
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
import java.nio.file.Paths;
import java.util.Arrays;

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

        try (final IndexWriter writer = openWriter(indexDir)){

            final StopWatch stopWatch = new LoggingStopWatch();

            process.proc(path, writer);
            stopWatch.stop(process.getClass().getSimpleName());

            final IndexSearcher searcher = openSearcher(indexDir);
            final TopDocs result = searcher.search(new MatchAllDocsQuery(), 100);

            LOG.info("Found {} results", result.totalHits);

            Arrays.asList(result.scoreDocs).forEach( scoreDoc -> {
                try {
                    final Document doc = searcher.doc(scoreDoc.doc);
                    LOG.debug(doc.get("id"));

                } catch (IOException ex ) {
                    LOG.error(ex.getMessage(), ex);
                }
            });

        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
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
}
