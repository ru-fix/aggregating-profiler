package ru.fix.aggregating.profiler;

import ru.fix.aggregating.profiler.engine.NameNormalizer;

/**
 * Attach fixed prefix to profiled calls and indicator names
 *
 * @author Kamil Asfandiyarov
 */
public class PrefixedProfiler implements Profiler {
    private final Profiler profiler;
    private final String profilerPrefix;

    public PrefixedProfiler(Profiler profiler, String profilerPrefix) {
        this.profiler = profiler;
        this.profilerPrefix = NameNormalizer.trimDots(profilerPrefix);
    }

    @Override
    public ProfiledCall profiledCall(String name) {
        return profiler.profiledCall(profilerPrefix + "." + NameNormalizer.trimDots(name));
    }

    @Override
    public void attachIndicator(String name, IndicationProvider indicationProvider) {
        profiler.attachIndicator(profilerPrefix + "." + NameNormalizer.trimDots(name), indicationProvider);
    }

    @Override
    public void detachIndicator(String name) {
        profiler.detachIndicator(profilerPrefix + "." + NameNormalizer.trimDots(name));
    }

    @Override
    public void setLabelSticker(LabelSticker labelSticker) {
        profiler.setLabelSticker(labelSticker);
    }

    @Override
    public ProfilerReporter createReporter() {
        return profiler.createReporter();
    }
}
