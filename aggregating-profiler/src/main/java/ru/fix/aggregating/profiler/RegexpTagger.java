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
    private final String tagName;
    private final Map<String, Set<Pattern>> groupSeparator = new HashMap<>();

    public RegexpTagger(String tagName,
                        Map<String, Set<Pattern>> groupSeparator) {
        this.groupSeparator.putAll(groupSeparator);
        this.tagName = tagName;
    }

    @Override
    public <T extends Tagged> T assignTag(String profiledCallName,
                                          T tagged) {
        for(Map.Entry<String, Set<Pattern>> entry : groupSeparator.entrySet()) {
             for(Pattern p : entry.getValue()) {
                 if(p.matcher(profiledCallName).matches()) {
                     tagged.setTag(tagName, entry.getKey());
                     return tagged;
                 }
             }
         }
        
        tagged.setTag(tagName, Tagger.EMPTY_VALUE);
        return tagged;
    }

}
