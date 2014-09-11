package com.mxmind.scraper.api.youtube;

import java.util.concurrent.Callable;

/**
 * The WebScraper solution.
 *
 * @author mxmind
 * @version 1.0-SNAPSHOT
 * @since 1.0-SNAPSHOT
 */
public interface Downloader extends Callable<Boolean> {

    @Override
    Boolean call();

    float getProgress();

    int getDownloadedSize();

    int getFileSize();

    DownloadingState getCurrentState();

    void setCurrentState(DownloadingState newState);

    public static enum DownloadingState {
        DOWNLOADING, COMPLETE, ERROR
    }
}
