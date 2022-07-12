package dev.ucdm.core.iosp;

import dev.ucdm.core.api.CdmFile;
import dev.ucdm.core.api.CdmFiles;
import dev.ucdm.core.io.RandomAccessFile;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;

public class TestBasic {

  public static Stream<Arguments> params() {
    return Stream.of(
            Arguments.of("src/test/data/hdf4/balloon_sonde.o3_knmi000_de.bilt_s2_20060905t112100z_002.hdf"),
            Arguments.of("src/test/data/hdf5/compound_complex.h5"),
            Arguments.of("src/test/data/hdfeos2/AMSR_E_L3_RainGrid_B05_200707.hdf"),
            Arguments.of("src/test/data/hdfeos5/structmetadata_eos.h5"),
            Arguments.of("src/test/data/netcdf3/uw_kingair-2005-01-19-113957.nc")
            );
  }

  @ParameterizedTest
  @MethodSource("params")
  public void testBasic(String filename) throws IOException {
    assertThat(CdmFiles.canOpen(filename)).isTrue();
    File file = new File (filename);
    try (CdmFile cdmFile = CdmFiles.open(filename)) {
      assertThat(cdmFile.getLastModified()).isEqualTo(file.lastModified());
    }
  }

  @ParameterizedTest
  @MethodSource("params")
  public void testSendIospMessage(String filename) throws IOException {
    assertThat(CdmFiles.canOpen(filename)).isTrue();
    try (CdmFile cdmFile = CdmFiles.open(filename)) {
      assertThat(cdmFile.sendIospMessage(CdmFile.IOSP_MESSAGE_GET_IOSP)).isInstanceOf(IOServiceProvider.class);
      assertThat(cdmFile.sendIospMessage(CdmFile.IOSP_MESSAGE_RANDOM_ACCESS_FILE)).isInstanceOf(RandomAccessFile.class);
      assertThat(cdmFile.sendIospMessage(CdmFile.IOSP_MESSAGE_GET_HEADER)).isNotNull();
    }
  }
}
