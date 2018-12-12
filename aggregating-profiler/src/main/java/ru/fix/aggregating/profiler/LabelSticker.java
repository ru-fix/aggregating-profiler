package ru.fix.aggregating.profiler;

import java.util.Map;

/**
 *
 * @author Andrey Kiselev
 */

public interface LabelSticker {
    String EMPTY_VALUE = "";
    Map<String, String> buildLabels(String identityName);
}
