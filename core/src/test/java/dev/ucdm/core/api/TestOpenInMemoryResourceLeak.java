/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.core.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static com.google.common.truth.Truth.assertThat;

/**
 * CdmFile.openInMemory(URI) and CdmFiles.openInMemory(URI) were leaking an open input stream.
 * This tests for a leak by trying to delete a file that's been opened in memory using the URI signature.
 *
 * See https://github.com/Unidata/netcdf-java/issues/166
 * Contributed by https://github.com/tayloj
 */
public class TestOpenInMemoryResourceLeak {
  private static Path outputDir;

  @BeforeAll
  public static void makeTempDir() throws IOException {
    outputDir = Files.createTempDirectory("TestOpenInMemoryResourceLeak");
    outputDir.toFile().deleteOnExit();
  }

  // holds the tempFile Path object created before each test runs
  Path tempFile;

  /*
   * Before each test, create a copy of an existing netcdf file from the built-in test datasets.
   * We will try to delete this temp file after opening it in memory.
   */
  @BeforeEach
  public void makeFileToBeDeleted() throws IOException {
    tempFile = Files.createTempFile(outputDir,"cdmTest", ".nc");
    Path ncfileToCopy = Paths.get(TestCdmFiles.coreLocalNetcdf3Dir + "jan.nc");
    Files.copy(ncfileToCopy, tempFile, StandardCopyOption.REPLACE_EXISTING);
  }

  /** Test for leak using CdmFile.openInMemory(URI) */
  @Test
  public void inputStreamCdmFileLeak() throws IOException {
    /*
     * Read the file into a CdmFile. try-with-resources ensures that the CdmFile's close()
     * method is called, so all resources with it are released.
     */
    try (CdmFile ncfile = CdmFiles.openInMemory(tempFile.toUri())) {
      // prove it's opened
      assertThat(ncfile.getCdmFileTypeId()).isEqualTo("NetCDF");
    }

    /*
     * Try to delete the temp file with Files.delete(). When this fails, it will throw an exception like:
     *
     * java.nio.file.FileSystemException:
     * C:\Users\\username\AppData\Local\Temp\file8726442302596323190.nc: The process cannot access
     * the file because it is being used by another process.
     */
    Files.delete(tempFile);
  }

  /** Test for leak using CdmFiles.openInMemory(URI) */
  @Test
  public void inputStreamCdmFilesLeak() throws IOException {
    try (CdmFile ncfile = CdmFiles.openInMemory(tempFile.toUri())) {
      // prove it's opened
      assertThat(ncfile.getCdmFileTypeId()).isEqualTo("NetCDF");
    }

    Files.delete(tempFile);
  }

  /** Check that the temp file has been deleted after each test */
  @AfterEach
  public void reallyDeleted() {
    assertThat(Files.notExists(tempFile)).isTrue();
  }

}
