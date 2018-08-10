package ru.fix.commons.profiler;

import java.util.Map;
import java.util.HashMap;
//import java.util.Set;
import java.util.regex.Pattern;

/**
 *
 * @author Andrey Kiselev
 */

public class IndicationProviderTaged implements Tagged {
    private final Map<String, String> tags = new HashMap<>();
    private final IndicationProvider provider;
    
    public IndicationProviderTag(IndicationProvider provider) {
        this.provider = provider;
    }

    public IndicationProvider getProvider() {
        return this.provider;
    }

    @Override
    public String getTags() {
        return this.tags;
    }
}
