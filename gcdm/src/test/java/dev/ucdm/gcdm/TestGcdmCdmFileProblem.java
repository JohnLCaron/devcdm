/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.gcdm;

import static com.google.common.truth.Truth.assertThat;

import dev.ucdm.core.api.CdmFile;
import dev.ucdm.core.util.CompareCdmFiles;
import dev.ucdm.dataset.api.CdmDatasets;
import dev.ucdm.dataset.util.CompareCdmDataset;
import org.junit.Test;

import dev.ucdm.gcdm.client.GcdmCdmFile;

import java.nio.file.Path;
import java.nio.file.Paths;

/** Test {@link GcdmCdmFile} problems */
public class TestGcdmCdmFileProblem {

  @Test
  public void testProblem() throws Exception {
    String localFilename = TestGcdmDatasets.coreLocalDir + "netcdf4/IntTimSciSamp.nc";
    Path path = Paths.get(localFilename);
    compareArrayToArray(path, "tim_records");
  }

  /*
   * ushort EV_1KM_RefSB(Band_1KM_RefSB=15, 10*nscans=2030, Max_EV_frames=1354);
   * 
   * GcdmServer getData
   * /media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/hdf4/MOD021KM.A2004328.
   * 1735.004.2004329164007.hdf
   * MODIS_SWATH_Type_L1B/Data_Fields/EV_1KM_RefSB(0:14, 0:2029, 0:1353)
   * Send one chunk MODIS_SWATH_Type_L1B/Data_Fields/EV_1KM_RefSB(0:8, 0:2029, 0:1353) size=49475160 bytes
   * Send one chunk MODIS_SWATH_Type_L1B/Data_Fields/EV_1KM_RefSB(9:14, 0:2029, 0:1353) size=32983440 bytes
   ** size=82,458,600 took=1.461 s
   * 
   * WARNING: readSection requestData failed failed:
   * io.grpc.StatusRuntimeException: RESOURCE_EXHAUSTED: gRPC message exceeds maximum size 51000000: 99265809
   * at io.grpc.Status.asRuntimeException(Status.java:535)
   * at io.grpc.stub.ClientCalls$BlockingResponseStream.hasNext(ClientCalls.java:648)
   * at dev.cdm.gcdm.protogen.client.GcdmCdmFile.readArrayData(GcdmCdmFile.java:85)
   * at ucar.nc2.Variable.proxyReadArray(Variable.java:829)
   * at ucar.nc2.Variable.readArray(Variable.java:738)
   * ...
   * 
   * what is 99265809 here? This implies my calculation of the data size is seriously wrong.
   * Temp fix is to put MAX = 101 Mbytes.
   * Confirmed this is an artifact of unsigned short, which doesnt have a direct protobug type, so we use uint32.
   * Ratios are sometimes ~2. see GcdmConverter.debugSize.
   * GcdmNetcdfProto.Data nelems = 24737580 type=ushort expected size =49475160 actual = 99265523 ratio = 2.006371
   * 
   */
  @Test
  public void testRequestTooBig() throws Exception {
    String localFilename = TestGcdmDatasets.testDir + "formats/hdf4/MOD021KM.A2004328.1735.004.2004329164007.hdf";
    Path path = Paths.get(localFilename);
    compareArrayToArray(path, "MODIS_SWATH_Type_L1B/Data_Fields/EV_1KM_RefSB");
  }

  @Test
  public void testRequestNotTooBig() throws Exception {
    String localFilename = TestGcdmDatasets.testDir + "formats/hdf4/MOD021KM.A2004328.1735.004.2004329164007.hdf";
    Path path = Paths.get(localFilename);
    compareArrayToArray(path, "MODIS_SWATH_Type_L1B/Data_Fields/EV_500_Aggr1km_RefSB_Samples_Used");
  }

  @Test
  public void testHdf4() throws Exception {
    String localFilename = TestGcdmDatasets.testDir + "formats/hdf4/MOD021KM.A2004328.1735.004.2004329164007.hdf";
    Path path = Paths.get(localFilename);
    compareArrayToArray(path, "MODIS_SWATH_Type_L1B/Data_Fields/EV_250_Aggr1km_RefSB");
  }

