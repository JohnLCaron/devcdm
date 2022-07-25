/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.collection;

import dev.ucdm.array.Immutable;

import com.google.common.base.Preconditions;
import dev.ucdm.core.calendar.CalendarDate;
import dev.ucdm.core.calendar.CalendarDateRange;
import dev.ucdm.core.util.StringUtil2;
import dev.ucdm.grib.coord.*;
import dev.ucdm.grib.inventory.MCollection;
import dev.ucdm.grib.inventory.MFile;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Superclass to build ncx indexes for collections of Grib files.
 */
public abstract class GribCollectionBuilder {
  protected final MCollection dcm;
  protected final org.slf4j.Logger logger;
  protected final boolean isGrib1;
  protected CollectionType type;

  protected final String name; // collection name
  protected final File directory; // top directory

  protected abstract List<? extends Group> makeGroups(List<MFile> allFiles, boolean singleRuntime, Formatter errlog)
      throws IOException;

  protected abstract boolean writeIndex(String name, String indexFilepath, CoordinateRuntime masterRuntime,
      List<? extends Group> groups, List<MFile> files, CalendarDateRange dateRange) throws IOException;

  GribCollectionBuilder(boolean isGrib1, String name, MCollection dcm, org.slf4j.Logger logger) {
    this.dcm = dcm;
    this.logger = logger;
    this.isGrib1 = isGrib1;

    this.name = StringUtil2.replace(name, ' ', "_");
    this.directory = new File(dcm.getRoot());
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////

  // Throw exception if failure
  public boolean createIndex(Formatter errlog) throws IOException {
      return createMultipleRuntimeCollections(errlog);
  }

  // Throw exception if failure
  private boolean createMultipleRuntimeCollections(Formatter errlog) throws IOException {
    long start = System.currentTimeMillis();

    List<MFile> files = new ArrayList<>();
    List<? extends Group> groups = makeGroups(files, false, errlog);
    List<MFile> allFiles = Collections.unmodifiableList(files);
    if (allFiles.isEmpty()) {
      throw new IllegalStateException("No files in this collection =" + name + " topdir=" + dcm.getRoot());
    }
    if (groups.isEmpty()) {
      throw new IllegalStateException("No records in this collection =" + name + " topdir=" + dcm.getRoot());
    }

    // Create the master runtimes, classify the result
    CalendarDateRange calendarDateRangeAll = null;
    boolean allTimesAreUnique = true;
    Set<Long> allRuntimes = new HashSet<>();
    for (Group g : groups) {
      allRuntimes.addAll(g.getCoordinateRuntimes());
      for (Coordinate coord : g.getCoordinates()) {
        if (coord instanceof CoordinateTime2D) {
          CoordinateTime2D coord2D = (CoordinateTime2D) coord;
          if (allTimesAreUnique) {
            allTimesAreUnique = coord2D.hasUniqueTimes();
          }
        }
        if (coord instanceof CoordinateTimeAbstract) {
          CalendarDateRange calendarDateRange = ((CoordinateTimeAbstract) coord).makeCalendarDateRange();
          if (calendarDateRangeAll == null) {
            calendarDateRangeAll = calendarDateRange;
          } else {
            calendarDateRangeAll = calendarDateRangeAll.extend(calendarDateRange);
          }
        }
      }
    }
    List<Long> sortedList = new ArrayList<>(allRuntimes);
    Collections.sort(sortedList);
    if (sortedList.isEmpty()) {
      throw new IllegalArgumentException("No runtimes in this collection =" + name);
    } else if (sortedList.size() == 1) {
      this.type = CollectionType.SRC;
    } else if (allTimesAreUnique) {
      this.type = CollectionType.MRUTC;
    } else {
      this.type = CollectionType.MRC;
    }

    CoordinateRuntime masterRuntimes = new CoordinateRuntime(sortedList, null);
    MFile indexFileForRuntime = GribCollection.makeIndexMFile(this.name, directory);
    boolean ok =
        writeIndex(this.name, indexFileForRuntime.getPath(), masterRuntimes, groups, allFiles, calendarDateRangeAll);

    long took = System.currentTimeMillis() - start;
    logger.debug("That took {} msecs", took);
    return ok;
  }

  public interface Group {
    CalendarDate getRuntime();

    List<Coordinate> getCoordinates();

    Set<Long> getCoordinateRuntimes();
  }

  @Immutable
  protected static class GroupAndRuntime {
    private final int hashCode;
    private final long runtime;

    GroupAndRuntime(int hashCode, long runtime) {
      this.hashCode = hashCode;
      this.runtime = runtime;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;

      GroupAndRuntime that = (GroupAndRuntime) o;
      if (hashCode != that.hashCode)
        return false;
      return runtime == that.runtime;
    }

    @Override
    public int hashCode() {
      int result = hashCode;
      result = 31 * result + (int) (runtime ^ (runtime >>> 32));
      return result;
    }
  }
}
