package dev.ucdm.grib.coord;

import dev.ucdm.array.Indent;
import dev.ucdm.core.calendar.CalendarDate;
import dev.ucdm.core.calendar.CalendarPeriod;
import dev.ucdm.core.util.Counters;
import dev.ucdm.grib.grib2.record.Grib2Record;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;

/**
 * Test {@link CoordinateTime2D}
 */
public class TestCoordinateTime2D {
  static int code = 0;
  static CalendarPeriod timeUnit = CalendarPeriod.of("1 hour");
  static CalendarDate startDate = CalendarDate.fromUdunitIsoDate(null, "1970-01-01T00:00:00").orElseThrow();

  @Test
  public void testCoordinateTime2D() {
    // TODO: orthogonal, regular, and intervals
    CoordinateTime2D subject = makeTimeCoordinate2D(12, 6);
    testShow(subject);
    testCalcDistributions(subject);
  }

  private void testShow(CoordinateTime2D subject) {
    Formatter f = new Formatter();
    subject.showInfo(f, new Indent(2));
    assertThat(f.toString()).contains("runtime=reftime nruns=12 ntimes=6 isOrthogonal=false isRegular=false");
    assertThat(f.toString()).contains("All time values= 0, 1, 2, 3, 4, 5, (n=6)");

    Formatter f2 = new Formatter();
    subject.showCoords(f2);
    assertThat(f2.toString()).contains("runtime=reftime nruns=12 ntimes=6 isOrthogonal=false isRegular=false");
    assertThat(f2.toString()).contains("Time offsets: (1 hours) ref=1970-01-01T00:00Z");
  }

  private void testCalcDistributions(CoordinateTime2D subject) {
    Counters counters = subject.calcDistributions();
    assertThat(counters.toString()).contains("1: count = 5");
  }

  private CoordinateTime2D makeTimeCoordinate2D(int nruns, int ntimes) {
    CoordinateRuntime.Builder2 runBuilder = new CoordinateRuntime.Builder2(timeUnit);
    Map<Object, CoordinateBuilderImpl<Grib2Record>> timeBuilders = new HashMap<>();

    List<CoordinateTime2D.Time2D> vals = new ArrayList<>(nruns * ntimes);
    for (int j = 0; j < nruns; j++) {
      CalendarDate runDate = startDate.add(j, CalendarPeriod.Field.Hour);
      for (int i = 0; i < ntimes; i++) {
        CoordinateTime2D.Time2D time2D = new CoordinateTime2D.Time2D(runDate, (long) i, null);
        vals.add(time2D);

        runBuilder.add(time2D.refDate);
        CoordinateBuilderImpl<Grib2Record> timeBuilder = timeBuilders.get(time2D.refDate);
        if (timeBuilder == null) {
          timeBuilder = new CoordinateTime.Builder2(code, timeUnit, time2D.getRefDate());
          timeBuilders.put(time2D.refDate, timeBuilder);
        }
        timeBuilder.add(time2D.time);
      }
    }
    CoordinateRuntime runCoord = (CoordinateRuntime) runBuilder.finish();

    List<Coordinate> times = new ArrayList<>(runCoord.getSize());
    for (int idx = 0; idx < runCoord.getSize(); idx++) {
      long runtime = runCoord.getRuntime(idx);
      CoordinateBuilderImpl<Grib2Record> timeBuilder = timeBuilders.get(runtime);
      times.add(timeBuilder.finish());
    }
    Collections.sort(vals);

    return new CoordinateTime2D(code, timeUnit, vals, runCoord, times, null);
  }
}
