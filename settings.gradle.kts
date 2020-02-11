rootProject.name = "aggregating-profiler"

for (project in listOf(
        "aggregating-profiler",
        "aggregating-profiler_graphite",
        "aggregating-profiler_jmh",
        "aggregating-profiler_prometheus")) {

    include(project)
}