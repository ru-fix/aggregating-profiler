package ru.fix.aggregating.profiler;

import java.util.Optional;

/**
 *
 * @author Andrey Kiselev
 */

public interface Tagging {
    default void setTagger(Tagger tagger) {
        setTagger(Optional.ofNullable(tagger));
    }
    
    void setTagger(Optional<Tagger> tagger);
}
