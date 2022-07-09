package dev.cdm.grib.coord;

import dev.cdm.grib.coord.TimeCoordIntvDateValue;
import dev.cdm.grib.coord.TimeCoordIntvValue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import dev.cdm.core.calendar.CalendarDate;
import dev.cdm.core.calendar.CalendarPeriod;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public class TestTimeCoord {

  @Test
  public void testTinvDate() {
    CalendarDate start = CalendarDate.of(1269820799000L);
    CalendarDate end = CalendarDate.of(1269824399000L);
    TimeCoordIntvDateValue tinvDate = new TimeCoordIntvDateValue(start, end);
    System.out.printf("tinvDate = %s%n", tinvDate);
    assertThat(tinvDate.toString()).isEqualTo("(2010-03-28T23:59:59Z,2010-03-29T00:59:59Z)");

    CalendarDate refDate = CalendarDate.of(1269820800000L);
    CalendarPeriod timeUnit = CalendarPeriod.of("Hour");

    TimeCoordIntvValue tinv = tinvDate.convertReferenceDate(refDate, timeUnit);
    System.out.printf("tinv = %s offset from %s%n", tinv, refDate);
    assertThat(refDate.toString()).isEqualTo("2010-03-29T00:00Z");
  }

}

