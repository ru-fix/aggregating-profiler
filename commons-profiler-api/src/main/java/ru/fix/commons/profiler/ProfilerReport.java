package ru.fix.commons.profiler;

import java.util.List;
import java.util.Map;

public class ProfilerReport {

    private Map<String, Long> indicators;

    private List<ProfilerCallReport> profilerCallReports;

    public Map<String, Long> getIndicators() {
        return indicators;
    }

    public List<ProfilerCallReport> getProfilerCallReports() {
        return profilerCallReports;
    }

    public void setIndicators(Map<String, Long> indicators) {
        this.indicators = indicators;
    }

    public void setProfilerCallReports(List<ProfilerCallReport> profilerCallReports) {
        this.profilerCallReports = profilerCallReports;
    }

    @Override
    public String toString() {
        StringBuilder sumReport = new StringBuilder();
        sumReport.append("Indicators:\n");
        indicators.entrySet().forEach(i ->
                sumReport.append(i.getKey())
                        .append(" = ")
                        .append(i.getValue())
                        .append('\n')
        );
        sumReport.append("Profilers:\n");
        profilerCallReports.forEach(pr -> {
            sumReport.append(pr.toString());
            sumReport.append('\n');
        });
        return sumReport.toString();
    }
}
