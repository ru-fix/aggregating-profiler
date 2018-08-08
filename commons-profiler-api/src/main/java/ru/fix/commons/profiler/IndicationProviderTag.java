package ru.fix.commons.profiler;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 *
 * @author Andrey Kiselev
 */

public class IndicationProviderTag {
    private MetricsGroupTag tag = new MetricsGroupTag();
    private IndicationProvider provider;
    
    public IndicationProviderTag(IndicationProvider provider) {
        this.provider = provider;
    }

    public IndicationProvider getProvider() {
        return this.provider;
    }

    public String getTagValue() {
        return this.tag.getTagValue();
    }
    
    public void evalGroupTag(String profiledCallName, Map<String, Set<Pattern>> groups) {
        tag.evalGroupTag(profiledCallName, groups);
    }
}
