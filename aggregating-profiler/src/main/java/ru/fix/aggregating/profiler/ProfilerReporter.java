package ru.fix.aggregating.profiler;

import java.util.List;
import java.util.regex.Pattern;

public interface ProfilerReporter extends AutoCloseable {

    ProfilerReport buildReportAndReset();

    /**
     * @return empty report in case of empty patterns
     */
    ProfilerReport buildReportAndReset(List<Pattern> patterns);
}
