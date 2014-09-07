package com.mxmind.scraper.api;

/**
 * The WebScraper solution
 *
 * @author mxmind
 * @version 1.0-SNAPSHOT
 * @since 1.0-SNAPSHOT
 */
public interface PageParser {

    PageContent fetchPageContent(String url);
}
