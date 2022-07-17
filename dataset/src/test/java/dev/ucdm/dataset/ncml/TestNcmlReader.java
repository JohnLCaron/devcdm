/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.dataset.ncml;

import dev.ucdm.array.CompareArrayToArray;
import dev.ucdm.core.api.*;
import dev.ucdm.core.constants.CDM;
import dev.ucdm.core.constants._Coordinate;
import dev.ucdm.dataset.api.*;
import dev.ucdm.test.util.CdmObjFilter;
import dev.ucdm.test.util.CompareCdmDataset;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Formatter;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static dev.ucdm.test.util.TestFilesKt.datasetLocalNcmlDir;
import static dev.ucdm.test.util.TestFilesKt.testFilesIn;

/** Compare CdmDatasets.openDataset with NcmlReader.readNcml on specific problem datasets. */
public class TestNcmlReader {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static Stream<Arguments> params() {
    return testFilesIn(datasetLocalNcmlDir)
            .addNameFilter(it -> it.endsWith(".ncml"))
            .withRecursion()
            .build();
  }

  @ParameterizedTest
  @MethodSource("params")
  public void testReadNcML(String ncmlLocation) throws IOException {
    File file = new File(ncmlLocation);
    String url = "file://" + file.getCanonicalPath();
    System.out.printf("testReadNcML %s: ", url);

    try (CdmDataset org = CdmDatasets.openDataset(url, false, null)) {
      // System.out.printf("NcMLReader == %s%n", org);
      try (CdmDataset withBuilder = NcmlReader.readNcml(url, null, null).build()) {
        // System.out.printf("NcMLReaderNew == %s%n", withBuilder);
        Formatter out = new Formatter();
        boolean ok = new CompareCdmDataset(out, false, false, false).compare(org, withBuilder, new NcmlObjFilter());
        System.out.printf("%s %s%n", ok ? "OK" : "NOT OK", out);
        assertThat(ok).isTrue();
      }
    }
  }

  private void compareVarData(String ncmlLocation, String varName) throws IOException {
    System.out.printf("Compare NcMLReader.readNcML %s%n", ncmlLocation);
    logger.info("TestNcmlReaders on {}%n", ncmlLocation);
    try (CdmDataset org = CdmDatasets.openDataset(ncmlLocation, false, null)) {
      Variable v = org.findVariable(varName);
      assert v != null;
      try (CdmDataset withBuilder = NcmlReader.readNcml(ncmlLocation, null, null).build()) {
        Variable vb = withBuilder.findVariable(varName);
        assert vb != null;
        boolean ok = CompareArrayToArray.compareData(varName, v.readArray(), vb.readArray());
        System.out.printf("%s%n", ok ? "OK" : "NOT OK");
        assertThat(ok).isTrue();
      }
    }
  }

  private void compareDS(String ncmlLocation) throws IOException {
    System.out.printf("Compare CdmDataset.openDataset %s%n", ncmlLocation);
    try (CdmDataset org = CdmDatasets.openDataset(ncmlLocation)) {
      try (CdmDataset withBuilder = CdmDatasets.openDataset(ncmlLocation)) {
        Formatter out = new Formatter();
        boolean ok = new CompareCdmDataset(out, false, false, false).compare(org, withBuilder, new NcmlObjFilter());
        System.out.printf("%s %s%n", ok ? "OK" : "NOT OK", out);
        assertThat(ok).isTrue();
      }
    }
  }

  public static class NcmlObjFilter extends CdmObjFilter {
    @Override
    public boolean attCheckOk(Attribute att) {
      return !att.getShortName().equals(_Coordinate._CoordSysBuilder) && !att.getShortName().equals(CDM.NCPROPERTIES);
    }

    // override att comparision if needed
    public boolean attsAreEqual(Attribute att1, Attribute att2) {
      if (att1.getShortName().equalsIgnoreCase(CDM.UNITS) && att2.getShortName().equalsIgnoreCase(CDM.UNITS)) {
        return att1.getStringValue().equals(att2.getStringValue());
      }
      return att1.equals(att2);
    }

  }


}
