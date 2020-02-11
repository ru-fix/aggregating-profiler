rootProject.name = "aggregating-profiler"

for (project in listOf(
        "aggregating-profiler",
        "aggregating-profiler-graphite",
        "aggregating-profiler-jmh",
        "aggregating-profiler-prometheus")) {

    include(project)
}