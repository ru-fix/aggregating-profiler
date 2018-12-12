package ru.fix.aggregating.profiler.engine;

import java.util.Map;

public interface AutoLabelStickerable {
    void setAutoLabel(String name, String value);
    Map<String, String> getAutoLabels();
}
