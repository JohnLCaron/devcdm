package cdm.dataset.coordsysbuild

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class TestKotlin {

    @Test
    fun testSplit() {
        val trailingSpace = "one two three "
        assertThat(trailingSpace.split(" ").size).isEqualTo(4)
        assertThat(trailingSpace.trim().split(" ").size).isEqualTo(3)
    }
}