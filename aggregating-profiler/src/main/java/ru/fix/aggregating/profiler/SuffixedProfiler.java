package ru.fix.aggregating.profiler;

import java.util.Optional;
import ru.fix.aggregating.profiler.engine.NameNormalizer;

public class SuffixedProfiler implements Profiler {

    private final Profiler profiler;

    private final String normalizedName;

    public SuffixedProfiler(Profiler profiler, String name) {
        this.profiler = profiler;
        this.normalizedName = NameNormalizer.trimDots(name);
    }

    @Override
    public ProfiledCall profiledCall(String name) {
        return profiler.profiledCall(normalizedName + "." + NameNormalizer.trimDots(name));
    }

    @Override
    public void attachIndicator(String name, IndicationProvider indicationProvider) {
        profiler.attachIndicator(normalizedName + "." + NameNormalizer.trimDots(name), indicationProvider);
    }

    @Override
    public void detachIndicator(String name) {
        profiler.detachIndicator(normalizedName + "." + NameNormalizer.trimDots(name));
    }

    @Override
    public void setTagger(Optional<Tagger> tagger) {
        profiler.setTagger(tagger);
    }

    @Override
    public ProfilerReporter createReporter() {
        return profiler.createReporter();
    }
}