  @Test
  public void testCombineStructure() throws Exception {
    String localFilename = TestGcdmDatasets.testDir + "formats/hdf5/IASI.h5";
    Path path = Paths.get(localFilename);
    compareArrayToArray(path, "U-MARF/EPS/IASI_xxx_1C/DATA/MDR_1C_IASI_L1_ARRAY_000001");
  }

  @Test
  public void testGcdmVlenCast() throws Exception {
    String localFilename = TestGcdmDatasets.coreLocalDir + "netcdf4/tst_opaque_data.nc4";
    Path path = Paths.get(localFilename);
    compareArrayToArray(path);
  }

  @Test
  public void testGcdmProblemNeeds() throws Exception {
    String localFilename =
        TestGcdmDatasets.testDir + "formats/netcdf4/e562p1_fp.inst3_3d_asm_Nv.20100907_00z+20100909_1200z.nc4";
    Path path = Paths.get(localFilename);
    compareArrayToArray(path, "O3");
  }

  @Test
  public void testDataTooLarge() throws Exception {
    String localFilename = TestGcdmDatasets.testDir + "formats/netcdf4/UpperDeschutes_t4p10_swemelt.nc";
    Path path = Paths.get(localFilename);
    compareArrayToArray(path);
  }

  @Test
  public void testAttributeStruct() throws Exception {
    String localFilename = TestGcdmDatasets.coreLocalDir + "netcdf4/attributeStruct.nc";
    Path path = Paths.get(localFilename);
    compareArrayToArray(path, "observations");
  }

  @Test
  public void testEnumProblem() throws Exception {
    String localFilename = TestGcdmDatasets.coreLocalDir + "netcdf4/tst_enums.nc";
    Path path = Paths.get(localFilename);
    compareArrayToArray(path);
  }

  // Send one chunk u(0:2, 0:39, 0:90997) size=43679040 bytes
  // Send one chunk u(3:5, 0:39, 0:90997) size=43679040 bytes
  // Send one chunk u(6:8, 0:39, 0:90997) size=43679040 bytes
  // Send one chunk u(0:0, 0:39, 0:90997) size=14559680 bytes
  @Test
  public void testChunkProblem() throws Exception {
    String localFilename = TestGcdmDatasets.testDir + "formats/netcdf4/multiDimscale.nc4";
    Path path = Paths.get(localFilename);
    compareArrayToArray(path);
  }

  @Test
  public void testOpaqueDataType() throws Exception {
    String localFilename = TestGcdmDatasets.coreLocalDir + "hdf5/test_atomic_types.nc";
    Path path = Paths.get(localFilename);
    compareArrayToArray(path);
  }

  @Test
  public void testGcdmProblem2() throws Exception {
    String localFilename = TestGcdmDatasets.datasetLocalDir + "hru_soil_moist_vlen_3hru_5timestep.nc";
    Path path = Paths.get(localFilename);
    compareArrayToArray(path);
  }

  ////////////////////////////////////////////////////////////////////////////

  public void compareArrayToArray(Path path) throws Exception {
    String gcdmUrl = "gcdm://localhost:16111/" + path.toAbsolutePath();
    try (CdmFile ncfile = CdmDatasets.openFile(path.toString(), null);
         GcdmCdmFile gcdmFile = GcdmCdmFile.builder().setRemoteURI(gcdmUrl).build()) {
      boolean ok = new CompareCdmDataset().compare(ncfile, gcdmFile);
      assertThat(ok).isTrue();
    }
  }

  public void compareArrayToArray(Path path, String varName) throws Exception {
    String gcdmUrl = "gcdm://localhost:16111/" + path.toAbsolutePath();
    try (CdmFile ncfile = CdmDatasets.openFile(path.toString(), null);
        GcdmCdmFile gcdmFile = GcdmCdmFile.builder().setRemoteURI(gcdmUrl).build()) {

      boolean ok = CompareCdmFiles.compareVariable(ncfile, gcdmFile, varName, true);
      assertThat(ok).isTrue();
    }
  }

}
