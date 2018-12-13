package ru.fix.aggregating.profiler;

import java.util.Collections;
import java.util.Map;

/**
 * Stub implementation of LabelSticker.
 * It does not tag anything
 */
public class NoopLabelSticker implements LabelSticker {

    @Override
    public Map<String, String> buildLabels(String identityName) {
        return Collections.emptyMap();
    }
}
