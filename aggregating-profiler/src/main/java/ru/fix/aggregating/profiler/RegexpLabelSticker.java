package ru.fix.aggregating.profiler;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 *
 * @author Andrey Kiselev
 */

public class RegexpLabelSticker implements LabelSticker {
    private final String tagName;
    private final Map<String, Set<Pattern>> labelValuesWithSelectors = new HashMap<>();

    public RegexpLabelSticker(String labelName,
                              Map<String, Set<Pattern>> labelValuesWithSelectors) {
        this.labelValuesWithSelectors.putAll(labelValuesWithSelectors);
        this.tagName = labelName;
    }

    @Override
    public Map<String, String> buildLabels(String identityName){
        HashMap<String, String> labels = new HashMap<>();

        for(Map.Entry<String, Set<Pattern>> entry : labelValuesWithSelectors.entrySet()) {
             for(Pattern p : entry.getValue()) {
                 if(p.matcher(identityName).matches()) {
                     labels.put(tagName, entry.getKey());
                     return labels;
                 }
             }
         }
        return labels;
    }

}
