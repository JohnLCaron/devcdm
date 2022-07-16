/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.dataset.ncml;

import dev.ucdm.dataset.api.CdmDataset;
import dev.ucdm.dataset.api.CdmDatasets;
import dev.ucdm.test.util.CompareCdmDataset;
import dev.ucdm.test.util.FileFilterSkipSuffixes;
import org.jdom2.Element;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.util.Formatter;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static dev.ucdm.test.util.TestFilesKt.coreLocalNetcdf3Dir;
import static dev.ucdm.test.util.TestFilesKt.testFilesIn;

/**
 * TestWrite NcML, read back and compare with original.
 */
public class TestNcmlWriteAndCompare {

  @TempDir
  public static File tempFolder;

  public static Stream<Arguments> params() {
    return testFilesIn(coreLocalNetcdf3Dir)
            .addNameFilter(new FileFilterSkipSuffixes(".cdl .txt"))
            .withRecursion()
            .build();
  }

  @ParameterizedTest
  @MethodSource("params")
  public void testNcmlWriteAndCompare(String ncmlLocation) throws IOException {
    File file = new File(ncmlLocation);
    String url = "file://" + file.getCanonicalPath();
    System.out.printf("testNcmlWriteAndCompare %s%n", url);

    try (CdmDataset org = CdmDatasets.openDataset(url, true, null, null)) {
      File outputFile = File.createTempFile("testNcmlWriteAndCompare", ".ncml", tempFolder);

      NcmlWriter ncmlWriter = new NcmlWriter();
      Element netcdfElement = ncmlWriter.makeNetcdfElement(org, ncmlLocation);
      ncmlWriter.writeToFile(netcdfElement, outputFile);
      System.out.printf("  ncml written to %s%n", outputFile.getCanonicalPath());

      // read it back in
      try (CdmDataset copy = CdmDatasets.openDataset(outputFile.getCanonicalPath(), true, null, null)) {
        Formatter out = new Formatter();
        boolean ok = new CompareCdmDataset(out, false, false, false).compare(org, copy);
        System.out.printf(  "%s %s%n", ok ? "OK" : "NOT OK", out);
        assertThat(ok).isTrue();
      }
    }
  }

}
