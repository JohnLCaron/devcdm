/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.cdm.grib.grid;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import dev.cdm.core.calendar.CalendarDate;
import dev.cdm.grid.api.CoordInterval;
import dev.cdm.grid.api.Grid;
import dev.cdm.grid.api.GridAxis;
import dev.cdm.grid.api.GridAxisInterval;
import dev.cdm.grid.api.GridAxisSpacing;
import dev.cdm.grid.api.GridCoordinateSystem;
import dev.cdm.grid.api.GridTimeCoordinateSystem;

import java.io.IOException;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

/** Compare reading Grid Axes through new and old GridDataset. */
public class TestGribGridDatasetAxes {

  @Test
  public void testTimeOffsetRegular() throws IOException {
    String endpoint = TestDir.cdmUnitTestDir + "tds_index/NCEP/NDFD/SPC/NDFD_SPC_CONUS_CONDUIT.ncx4";

    Formatter errlog = new Formatter();
    try (GribGridDataset gds = GribGridDataset.open(endpoint, errlog).orElse(null)) {
      assertThat(gds).isNotNull();
      System.out.println("readGridDataset: " + gds.getLocation());
      // float Convective_Hazard_Outlook_surface_24_hours_Average(
      Grid grid = gds.findGrid("Convective_Hazard_Outlook_surface_24_Hour_Average")
          .orElseThrow(() -> new RuntimeException("Cant find grid"));

      GridCoordinateSystem csys = grid.getCoordinateSystem();
      GridTimeCoordinateSystem tsys = csys.getTimeCoordinateSystem();
      assertThat(tsys).isNotNull();
      assertThat(tsys.getType()).isEqualTo(GridTimeCoordinateSystem.Type.OffsetRegular);

      GridAxis<?> runtimeAxis = tsys.getRunTimeAxis();
      assertThat((Object) runtimeAxis).isNotNull();
      assertThat(runtimeAxis.getNominalSize()).isGreaterThan(10);

      GridAxis<?> timeOffset = tsys.getTimeOffsetAxis(10);
      assertThat((Object) timeOffset).isNotNull();
      assertThat(timeOffset.getSpacing()).isEqualTo(GridAxisSpacing.regularInterval);
      CalendarDate wantRuntime = tsys.getRuntimeDate(10);

      assertThat(timeOffset.getNominalSize()).isGreaterThan(1);
      assertThat((Object) timeOffset).isInstanceOf(GridAxisInterval.class);
      GridAxisInterval toAxisItv = (GridAxisInterval) timeOffset;
      CoordInterval coord = toAxisItv.getCoordInterval(1);

      grid.getReader().setRunTime(wantRuntime).setTimeOffsetCoord(coord);
    }
  }

  @Test
  public void testGridWithMonthUnits() throws IOException {
    String endpoint = TestDir.cdmUnitTestDir + "formats/grib1/cfs.wmo";
    String vname = "Volumetric_soil_moisture_content_layer_between_two_depths_below_surface_layer_1_Month_Average";

    Formatter errlog = new Formatter();
    try (GribGridDataset gds = GribGridDataset.open(endpoint, errlog).orElse(null)) {
      assertThat(gds).isNotNull();
      Grid grid = gds.findGrid(vname).orElseThrow();
      assertThat(grid).isInstanceOf(GribGrid.class);

      GridCoordinateSystem gdc = grid.getCoordinateSystem();
      GridAxis<?> timeAxis = gdc.findAxis("time").orElseThrow();
      System.out.printf("timeAxis=%s%n", timeAxis.getUnits());
      GridAxis<?> runtimeAxis = gdc.findAxis("reftime").orElseThrow();
      System.out.printf("runtimeAxis=%s%n", runtimeAxis.getUnits());
    }
  }

}
