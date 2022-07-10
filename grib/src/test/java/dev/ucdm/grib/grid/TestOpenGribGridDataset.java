/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.grid;

import org.junit.jupiter.api.Test;
import static dev.cdm.test.util.TestFilesKt.oldTestDir;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;
import static dev.cdm.test.util.TestFilesKt.coreLocalDir;
import static org.junit.Assert.fail;

/** Test {@link GribGridDataset} */
public class TestOpenGribGridDataset {

  @Test
  public void testTwod() throws IOException {
    String endpoint = oldTestDir + "tds_index/NCEP/NDFD/NWS/NDFD_NWS_CONUS_CONDUIT.ncx4";
    String gridName = "Total_precipitation_surface_Mixed_intervals_Accumulation_probability_above_0p254";
    TestGridGribDataset.testOpen(endpoint, gridName, new int[] {1479, 15}, new int[] {}, new int[] {1377, 2145});
  }

  @Test
  public void testTwodWhyNotMRUTP() throws IOException {
    // TODO why not MRUTP?
    String endpoint = oldTestDir + "tds_index/NCEP/NDFD/CPC/NDFD_CPC_CONUS_CONDUIT.ncx4";
    String gridName = "Temperature_surface_6_Day_Average_probability_below_0";
    TestGridGribDataset.testOpen(endpoint, gridName, new int[] {51, 1}, new int[] {}, new int[] {1377, 2145});
  }

  @Test
  public void testTwodRegular() throws IOException {
    String endpoint = oldTestDir + "tds_index/NCEP/NBM/Ocean/NCEP_OCEAN_MODEL_BLEND.ncx4";
    String gridName = "Wind_speed_height_above_ground";
    TestGridGribDataset.testOpen(endpoint, gridName, new int[] {59, 75}, new int[] {1}, new int[] {1817, 2517});
  }

  @Test
  public void testTwodOrthogonal() throws IOException {
    String endpoint = oldTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4";
    String gridName = "Ozone_Mixing_Ratio_isobaric";
    TestGridGribDataset.testOpen(endpoint, gridName, new int[] {4, 93}, new int[] {12}, new int[] {73, 144});
  }

  @Test
  public void testMRUTC() throws IOException {
    String endpoint = oldTestDir + "tds_index/NCEP/MRMS/Radar/MRMS_Radar_20201027_0000.grib2.ncx4";
    String gridName = "VIL_altitude_above_msl";
    TestGridGribDataset.testOpen(endpoint, gridName, new int[] {30}, new int[] {1}, new int[] {3500, 7000});
  }

  @Test
  public void testMRUTP() throws IOException {
    String endpoint = oldTestDir + "tds_index/NCEP/MRMS/Radar/MRMS-Radar.ncx4";
    String gridName = "MESHMax1440min_altitude_above_msl";
    TestGridGribDataset.testOpen(endpoint, gridName, new int[] {1476}, new int[] {1}, new int[] {3500, 7000});
  }

  @Test
  public void testSRC() throws IOException {
    String endpoint = oldTestDir + "tds_index/NCEP/NAM/CONUS_80km/NAM_CONUS_80km_20201027_0000.grib1.ncx4";
    String gridName = "Temperature_isobaric";
    TestGridGribDataset.testOpen(endpoint, gridName, new int[] {1, 11}, new int[] {19}, new int[] {65, 93});
  }

  @Test
  public void testEns() throws IOException {
    String filename = oldTestDir + "ft/grid/ensemble/jitka/MOEASURGEENS20100709060002.grib";
    String gridName = "VAR10-3-192_FROM_74-0--1_surface_ens";
    TestGridGribDataset.testOpen(filename, gridName, new int[] {1, 477}, new int[] {1}, new int[] {207, 198});
  }

  @Test
  public void testEns2() throws IOException {
    String filename = oldTestDir + "ft/grid/ensemble/jitka/ECME_RIZ_201201101200_00600_GB.ncx4";
    String gridName = "Total_precipitation_surface";
    TestGridGribDataset.testOpen(filename, gridName, new int[] {1, 1}, new int[] {51}, new int[] {21, 31});
  }

  @Test
  public void testFileNotFound() throws IOException {
    String filename = coreLocalDir + "conventions/fileNot.nc";
    Formatter errlog = new Formatter();
    try (GribGridDataset gds = GribGridDataset.open(filename, errlog)) {
      assertThat(gds).isNull();
      fail();
    } catch (FileNotFoundException e) {
      assertThat(e.getMessage()).contains("(No such file or directory)");
    }
  }

  @Test
  public void testFileNotGrid() throws IOException {
    String filename = coreLocalDir + "point/point.ncml";
    Formatter errlog = new Formatter();
    try (GribGridDataset gds = GribGridDataset.open(filename, errlog)) {
      assertThat(gds).isNull();
    }
  }
}
