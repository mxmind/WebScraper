package com.mxmind.scraper.api;

import java.util.List;

/**
 * The WebScraper solution.
 *
 * @author mxmind
 * @version 1.0-SNAPSHOT
 * @since 1.0-SNAPSHOT
 */
public interface PageContent {

    public String getContent();

    public List<String> getVideoLinks();

    public List<String> getPaginationLinks();

    public String getTitle();

    public String getPath();

}
