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

    @Test
    // 1 3 done (continue)
    fun testContinue() {
        listOf(1, 2, 3).forEach {
            if (it == 2) return@forEach
            println(it)
        }
        println("done")
    }

    @Test
    // 1 2 (exit fun)
    fun testExitFun() {
        listOf(1, 2, 3).forEach {
            if (it == 3) return
            println(it)
        }
        println("done")
    }

    @Test
    // 1 2 done (same as forEach)
    fun testReturnLabel() {
        listOf(1, 2, 3).forEach outer@ {
            if (it == 3) return@outer
            println(it)
        }
        println("done loop")
    }

    @Test
    // nothing
    fun testReturnLet() {
        listOf(1, 2, 3).let {
            println("inside loop")
            if (it.size == 3) return
        }
        println("exit loop")
    }

    @Test
    // inside 1 inside 2 inside (exits function)
    fun testReturnLetExitFun() {
        listOf(1, 2, 3, 4).forEach {
            print("inside ")
            it?.let {
                if (it == 3) return
                println(it)
            }
        }
        println("exit loop") // doesnt happen
    }

    @Test
    // inside 1 inside inside 2 exit (continue)
    fun testBreak() {
        listOf(1, 2, 3).forEach label@{
            print("inside ")
            it?.let {
                if (it == 2) return@label
                println(it)
            }
        }
        println("exit loop") //happens
    }
}