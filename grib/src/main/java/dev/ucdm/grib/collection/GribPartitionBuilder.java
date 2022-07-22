/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.collection;

import dev.ucdm.core.calendar.CalendarDateRange;
import dev.ucdm.grib.common.GribConfig;
import dev.ucdm.grib.coord.*;
import dev.ucdm.grib.inventory.MPartition;
import dev.ucdm.grib.protoconvert.GribPartitionIndexWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GribPartitionBuilder {
  private static final Logger logger = LoggerFactory.getLogger(GribPartitionBuilder.class);

  protected final String name;
  private final MPartition mpartition;
  private final File directory;
  private final GribConfig gribConfig;
  private final boolean isGrib1;

  public GribPartitionBuilder(String name, File directory, MPartition tpc, GribConfig gribConfig, boolean isGrib1) {
    this.name = name;
    this.directory = directory;
    this.mpartition = tpc;
    this.gribConfig = gribConfig;
    this.isGrib1 = isGrib1;
  }

  ///////////////////////////////////////////////////
  // build the index
  private GribPartition result;

  // return true if changed, exception on failure
  public boolean createPartitionedIndex(Formatter errlog) throws IOException {
    this.result = new GribPartition(name, directory, gribConfig, true);

    if (mpartition.isPartitionOfPartition()) {
      mpartition.iterateOverMPartitions(tpc -> {
        result.addChildCollection(tpc);
      });
    } else {
      mpartition.iterateOverMCollections(dcmp -> {
        result.addChildCollection(dcmp);
      });
    }

    result.sortChildCollections(); // after this the partition list is immutable

    // choose the "canonical" partition, aka prototype, only used in copyInfo
    int n = result.childCollections.size();
    if (n == 0) {
      errlog.format("ERR Nothing in this partition = %s%n", result.showLocation());
      throw new IllegalStateException("Nothing in this partition =" + result.showLocation());
    }
    // LOOK removed: int idx = pCollection.getCanonicalIndex(n);
    GribPartition.ChildCollection canon = result.getPartition(0);
    logger.debug("     Using canonical partition {}", canon.name);

    try (GribCollection gc = canon.makeGribCollection()) {
      if (gc == null) {
        throw new IllegalStateException("canon.makeGribCollection failed on =" + result.showLocation() + " "
                + canon.name + "; errs=" + errlog);
      }

      // copy info from canonical gribCollection to result
      result.setCanonicalCollection(gc);
      result.isPartitionOfPartitions = false; // TODO (gc instanceof GribPartition);
    }

    // check consistency across vert and ens coords
    // create partitioned variables
    // partition index is used - do not resort partitions
    GribPartition.DatasetP ds2D = makeDataset2D(errlog);
    if (ds2D == null) {
      errlog.format(" ERR makeDataset2D failed, index not written on %s%n", result.showLocation());
      throw new IllegalStateException(
              "makeDataset2D failed, index not written on =" + result.showLocation() + "; errs=" + errlog);
    }

    // Make Best for a TwoD
    // if (ds2D.gctype == CollectionType.TwoD)
    //  makeDatasetBest(ds2D, false);
    // else if (ds2D.gctype == CollectionType.MRUTC)
    // makeTime2runtime(ds2D, false);

    // write the partition index file
    var writer = new GribPartitionIndexWriter(name, mpartition);
    return writer.writeIndex(result, isGrib1, errlog);
  }

  // each dataset / group has one of these, across all partitions
  private class GroupPartitions {
    final GribPartition.GroupP resultGroup;
    final GribCollection.GroupGC[] componentGroups; // one for each partition;null if group is not in the
    // partition
    final int[] componentGroupIndex; // one for each partition; the index into the partition.ds2d.groups() array
    final int npart;

    GroupPartitions(GribPartition.GroupP resultGroup, int npart) {
      this.resultGroup = resultGroup;
      this.npart = npart;
      this.componentGroups = new GribCollection.GroupGC[npart];
      this.componentGroupIndex = new int[npart];
    }

    void makeVariableIndexPartitioned() {
      // find unique variables across all partitions
      var variableMap = new HashMap<Object, VariableIndex>();
      for (GribCollection.GroupGC group : componentGroups) {
        if (group == null) {
          continue;
        }
        for (VariableIndex vi : group.variList) {
          variableMap.put(vi.gribVariable, vi); // this will use the last one found
        }
      }
      for (VariableIndex vi : variableMap.values()) {
        // convert each VariableIndex to VariableIndexPartitioned in result.
        // note not using canon vi, but last one found
        result.makeVariableIndexPartitioned(resultGroup, vi, npart); // this adds to resultGroup
      }
    }
  }

  @Nullable
  private GribPartition.DatasetP makeDataset2D(Formatter f) throws IOException {
    GribConfig.GribIntvFilter intvMap = (gribConfig != null) ? gribConfig.intvFilter : null;
    GribPartition.DatasetP ds2D = result.makeDataset(CollectionType.TwoD);
    int npart = result.childCollections.size();

    // make a list of unique groups across all partitions as well as component groups for each group
    List<CoordinateRuntime> masterRuntimes = new ArrayList<>();
    Map<Object, GroupPartitions> groupMap = new HashMap<>(40); // gdsHashObject, GroupPartition
    // ok to use Builder2 for both grib1 and grib2 because not extracting
    CoordinateBuilder<?> runtimeAllBuilder = new CoordinateRuntime.Builder2(null);

    int countPartition = 0;
    CalendarDateRange dateRangeAll = null;
    boolean rangeOverlaps = false;
    for (GribPartition.ChildCollection tpp : result.childCollections) {
      // TODO open/close each child partition. could leave open ? they are NOT in cache
      try (GribCollection gc = tpp.makeGribCollection()) {
        if (gc == null) {
          continue; // skip if they dont exist
        }

        // note its not recursive, maybe leave open, or cache; actually we keep a pointer to the partition's group in
        // the GroupPartitions
        CoordinateRuntime partRuntime = gc.masterRuntime;
        runtimeAllBuilder.addAll(partRuntime); // make a complete set of runtime Coordinates
        masterRuntimes.add(partRuntime); // make master runtimes

        GribCollection.Dataset ds2dp = gc.getDatasetCanonical(); // the twoD or GC dataset

        // date ranges must not overlap in order to use MRUTP
        if (dateRangeAll == null) {
          // System.out.printf(" %s = %s%n", gc.name, gc.dateRange);
          dateRangeAll = gc.dateRange;
        } else if (!rangeOverlaps) {
          rangeOverlaps = dateRangeAll.intersects(gc.dateRange);
          dateRangeAll = dateRangeAll.extend(gc.dateRange);
        }

        /*
         * see if its only got one time coord
         * if (ds2dp.gctype == CollectionType.SRC) {
         * for (GribCollection.GroupGC group : ds2dp.getGroups()) {
         * for (Coordinate coord : group.getCoordinates()) { // all time coords must have only one time
         * if (coord instanceof CoordinateTime2D) {
         * CoordinateTime2D coord2D = (CoordinateTime2D) coord;
         * if (coord2D.getNtimes() > 1)
         * allAre1D = false;
         *
         * } else if (coord instanceof CoordinateTimeAbstract && coord.getSize() > 1)
         * allAre1D = false;
         * }
         * }
         * } else if (ds2dp.gctype == CollectionType.MRC || ds2dp.gctype ==
         * CollectionType.TwoD) {
         * allAre1D = false;
         * }
         */

        int groupIdx = 0;
        for (GribCollection.GroupGC groupGC : ds2dp.groups) { // for each group in the partition
          GroupPartitions gs = groupMap.get(groupGC.getGdsHash());
          if (gs == null) {
            gs = new GroupPartitions(ds2D.addGroupCopy(groupGC), npart);
            groupMap.put(groupGC.getGdsHash(), gs);
          }
          gs.componentGroups[countPartition] = groupGC;
          gs.componentGroupIndex[countPartition] = groupIdx++;
        }
      } // close the gc
      countPartition++;
    } // loop over partition

    List<GroupPartitions> groupPartitions = new ArrayList<>(groupMap.values());
    result.masterRuntime = (CoordinateRuntime) runtimeAllBuilder.finish();
    if (result.isPartitionOfPartitions) {// cache calendar dates for efficiency
      CoordinateTimeAbstract.cdf = new CalendarDateFactory(result.masterRuntime);
    }
    result.dateRange =  dateRangeAll;

    if (!rangeOverlaps) {
      ds2D.gctype = CollectionType.MRUTP;
      // else if (allAre1D)
      // ds2D.gctype = CollectionType.MRSTP;
    } else {
      ds2D.gctype = CollectionType.TwoD;
    }

    // create run2part: for each run, which partition to use
    result.run2part = new int[result.masterRuntime.getSize()];
    int partIdx = 0;
    for (CoordinateRuntime partRuntime : masterRuntimes) {
      for (Object val : partRuntime.getValues()) {
        int idx = result.masterRuntime.getIndex(val);
        // note that later partitions will override earlier if they have the same runtime
        result.run2part[idx] = partIdx;
      }
      partIdx++;
    }

    // do each horiz group
    for (GroupPartitions gp : groupPartitions) {
      GribPartition.GroupP resultGroup = gp.resultGroup;
      gp.makeVariableIndexPartitioned();

      String gname = resultGroup.getId();

      // for each partition in this group
      for (int partno = 0; partno < npart; partno++) {
        GribCollection.GroupGC group = gp.componentGroups[partno];
        if (group == null) { // missing group in this partition
          f.format(" INFO canonical group %s not in partition %s%n", gname, result.getPartition(partno).name);
          continue;
        }
        int groupIdx = gp.componentGroupIndex[partno];

        // for each variable in this Partition, add reference to it in the vip
        for (int varIdx = 0; varIdx < group.variList.size(); varIdx++) {
          VariableIndex vi = group.variList.get(varIdx);
          GribPartition.VariableIndexPartitioned vip = resultGroup.findVariableByHash(vi.gribVariable);
          if (vip == null) {
            System.out.printf("HEY");
            resultGroup.findVariableByHash(vi.gribVariable);
          }
          vip.addPartition(partno, groupIdx, varIdx, vi.ndups, vi.nrecords, vi.nmissing);
        } // loop over variable
      } // loop over partition

      // each VariableIndexPartitioned now has its list of PartitionForVariable

      // overall set of unique coordinates
      // (config != null) && "dense".equals(config.gribConfig.getParameter("CoordSys"));
      // for now, assume non-dense
      boolean isDense = false;
      CoordinateSharer<?> sharify = new CoordinateSharer<>(isDense, logger);

      // for each variable, create union of coordinates across the partitions
      for (GribPartition.VariableIndexPartitioned vip : resultGroup.variList) {
        vip.finish(); // create the SA, remove list

        // loop over partitions, make union coordinate; also time filter the intervals
        var unionizer = new CoordinatePartitionUnionizer(vip, intvMap, logger);
        for (int partno = 0; partno < npart; partno++) {
          GribCollection.GroupGC group = gp.componentGroups[partno];
          if (group == null) {
            continue; // tolerate missing groups
          }
          VariableIndex vi = group.findVariableByHash(vip.vi.gribVariable);
          if (vi == null) {
            continue; // tolerate missing variables
          }
          try {
            GribPartition.ChildCollection part = ds2D.gctype.isUniqueTime() ? null : result.getPartition(partno);
            unionizer.addCoords(vi.getCoordinates(), part);
          } catch (IllegalStateException e) {
            logger.error(e.getMessage() + " on dataset " + name);
            return null;
          }
        } // loop over partition

        vip.coords = unionizer.finish(); // the viResult coordinates have been ortho/regularized
        sharify.addCoords(vip.coords);
      } // loop over variable

      // create a list of common coordinates, put them into the group, and now variables just reference those by index
      sharify.finish();
      resultGroup.coords = sharify.getUnionCoords();

      // debug
      List<CoordinateTime2D> time2DCoords = new ArrayList<>();
      Map<CoordinateRuntime, CoordinateRuntime> runtimes = new HashMap<>();
      for (Coordinate coord : resultGroup.coords) {
        Coordinate.Type type = coord.getType();
        switch (type) {
          case runtime -> {
            CoordinateRuntime reftime = (CoordinateRuntime) coord;
            runtimes.put(reftime, reftime);
          }
          case time2D -> {
            CoordinateTime2D t2d = (CoordinateTime2D) coord;
            time2DCoords.add(t2d);
          }
        }
      }
      for (CoordinateTime2D t2d : time2DCoords) {
        CoordinateRuntime runtime2D = t2d.getRuntimeCoordinate();
        CoordinateRuntime runtime = runtimes.get(runtime2D);
        if (runtime == null)
          logger.warn("assignRuntimeNames failed on {} group {}", t2d.getName(), resultGroup.getId());
      } // end debug


      for (GribPartition.VariableIndexPartitioned vip : resultGroup.variList) {
        // redo the variables against the shared coordinates
        vip.coordIndex = sharify.reindex2shared(vip.coords); // ok
        vip.coords = null; // dont use anymore, now use coordIndex into group coordinates
      }

    } // loop over groups

    CoordinateTimeAbstract.cdf = null; // LOOK wtf ?
    return ds2D;
  }
}
