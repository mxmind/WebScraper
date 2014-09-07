package com.mxmind.scraper.api;

import java.io.Closeable;

/**
 * The WebScraper solution.
 *
 * @author mxmind
 * @version 1.0-SNAPSHOT
 * @since 1.0-SNAPSHOT
 */
public interface Indexer extends Closeable {

    void commit();

    void index(PageContent pageContent);

    @Override
    void close();

}
