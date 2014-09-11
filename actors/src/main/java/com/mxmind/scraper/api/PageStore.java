package com.mxmind.scraper.api;

import java.util.Collection;

/**
 * The WebScraper solution.
 *
 * @author mxmind
 * @version 1.0-SNAPSHOT
 * @since 1.0-SNAPSHOT
 */
public interface PageStore {

    public void add(String page);

    public void addAll(Collection<String> pages);

    public String next();

    public Collection<String> nextBatch();

    int size();

    public void finished(String page);

    public boolean isFinished();
}
