package com.mxmind.scraper.api;

import org.apache.lucene.index.IndexWriter;

/**
 * The WebScraper solution.
 *
 * @author mxmind
 * @version 1.0-SNAPSHOT
 * @since 1.0-SNAPSHOT
 */
public interface Process {

    public void proc(String path, IndexWriter writer);
}
