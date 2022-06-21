package dev.cdm.core.api;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

public class TestCdmFilesCompressed {

  @Test
  public void testCompressionZ() throws IOException {
    File uncompressedFile = new File(TestCdmFiles.coreLocalDir + "compress/testCompress.nc");
    uncompressedFile.delete();

    try (CdmFile ncfile = CdmFiles.open(TestCdmFiles.coreLocalDir + "compress/testCompress.nc.Z")) {
      // global attributes
      assertThat(ncfile.getRootGroup().findAttributeString("yo", "barf")).isEqualTo("face");

      Variable temp = ncfile.findVariable("temperature");
      assertThat(temp).isNotNull();
      assertThat(temp.findAttributeString("units", "barf")).isEqualTo("K");
    }

    // repeat, to read from cache
    try (CdmFile ncfile = CdmFiles.open(TestCdmFiles.coreLocalDir + "compress/testCompress.nc.Z")) {
      // global attributes
      assertThat(ncfile.getRootGroup().findAttributeString("yo", "barf")).isEqualTo("face");

      Variable temp = ncfile.findVariable("temperature");
      assertThat(temp).isNotNull();
      assertThat(temp.findAttributeString("units", "barf")).isEqualTo("K");
    }
  }

  @Test
  public void testCompressionZip() throws IOException {
    File uncompressedFile = new File(TestCdmFiles.coreLocalDir + "compress/testZip.nc");
    uncompressedFile.delete();

    try (CdmFile ncfile = CdmFiles.open(TestCdmFiles.coreLocalDir + "compress/testZip.nc.zip")) {
      // global attributes
      assertThat(ncfile.getRootGroup().findAttributeString("yo", "barf")).isEqualTo("face");

      Variable temp = ncfile.findVariable("temperature");
      assertThat(temp).isNotNull();
      assertThat(temp.findAttributeString("units", "barf")).isEqualTo("K");
    }

    // repeat, to read from cache
    try (CdmFile ncfile = CdmFiles.open(TestCdmFiles.coreLocalDir + "compress/testZip.nc.zip")) {
      // global attributes
      assertThat(ncfile.getRootGroup().findAttributeString("yo", "barf")).isEqualTo("face");

      Variable temp = ncfile.findVariable("temperature");
      assertThat(temp).isNotNull();
      assertThat(temp.findAttributeString("units", "barf")).isEqualTo("K");
    }
  }

  @Test
  public void testCompressionGzip() throws IOException {
    File uncompressedFile = new File(TestCdmFiles.coreLocalDir + "compress/testGzip.nc");
    uncompressedFile.delete();

    try (CdmFile ncfile = CdmFiles.open(TestCdmFiles.coreLocalDir + "compress/testGzip.nc.gz")) {
      // global attributes
      assertThat(ncfile.getRootGroup().findAttributeString("yo", "barf")).isEqualTo("face");

      Variable temp = ncfile.findVariable("temperature");
      assertThat(temp).isNotNull();
      assertThat(temp.findAttributeString("units", "barf")).isEqualTo("K");
    }

    // repeat, to read from cache
    try (CdmFile ncfile = CdmFiles.open(TestCdmFiles.coreLocalDir + "compress/testGzip.nc.zip")) {
      // global attributes
      assertThat(ncfile.getRootGroup().findAttributeString("yo", "barf")).isEqualTo("face");

      Variable temp = ncfile.findVariable("temperature");
      assertThat(temp).isNotNull();
      assertThat(temp.findAttributeString("units", "barf")).isEqualTo("K");
    }
  }

  @Test
  public void testCompressionBzip() throws IOException {
    File uncompressedFile = new File(TestCdmFiles.coreLocalDir + "compress/testBzip.nc");
    uncompressedFile.delete();

    try (CdmFile ncfile = CdmFiles.open(TestCdmFiles.coreLocalDir + "compress/testBzip.nc.bz2")) {
      // global attributes
      assertThat(ncfile.getRootGroup().findAttributeString("yo", "barf")).isEqualTo("face");

      Variable temp = ncfile.findVariable("temperature");
      assertThat(temp).isNotNull();
      assertThat(temp.findAttributeString("units", "barf")).isEqualTo("K");
    }

    // repeat, to read from cache
    try (CdmFile ncfile = CdmFiles.open(TestCdmFiles.coreLocalDir + "compress/testBzip.nc.bz2")) {
      // global attributes
      assertThat(ncfile.getRootGroup().findAttributeString("yo", "barf")).isEqualTo("face");

      Variable temp = ncfile.findVariable("temperature");
      assertThat(temp).isNotNull();
      assertThat(temp.findAttributeString("units", "barf")).isEqualTo("K");
    }
  }
}
