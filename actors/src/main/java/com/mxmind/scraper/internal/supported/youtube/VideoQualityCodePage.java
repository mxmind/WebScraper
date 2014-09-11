package com.mxmind.scraper.internal.supported.youtube;

import java.util.HashMap;
import java.util.Map;

/**
 * The WebScraper solution.
 *
 * @author mxmind
 * @version 1.0-SNAPSHOT
 * @since 1.0-SNAPSHOT
 */
public class VideoQualityCodePage {

    public static enum Code implements Comparable<Code> {
        p3072, p1080, p720, p520, p480, p360, p270, p240, p144
    }

    // http://en.wikipedia.org/wiki/YouTube#Quality_and_codecs
    public static final Map<Integer, Code> codeMap = new HashMap<Integer, Code>() {{
        put(120, Code.p720);
        put(102, Code.p720);
        put(101, Code.p360);
        put(100, Code.p360);
        put(85, Code.p520);
        put(84, Code.p720);
        put(83, Code.p240);
        put(82, Code.p360);
        put(46, Code.p1080);
        put(45, Code.p720);
        put(44, Code.p480);
        put(43, Code.p360);
        put(38, Code.p3072);
        put(37, Code.p1080);
        put(36, Code.p240);
        put(35, Code.p480);
        put(34, Code.p360);
        put(22, Code.p720);
        put(18, Code.p360);
        put(17, Code.p144);
        put(6, Code.p270);
        put(5, Code.p240);
    }};
}
