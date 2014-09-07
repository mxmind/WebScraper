package com.mxmind.scraper.internal.stores;

import com.mxmind.scraper.api.PageStore;

import java.util.*;

/**
 * The WebScraper solution.
 *
 * @author mxmind
 * @version 1.0-SNAPSHOT
 * @since 1.0-SNAPSHOT
 */
public final class VisitedPageStore implements PageStore {

    private Set<String> pagesToVisit = new HashSet<>();
    private Set<String> pages = new HashSet<>();
    private Set<String> inProgress = new HashSet<>();

    @Override
    public void add(String page) {
        if (!pages.contains(page)) {
            pagesToVisit.add(page);
            pages.add(page);
        }
    }

    @Override
    public void addAll(Collection<String> pages) {
        pages.forEach(this::add);
    }

    @Override
    public String next() {
        if (pagesToVisit.isEmpty()) {
            return null;
        }

        final String next = pagesToVisit.iterator().next();
        pagesToVisit.remove(next);
        inProgress.add(next);

        return next;
    }

    @Override
    public Collection<String> nextBatch() {
        final Set<String> pages = new HashSet<>();
        pages.addAll(pagesToVisit);
        pagesToVisit.clear();
        inProgress.addAll(pages);

        return pages;
    }

    @Override
    public void finished(String page) {
        inProgress.remove(page);
    }

    @Override
    public boolean isFinished() {
        return pagesToVisit.isEmpty() && inProgress.isEmpty();
    }

    @Override
    public String toString() {
        return String.format("inProgress: %1$3s, pages: %2$3s", inProgress.size(), pages.size());
    }

}
