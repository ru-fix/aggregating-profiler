package ru.fix.aggregating.profiler;

import ru.fix.aggregating.profiler.engine.NameNormalizer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Attach fixed prefix to profiled calls and indicator names
 *
 * @author Kamil Asfandiyarov
 */
public class PrefixedProfiler implements Profiler {
    private final Profiler profiler;
    private final String identityNamePrefix;
    private final Map<String, String> tags;

    public PrefixedProfiler(Profiler profiler, String identityNamePrefix) {
        this(profiler, identityNamePrefix, Collections.emptyMap());
    }

    public PrefixedProfiler(Profiler profiler, String identityNamePrefix, Map<String, String> tags) {
        this.profiler = profiler;
        this.identityNamePrefix = NameNormalizer.trimDots(identityNamePrefix);
        this.tags = tags;
    }

    @Override
    public ProfiledCall profiledCall(String name) {
        return profiler.profiledCall(prefixedIdentity(new Identity(name)));
    }

    @Override
    public ProfiledCall profiledCall(Identity identity) {
        return profiler.profiledCall(prefixedIdentity(identity));
    }

    @Override
    public void attachIndicator(String name, IndicationProvider indicationProvider) {
        profiler.attachIndicator(prefixedIdentity(new Identity(name)), indicationProvider);
    }

    @Override
    public void attachIndicator(Identity identity, IndicationProvider indicationProvider) {
        profiler.attachIndicator(prefixedIdentity(identity), indicationProvider);
    }

    @Override
    public void detachIndicator(String name) {
        profiler.detachIndicator(prefixedIdentity(new Identity(name)));
    }

    @Override
    public void detachIndicator(Identity identity) {
        profiler.detachIndicator(prefixedIdentity(identity));
    }

    @Override
    public ProfilerReporter createReporter() {
        return profiler.createReporter();
    }

    private Identity prefixedIdentity(Identity identity){
        HashMap<String, String> newIdentityTags;
        if(!this.tags.isEmpty()){
            newIdentityTags = new HashMap<>();
            newIdentityTags.putAll(this.tags);
            newIdentityTags.putAll(identity.tags);
        } else {
            newIdentityTags = identity.tags;
        }

        String newIdentityName;
        if(identityNamePrefix != null && !identityNamePrefix.isEmpty()){
            newIdentityName = identityNamePrefix + "." +NameNormalizer.trimDots(identity.name);
        } else {
             newIdentityName = identity.name;
        }

        return new Identity(newIdentityName, newIdentityTags);
    }
}
