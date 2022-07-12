/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.gcdm;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Stopwatch;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import dev.ucdm.core.api.Variable;
import org.junit.Test;
import dev.ucdm.array.Array;
import dev.ucdm.gcdm.client.GcdmCdmFile;

import static dev.ucdm.test.util.TestFilesKt.extraTestDir;


/** Time {@link GcdmCdmFile} 759MB takes ~ 3 minutes */
public class TimeGcdm {
  String localFilename = extraTestDir + "formats/netcdf4/e562p1_fp.inst3_3d_asm_Nv.20100907_00z+20100909_1200z.nc4";

  @Test
  public void readCmdrArray() throws IOException {
    String gcdmUrl = "gcdm://localhost:16111/" + localFilename;

    long total = 0;
    Stopwatch stopwatchAll = Stopwatch.createStarted();
    try (GcdmCdmFile gcdmFile = GcdmCdmFile.builder().setRemoteURI(gcdmUrl).build()) {
      System.out.println("Test input: " + gcdmFile.getLocation());
      boolean ok = true;
      for (Variable v : gcdmFile.getVariables()) {
        System.out.printf("  read variable though array : %s %s", v.getArrayType(), v.getShortName());
        Stopwatch stopwatch = Stopwatch.createStarted();
        Array<?> data = v.readArray();
        stopwatch.stop();
        long size = data.length();
        double rate = ((double) size) / stopwatch.elapsed(TimeUnit.MICROSECONDS);
        System.out.printf("    size = %d, time = %s rate = %10.4f MB/sec%n", size, stopwatch, rate);
        total += size;
      }
      assertThat(ok).isTrue();
    }
    stopwatchAll.stop();
    double rate = ((double) total) / stopwatchAll.elapsed(TimeUnit.MICROSECONDS);
    System.out.printf("*** %d bytes took %s = %10.4f MB/sec%n", total, stopwatchAll, rate);
  }
}
