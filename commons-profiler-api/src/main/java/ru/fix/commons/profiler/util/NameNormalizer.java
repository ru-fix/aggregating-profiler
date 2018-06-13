package ru.fix.commons.profiler.util;

public class NameNormalizer {

    public static String trimDots(String s) {
        if (s == null) {
            throw new NullPointerException("Given null instead of string");
        }
        s = s.trim();

        if (s.startsWith(".")) {
            s = s.substring(1);
        }

        if (s.endsWith(".")) {
            s = s.substring(0, s.length() - 1);
        }

        return s;
    }
}
