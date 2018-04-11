package ru.fix.commons.profiler.impl;

public class TrimmedDotsString implements CharSequence {

    private String s;

    private String trimmedDots;

    public TrimmedDotsString(String s) {
        this.s = s;
    }

    @Override
    public String toString() {
        return getTrimmedString();
    }

    private String getTrimmedString() {
        if (null == trimmedDots) {
            trimmedDots = trimDots(s);
        }
        return trimmedDots;
    }

    private String trimDots(String s) {
        if (s == null) {
            throw new NullPointerException("Given null instead of string");
        }
        s = s.trim();

        if (s.startsWith(".")) {
            s = s.substring(1);
        }

        if (s.endsWith(".")) {
            s = s.substring(0, s.length()-1);
        }

        return s;
    }

    @Override
    public int length() {
        return getTrimmedString().length();
    }

    @Override
    public char charAt(int index) {
        return getTrimmedString().charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return getTrimmedString().subSequence(start, end);
    }
}
