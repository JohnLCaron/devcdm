/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.grib.inventory;

import com.google.common.collect.ImmutableList;
import dev.ucdm.core.calendar.CalendarDate;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

/** A CollectionManager consisting of a single file */
public class MCollectionSingleFile extends AbstractMCollection implements MCollection {
  protected final List<MFile> mfiles;

  public MCollectionSingleFile(MFile file) {
    super(file.getShortName());
    mfiles = ImmutableList.of(file);
  }

  @Override
  public void iterateOverMFiles(Visitor visitor) {
    for (MFile mfile : mfiles) {
      visitor.visit(mfile);
    }
  }

  @Override
  public String getCollectionName() {
    return mfiles.get(0).getShortName();
  }

  @Override
  public CalendarDate getLastModified() {
    return CalendarDate.of(mfiles.get(0).getLastModified());
  }

  @Override
  public @Nullable String getRoot() {
    try {
      return mfiles.get(0).getParent().getPath();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
