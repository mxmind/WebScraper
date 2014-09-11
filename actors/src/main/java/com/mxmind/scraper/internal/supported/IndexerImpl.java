package com.mxmind.scraper.internal.supported;

import com.gargoylesoftware.htmlunit.*;
import com.mxmind.scraper.api.Indexer;
import com.mxmind.scraper.api.PageContent;
import com.mxmind.scraper.internal.supported.youtube.VideoInfo;
import com.mxmind.scraper.internal.supported.youtube.VideoQualityCodePage;
import org.apache.commons.collections4.MultiMap;
import org.apache.commons.collections4.map.MultiValueMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
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

    private static final String ENCODING = StandardCharsets.UTF_8.name();

    private static final String GET_INFO_URL = "http://www.youtube.com/get_video_info?authuser=0&video_id=%s&el=embedded";

    public static final String DEF_FILE_EXT = "flv";

    private final IndexWriter writer;

    public IndexerImpl(IndexWriter indexWriter) {
        this.writer = indexWriter;
    }

    @Override
    public void commit() {
        try {
            writer.commit();
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public void index(PageContent content) {
        try {
            Optional<Document> doc = toDocument(content);
            if (doc.isPresent()) {
                writer.addDocument(doc.get());
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void close() {
        try {
            writer.close(true);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private Optional<Document> toDocument(PageContent content) throws Exception {
        final VideoInfo info = extract(content);
        if (info.getVideoLink().isPresent()) {
            String[] linkTokens = info.getVideoLink().get().split("&scraper-ext=");
            final Document doc = new Document();
            doc.add(new StringField("id", content.getPath(), Field.Store.YES));
            doc.add(new StringField("filename", info.getFilename(), Field.Store.YES));
            doc.add(new StringField("link", linkTokens[0], Field.Store.YES));
            doc.add(new StringField("ext", linkTokens[1], Field.Store.YES));
            return Optional.of(doc);
        }
        return Optional.empty();
    }

    public VideoInfo extract(PageContent content) throws Exception {
        VideoInfo info = new VideoInfo();
        if (isVideoLink(content.getPath())) {
            info = extractEmbedded(content);
        }
        return info;
    }

    private boolean isVideoLink(String path) {
        return path.contains("youtube.com") && path.contains("watch");
    }

    VideoInfo extractEmbedded(PageContent content) throws Exception {
        // 0) extract video file id;
        final String path = content.getPath();
        final String id = extractId(path);
        if (id == null) {
            throw new MalformedURLException("Unknown id to build download link");
        }

        // 1) get video info;
        final String url = String.format(GET_INFO_URL, id);
        final WebClient webClient = WebClientStaticProvider.getBaseInstance();
        final WebRequest request = new WebRequest(new URL(url));
        final Page page = webClient.getPage(request);

        // 2) prepare default values;
        String title = null;
        MultiMap<VideoQualityCodePage.Code, String> videos = MultiValueMap.multiValueMap(Collections.emptyMap());

        // 3) check page status;
        final int status = page.getWebResponse().getStatusCode();
        if (status >= 200 && status < 300) {

            // 3.0) read info;
            String info;
            try (
                final InputStream inputStream = ((UnexpectedPage) page).getInputStream();
                final StringWriter writer = new StringWriter()
            ) {
                IOUtils.copy(inputStream, writer, ENCODING);
                info = writer.toString();
            }
            Map<String, String> pairs = getQueryMap(info);

            // 3.1) is video embedded?
            if (!pairs.get("status").equalsIgnoreCase("OK")) {
                throw new EmbeddingDisabled(id);
            }

            // 3.2) write info to immutable object;
            final String fmtStream = URLDecoder.decode(pairs.get("url_encoded_fmt_stream_map"), ENCODING);
            title = pairs.get("title");
            videos = getEncodedVideos(fmtStream);
        }

        // 4) close page and return info;
        page.cleanUp();
        return new VideoInfo(title, videos);
    }

    public String extractId(String url) {
        {
            final Pattern pattern = Pattern.compile("youtube.com/watch?.*v=([^&]*)");
            final Matcher matcher = pattern.matcher(url);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        {
            final Pattern pattern = Pattern.compile("youtube.com/v/([^&]*)");
            final Matcher matcher = pattern.matcher(url);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    private Map<String, String> getQueryMap(String info) {
        try {
            info = info.trim();
            final Map<String, String> map = new HashMap<>();
            final URI body = new URI(null, null, null, -1, null, info, null);

            URLEncodedUtils.parse(body, ENCODING).stream().forEach(pair -> map.put(pair.getName(), pair.getValue()));
            return map;
        } catch (URISyntaxException ex) {
            throw new RuntimeException(info, ex);
        }
    }

    private MultiMap<VideoQualityCodePage.Code, String> getEncodedVideos(String stream) throws Exception {
        final String[] links = stream.split("url=");
        final MultiMap<VideoQualityCodePage.Code, String> videos = new MultiValueMap<>();

        // 0) split on raw video links;
        for (String link : links) {
            link = StringEscapeUtils.unescapeJava(link);
            final String decodedLink = URLDecoder.decode(link, ENCODING);
            // n) start;
            {
                // n.0) obtain url;
                String url = null;
                {
                    final Pattern pattern = Pattern.compile("([^&,]*)[&,]");
                    final Matcher matcher = pattern.matcher(link);
                    if (matcher.find()) {
                        url = matcher.group(1);
                        url = URLDecoder.decode(url, ENCODING);
                    }
                }
                // n.1) obtain quality tag;
                String itag = null;
                {
                    final Pattern pattern = Pattern.compile("itag=(\\d+)");
                    final Matcher matcher = pattern.matcher(decodedLink);
                    if (matcher.find()) {
                        itag = matcher.group(1);
                    }
                }

                // n.2) obtain signature;
                String signature = null;
                {
                    Pattern pattern = Pattern.compile("signature=([^&,]*)");
                    Matcher matcher = pattern.matcher(decodedLink);
                    if (matcher.find()) {
                        signature = matcher.group(1);
                    } else {
                        pattern = Pattern.compile("signature%3D([^&,%]*)");
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

                // n.3) video extension;
                String ext = null;
                {
                    Pattern pattern = Pattern.compile("type=video/([^&,%]*)");
                    Matcher matcher = pattern.matcher(decodedLink);
                    if (matcher.find()) {
                        ext = matcher.group(1).split(";")[0];
                        if (ext != null) {
                            ext = ext.trim();
                            ext = ext.equalsIgnoreCase("x-flv") ? DEF_FILE_EXT : ext;
                        }
                    }
                    ext = ext == null ? DEF_FILE_EXT : ext;
                }

                // n.4) build quality map;
                if (url != null && itag != null && signature != null) {
                    url += "&signature=" + signature + "&scraper-ext=" + ext;
                    final VideoQualityCodePage.Code quality = VideoQualityCodePage.codeMap.get(Integer.decode(itag));
                    videos.put(quality, url);

                    // noinspection UnnecessaryContinue
                    continue;

                }
                // n) end;
            }
        }
        return videos;
    }

    public static class EmbeddingDisabled extends RuntimeException {

        private final static String MESSAGE_PATTERN = "The video with ID: %s has disabled embedding";

        public EmbeddingDisabled(String id) {
            super(String.format(MESSAGE_PATTERN, id));
        }
    }
}
