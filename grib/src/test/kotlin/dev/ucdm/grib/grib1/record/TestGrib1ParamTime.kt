package dev.ucdm.grib.grib1.record

import com.google.common.truth.Truth
import dev.ucdm.core.api.CdmFile
import dev.ucdm.grib.common.GribStatType
import dev.ucdm.grib.grib1.table.Grib1Customizer
import dev.ucdm.grib.grib1.table.Grib1ParamTables
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.IOException
import java.util.stream.Stream

class TestGrib1ParamTime {
    val cust = Grib1Customizer.factory(0, 0, 0, Grib1ParamTables())

    companion object {
        @Throws(IOException::class)
        @JvmStatic
        fun params(): Stream<Arguments?>? {
            return Stream.of(
                // indicate, forecast, intvSize, stat, timeCoord
                Arguments.of(0, 7, null, null, "7"),
                Arguments.of(1, 0, null, null, "0"),
                Arguments.of(2, 0, intArrayOf(7, 11), null, "7-11"),
                Arguments.of(3, 0, intArrayOf(7, 11), GribStatType.Average, "7-11"),
                Arguments.of(5, 0, intArrayOf(7, 11), GribStatType.DifferenceFromEnd, "7-11"),
                Arguments.of(6, 0, intArrayOf(-7, -11), GribStatType.Average, "-7--11"),
                Arguments.of(7, 0, intArrayOf(-7, 11), GribStatType.Average, "-7-11"),
                Arguments.of(10, 1803, null, null, "1803"),
                Arguments.of(51, 11, null, GribStatType.Average, "11"),
                Arguments.of(113, 7, intArrayOf(7, 40), GribStatType.Average, "7-40"),
                Arguments.of(114, 7, intArrayOf(7, 40), GribStatType.Accumulation, "7-40"),
                Arguments.of(115, 7, intArrayOf(7, 40), GribStatType.Average, "7-40"),
                Arguments.of(116, 7, intArrayOf(7, 40), GribStatType.Accumulation, "7-40"),
                Arguments.of(117, 0, intArrayOf(0, 7), GribStatType.Average, "0-7"),
                Arguments.of(118, 0, intArrayOf(0, 44), GribStatType.Covariance, "0-44"),
                Arguments.of(119, 0, intArrayOf(7, 51), GribStatType.StandardDeviation, "7-51"),

                Arguments.of(120, 0, intArrayOf(7, 11), GribStatType.Average, "7-11"),
                Arguments.of(123, 0, intArrayOf(0, 44), GribStatType.Average, "0-44"),
                Arguments.of(124, 0, intArrayOf(0, 44), GribStatType.Accumulation, "0-44"),
                Arguments.of(125, 0, intArrayOf(0, 51), GribStatType.StandardDeviation, "0-51"),
            )
        }
    }

    @ParameterizedTest
    @MethodSource("params")
    fun testGrib1ParamTime(indi: Int, forecastTime: Int, interval: IntArray?, statType : GribStatType?,
        timeCoord: String) {

        val pds = mockk<Grib1SectionProductDefinition>()
        every { pds.getTimeValue1() } returns 7
        every { pds.getTimeValue2() } returns 11
        every { pds.getTimeRangeIndicator() } returns indi
        every { pds.getNincluded() } returns 4

        val pt = Grib1ParamTime.factory(cust, pds)
        Truth.assertThat(pt.cust).isEqualTo(cust)
        Truth.assertThat(pt.forecastTime).isEqualTo(forecastTime)
        Truth.assertThat(pt.isInterval).isEqualTo(interval != null)
        if (interval != null) {
            Truth.assertThat(pt.interval).isEqualTo(interval)
            Truth.assertThat(pt.interval.size).isEqualTo(2)
            Truth.assertThat(pt.intervalSize).isEqualTo(interval[1] - interval[0])
        }
        Truth.assertThat(pt.statType).isEqualTo(statType)
        Truth.assertThat(pt.timeCoord).isEqualTo(timeCoord)
        Truth.assertThat(pt.timeTypeName).isEqualTo(Grib1ParamTime.getTimeTypeName(indi))
        Truth.assertThat(pt.timeTypeName).isEqualTo(Grib1ParamTime.getTimeTypeName(indi))

        println("${indi} ${pt.timeTypeName} = '${pt.timeCoord}'")
    }
}