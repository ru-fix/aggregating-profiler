package ru.fix.aggregating.profiler;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class Identity {
    final String name;
    final Map<String, String> tags = new HashMap<>();

    public Identity(String name) {
        this.name = name;
    }

    public Identity(String name, String... tags) {
        this.name = name;
        if (tags.length % 2 != 0) {
            throw new IllegalArgumentException("Invalid tags array size: " + tags.length + ". Expected even size.");
        }
        for (int i = 0; i < tags.length - 1; i += 2) {
            this.tags.put(tags[i], tags[i + 1]);
        }
    }

    public Identity(String name, Map<String, String> tags) {
        this.name = name;

        for (Map.Entry<String, String> tag : tags.entrySet()) {
            this.tags.put(tag.getKey(), tag.getValue());
        }
    }


    public Map<String, String> getTags() {
        return Collections.unmodifiableMap(this.tags);
    }

    public boolean hasTag(String tagName, String tagValue) {
        return tags.containsKey(tagName) && tags.get(tagName).equals(tagValue);
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Identity identity = (Identity) o;
        return name.equals(identity.name) &&
                tags.equals(identity.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, tags);
    }

    @Override
    public String toString() {
        if (tags.isEmpty()) {
            return name;
        } else {
            return name +
                    tags.entrySet()
                            .stream()
                            .map(entry -> entry.getKey() + "=" + entry.getValue())
                            .collect(Collectors.joining(",", "{", "}"));
        }
    }
}

