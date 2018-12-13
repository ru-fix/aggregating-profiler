package ru.fix.aggregating.profiler;

public class ReportFilters {

    public static ReportFilter containsLabel(String labelName, String labelValue) {
        return (metric, labels) -> {
            String value = labels.get(labelName);
            return value != null && value.equals(labelValue);
        };
    }

    public static ReportFilter notContainsLabel(String labelName, String labelValue) {
        return (metric, labels) -> {
            String value = labels.get(labelName);
            return value == null || !value.equals(labelValue);
        };
    }

    public static ReportFilter notContainsLabel(String labelName) {
        return (metric, labels) -> !labels.containsKey(labelName);
    }
}
