/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.cdm.dataset.ncml;

import dev.cdm.core.api.CdmFile;
import dev.cdm.dataset.testutil.CompareCdmFiles;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.cdm.core.api.Attribute;
import dev.cdm.core.constants.CDM;
import dev.cdm.core.constants._Coordinate;
import dev.cdm.dataset.api.CdmDataset;
import dev.cdm.dataset.api.CdmDatasets;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

/** Test NetcdfDatasets.openDataset() of NcML files. */
public class TestNcmlEquals {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testEquals() throws IOException {
    testEquals("file:" + TestNcmlRead.topDir + "testEquals.xml");
    testEnhanceEquals("file:" + TestNcmlRead.topDir + "testEqualsEnhance.xml");
  }

  public void problem() throws IOException {
    // testEnhanceEquals("file:G:/zg500_MM5I_1979010103.ncml");
    testEquals(
        "file:R:/testdata/2008TrainingWorkshop/tds/knmi/RAD___TEST_R_C_NL25PCP_L___20080720T000000_200807201T015500_0001_resampledto256x256.ncml");
    testEnhanceEquals(
        "file:R:/testdata/2008TrainingWorkshop/tds/knmi/RAD___TEST_R_C_NL25PCP_L___20080720T000000_200807201T015500_0001_resampledto256x256.ncml");
  }

  private void testEquals(String ncmlLocation) throws IOException {
    try (CdmDataset ncd = NcmlReader.readNcml(ncmlLocation, null, null).build()) {
      CdmFile refFile = (CdmFile) ncd.sendIospMessage(CdmDataset.IOSP_MESSAGE_GET_REFERENCED_FILE);
      assertThat(refFile).isNotNull();
      String locref = refFile.getLocation();
      try (CdmDataset ncdref = CdmDatasets.openDataset(locref, false, null)) {
        Formatter f = new Formatter();
        CompareCdmFiles compare = new CompareCdmFiles(f, false, false, false);
        boolean ok = compare.compare(ncd, ncdref, new CoordsObjFilter());
        System.out.printf("%s %s%n", ok ? "OK" : "NOT OK", f);
        assertThat(ok).isTrue();
      }
    }
  }

  private void testEnhanceEquals(String ncmlLocation) throws IOException {
    try (CdmDataset ncd = NcmlReader.readNcml(ncmlLocation, null, null).build()) {
      CdmFile refFile = (CdmFile) ncd.sendIospMessage(CdmDataset.IOSP_MESSAGE_GET_REFERENCED_FILE);
      assertThat(refFile).isNotNull();
      String locref = refFile.getLocation();
      try (CdmDataset ncdref = CdmDatasets.openDataset(locref, true, null)) {
        Formatter f = new Formatter();
        CompareCdmFiles compare = new CompareCdmFiles(f, false, false, false);
        boolean ok = compare.compare(ncd, ncdref, new CoordsObjFilter());
        System.out.printf("%s %s%n", ok ? "OK" : "NOT OK", f);
        assertThat(ok).isTrue();
      }
    }
  }

  public static class NcmlFilter implements FileFilter {

    @Override
    public boolean accept(File pathname) {
      String name = pathname.getName();
      // Made to fail, so skip
      if (name.contains("aggExistingInequivalentCals.xml"))
        return false;
      // NcMLReader does not change variable to type int, so fails.
      if (name.contains("aggSynthetic.xml"))
        return false;
      // Bug in old reader
      if (name.contains("testStandaloneNoEnhance.ncml"))
        return false;
      if (name.contains("AggFmrc"))
        return false; // not implemented
      if (name.endsWith("ml"))
        return true; // .xml or .ncml
      return false;
    }
  }

  public static class CoordsObjFilter implements CompareCdmFiles.ObjFilter {
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
