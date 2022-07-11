/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.grib.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/*
 * Static methods to read resource files.
 */
public class GribResourceReader {

  /**
   * Get the input stream to the given resource
   *
   * @param resourceName The resource name. May be a resource on the class path or a file
   * @return The input stream to the resource
   */
  public static InputStream getInputStream(String resourceName) throws FileNotFoundException {

    // Try class loader to get resource
    ClassLoader cl = GribResourceReader.class.getClassLoader();
    InputStream s = cl.getResourceAsStream(resourceName);
    if (s != null) {
      return s;
    }

    // Try the file system
    File f = new File(resourceName);
    if (f.exists())
      return new FileInputStream(f);

    // give up
    throw new FileNotFoundException("Cant find resource " + resourceName);
  }

}

