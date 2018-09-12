package ru.fix.aggregating.profiler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collections;

/**
 *
 * @author Andrey Kiselev
 */

public class TaggedIndicationProvider implements Tagged {
    private final Map<String, String> tags = new ConcurrentHashMap<>();
    private final IndicationProvider provider;
    
    public TaggedIndicationProvider(IndicationProvider provider) {
        this.provider = provider;
    }

    public IndicationProvider getProvider() {
        return this.provider;
    }

    @Override
    public Map<String, String> getTags() {
        return Collections.unmodifiableMap(this.tags);
    }

    @Override
    public boolean hasTag(String tagName, String tagValue) {
        return tags.containsKey(tagName) && tags.get(tagName).equals(tagValue);
    }
    
    @Override
    public void setTag(String name, String value) {
        this.tags.put(name, value);
    }
}
