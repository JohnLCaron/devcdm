/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.cdm.grib.grid;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import dev.cdm.array.Arrays;
import dev.cdm.array.InvalidRangeException;
import dev.cdm.core.calendar.CalendarDate;
import dev.cdm.grid.api.Grid;
import dev.cdm.grid.api.GridAxis;
import dev.cdm.grid.api.GridAxisPoint;
import dev.cdm.grid.api.GridAxisSpacing;
import dev.cdm.grid.api.GridCoordinateSystem;
import dev.cdm.grid.api.GridDataset;
import dev.cdm.grid.api.GridDatasetFactory;
import dev.cdm.grid.api.GridReferencedArray;
import dev.cdm.grid.api.GridTimeCoordinateSystem;
import dev.cdm.grid.api.MaterializedCoordinateSystem;

import java.io.IOException;
import java.util.Formatter;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

public class TestReadGribGridSubset {

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testTimeOffsetRegular() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "tds_index/NCEP/NDFD/SPC/NDFD_SPC_CONUS_CONDUIT.ncx4";

    Formatter infoLog = new Formatter();
    try (GridDataset gridDataset = GridDatasetFactory.openGridDataset(filename, infoLog)) {
      assertThat(gridDataset).isNotNull();
      System.out.println("readGridDataset: " + gridDataset.getLocation());
      // float Convective_Hazard_Outlook_surface_24_hours_Average(
      Grid grid = gridDataset.findGrid("Convective_Hazard_Outlook_surface_24_Hour_Average")
          .orElseThrow(() -> new RuntimeException("Cant find grid"));

      GridCoordinateSystem csys = grid.getCoordinateSystem();
      GridTimeCoordinateSystem tsys = csys.getTimeCoordinateSystem();
      assertThat(tsys).isNotNull();
      GridAxisPoint runtimeAxis = tsys.getRunTimeAxis();
      assertThat((Object) runtimeAxis).isNotNull();
      assertThat(runtimeAxis.getNominalSize()).isGreaterThan(10);
      CalendarDate wantRuntime = tsys.getRuntimeDate(10);
      assertThat(wantRuntime).isNotNull();

      GridAxis<?> timeOffset = tsys.getTimeOffsetAxis(10);
      assertThat((Object) timeOffset).isNotNull();
      assertThat(timeOffset.getSpacing()).isEqualTo(GridAxisSpacing.regularInterval);
      assertThat(timeOffset.getNominalSize()).isGreaterThan(3);
      Object wantTime = timeOffset.getCoordinate(3);
      assertThat(wantTime).isNotNull();
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testTimeOffset() throws IOException, InvalidRangeException {
    String filename = TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4";

    Formatter infoLog = new Formatter();
    try (GridDataset gridDataset = GridDatasetFactory.openGridDataset(filename, infoLog)) {
      assertThat(gridDataset).isNotNull();
      System.out.println("readGridDataset: " + gridDataset.getLocation());

      Grid grid =
          gridDataset.findGrid("Sunshine_Duration_surface").orElseThrow(() -> new RuntimeException("Cant find grid"));

      GridCoordinateSystem csys = grid.getCoordinateSystem();
      GridTimeCoordinateSystem tsys = csys.getTimeCoordinateSystem();
      assertThat(tsys).isNotNull();
      GridAxisPoint runtimeAxis = tsys.getRunTimeAxis();
      assertThat((Object) runtimeAxis).isNotNull();
      assertThat(runtimeAxis.getNominalSize()).isEqualTo(4);
      CalendarDate wantRuntime = tsys.getRuntimeDate(1);

      GridAxis<?> timeOffset = tsys.getTimeOffsetAxis(1);
      assertThat((Object) timeOffset).isNotNull();
      assertThat(timeOffset.getSpacing()).isEqualTo(GridAxisSpacing.irregularPoint);
      assertThat(timeOffset.getNominalSize()).isEqualTo(93);
      Object wantTime = timeOffset.getCoordinate(66);

      GridReferencedArray geoArray = grid.getReader().setRunTime(wantRuntime).setTimeOffsetCoord(wantTime).read();
      testGeoArray(geoArray, 2, wantRuntime, wantTime);
    }
  }

  private void testGeoArray(GridReferencedArray geoArray, int expected, CalendarDate wantRuntime, Object wantTime) {
    assertThat(Arrays.reduce(geoArray.data()).getRank()).isEqualTo(expected);
    int[] dataShape = geoArray.data().getShape();

    MaterializedCoordinateSystem mcsys = geoArray.materializedCoordinateSystem();
    List<Integer> mshapes = mcsys.getMaterializedShape();
    int count = 0;
    for (int mshape : mshapes) {
      assertThat(mshape).isEqualTo(dataShape[count]);
      count++;
    }

    count = 0;
    for (GridAxis<?> axis : mcsys.getGridAxes()) {
      assertThat(axis.getNominalSize()).isEqualTo(dataShape[count]);
      count++;
    }
  }
}
