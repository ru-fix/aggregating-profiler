package ru.fix.aggregating.profiler;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 *
 * @author Andrey Kiselev
 */

public class RegexpTagger extends DefaultTagger {
    private final Map<String, Set<Pattern>> groupSeparator = new HashMap<>();

    public RegexpTagger(Map<String, Set<Pattern>> groupSeparator) {
        this.groupSeparator.putAll(groupSeparator);
    }

    @Override
    public <T extends Tagged> T assignTag(String tagName,
                                          String profiledCallName,
                                          T tagged) {
        for(Map.Entry<String, Set<Pattern>> entry : groupSeparator.entrySet()) {
             for(Pattern p : entry.getValue()) {
                 if(p.matcher(profiledCallName).matches()) {
                     tagged.setTag(tagName, entry.getKey());
                     return tagged;
                 }
             }
         }

        return super.assignTag(tagName, profiledCallName, tagged);
    }

}
