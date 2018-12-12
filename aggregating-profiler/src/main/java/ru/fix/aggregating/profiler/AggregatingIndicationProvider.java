package ru.fix.aggregating.profiler;

import ru.fix.aggregating.profiler.engine.AutoLabelStickerable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AggregatingIndicationProvider implements AutoLabelStickerable {
    private final Map<String, String> autoLabels = new ConcurrentHashMap<>();
    private final IndicationProvider provider;
    
    public AggregatingIndicationProvider(IndicationProvider provider) {
        this.provider = provider;
    }

    public IndicationProvider getProvider() {
        return this.provider;
    }

    @Override
    public void setAutoLabel(String name, String value) {
        this.autoLabels.put(name, value);
    }

    @Override
    public Map<String, String> getAutoLabels() {
        return autoLabels;
    }
}
