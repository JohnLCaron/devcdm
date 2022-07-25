package dev.ucdm.grib.common;

import dev.ucdm.core.constants.CF;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

public class TestGribStatType {
  private Set<GribStatType> noCF = Set.of(GribStatType.DifferenceFromEnd, GribStatType.DifferenceFromStart,
          GribStatType.RootMeanSquare, GribStatType.Ratio);
  @Test
  public void testGribStatType() {
    for (GribStatType stat : GribStatType.values()) {
      CF.CellMethods cf = GribStatType.getCFCellMethod(stat);
      if (!noCF.contains(stat)) {
        assertWithMessage(stat.name()).that(cf).isNotNull();
      }
    }
  }

  @Test
  public void testGribStatCode() {
      for (int grib2StatCode = 0; grib2StatCode < 10; grib2StatCode++) {
        GribStatType stat = GribStatType.getStatTypeFromGrib2(grib2StatCode);
        assertThat(stat).isNotNull();
        assertWithMessage(stat.name()).that(GribStatType.getStatTypeNumber(stat.name())).isEqualTo(grib2StatCode);
      }
  }
}
