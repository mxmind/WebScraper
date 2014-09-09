package com.mxmind.scraper.internal.supported;

import com.gargoylesoftware.htmlunit.*;
import com.mxmind.scraper.api.Indexer;
import com.mxmind.scraper.api.PageContent;
import com.mxmind.scraper.internal.supported.utube.VideoInfo;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.lucene.document.*;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The WebScraper solution.
 *
 * @author mxmind
 * @version 1.0-SNAPSHOT
 * @since 1.0-SNAPSHOT
 */
public final class IndexerImpl implements Indexer {

    public static final String ENCODING = StandardCharsets.UTF_8.name();

    private final IndexWriter writer;

    public IndexerImpl(IndexWriter indexWriter) {
        this.writer = indexWriter;
    }

    @Override
    public void index(PageContent pageContent) {
        try {
            writer.addDocument(toDocument(pageContent));

            final String path = pageContent.getPath();
            if(path.contains("youtube.com") && path.contains("watch")){
                MultiMap videos = extractEmbedded(path);
                System.out.format("%s -> %d\n", pageContent.getTitle(), videos.size());
            } else {
                System.out.format("%s -> %s\n", pageContent.getTitle(), "NONE");
            }
        } catch (Exception ex) {
            //ex.printStackTrace();
        }
    }

    public void extract(final VideoInfo info, final AtomicBoolean stop, final Runnable notify) {
        try {
            try {
                //  return extractEmbedded(null, info, stop, notify);
            } catch (EmbeddingDisabled ex) {
                // return streamCpature(info, stop, notify);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public String extractId(String url) {
        watch: {
            final Pattern pattern = Pattern.compile("youtube.com/watch?.*v=([^&]*)");
            final Matcher matcher = pattern.matcher(url);
            if (matcher.find())
                return matcher.group(1);
        }
        v: {
            final Pattern pattern = Pattern.compile("youtube.com/v/([^&]*)");
            final Matcher matcher = pattern.matcher(url);
            if (matcher.find())
                return matcher.group(1);
        }
        return null;
    }

    MultiMap extractEmbedded(String path) throws Exception {
        final String id = extractId(path);
        if (id == null) {
            throw new MalformedURLException("Unknown id to build download link");
        }
        final String url = String.format("http://www.youtube.com/get_video_info?authuser=0&video_id=%s&el=embedded", id);

        final WebClient webClient = WebClientSingleton.getInstance();
        WebRequest request = new WebRequest(new URL(url));

        final Page page = webClient.getPage(request);

        if(page instanceof UnexpectedPage && page.getWebResponse().getStatusCode() == 200){
            final InputStream inputStream = ((UnexpectedPage) page).getInputStream();
            final StringWriter writer = new StringWriter();
            IOUtils.copy(inputStream, writer, ENCODING);
            String query = writer.toString();

            Map<String, String> pairs = getQueryMap(query);
            if(!pairs.get("status").equalsIgnoreCase("OK")){
                throw new EmbeddingDisabled(id);
            }

            final String fmtStream = URLDecoder.decode(pairs.get("url_encoded_fmt_stream_map"), ENCODING);

            return getEncodedVideos(fmtStream);
        }
        return MultiValueMap.decorate(Collections.emptyMap());
    }

    private Map<String, String> getQueryMap(String query) {
        try {
            query = query.trim();
            final Map<String, String> map = new HashMap<>();
            final URI body = new URI(null, null, null, -1, null, query, null);

            URLEncodedUtils.parse(body, ENCODING).stream().forEach(pair -> map.put(pair.getName(), pair.getValue()));
            return map;
        } catch (URISyntaxException ex) {
            throw new RuntimeException(query, ex);
        }
    }

    private MultiMap getEncodedVideos(String stream) throws Exception {
        final String[] links = stream.split("url=");
        final MultiMap videos = new MultiValueMap();
        // 0) split on raw video links;
        for (String link : links) {
            link = StringEscapeUtils.unescapeJava(link);
            final String decodedLink = URLDecoder.decode(link, ENCODING);
            // n) start;
            {
                // n.0) obtain url;
                String url = null;
                url: {
                    final Pattern pattern = Pattern.compile("([^&,]*)[&,]");
                    final Matcher matcher = pattern.matcher(link);
                    if (matcher.find()) {
                        url = matcher.group(1);
                        url = URLDecoder.decode(url, ENCODING);
                    }
                }
                // n.1) obtain quality tag;
                String itag = null;
                itag: {
                    final Pattern pattern = Pattern.compile("itag=(\\d+)");
                    final Matcher matcher = pattern.matcher(decodedLink);
                    if (matcher.find()) {
                        itag = matcher.group(1);
                    }
                }

                // n.2) obtain signature;
                String signature = null;
                signature: {
                    Pattern pattern;
                    Matcher matcher;
                    if (signature == null) {
                        pattern = Pattern.compile("&signature=([^&,]*)");
                        matcher = pattern.matcher(decodedLink);
                        if (matcher.find()) {
                            signature = matcher.group(1);
                        }
                    }
                    if (signature == null) {
                        pattern = Pattern.compile("sig=([^&,]*)");
                        matcher = pattern.matcher(decodedLink);
                        if (matcher.find()) {
                            signature = matcher.group(1);
                        }
                    }
                    if (signature == null) {
                        pattern = Pattern.compile("[&,]s=([^&,]*)");
                        matcher = pattern.matcher(decodedLink);
                        if (matcher.find()) {
                            signature = matcher.group(1);
                        }
                    }
                }
                // n.3) build quality map;
                if (url != null && itag != null && signature != null) {
                    try {
                        url += "&signature=" + signature;
                        final VideoInfo.VideoQuality quality = VideoInfo.itagMap.get(Integer.decode(itag));
                        videos.put(quality, new URL(url));
                        continue;
                    } catch (MalformedURLException ignored) {
                        // e) should never happen, we use scraped urls
                    }
                }
                // n) end;
            }
        }
        return videos;
    }



    private Document toDocument(PageContent content) {
        final Document doc = new Document();
        doc.add(new StringField("id", content.getPath(), Field.Store.YES));
        doc.add(new StringField("title", content.getTitle(), Field.Store.YES));
        doc.add(new StringField("content", content.getContent(), Field.Store.NO));
        return doc;
    }

    public static class EmbeddingDisabled extends RuntimeException {
        private final static String MESSAGE_PATTERN = "The video with ID: %s has disabled embedding";

        public EmbeddingDisabled(String id) {
            super(String.format(MESSAGE_PATTERN, id));
        }
    }

    // -- old -- //

    @Override
    public void commit() {
        try {
            writer.commit();
        } catch (CorruptIndexException ex) {
            throw new IllegalStateException(ex);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public void close() {
        try {
            writer.close(true);
        } catch (CorruptIndexException ex) {
            throw new IllegalStateException(ex);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }

    }

}
