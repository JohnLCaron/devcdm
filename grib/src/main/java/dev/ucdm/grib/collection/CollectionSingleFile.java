/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.grib.collection;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A CollectionManager consisting of a single file
 *
 * @author caron
 * @since 12/23/11
 */
public class CollectionSingleFile implements MCollection {
  protected List<MFile> mfiles = new ArrayList<>();

  public CollectionSingleFile(MFile file) {
    mfiles.add(file);
  }

  public Iterator<MFile> iterator() {
    return mfiles.iterator();
  }


  @Override
  public String getCollectionName() {
    return mfiles.get(0).getShortName();
  }

  @Override
  public void close() {
    // NOOP
  }

  @Override
  public long getLastModified() {
    return mfiles.get(0).getLastModified();
  }

  @Override
  public @Nullable String getRoot() {
    try {
      return mfiles.get(0).getParent().getPath();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getIndexFilename(String suffix) {
    return getRoot() + "/" + getCollectionName() + suffix;
  }

  private Map<String, Object> auxMap = new HashMap<>();
  @Override
  public Object getAuxInfo(String key) {
    return auxMap.get(key);
  }

  @Override
  public MCollection setAuxInfo(String key, Object value) {
    auxMap.put(key, value);
    return this;
  }
}
