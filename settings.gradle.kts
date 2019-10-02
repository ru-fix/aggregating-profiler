rootProject.name = "aggregating-profiler"

Projs.values().forEach {
    include(it.directory)
}