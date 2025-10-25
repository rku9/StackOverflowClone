package com.mountblue.stackoverflowclone.models;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SearchQuery {

    private final List<String> keywords;
    private final Map<String, Integer> numericFilters;
    private final Map<String, String> stringFilters;
    private final List<String> tags;

    public SearchQuery(List<String> keywords,
                       Map<String, Integer> numericFilters,
                       Map<String, String> stringFilters,
                       List<String> tags) {
        this.keywords = keywords == null ? List.of() : List.copyOf(keywords);
        this.numericFilters = numericFilters == null ? Map.of() : Map.copyOf(numericFilters);
        this.stringFilters = stringFilters == null ? Map.of() : Map.copyOf(stringFilters);
        this.tags = tags == null ? List.of() : List.copyOf(tags);
    }

    public List<String> getKeywords() {
        return Collections.unmodifiableList(keywords);
    }

    public Map<String, Integer> getNumericFilters() {
        return Collections.unmodifiableMap(numericFilters);
    }

    public Map<String, String> getStringFilters() {
        return Collections.unmodifiableMap(stringFilters);
    }

    public boolean isEmpty() {
        return keywords.isEmpty() && numericFilters.isEmpty() && stringFilters.isEmpty() && tags.isEmpty();
    }

    public List<String> getTags() {
        return Collections.unmodifiableList(tags);
    }
}
