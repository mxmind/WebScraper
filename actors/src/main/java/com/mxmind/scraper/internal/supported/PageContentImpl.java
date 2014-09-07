package com.mxmind.scraper.internal.supported;

import com.mxmind.scraper.api.PageContent;

import java.util.List;

/**
 * The WebScraper solution.
 *
 * @author mxmind
 * @version 1.0-SNAPSHOT
 * @since 1.0-SNAPSHOT
 */
public class PageContentImpl implements PageContent {

    private final List<String> videoLinks;

    private final String title;

    private final String content;

    private final String path;

    public PageContentImpl(String path, List<String> videoLinks, String title, String content) {
        this.path = path;
        this.videoLinks = videoLinks;
        this.title = title;
        this.content = content;
    }

    @Override
    public String getContent() {
        return content;
    }

    @Override
    public List<String> getVideoLinks() {
        return videoLinks;
    }

    @Override
    public List<String> getPaginationLinks() {
        return null;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "PageContent{title=" + title + ", content=" + content + ", videoLinks=" + videoLinks + "}";
    }
}
