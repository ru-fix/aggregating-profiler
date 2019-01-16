package ru.fix.aggregating.profiler.engine

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import ru.fix.aggregating.profiler.Identity

class IdentityTest {
    @Test
    fun `no tags`() {
        val identity = Identity("name")
        assertThat(identity.name, equalTo("name"))
        assertThat(identity.getTags().size, equalTo(0))
    }

    @Test
    fun `single tag via array`() {
        val identity = Identity("name", "foo", "bar")
        assertThat(identity.name, equalTo("name"))
        assertThat(identity.tags.size, equalTo(1))
        assertThat(identity.tags["foo"], equalTo("bar"))
    }

    @Test
    fun `single tag via map`() {
        val identity = Identity("name", mapOf("foo" to "bar"))
        assertThat(identity.name, equalTo("name"))
        assertThat(identity.tags.size, equalTo(1))
        assertThat(identity.tags["foo"], equalTo("bar"))
    }

    @Test
    fun `identity string with tags`() {
        val identity = Identity("name", mapOf("foo" to "bar", "loo" to "moo"))
        assertThat(identity.identityString(), equalTo("name{foo=bar,loo=moo}"))
    }

    @Test
    fun `identity string without tags`() {
        val identity = Identity("name")
        assertThat(identity.identityString(), equalTo("name"))
    }
}
