package com.mxmind.scraper.internal.supported.youtube;

import com.mxmind.scraper.Main;
import org.apache.commons.collections4.MultiMap;
import org.apache.commons.collections4.map.MultiValueMap;
import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;
import java.net.URL;
import java.util.*;

/**
 * The WebScraper solution.
 *
 * @author mxmind
 * @version 1.0-SNAPSHOT
 * @since 1.0-SNAPSHOT
 */
public final class VideoInfo implements Serializable {

    private static final String CODE_NAME = Main.props.getString("scraper.video.quality", "p144");

    private transient final String title;

    private final MultiMap<VideoQualityCodePage.Code, URL> videoLinks;

    public VideoInfo() {
        this(null, MultiValueMap.multiValueMap(Collections.emptyMap()));
    }

    public VideoInfo(String title, MultiMap<VideoQualityCodePage.Code, URL> videoLinks) {
        this.title = title;
        this.videoLinks = videoLinks;
    }

    public Optional<URL> getVideoLink() {
        final VideoQualityCodePage.Code filterCode = VideoQualityCodePage.Code.valueOf(CODE_NAME);

        if (!getVideoLinks().isEmpty()) {
            final Optional<VideoQualityCodePage.Code> min = getVideoLinks().keySet()
                .stream()
                .filter(key -> key.equals(filterCode))
                .max(Comparator.<VideoQualityCodePage.Code>naturalOrder());
            if (min.isPresent()) {
                final VideoQualityCodePage.Code key = min.get();

                @SuppressWarnings("unchecked")
                final List<URL> links = (List<URL>) getVideoLinks().get(key);
                final URL link = links.get(0);

                return Optional.of(SerializationUtils.clone(link));
            }
        }
        return Optional.empty();
    }

    public MultiMap<VideoQualityCodePage.Code, URL> getVideoLinks() {
        return MultiValueMap.<VideoQualityCodePage.Code, URL>multiValueMap(videoLinks);
    }

    public String getFilename() {
        return String.format("%s.flv", getTitle().replaceAll("[^a-zA-Z0-9]", "_").trim());
    }

    public String getTitle() {
        return title;
    }
}
