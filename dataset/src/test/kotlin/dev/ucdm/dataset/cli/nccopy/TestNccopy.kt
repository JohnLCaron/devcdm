package dev.ucdm.dataset.cli.nccopy

import org.junit.jupiter.api.Test


class TestNccopy {

    @Test
    fun testNccopy() {
        main(
            arrayOf(
                "-in", "barf",
                "-out", "barf",
                "--help"
            )
        )
    }

}