/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.collection;

import com.google.common.collect.ImmutableList;
import dev.ucdm.core.calendar.CalendarDate;
import dev.ucdm.core.io.RandomAccessFile;
import dev.ucdm.grib.common.GribCollectionIndex;
import dev.ucdm.grib.common.GribConstants;
import dev.ucdm.grib.common.PartitionedReaderRecord;
import dev.ucdm.grib.common.util.SmartArrayInt;
import dev.ucdm.grib.coord.Coordinate;
import dev.ucdm.grib.coord.CoordinateRuntime;
import dev.ucdm.grib.coord.CoordinateTime2D;
import dev.ucdm.grib.coord.CoordinateTimeAbstract;

import dev.ucdm.grib.protoconvert.GribHorizCoordSystem;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Stored into, and read back from a partition collection index. */
public class Partitions implements Closeable {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Partitions.class);

  public record VariablePartition(int nparts, SmartArrayInt partnoSA, SmartArrayInt groupnoSA, SmartArrayInt varnoSA) {
  }

  public record Partition(String name, String indexFile, long lastModified, long fileSize, CalendarDate partitionDate, File parentDirectory)
          implements Comparable<Partition> {

    public String getIndexPath() {
      return parentDirectory.toPath().resolve(indexFile).toString();
    }

    @Override
    public int compareTo(Partition o) {
      if (partitionDate != null && o.partitionDate != null)
        return partitionDate.compareTo(o.partitionDate);
      return name.compareTo(o.name);
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public final GribCollection topCollection;
  public final boolean isPartitionOfPartitions;
  public final ImmutableList<Partition> partitions;
  public final int[] run2part; // masterRuntime.length; which partition to use for masterRuntime i
  public final Map<Partition, GribCollection> partitionMap = new HashMap<>();

  public Partitions(GribCollection topCollection, boolean isPartitionOfPartitions, List<Partition> partitions, int[] run2part) {
    this.topCollection = topCollection;
    this.isPartitionOfPartitions = isPartitionOfPartitions;
    this.partitions = ImmutableList.copyOf(partitions.stream().sorted().toList());
    this.run2part = run2part;
  }

  public String name() {
    return topCollection.getName();
  }

  @Override
  public void close() throws IOException {
    // LOOK remove these? call from GC.close ?
    partitionMap.values().forEach( gc -> {
      try {
        gc.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  // reader must close
  public GribCollection getGribCollectionForPartition(Partition partition) {
    return partitionMap.computeIfAbsent(partition, (p) -> {
      Formatter errlog = new Formatter();
      try {
        return GribCollectionIndex.readCollectionFromIndex(p.getIndexPath(), true, errlog);
      } catch (IOException e) {
        throw new RuntimeException(errlog.toString(), e);
      }
    }); // not closing (!)
  }

  public RandomAccessFile getDataRaf(int partno, int fileno) throws IOException {
    Partition partition = partitions.get(partno);
    GribCollection gc = getGribCollectionForPartition(partition);
    return new RandomAccessFile(gc.getFilename(fileno), "r");
  }

  // debugging TODO
  public String getFilename(int partno, int fileno) throws IOException {
    Partition part = partitions.get(partno);
    return part.indexFile;
  }

  public void showIndex(Formatter f, CoordinateRuntime masterRuntime) {
    int count = 0;
    f.format("isPartitionOfPartitions=%s%n", isPartitionOfPartitions);
    f.format("Partitions%n");
    for (Partition p : partitions)
      f.format("%d:  %s%n", count++, p);
    f.format("%n");

    if (run2part == null)
      f.format("run2part null%n");
    else {
      f.format(" master runtime -> partition %n");
      for (int idx = 0; idx < masterRuntime.getSize(); idx++) {
        int partno = run2part[idx];
        Partition part = partitions.get(partno);
        f.format(" %d:  %s -> part %3d %s%n", count, masterRuntime.getRuntimeDate(idx), partno, part);
        count++;
      }
      f.format("%n");
    }
  }

  /**
   * find the data record for a request. TODO review, too complicated
   *
   * @param indexWanted the source index request, excluding x and y
   * @return DataRecord pointing to where the data is, or null if missing
   */
  @Nullable
  public static PartitionedReaderRecord getPartitionedReaderRecord(VariableIndex vi, int[] indexWanted) throws IOException {

    if (GribConstants.debugRead) {
      logger.debug("PartitionCollection.getDataRecord index wanted = ({}) on {} type={}",
              Arrays.toString(indexWanted), vi.gribCollection.indexFilename, vi.group.ds.gctype);
    }

    // find the runtime index
    int firstIndex = indexWanted[0];
    int masterIdx;
    if (vi.group.ds.gctype == CollectionType.TwoD) {
      // find the partition by matching run coordinate with master runtime
      CoordinateRuntime runtime = (CoordinateRuntime) vi.getCoordinate(Coordinate.Type.runtime);
      if (runtime == null) {
        throw new IllegalStateException("Type.TwoD must have runtime coordinate");
      }
      Object val = runtime.getValue(firstIndex);
      masterIdx = vi.gribCollection.masterRuntime.getIndex(val);
      if (GribConstants.debugRead)
        logger.debug("  TwoD firstIndex = {} val={} masterIdx={}", firstIndex, val, masterIdx);

    } else if (vi.group.ds.gctype == CollectionType.Best) {
      // find the partition from the "time2runtime" array in the time coordinate
      CoordinateTimeAbstract time = vi.getCoordinateTime();
      if (time == null) {
        throw new IllegalStateException("Type.Best must have time coordinate");
      }
      masterIdx = time.getMasterRuntimeIndex(firstIndex) - 1;
      if (GribConstants.debugRead)
        logger.debug("  Best firstIndex = {} masterIdx={}", firstIndex, masterIdx);

    } else if (vi.group.ds.gctype == CollectionType.MRUTP) {
      CoordinateTime2D time2D = (CoordinateTime2D) vi.getCoordinateTime();
      if (time2D == null) {
        throw new IllegalStateException("Type.MRUTP must have time coordinate");
      }
      Object val = time2D.getRefDate(firstIndex);
      masterIdx = vi.gribCollection.masterRuntime.getIndex(val);

      if (GribConstants.debugRead)
        logger.debug("  MRUTP firstIndex = {} masterIdx={}", firstIndex, masterIdx);

    } else {
      throw new IllegalStateException("Unknown gctype= " + vi.group.ds.gctype + " on " + vi.gribCollection.indexFilename);
    }

    // TODO may be the topCollection ??
    int partno = vi.gribCollection.partitions.run2part[masterIdx];
    if (partno < 0) {
      return null; // may be impossible?
    }

    // the 2D component variable in the partno partition
    VariableIndex vindex2Dpart = getVindex2D(vi, partno);
    if (vindex2Dpart == null) {
      return null; // missing
    }

    if (vi.gribCollection.isPartitionOfPartitions()) {
      return getDataRecordPofP(vi, indexWanted, vindex2Dpart);
    }

    // translate to coordinates in vindex
    int[] sourceIndex = translateIndex2D(vi, indexWanted, vindex2Dpart);

    if (sourceIndex == null) {
      return null; // missing
    }
    
    GribCollection.ReadRecord record = vindex2Dpart.getRecordAt(sourceIndex);
    if (record == null) {
      return null;
    }

    if (GribConstants.debugRead) {
      logger.debug("  result success: partno={} fileno={}", partno, record.fileno());
    }
    return new PartitionedReaderRecord(vi.gribCollection.partitions, partno, vindex2Dpart.group.getGdsHorizCoordSys(), record);
  }

  /**
   * Get VariableIndex (2D) for this partition
   *
   * @param partno master partition number
   * @return VariableIndex or null if not exists
   */
  @Nullable
  private static VariableIndex getVindex2D(VariableIndex vi, int partno) throws IOException {
    // at this point, we need to instantiate the Partition and the vindex.records

    // the 2D vip for this variable
    VariableIndex vi2d = vi.gribCollection.isPartitionOfPartitions() ? getVariable2DByHash(vi, vi.group.horizCoordSys) : vi;
    VariablePartition vip = vi2d.vpartition;

    // which partition? index into PartitionCollectionImmutable.partitions[]: variable may not exist in all partitions
    int partWant = vip.partnoSA.findIdx(partno);
    if (partWant < 0 || partWant >= vip.nparts) {
      if (GribConstants.debugRead)
        logger.debug("  cant find partition={} in vip={}", partno, vip);
      return null;
    }

    // ensure that the vpart.readRecords() sparseArray feilds was read in
    Partition p = vi.gribCollection.partitions.partitions.get(partno);
    GribCollection gc = vi.gribCollection.partitions.getGribCollectionForPartition(p);
    GribCollection.Dataset ds = gc.getDatasetCanonical(); // always references the twoD or GC dataset
    // the group and variable index may vary across partitions
    int groupno = vip.groupnoSA.get(partWant); // TODO partWant vs partno ??
    GribCollection.GroupGC gpart = ds.groups.get(groupno);
    int vpno = vip.varnoSA.get(partWant);
    VariableIndex vpart = gpart.variList.get(vpno);
    vpart.readRecords(gc);
    return vpart;
  }

  @Nullable
  private static VariableIndex getVariable2DByHash(VariableIndex vi, GribHorizCoordSystem hcs) {
    GribCollection.Dataset ds2d = vi.gribCollection.getDatasetCanonical();
    if (ds2d == null) {
      return null;
    }
    for (GribCollection.GroupGC groupHcs : ds2d.getGroups()) {
      if (groupHcs.getGdsHash().equals(hcs.getGdsHash())) {
        return groupHcs.findVariableByHash(vi.gribVariable);
      }
    }
    return null;
  }

  /**
   * TwoD
   * Given the index in the whole (wholeIndex), translate to index in component (compVindex2D) by matching the
   * coordinate values
   *
   * @param wholeIndex index in the whole
   * @param compVindex2D want index in here
   * @return index into compVindex2D, or null if missing
   */
  @Nullable
  private static int[] translateIndex2D(VariableIndex vi, int[] wholeIndex, VariableIndex compVindex2D) {
    int[] result = new int[wholeIndex.length];
    int countDim = 0;

    // special case for 2D time
    CoordinateTime2D compTime2D = (CoordinateTime2D) compVindex2D.getCoordinate(Coordinate.Type.time2D);
    if (compTime2D != null) {
      CoordinateTime2D time2D = (CoordinateTime2D) vi.getCoordinate(Coordinate.Type.time2D);
      if (time2D == null)
        throw new IllegalStateException("CoordinateTime2D has no time2D");
      CoordinateTime2D.Time2D want = time2D.getOrgValue(wholeIndex[0], wholeIndex[1]);
      if (GribConstants.debugRead)
        logger.debug("  translateIndex2D[runIdx={}, timeIdx={}] in componentVar coords = ({}, {})", wholeIndex[0],
                wholeIndex[1], (want == null) ? "null" : want.getRefDate(), want);
      if (want == null) {
        // time2D.getOrgValue(wholeIndex[0], wholeIndex[1], Grib.debugRead); // debug
        return null;
      }
      if (!compTime2D.getIndex(want, result)) {// sets the first 2 indices - run and time
        // compTime2D.getIndex(want, result); // debug
        return null; // missing data
      }
      countDim = 2;
    }

    // the remaining dimensions, if any
    while (countDim < wholeIndex.length) {
      int idx = wholeIndex[countDim];
      int resultIdx = matchCoordinate(vi.getCoordinate(countDim), idx, compVindex2D.getCoordinate(countDim));
      if (GribConstants.debugRead)
        logger.debug("  translateIndex2D[idx={}] resultIdx= {}", idx, resultIdx);
      if (resultIdx < 0) { // partition variable doesnt have a coordinate value that is in the "whole" variable
        return null;
      }
      result[countDim] = resultIdx;
      countDim++;
    }

    return result;
  }

  private static int matchCoordinate(Coordinate whole, int wholeIdx, Coordinate part) {
    Object val = whole.getValue(wholeIdx);
    if (val == null)
      return -1;
    return part.getIndex(val);
  }

  /**
   * find DataRecord in a PofP
   *
   * @param indexWanted index into this PoP
   * @param compVindex2Dp 2D variable from the desired partition; may be PofP or PofGC
   * @return desired record to be read, from the GC, or null if missing
   */
  @Nullable
  private static PartitionedReaderRecord getDataRecordPofP(VariableIndex vi, int[] indexWanted, VariableIndex compVindex2Dp) throws IOException {
      // corresponding index into compVindex2Dp
      int[] indexWantedP = translateIndex2D(vi, indexWanted, compVindex2Dp);
      if (GribConstants.debugRead)
        logger.debug("  (2D) getDataRecordPofP= {}", Arrays.toString(indexWantedP));
      if (indexWantedP == null) {
        return null;
      }
      return getPartitionedReaderRecord(compVindex2Dp, indexWantedP);
  }
}
