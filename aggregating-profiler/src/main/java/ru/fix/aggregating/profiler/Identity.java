package ru.fix.aggregating.profiler;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class Identity {
    final String name;
    final Map<String, String> tags = new HashMap<>();

    public Identity(String name) {
        this.name = name;
    }

    /**
     * @param tags should not contains null values. It should have even size (key-value)
     * */
    public Identity(@NotNull String name, @NotNull String... tags) {
        if(List.of(tags).contains(null)) {
            throw new NullPointerException("tags must not contains nulls. tags = " + Arrays.toString(tags));
        }
        if (tags.length % 2 != 0) {
            throw new IllegalArgumentException("Invalid tags array size: " + tags.length + ". Expected even size.");
        }

        this.name = name;

        for (int i = 0; i < tags.length - 1; i += 2) {
            this.tags.put(tags[i], tags[i + 1]);
        }
    }

    /**
     * @param tags should not contains null values or keys
     * */
    public Identity(@NotNull String name, @NotNull Map<String, String> tags) {
        if (tags.containsKey(null) || tags.containsValue(null)) {
            throw new NullPointerException("tags must not contains null keys or values. tags = " + tags);
        }

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

    public String identityString() {
        if (tags.isEmpty()) {
            return name;
        } else {
            return name +
                    tags.entrySet()
                            .stream()
                            .map(entry -> entry.getKey() + "=" + entry.getValue())
                            .sorted()
                            .collect(Collectors.joining(",", "{", "}"));
        }
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
        return identityString();
    }
}

