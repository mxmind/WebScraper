package com.mxmind.scraper.internal.supported.youtube;

import com.mxmind.scraper.Main;
import com.mxmind.scraper.api.youtube.Downloader;
import org.apache.http.HttpStatus;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import static java.lang.System.out;

/**
 * @author mxmind
 * @version 1.0.0
 * @since 1.0.0
 */
public final class VideoDownloder extends Observable implements Downloader {

    private static final int BLOCK_SIZE = 4096;

    private static final int BUFFER_SIZE = 4096;

    private static final int MIN_DOWNLOAD_SIZE = BLOCK_SIZE * 100;

    private static final int CONN_TIMEOUT = Main.props.getInt("scraper.video.timeout", 10000);

    private final URL url;

    private final File outputFile;

    private final int connections;

    private DownloadingState currentState;

    private int fileSize;

    private int downloadedSize;

    private List<DownloadThread> threads;

    public VideoDownloder(URL url, File outputFile, int connections) {
        this.url = url;
        this.outputFile = outputFile;
        this.connections = connections;

        currentState = DownloadingState.DOWNLOADING;
        fileSize = -1;
        downloadedSize = 0;
        threads = new ArrayList<>();
    }

    @Override
    public Boolean call() {
        HttpURLConnection conn = null;
        try {
            // 0) open connection to url;
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(CONN_TIMEOUT);

            // 1) connect to server;
            conn.connect();

            // 2) make sure the response code is in the 200 range.
            if (conn.getResponseCode() != HttpStatus.SC_OK) {
                return error();
            }

            // 3) heck for valid content length.
            int contentLength = conn.getContentLength();
            if (contentLength < 1) {
                return error();
            }

            if (fileSize == -1) {
                fileSize = contentLength;
                stateChanged();
            }

            // 4) if the state is DOWNLOADING (no error) -> start downloading
            if (getCurrentState().equals(DownloadingState.DOWNLOADING)) {

                // 4.0) check whether we have list of download threads or not, if not -> init download
                if (threads.size() == 0) {
                    if (fileSize > MIN_DOWNLOAD_SIZE) {
                        // 4.0.0) downloading size for each thread
                        int partSize = Math.round(((float) fileSize / connections) / BLOCK_SIZE) * BLOCK_SIZE;
                        out.format("- %s (%,8d bytes) \n", outputFile.getName(), partSize);

                        // 4.0.1) start and end for each thread
                        int startByte = 0, endByte = partSize - 1, i = 2;
                        DownloadThread thread = new DownloadThread(1, url, outputFile, startByte, endByte);
                        threads.add(thread);

                        while (endByte < fileSize) {
                            startByte = endByte + 1;
                            endByte += partSize;
                            thread = new DownloadThread(i, url, outputFile, startByte, endByte);
                            threads.add(thread);
                            ++i;
                        }
                    } else {
                        DownloadThread thread = new DownloadThread(1, url, outputFile, 0, fileSize);
                        threads.add(thread);
                    }
                } else {
                    // 4.0.2) resume all downloading threads
                    threads.stream().filter(thread -> !thread.isFinished()).forEach(DownloadThread::download);
                }

                // 4.1) waiting for all threads to complete
                for (DownloadThread thread : threads) {
                    thread.waitFinish();
                }

                // 4.2) check the current state again
                if (getCurrentState().equals(DownloadingState.DOWNLOADING)) {
                    setCurrentState(DownloadingState.COMPLETE);
                }
            }
        } catch (Throwable ex) {
            return error();
        } finally {
            if (conn != null) {
                conn.disconnect();

            }
        }
        return true;
    }

    @Override
    @SuppressWarnings("unused")
    public float getProgress() {
        return ((float) getDownloadedSize() / getFileSize()) * 100;
    }

    @Override
    public int getDownloadedSize() {
        return downloadedSize;
    }

    @Override
    public int getFileSize() {
        return fileSize;
    }

    protected synchronized void downloaded(int value) {
        downloadedSize += value;
        stateChanged();
    }

    private void stateChanged() {
        setChanged();
        notifyObservers();
    }

    @Override
    public DownloadingState getCurrentState() {
        return currentState;
    }

    @Override
    public void setCurrentState(DownloadingState newState) {
        if (!newState.equals(currentState)) {
            currentState = newState;
            stateChanged();
        }
    }

    // states;
    private boolean error() {
        setCurrentState(DownloadingState.ERROR);
        return false;
    }

    /**
     * Thread to download part of a file
     */
    private final class DownloadThread implements Runnable {

        protected int threadId;

        protected URL url;

        protected File outputFile;

        protected int start;

        protected int end;

        protected boolean finished;

        protected Thread thread;

        public DownloadThread(int threadId, URL url, File outputFile, int start, int end) {
            this.threadId = threadId;
            this.url = url;
            this.outputFile = outputFile;
            this.start = start;
            this.end = end;
            finished = false;

            download();
        }

        /**
         * Start or resume the download
         */
        public void download() {
            thread = new Thread(this);
            thread.start();
        }

        @Override
        public void run() {

            BufferedInputStream in = null;
            RandomAccessFile raf = null;

            try {
                // 0) open connection to url
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                // 1) set the range of byte to download and connect to server
                conn.setRequestProperty("Range", String.format("bytes=%d-%d", start, end));
                conn.connect();

                // 2) get the input stream
                in = new BufferedInputStream(conn.getInputStream());

                // 3) open the output file and seek to the start location
                raf = new RandomAccessFile(outputFile, "rw");
                raf.seek(start);

                // 4) write data;
                final byte data[] = new byte[BUFFER_SIZE];
                int inRead;
                while (getCurrentState().equals(DownloadingState.DOWNLOADING) && ((inRead = in.read(data, 0, BUFFER_SIZE)) != -1)) {

                    // 4.0) write to buffer
                    raf.write(data, 0, inRead);

                    // 4.1) increase the start for resume later
                    start += inRead;

                    // 4.2) increase the downloaded size
                    downloaded(inRead);
                }

                if (getCurrentState().equals(DownloadingState.DOWNLOADING)) {
                    finished = true;
                }
            } catch (IOException e) {
                error();
            } finally {
                if (raf != null) {
                    try {
                        raf.close();
                    } catch (IOException ignored) {
                    }
                }

                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }

        public boolean isFinished() {
            return finished;
        }

        public void waitFinish() throws InterruptedException {
            thread.join();
        }
    }
}
