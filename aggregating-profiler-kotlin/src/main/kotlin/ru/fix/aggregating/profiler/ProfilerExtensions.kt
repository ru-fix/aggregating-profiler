package ru.fix.aggregating.profiler


/**
 * Allows for profiling suspend functions and omitting the Supplier keyword.
 * Names are different because overloading [Profiler.profile] doesn't work properly
 */
inline fun <R> ProfiledCall.profileInline(block: () -> R): R = use {
    start()
    return block()
        .also { stop() }
}

inline fun <R> Profiler.profileInline(name: String, block: () -> R): R = profiledCall(name).profileInline(block)

/**
 * Convenience method
 */
inline fun <R> Profiler.profileInline(identity: Identity, block: () -> R) = profiledCall(identity).profileInline(block)
