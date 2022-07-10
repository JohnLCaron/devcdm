package dev.ucdm.grib.coord;

import dev.cdm.core.calendar.CalendarDate;
import dev.cdm.core.calendar.CalendarPeriod;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

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

