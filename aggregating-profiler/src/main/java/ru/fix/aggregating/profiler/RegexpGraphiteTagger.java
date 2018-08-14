package ru.fix.aggregating.profiler;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 *
 * @author Andrey Kiselev
 */

public class RegexpGraphiteTagger extends DefaultGraphiteTagger {
    private final Map<String, Set<Pattern>> groupSeparator;

    public RegexpGraphiteTagger() {
        this(new HashMap<>());
    }
    
    public RegexpGraphiteTagger(Map<String, Set<Pattern>> groupSeparator) {
        this.groupSeparator = groupSeparator;
    }

    @Override
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

        return super.setTag(tagName, profiledCallName, obj);
    }

}
