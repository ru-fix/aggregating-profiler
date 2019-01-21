package ru.fix.aggregating.profiler;

import java.util.Map;

@FunctionalInterface
public interface ReportFilter {
    boolean filter(Identity metric, Map<String, String> labels);
}
