/*
 * Copyright (c) 1998-2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.test.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TestDir {
  private static final Logger logger = LoggerFactory.getLogger(TestDir.class);

  private static final String testdataDirPropName = "cdm.testdata.path";

  private static String testdataDir;

  public static final String datasetLocalDir = "../dataset/src/test/data/";
  public static final String datasetLocalNcmlDir = "../dataset/src/test/data/ncml/";
  public static final String coreLocalDir = "../core/src/test/data/netcdf3/";
  public static final String extraTestDir = "/home/snake/tmp/testData/";
  public static final String oldTestDir = "/media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/";

  /**
   * cdm-test data directory (distributed with code but can depend on data not in github (e.g. NcML files can reference
   * data not in github)
   */
  public static String cdmTestDataDir = "../cdm-test/src/test/data/";

  static {
    testdataDir = System.getProperty(testdataDirPropName); // Check the system property.

    // Use default paths if needed.
    if (testdataDir == null) {
      testdataDir = "/share/testdata/";
      logger.warn("No '{}' property found; using default value '{}'.", testdataDirPropName, testdataDir);
    }

    // Make sure paths ends with a slash.
    testdataDir = testdataDir.replace('\\', '/'); // canonical
    if (!testdataDir.endsWith("/")) {
      testdataDir += "/";
    }
  }

  // Calling routine passes in an action.
  public interface Act {
    int doAct(String filename) throws IOException;
  }

  // list of suffixes to include
  public static class FileFilterIncludeSuffixes implements FileFilter {
    String[] suffixes;

    public FileFilterIncludeSuffixes(String suffixes) {
      this.suffixes = suffixes.split(" ");
    }

    @Override
    public boolean accept(File file) {
      for (String s : suffixes)
        if (file.getPath().endsWith(s))
          return true;
      return false;
    }
  }

  // list of suffixes to exclude
  public static FileFilter FileFilterSkipSuffixes(String suffixes) {
    return new FileFilterNoWant(suffixes);
  }

  private static class FileFilterNoWant implements FileFilter {
    String[] suffixes;

    FileFilterNoWant(String suffixes) {
      this.suffixes = suffixes.split(" ");
    }

    @Override
    public boolean accept(File file) {
      for (String s : suffixes) {
        if (file.getPath().endsWith(s)) {
          return false;
        }
      }
      return true;
    }
  }

  /** Call act.doAct() on each file in dirName that passes the file filter, recurse into subdirs. */
  public static int actOnAll(String dirName, FileFilter ff, Act act) throws IOException {
    return actOnAll(dirName, ff, act, true);
  }

  /**
   * Call act.doAct() on each file in dirName passing the file filter
   *
   * @param dirName recurse into this directory
   * @param ff for files that pass this filter, may be null
   * @param act perform this acction
   * @param recurse recurse into subdirectories
   * @return count
   * @throws IOException on IO error
   */
  public static int actOnAll(String dirName, FileFilter ff, Act act, boolean recurse) throws IOException {
    int count = 0;

    logger.debug("---------------Reading directory {}", dirName);
    File allDir = new File(dirName);
    File[] allFiles = allDir.listFiles();
    if (null == allFiles) {
      logger.debug("---------------INVALID {}", dirName);
      throw new FileNotFoundException("Cant open " + dirName);
    }

    List<File> flist = Arrays.asList(allFiles);
    Collections.sort(flist);

    for (File f : flist) {
      String name = f.getAbsolutePath();
      if (f.isDirectory()) {
        continue;
      }
      if (((ff == null) || ff.accept(f)) && !name.endsWith(".exclude")) {
        name = name.replace("\\", "/");
        logger.debug("----acting on file {}", name);
        count += act.doAct(name);
      }
    }

    if (!recurse) {
      return count;
    }

    for (File f : allFiles) {
      if (f.isDirectory() && !f.getName().equals("exclude") && !f.getName().equals("problem")) {
        count += actOnAll(f.getAbsolutePath(), ff, act);
      }
    }

    return count;
  }


  // random
  public static void showClassPath() {
    System.out.println("Working Directory = " + System.getProperty("user.dir"));
    String classpath = System.getProperty("java.class.path");
    System.out.printf("Classpath =%n");
    for (String cp : classpath.split(":")) {
      System.out.printf("  %s%n", cp);
    }
  }
}
