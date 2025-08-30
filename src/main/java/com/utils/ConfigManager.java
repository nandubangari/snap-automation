package com.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Loads config.properties from classpath and provides parsing helpers.
 */
public final class ConfigManager {
    private static final Properties props = new Properties();

    static {
        try (InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("config.properties")) {
            if (is == null) throw new RuntimeException("config.properties not found on classpath");
            // load using UTF-8
            props.load(new java.io.InputStreamReader(is, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }
    }

    private ConfigManager() {}

    public static String get(String key) {
        String v = props.getProperty(key);
        return v == null ? "" : v.trim();
    }

    public static boolean getBoolean(String key) {
        String v = get(key);
        if (v.isEmpty()) return false;
        return v.equalsIgnoreCase("true") || v.equalsIgnoreCase("yes") || v.equals("1");
    }

    /** Return CSV as list of trimmed non-empty strings. If key missing or blank => empty list. */
    public static List<String> getList(String key) {
        String v = get(key);
        if (v.isEmpty()) return Collections.emptyList();
        return Arrays.stream(v.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /** Return CSV as a lowercase set (good for case-insensitive contains checks). */
    public static Set<String> getSetLowerCase(String key) {
        return getList(key).stream()
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }
}
