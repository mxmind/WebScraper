package com.mxmind.scraper.internal.utils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.text.StrSubstitutor;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The WebScraper solution.
 *
 * @author mxmind
 * @version 1.0-SNAPSHOT
 * @since 1.0-SNAPSHOT
 */
@SuppressWarnings("unused")
public class Props extends HashMap<String, String> {

    private static final long serialVersionUID = 2211405016738281987L;

    public Props() {
    }

    public Props(URL url) {
        load(url);
        expandVariables();
    }

    public void load(URL url) {
        try {
            load(url.openStream());
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to open " + url, e);
        }
    }

    public void expandVariables() {
        final Props lookupProps = new Props();
        lookupProps.putAll(this);
        lookupProps.fromProperties(System.getProperties());

        StrSubstitutor substitutor = new StrSubstitutor(lookupProps);
        for (Map.Entry<String, String> entry : entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            if (value == null) {
                continue;
            }
            String newVal = substitutor.replace(value);
            if (!newVal.equals(value)) {
                put(name, newVal);
            }
        }
    }

    public void load(InputStream inStream) {
        Properties props = null;
        try {
            props = new Properties();
            props.load(inStream);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            if (inStream != null) {
                IOUtils.closeQuietly(inStream);
            }
        }
        fromProperties(props);
    }

    public void fromProperties(Properties props) {
        @SuppressWarnings({"unchecked", "rawtypes"})
        Map<String, String> map = (Map) props;
        super.putAll(map);
    }

    public Props(Properties props) {
        fromProperties(props);
        expandVariables();
    }

    public Props(Map<String, String> map) {
        putAll(map);
        expandVariables();
    }

    public List<String> getGroupKeys(String groupKey) {
        return keySet().stream().filter(key -> key.startsWith(groupKey)).collect(Collectors.toList());
    }

    public int getInt(String key) {
        final String value = getString(key);
        return Integer.parseInt(value);
    }

    public String getString(String key) {
        if (!containsKey(key)) {
            throw new IllegalArgumentException("Map key not found: " + key);
        }
        return get(key);
    }

    public int getInt(String key, int def) {
        final String value = getString(key, "" + def);
        return Integer.parseInt(value);
    }

    public String getString(String key, String def) {
        String result = get(key);
        if (result == null) {
            result = def;
        }
        return result;
    }

    public boolean getBoolean(String key) {
        final String value = getString(key);
        return Boolean.parseBoolean(value);
    }

    public boolean getBoolean(String key, boolean def) {
        final String value = getString(key, "" + def);
        return Boolean.parseBoolean(value);
    }

    public long getLong(String key) {
        final String value = getString(key);
        return Long.parseLong(value);
    }

    public long getLong(String key, long def) {
        final String value = getString(key, "" + def);
        return Long.parseLong(value);
    }

    public double getDouble(String key) {
        final String value = getString(key);
        return Double.parseDouble(value);
    }

    public double getDouble(String key, double def) {
        final String value = getString(key, "" + def);
        return Double.parseDouble(value);
    }

    /**
     * Clone this map and return it as java.util.Properties
     */
    @SuppressWarnings("unchecked")
    public Properties toProperties() {
        Properties properties = new Properties();
        properties.putAll((Map<String, String>) this.clone());
        return properties;
    }
}
