package ru.fix.commons.profiler;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;


/**
 *
 * @author Andrey Kiselev
 */

public class MetricsGroupTag {
    public final static String DEFAULT_TAG_NAME = "default";
    private volatile String tagValue = DEFAULT_TAG_NAME;

    public String getTagValue() {
        return this.tagValue;
    }
    
    public void evalGroupTag(String profiledCallName, Map<String, Set<Pattern>> groups) {
        if(! groups.isEmpty()) {
            for(Map.Entry<String, Set<Pattern>> entry : groups.entrySet()) {
                for(Pattern p : entry.getValue()) {
                    if(p.matcher(profiledCallName).matches()) {
                        tagValue = entry.getKey();
                        return;
                    }
                }
            }
        }
        tagValue = DEFAULT_TAG_NAME;
    }
}
