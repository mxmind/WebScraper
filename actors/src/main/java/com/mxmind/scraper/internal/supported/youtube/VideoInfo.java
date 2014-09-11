package com.mxmind.scraper.internal.supported.youtube;

import com.gargoylesoftware.htmlunit.util.UrlUtils;
import com.mxmind.scraper.Main;
import org.apache.commons.collections4.MultiMap;
import org.apache.commons.collections4.map.MultiValueMap;
import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;
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

    private final MultiMap<VideoQualityCodePage.Code, String> videoLinks;

    public VideoInfo() {
        this(null, MultiValueMap.multiValueMap(Collections.emptyMap()));
    }

    public VideoInfo(String title, MultiMap<VideoQualityCodePage.Code, String> videoLinks) {
        this.title = title;
        this.videoLinks = videoLinks;
    }

    public Optional<String> getVideoLink() {
        final VideoQualityCodePage.Code filterCode = VideoQualityCodePage.Code.valueOf(CODE_NAME);

        if (!getVideoLinks().isEmpty()) {
            final Optional<VideoQualityCodePage.Code> min = getVideoLinks().keySet()
                .stream()
                .filter(key -> key.equals(filterCode))
                .max(Comparator.<VideoQualityCodePage.Code>naturalOrder());
            if (min.isPresent()) {
                final VideoQualityCodePage.Code key = min.get();

                @SuppressWarnings("unchecked")
                final List<String> links = (List<String>) getVideoLinks().get(key);
                final String link = links.get(0);

                return Optional.of(SerializationUtils.clone(link));
            }
        }
        return Optional.empty();
    }

    public MultiMap<VideoQualityCodePage.Code, String> getVideoLinks() {
        return MultiValueMap.<VideoQualityCodePage.Code, String>multiValueMap(videoLinks);
    }

    public String getFilename() {
        return UrlUtils.decode(getTitle()).replaceAll("[!@#\\$%^&*()]", "_").trim();
    }

    public String getTitle() {
        return title;
    }
}
