/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.core.api;

import org.junit.jupiter.api.Test;
import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

/** Test reading a ncfile with long offsets "large format". */
public class TestLongOffset {

  @Test
  public void testReadLongOffset() throws IOException {
    try (CdmFile cdmFile = CdmFiles.open(TestCdmFiles.coreLocalNetcdf3Dir + "longOffset.nc", -1, null,
        CdmFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE)) {
      assertThat(cdmFile.getCdmFileTypeVersion()).isEqualTo("2");
    }
  }

  @Test
  public void testReadLongOffsetV3mode() throws IOException {
    try (CdmFile cdmFile = CdmFiles.open(TestCdmFiles.coreLocalNetcdf3Dir + "longOffset.nc")) {
    }
  }
}
