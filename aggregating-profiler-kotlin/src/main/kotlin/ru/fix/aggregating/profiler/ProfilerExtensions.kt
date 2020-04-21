package ru.fix.aggregating.profiler


/**
 * Allows for profiling suspend functions and omitting the Supplier keyword.
 * Names are different because overloading [Profiler.profile] doesn't work properly
 */
inline fun <R> ProfiledCall.profileBlock(block: () -> R): R = use {
    start()
    return block()
        .also { stop() }
}

inline fun <R> Profiler.profileBlock(name: String, block: () -> R): R = profiledCall(name).profileBlock(block)

/**
 * Convenience method
 */
inline fun <R> Profiler.profileBlock(identity: Identity, block: () -> R) = profiledCall(identity).profileBlock(block)
