package ru.fix.aggregating.profiler;

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
    public ProfilerReporter createReporter() {
        return profiler.createReporter();
    }

    private Identity suffixedIdentity(Identity identity){
        return new Identity(
                normalizedName + "." + NameNormalizer.trimDots(identity.name),
                identity.getTags()
        );
    }

    @Override
    public ProfiledCall profiledCall(Identity identity) {
        return profiler.profiledCall(
                suffixedIdentity(identity)
        );
    }

    @Override
    public void attachIndicator(Identity identity, IndicationProvider indicationProvider) {
        profiler.attachIndicator(suffixedIdentity(identity), indicationProvider);
    }

    @Override
    public void detachIndicator(Identity identity) {
        profiler.detachIndicator(suffixedIdentity(identity));

    }
}
