/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.core.write;

import dev.ucdm.core.api.TestCdmFiles;
import org.junit.jupiter.api.Test;
import dev.ucdm.core.api.CdmFile;
import dev.ucdm.core.api.CdmFiles;

import java.io.IOException;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link CDLWriter} */
public class TestCdlWriter {
  @Test
  public void testAtomicTypes() throws IOException {
    try (CdmFile ncfile = CdmFiles.open(TestCdmFiles.coreLocalDir + "netcdf4/test_atomic_types.nc", null)) {
      Formatter out = new Formatter();
      CDLWriter.writeCDL(ncfile, out, true, null);
      String cdl = out.toString();

      assertThat(cdl).contains("byte v8;");
      assertThat(cdl).contains("ubyte vu8;");
      assertThat(cdl).contains("short v16;");
      assertThat(cdl).contains("ushort vu16;");
      assertThat(cdl).contains("int v32;");
      assertThat(cdl).contains("uint vu32;");
      assertThat(cdl).contains("int64 v64;");
      assertThat(cdl).contains("uint64 vu64;");
      assertThat(cdl).contains("float vf;");
      assertThat(cdl).contains("double vd;");
      assertThat(cdl).contains("char vc;");
      assertThat(cdl).contains("string vs;");
    }
  }


}
