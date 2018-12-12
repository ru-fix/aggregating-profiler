package ru.fix.aggregating.profiler.engine;

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

    public Map<String, String> getTags() {
        return this.tags;
    }

    public void setTag(String name, String value) {
        this.tags.put(name, value);
    }

    public boolean hasTag(String tagName, String tagValue) {
        return tags.containsKey(tagName) && tags.get(tagName).equals(tagValue);
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
        return name +
                tags.entrySet()
                        .stream()
                        .map(entry -> entry.getKey() + "=" + entry.getValue())
                        .collect(Collectors.joining(",", "{", "}"));
    }
}

