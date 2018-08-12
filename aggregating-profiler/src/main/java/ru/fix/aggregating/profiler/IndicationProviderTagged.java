package ru.fix.aggregating.profiler;

import java.util.Map;
import java.util.HashMap;
//import java.util.Set;
import java.util.regex.Pattern;

/**
 *
 * @author Andrey Kiselev
 */

public class IndicationProviderTagged implements Tagged {
    private final Map<String, String> tags = new HashMap<>();
    private final IndicationProvider provider;
    
    public IndicationProviderTagged(IndicationProvider provider) {
        this.provider = provider;
    }

    public IndicationProvider getProvider() {
        return this.provider;
    }

    @Override
    public Map<String, String> getTags() {
        return this.tags;
    }
}
