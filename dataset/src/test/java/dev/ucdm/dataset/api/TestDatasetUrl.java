/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.dataset.api;

import dev.ucdm.dataset.api.DatasetUrl.ServiceType;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/**
 * Test DatasetUrl protocol parsing.
 *
 * @author caron
 * @since 10/20/2015.
 */
public class TestDatasetUrl {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  static final boolean show = true;

  protected void protocheck(String path, String expected) {
    if (expected == null)
      expected = "";

    List<String> protocols = DatasetUrl.getProtocols(path);

    StringBuffer buff = new StringBuffer();
    protocols.forEach(p -> buff.append(p).append(":"));
    String result = buff.toString();
    boolean ok = expected.equals(result);
    if (show || !ok)
      System.out.printf(" %s <- %s%n", result, path);
    if (!ok)
      System.out.printf("  !!!EXPECTED '%s'%n", expected);
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void testGetProtocols() {
    protocheck("http://server/thredds/dodsC/", "http:");
    protocheck("dods://thredds-test.unidata.ucar.edu/thredds/dodsC/grib/NCEP/NAM/CONUS_12km/best", "dods:");
    protocheck("dap4://ucar.edu:8080/x/y/z", "dap4:");
    protocheck("dap4:https://ucar.edu:8080/x/y/z", "dap4:https:");
    protocheck("file:///x/y/z", "file:");
    protocheck("file://c:/x/y/z", "file:");
    protocheck("file:c:/x/y/z", "file:");
    protocheck("file:/blah/blah/some_file_2014-04-13_16:00:00.nc.dds", "file:");
    protocheck("/blah/blah/some_file_2014-04-13_16:00:00.nc.dds", "");
    protocheck("c:/x/y/z", null);
    protocheck("x::a/y/z", null);
    protocheck("x::/y/z", null);
    protocheck("::/y/z", "");
    protocheck("dap4:&/y/z", null);
    protocheck("file:x/z::a", "file:");
    protocheck("x/z::a", null);

    protocheck("thredds:http://localhost:8080/test/addeStationDataset.xml#surfaceHourly", "thredds:http:");
    protocheck("thredds:file:c:/dev/netcdf-java-2.2/test/data/catalog/addeStationDataset.xml#AddeSurfaceData",
        "thredds:file:");
    protocheck("thredds:resolve:http://thredds.ucar.edu:8080/thredds/catalog/model/NCEP/NAM/CONUS_12km/latest.xml",
        "thredds:resolve:http:");
    protocheck("cdmremote:http://server:8080/thredds/cdmremote/data.nc", "cdmremote:http:");
    protocheck(
        "dap4:http://thredds.ucar.edu:8080/thredds/fmrc/NCEP/GFS/CONUS_95km/files/GFS_CONUS_95km_20070319_0600.grib1",
        "dap4:http:");

    protocheck(
        "dynamic:http://thredds.ucar.edu:8080/thredds/fmrc/NCEP/GFS/CONUS_95km/files/GFS_CONUS_95km_20070319_0600.grib1",
        "dynamic:http:");
  }

  protected void testFind(String path, ServiceType expected) throws IOException {
    DatasetUrl result = DatasetUrl.findDatasetUrl(path);
    boolean ok = (expected == null) ? result.getServiceType() == null : expected == result.getServiceType();
    if (show || !ok)
      System.out.printf(" %s <- %s%n", result.getServiceType(), path);
    if (!ok)
      System.out.printf("  !!!EXPECTED '%s'%n", expected);
    assertThat(result.getServiceType()).isEqualTo(expected);
  }

  @Test
  public void problem() throws IOException {
    testFind(
        "dynamic:http://thredds.ucar.edu:8080/thredds/fmrc/NCEP/GFS/CONUS_95km/files/GFS_CONUS_95km_20070319_0600.grib1",
        null);
  }

  @Test
  public void testFindDatasetUrl() throws IOException {
    testFind("file:///x/y/z", null);
    testFind("file://c:/x/y/z", null);
    testFind("file:c:/x/y/z", null);
    testFind("c:/x/y/z", null);
    testFind("x::a/y/z", null);
    testFind("x::/y/z", null);
    testFind("::/y/z", null);
    testFind("dap4:&/y/z", null);
    testFind("file:x/z::a", null);
    testFind("x/z::a", null);

    testFind(
        "dynamic:http://thredds.ucar.edu:8080/thredds/fmrc/NCEP/GFS/CONUS_95km/files/GFS_CONUS_95km_20070319_0600.grib1",
        null);
  }
}
