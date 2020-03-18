package ru.fix.aggregating.profiler

import java.util.*
import kotlin.collections.HashMap

class Identity {
    @JvmField
    val name: String
    val tags: Map<String, String>
        get() = Collections.unmodifiableMap(field)

    /**
     * @param tags should have even size (key-value)
     */
    constructor(name: String, vararg tags: String) {
        require(tags.size % 2 == 0) {
            "Invalid tags array size: ${tags.size}. Expected even size."
        }
        this.name = name
        this.tags = tags.asList().chunked(2).associate { (a, b) -> a to b }
    }

    constructor(name: String, tags: Map<String, String>) {
        this.name = name
        this.tags = HashMap(tags)
    }

    fun hasTag(tagName: String, tagValue: String): Boolean {
        return tags.containsKey(tagName) && tags[tagName] == tagValue
    }

    fun identityString(): String = if (tags.isEmpty()) {
        name
    } else {
        name + tags.entries.map { "${it.key}=${it.value}" }.sorted().joinToString(",", "{", "}")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val identity = other as Identity
        return name == identity.name && tags == identity.tags
    }

    override fun hashCode(): Int {
        return Objects.hash(name, tags)
    }

    override fun toString(): String {
        return identityString()
    }
}