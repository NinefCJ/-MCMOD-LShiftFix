package com.ninefyu.lshiftfix.update;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal JSON parser for Modrinth API responses.
 *
 * <p>Extracted from {@link UpdateChecker} so the parsing logic can be unit
 * tested without making HTTP requests. We intentionally avoid pulling in
 * Gson/Jackson to keep the jar small — the Modrinth version payload is simple
 * enough to parse with a single regex.</p>
 */
public final class VersionJsonParser {

    /**
     * Matches {@code "version_number":"1.2.3"} with optional whitespace.
     * Captures the version string (everything up to the next unescaped quote).
     */
    private static final Pattern VERSION_PATTERN =
        Pattern.compile("\"version_number\"\\s*:\\s*\"([^\"]+)\"");

    private VersionJsonParser() {}

    /**
     * Extract the first {@code version_number} value from a Modrinth
     * {@code /project/{id}/version} response body.
     *
     * @param json raw response body (may be pretty-printed or compact)
     * @return the version string, or {@code null} if not found
     */
    public static String parseFirstVersionNumber(String json) {
        if (json == null || json.isEmpty()) return null;
        Matcher m = VERSION_PATTERN.matcher(json);
        if (!m.find()) return null;
        return m.group(1);
    }
}
