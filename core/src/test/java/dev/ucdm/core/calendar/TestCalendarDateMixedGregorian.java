package dev.ucdm.core.calendar;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;

/**
 * Test {@link CalendarDateUnit}
 * These test difference of mixed julian/gregorian vs proleptic gregorian.
 */
public class TestCalendarDateMixedGregorian {

  public static Stream<Arguments> params() {
    return Stream.of(
            Arguments.of("ChangeoverDate", null, "secs since 1582-10-01"),
            Arguments.of("ChangeoverDate", null, "secs since 1582-10-02"),
            Arguments.of("ChangeoverDate", null, "secs since 1582-10-03"),
            Arguments.of("ChangeoverDate", null, "secs since 1582-10-04"),
            Arguments.of("yearZero", null, "secs since 0000-01-01"),
            Arguments.of("yearZero", null, "secs since 0001-01-01"),
            Arguments.of("yearZero", null, "secs since -0001-01-01"),
            Arguments.of("yearZero", "gregorian", "secs since 0001-01-01"),
            Arguments.of("yearZero", "gregorian", "secs since -0001-01-01"),
            // UNIT since [-]Y[Y[Y[Y]]]-MM-DD[(T| )hh[:mm[:ss[.sss*]]][ [+|-]hh[[:]mm]]]
            Arguments.of("UDUnitsSeconds", null, "secs since 1992-10-8 15:15:42.534"), // udunit only accepts 2 decimals in seconds
            Arguments.of("UDUnits", null, "secs since 199-10-8"),
            Arguments.of("UDUnits", null, "secs since 19-10-8"),
            Arguments.of("UDUnits", null, "secs since 1-10-8"),
            Arguments.of("UDUnits", null, "secs since +1101-10-8"),
            Arguments.of("UDUnits", null, "secs since -1101-10-8"));
  }

  @ParameterizedTest
  @MethodSource("params")
  public void testBase(String category, String calendar, String datestring) {
    CalendarDateUnit base = CalendarDateUnit.fromUdunitString(null, datestring).orElseThrow();;
    Calendar cal = Calendar.get(calendar).orElse(Calendar.getDefault());
    CalendarDateUnit cdu = CalendarDateUnit.fromUdunitString(cal, datestring).orElseThrow();
    // assertThat(cdu.getBaseDateTime()).isNotEqualTo(base.getBaseDateTime());
    System.out.printf("%s: CalendarDateUnit '%s' vs udunits '%s'%n", category, cdu.getBaseDateTime(), base.getBaseDateTime());
  }

}
