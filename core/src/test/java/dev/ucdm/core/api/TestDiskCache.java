/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.core.api;

import org.junit.jupiter.api.Test;
import dev.ucdm.core.calendar.CalendarDate;
import dev.ucdm.core.util.DiskCache;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.google.common.truth.Truth.assertThat;

/** Test DiskCache */
public class TestDiskCache {

  @TempDir
  public static File tempFolder;

  static void make(String filename) throws IOException {
    File want = DiskCache.getCacheFile(filename);
    System.out.println("make=" + want.getPath() + "; exists = " + want.exists());
    if (!want.exists()) {
      boolean ret = want.createNewFile();
      assertThat(ret).isTrue();
    }
    System.out.println(" canRead= " + want.canRead() + " canWrite = " + want.canWrite() + " lastMod = "
        + CalendarDate.of(want.lastModified()));
    System.out.println(" original=" + filename);
  }

  @Test
  public void testDiskCacheCreateNewFile() throws IOException {
    DiskCache.setRootDirectory(tempFolder.toString());
    make("C:/junk.txt");
    make("C:/some/enchanted/evening/joots+3478.txt");
    make("http://www.unidata.ucar.edu/some/enc hanted/eve'ning/nowrite.gibberish");

    DiskCache.showCache(System.out);
    StringBuilder sbuff = new StringBuilder();
    DiskCache.cleanCache(1000 * 1000 * 10, sbuff);
    System.out.println(sbuff);
    assertThat(DiskCache.getCacheFile("any")).isNotNull();
  }

}
