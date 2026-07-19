package com.ninefyu.lshiftfix.update;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for {@link VersionJsonParser}.
 *
 * <p>Verifies that the Modrinth version payload is parsed correctly regardless
 * of formatting (compact vs pretty-printed) and that malformed input is
 * handled gracefully.</p>
 */
public class VersionJsonParserTest {

    // ------------------------------------------------------------------
    // Happy paths
    // ------------------------------------------------------------------

    @Test
    public void parses_compact_json() {
        String json = "[{\"name\":\"1.0.1\",\"version_number\":\"1.0.1\",\"game_versions\":[\"1.8.9\"]}]";
        assertEquals("1.0.1", VersionJsonParser.parseFirstVersionNumber(json));
    }

    @Test
    public void parses_pretty_printed_json() {
        String json = "[\n" +
            "  {\n" +
            "    \"name\": \"LShiftFix 1.0.1\",\n" +
            "    \"version_number\": \"1.0.1\",\n" +
            "    \"game_versions\": [\"1.8.9\"]\n" +
            "  }\n" +
            "]";
        assertEquals("1.0.1", VersionJsonParser.parseFirstVersionNumber(json));
    }

    @Test
    public void parses_version_with_pre_release_suffix() {
        String json = "{\"version_number\":\"2.0.0-beta.3\"}";
        assertEquals("2.0.0-beta.3", VersionJsonParser.parseFirstVersionNumber(json));
    }

    @Test
    public void parses_version_with_build_metadata() {
        String json = "{\"version_number\":\"1.0.0+build.42\"}";
        assertEquals("1.0.0+build.42", VersionJsonParser.parseFirstVersionNumber(json));
    }

    @Test
    public void returns_first_version_when_multiple_present() {
        // Modrinth returns newest first; we should pick the first one.
        String json = "[{\"version_number\":\"1.1.0\"},{\"version_number\":\"1.0.0\"}]";
        assertEquals("1.1.0", VersionJsonParser.parseFirstVersionNumber(json));
    }

    @Test
    public void handles_extra_whitespace_around_colon() {
        String json = "{\"version_number\"   :   \"1.2.3\"}";
        assertEquals("1.2.3", VersionJsonParser.parseFirstVersionNumber(json));
    }

    @Test
    public void handles_tab_whitespace() {
        String json = "{\"version_number\":\t\"1.2.3\"}";
        assertEquals("1.2.3", VersionJsonParser.parseFirstVersionNumber(json));
    }

    // ------------------------------------------------------------------
    // Edge cases / failure modes
    // ------------------------------------------------------------------

    @Test
    public void returns_null_for_null_input() {
        assertNull(VersionJsonParser.parseFirstVersionNumber(null));
    }

    @Test
    public void returns_null_for_empty_string() {
        assertNull(VersionJsonParser.parseFirstVersionNumber(""));
    }

    @Test
    public void returns_null_for_missing_field() {
        String json = "{\"name\":\"1.0.0\",\"game_versions\":[\"1.8.9\"]}";
        assertNull(VersionJsonParser.parseFirstVersionNumber(json));
    }

    @Test
    public void returns_null_for_empty_version_value() {
        // Empty string between quotes — regex ([^\"]+) requires at least one char,
        // so an empty value won't match. This is the desired behavior: we don't
        // want to report an empty string as "latest version".
        String json = "{\"version_number\":\"\"}";
        assertNull(VersionJsonParser.parseFirstVersionNumber(json));
    }

    @Test
    public void returns_null_for_plain_text() {
        assertNull(VersionJsonParser.parseFirstVersionNumber("not json at all"));
    }

    @Test
    public void returns_null_for_html_error_page() {
        String html = "<html><body>503 Service Unavailable</body></html>";
        assertNull(VersionJsonParser.parseFirstVersionNumber(html));
    }

    @Test
    public void does_not_match_version_number_inside_other_strings() {
        // The substring "version_number" appears inside a string value, but
        // not as a JSON key. The regex requires the quote-colon-quote structure
        // so this should not match.
        String json = "{\"description\":\"the version_number field is required\",\"version_number\":\"1.0.0\"}";
        assertEquals("1.0.0", VersionJsonParser.parseFirstVersionNumber(json));
    }
}
