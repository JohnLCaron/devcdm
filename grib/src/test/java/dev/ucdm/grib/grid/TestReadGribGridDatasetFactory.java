/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.grid;

import dev.cdm.array.MinMax;
import dev.cdm.grid.api.*;
import org.junit.jupiter.api.Test;
import static dev.cdm.test.util.TestFilesKt.oldTestDir;

import java.io.IOException;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

/** Test reading Grib through {@link GridDatasetFactory} */
public class TestReadGribGridDatasetFactory {

  @Test
  public void testCsysTooManyAxes() throws IOException {
    String filename = oldTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4";
    System.out.printf("filename %s%n", filename);

    Formatter errlog = new Formatter();
    try (GridDataset gridDataset = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gridDataset).isNotNull();
      assertThat(gridDataset.getGridCoordinateSystems()).hasSize(23);
      assertThat(gridDataset.getGridAxes()).hasSize(25);
      assertThat(gridDataset.getGrids()).hasSize(135);
      for (GridCoordinateSystem csys : gridDataset.getGridCoordinateSystems()) {
        assertThat(csys.getGridAxes().size()).isLessThan(6);
      }
    }
  }

  @Test
  public void testOneProblem() throws IOException {
    String filename = oldTestDir + "tds_index/NCEP/NDFD/SPC/NDFD_SPC_CONUS_CONDUIT.ncx4";
    System.out.printf("filename %s%n", filename);

    Formatter errlog = new Formatter();
    try (GridDataset gridDataset = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gridDataset).isNotNull();
      assertThat(gridDataset.getGridCoordinateSystems()).hasSize(4);
      assertThat(gridDataset.getGridAxes()).hasSize(10);
      assertThat(gridDataset.getGrids()).hasSize(4);
    }
  }

  @Test
  public void testRegularIntervalCoordinate() throws IOException {
    String filename = oldTestDir + "tds_index/NCEP/NDFD/SPC/NDFD_SPC_CONUS_CONDUIT.ncx4";
    System.out.printf("filename %s%n", filename);

    Formatter errlog = new Formatter();
    try (GridDataset gridDataset = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gridDataset).isNotNull();
      Grid grid = gridDataset.findGrid("Convective_Hazard_Outlook_surface_24_Hour_Average")
          .orElseThrow(() -> new RuntimeException("Cant find grid"));
      GridCoordinateSystem csys = grid.getCoordinateSystem();
      GridTimeCoordinateSystem tcsys = csys.getTimeCoordinateSystem();
      assertThat(tcsys).isNotNull();
      GridAxis<?> timeAxis = tcsys.getTimeOffsetAxis(0);
      assertThat((Object) timeAxis).isNotNull();
      assertThat((Object) timeAxis).isInstanceOf(GridAxisInterval.class);
      GridAxisInterval timeAxis1D = (GridAxisInterval) timeAxis;

      assertThat(timeAxis1D.getSpacing()).isEqualTo(GridAxisSpacing.regularInterval);
      assertThat(timeAxis1D.getNominalSize()).isEqualTo(3);
      double[] expected = new double[] {-13, 11, 35, 59};
      for (int i = 0; i < timeAxis1D.getNominalSize(); i++) {
        CoordInterval intv = timeAxis1D.getCoordInterval(i);
        assertThat(intv.start()).isEqualTo(expected[i]);
        assertThat(intv.end()).isEqualTo(expected[i + 1]);
        assertThat(timeAxis1D.getCoordDouble(i)).isEqualTo((expected[i] + expected[i + 1]) / 2);
      }
      MinMax maxmin = Grids.getCoordEdgeMinMax(timeAxis1D);
      assertThat(maxmin.min()).isEqualTo(expected[0]);
      assertThat(maxmin.max()).isEqualTo(expected[3]);
    }
  }

  @Test
  public void testIrregularPointCoordinate() throws IOException {
    String filename =
        oldTestDir + "gribCollections/rdavm/ds083.2/PofP/2004/200406/ds083.2-pofp-200406.ncx4";
    System.out.printf("filename %s%n", filename);

    Formatter errlog = new Formatter();
    try (GridDataset gridDataset = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gridDataset).isNotNull();
      Grid grid =
          gridDataset.findGrid("Ozone_mixing_ratio_isobaric").orElseThrow(() -> new RuntimeException("Cant find grid"));
      GridCoordinateSystem csys = grid.getCoordinateSystem();
      GridAxis<?> vertAxis = csys.getVerticalAxis();
      assertThat((Object) vertAxis).isNotNull();
      assertThat(vertAxis.getSpacing()).isEqualTo(GridAxisSpacing.irregularPoint);
      int ncoords = 6;
      assertThat(vertAxis.getNominalSize()).isEqualTo(ncoords);
      double[] expected = new double[] {10.000000, 20.000000, 30.000000, 50.000000, 70.000000, 100.000000};
      double[] bounds = new double[] {5, 15.000000, 25.000000, 40.000000, 60.000000, 85.000000, 115.000000};
      for (int i = 0; i < vertAxis.getNominalSize(); i++) {
        assertThat(vertAxis.getCoordDouble(i)).isEqualTo(expected[i]);
        CoordInterval intv = vertAxis.getCoordInterval(i);
        assertThat(intv.start()).isEqualTo(bounds[i]);
        assertThat(intv.end()).isEqualTo(bounds[i + 1]);
      }
    }
  }

  @Test
  public void testDiscontiguousIntervalCoordinate() throws IOException {
    String filename = oldTestDir + "tds_index/NCEP/NDFD/NWS/NDFD_NWS_CONUS_CONDUIT.ncx4";
    String gname = "Maximum_temperature_height_above_ground_Mixed_intervals_Maximum";
    System.out.printf("filename %s%n", filename);

    Formatter errlog = new Formatter();
    try (GridDataset gridDataset = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gridDataset).isNotNull();
      Grid grid = gridDataset.findGrid(gname).orElseThrow();
      GridCoordinateSystem csys = grid.getCoordinateSystem();
      GridTimeCoordinateSystem tcsys = csys.getTimeCoordinateSystem();
      assertThat(tcsys).isNotNull();

      GridAxis<?> timeAxis = tcsys.getTimeOffsetAxis(0);
      assertThat((Object) timeAxis).isNotNull();
      assertThat((Object) timeAxis).isInstanceOf(GridAxisInterval.class);
      assertThat(timeAxis.getSpacing()).isEqualTo(GridAxisSpacing.discontiguousInterval);
      GridAxisInterval timeAxisIntv = (GridAxisInterval) timeAxis;

      double[] bounds1 = new double[] {12, 36, 60, 84, 108, 132, 156};
      double[] bounds2 = new double[] {24, 48, 72, 96, 120, 144, 168};

      for (int i = 0; i < timeAxisIntv.getNominalSize(); i++) {
        CoordInterval intv = timeAxisIntv.getCoordInterval(i);
        assertThat(intv.start()).isEqualTo(bounds1[i]);
        assertThat(intv.end()).isEqualTo(bounds2[i]);
        assertThat(timeAxisIntv.getCoordDouble(i)).isEqualTo((bounds1[i] + bounds2[i]) / 2);
      }

      int count = 0;
      for (CoordInterval val : timeAxisIntv) {
        assertThat(val).isEqualTo(new CoordInterval(bounds1[count], bounds2[count]));
        count++;
      }
    }
  }

}
