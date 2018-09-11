package ru.fix.aggregating.profiler;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 *
 * @author Andrey Kiselev
 */

public class RegexpTagger implements Tagger {
    private final String defaultTagName;
    private final Map<String, Set<Pattern>> groupSeparator = new HashMap<>();

    public RegexpTagger(String defaultTagName,
                        Map<String, Set<Pattern>> groupSeparator) {
        this.groupSeparator.putAll(groupSeparator);
        this.defaultTagName = defaultTagName;
    }

    @Override
    public <T extends Tagged> T assignTag(String profiledCallName,
                                          T tagged) {
        for(Map.Entry<String, Set<Pattern>> entry : groupSeparator.entrySet()) {
             for(Pattern p : entry.getValue()) {
                 if(p.matcher(profiledCallName).matches()) {
                     tagged.setTag(defaultTagName, entry.getKey());
                     return tagged;
                 }
             }
         }
        
        tagged.setTag(defaultTagName, defaultTagName);
        return tagged;
    }

}
