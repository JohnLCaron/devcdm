/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.grib.collection;

import org.jetbrains.annotations.Nullable;

import java.io.Closeable;

public interface MCollection extends Closeable, Iterable<MFile> {
  String getCollectionName();
  void close();
  long getLastModified();

  /** Get common root directory of all MFiles in the collection - may be null */
  @Nullable
  String getRoot();

  // TODO wtf ?
  String getIndexFilename(String suffix);

    ////////////////////////////////////////////////////
  // ability to pass arbitrary information to users of the mcollection .

  Object getAuxInfo(String key);

  // return this for chaining
  MCollection setAuxInfo(String key, Object value);

}
