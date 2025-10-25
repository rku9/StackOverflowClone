package com.mountblue.stackoverflowclone.services;

import com.mountblue.stackoverflowclone.models.SearchQuery;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SearchQueryParser {

    // 1) Either: "quoted phrase"  2) Or: any non-space token
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\"([^\"]+)\"|(\\S+)");
    // unquoted tokens become tags; allow letters, digits, hyphens
    private static final Pattern TAG_PATTERN = Pattern.compile("[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*");

    // configure your supported filters
    private static final Set<String> NUMERIC_FILTER_KEYS = Set.of("score", "answers", "views");
    private static final Set<String> STRING_FILTER_KEYS  = Set.of("user", "tag");

    public SearchQuery parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return new SearchQuery(List.of(), Map.of(), Map.of(), List.of());
        }

        List<String> keywords = new ArrayList<>();                // quoted phrases only
        Map<String, Integer> numericFilters = new LinkedHashMap<>();
        Map<String, String>  stringFilters  = new LinkedHashMap<>();
        List<String> tags     = new ArrayList<>();                // all unquoted words

        Matcher m = TOKEN_PATTERN.matcher(raw);
        while (m.find()) {
            boolean isQuoted = m.group(1) != null; //the quoted words are keywords belonging to the group 1.
            String token = isQuoted ? m.group(1) : m.group(2);
            if (token == null || token.isBlank()) continue;

            if (isQuoted) {
                // Rule: quoted => keyword phrase (ignore any : inside the quotes)
                keywords.add(token);
                continue;
            }

            // Not quoted: check for key:value
            int idx = token.indexOf(':');
            if (idx > 0 && idx < token.length() - 1) {
                String key = token.substring(0, idx).toLowerCase(Locale.ROOT);
                String val = token.substring(idx + 1);

                if (NUMERIC_FILTER_KEYS.contains(key)) {
                    try {
                        numericFilters.put(key, Integer.parseInt(val));
                        continue;
                    } catch (NumberFormatException ignore) {
                        // fall through, treat as tag/keyword by rule below
                    }
                }
                if (STRING_FILTER_KEYS.contains(key)) {
                    // special-case: tag:java should behave like a tag filter OR you can also push into tags
                    if ("tag".equals(key)) {
                        String t = normalizeTag(val);
                        if (t != null) tags.add(t);
                        // and still store it as a string filter if you want both:
                        stringFilters.put(key, val);
                    } else {
                        stringFilters.put(key, val);
                    }
                    continue;
                }
                // Unknown key => per your rule set there are only filters with ":".
                // If key is unknown, treat whole token as a plain tag (least surprise).
            }

            // Unquoted, not key:value => tag
            String t = normalizeTag(token);
            // If it doesn't look like a tag, still keep it as a tag to honor the rule,
            // but normalization filters out obvious junk.
            tags.add(t != null ? t : token.toLowerCase(Locale.ROOT));
        }

        return new SearchQuery(keywords, numericFilters, stringFilters, tags);
    }

    private String normalizeTag(String s) {
        String x = s.trim().toLowerCase(Locale.ROOT);
        return TAG_PATTERN.matcher(x).matches() ? x : null;
    }
}
