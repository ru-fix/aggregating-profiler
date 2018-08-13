package ru.fix.aggregating.profiler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 *
 * @author Andrey Kiselev
 */

public class Tagger {
    private final String DEFAULT_TAG_VALUE = "default";
    private final Map<String, Set<Pattern>> groupSeparator = new ConcurrentHashMap<>();

    public Tagger() {
        return;
    }
    
    public Tagger(Map<String, Set<Pattern>> groupSeparator) {
        this.groupSeparator.putAll(groupSeparator);
    }

    public <T extends Tagged> T setTag(String profiledCallName,
                                       T obj) {
        return setTag(Tagged.GRAPHITE_SELECTOR,
                      profiledCallName,
                      obj);
    }
    
    public <T extends Tagged> T setTag(String tagName,
                                       String profiledCallName,
                                       T obj) {
        for(Map.Entry<String, Set<Pattern>> entry : groupSeparator.entrySet()) {
             for(Pattern p : entry.getValue()) {
                 if(p.matcher(profiledCallName).matches()) {
                     obj.getTags().put(tagName, entry.getKey());
                     return obj;
                 }
             }
         }

        obj.getTags().put(tagName, DEFAULT_TAG_VALUE);
        return obj;
    }

    public String toString() {
        return "Tagger "+hashCode()+" gs="+groupSeparator.toString();
    }
}
