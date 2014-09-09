package com.mxmind.scraper.internal.supported.utube;

import java.util.HashMap;
import java.util.Map;

/**
 * The WebScraper solution.
 *
 * @author mxmind
 * @version 1.0-SNAPSHOT
 * @since 1.0-SNAPSHOT
 */
public class VideoInfo {

    public static enum VideoQuality {
        p3072, p1080, p720, p520, p480, p360, p270, p240, p144;
    };

    // http://en.wikipedia.org/wiki/YouTube#Quality_and_codecs
    public static final Map<Integer, VideoQuality> itagMap = new HashMap<Integer, VideoQuality>() {{
            put(120, VideoQuality.p720);
            put(102, VideoQuality.p720);
            put(101, VideoQuality.p360);
            put(100, VideoQuality.p360);
            put(85, VideoQuality.p520);
            put(84, VideoQuality.p720);
            put(83, VideoQuality.p240);
            put(82, VideoQuality.p360);
            put(46, VideoQuality.p1080);
            put(45, VideoQuality.p720);
            put(44, VideoQuality.p480);
            put(43, VideoQuality.p360);
            put(38, VideoQuality.p3072);
            put(37, VideoQuality.p1080);
            put(36, VideoQuality.p240);
            put(35, VideoQuality.p480);
            put(34, VideoQuality.p360);
            put(22, VideoQuality.p720);
            put(18, VideoQuality.p360);
            put(17, VideoQuality.p144);
            put(6, VideoQuality.p270);
            put(5, VideoQuality.p240);
    }};
}
