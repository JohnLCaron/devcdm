/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.protoconvert;

import org.jetbrains.annotations.Nullable;

import dev.ucdm.grib.collection.CollectionType;
import dev.ucdm.grib.inventory.GcMFile;
import dev.ucdm.grib.collection.GribCollection;
import dev.ucdm.grib.inventory.MFile;
import dev.ucdm.grib.collection.Partitions;
import dev.ucdm.grib.collection.VariableIndex;
import dev.ucdm.grib.common.GdsHorizCoordSys;
import dev.ucdm.grib.common.GribCollectionIndex;
import dev.ucdm.grib.common.GribTables;
import dev.ucdm.grib.common.util.SmartArrayInt;
import dev.ucdm.grib.coord.*;
import dev.ucdm.grib.common.GribConfig;
import dev.ucdm.grib.protogen.GribCollectionProto;

import dev.ucdm.core.calendar.CalendarDate;
import dev.ucdm.core.calendar.CalendarDateUnit;
import dev.ucdm.core.calendar.CalendarPeriod;
import dev.ucdm.core.io.RandomAccessFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/** Superclass to read GribCollection from ncx4 file. */
public abstract class GribCollectionIndexReader {
  static final Logger logger = LoggerFactory.getLogger(GribCollectionIndexReader.class);

  protected static final boolean debug = false;
  private static final boolean stackTrace = true;

  protected GribCollection gc;
  protected final GribConfig gribConfig; // TODO should come from the index file
  protected GribTables tables;

  public abstract GribHorizCoordSystem importGribHorizCoordSystem(GribCollectionProto.Gds p);

  public abstract GribTables makeCustomizer() throws IOException;

  protected abstract String getLevelNameShort(int levelCode);

  protected abstract int getVersion();

  protected abstract int getMinVersion();

  public GribCollectionIndexReader(GribCollection gc, GribConfig config) {
    this.gribConfig = config;
    this.gc = gc;
  }

  protected abstract String getMagicStart();

  public boolean readIndex(RandomAccessFile raf) {

    gc.setIndexRaf(raf);
    try {
      raf.order(RandomAccessFile.BIG_ENDIAN);
      raf.seek(0);

      //// header message
      GribCollectionIndex.Type type = GribCollectionIndex.getType(raf);
      if (type == GribCollectionIndex.Type.none) {
        logger.warn("GribCollectionBuilderFromIndex {}: invalid index raf={}", gc.getName(), raf.getLocation());
        // throw new IllegalStateException(); // temp debug
        return false;
      }

      gc.version = raf.readInt();
      if (gc.version < getVersion()) {
        logger.debug("GribCollectionBuilderFromIndex {}: index found version={}, current version= {} on file {}",
            gc.getName(), gc.version, GribCollectionIndexWriter.currentVersion, raf.getLocation());
        // throw new IllegalStateException(); // temp debug
        if (gc.version < getMinVersion())
          return false;
      }

      // these are the variable records
      long skip = raf.readLong();
      raf.skipBytes(skip);
      logger.debug("GribCollectionBuilderFromIndex {} ({}) records len = {}", raf.getLocation(), getMagicStart(), skip);

      int size = Streams.readVInt(raf);
      if ((size < 0) || (size > 300 * 1000 * 1000)) { // ncx bigger than 300 MB?
        logger.warn("GribCollectionBuilderFromIndex {}: invalid index size on file {}", gc.getName(),
            raf.getLocation());
        throw new IllegalStateException(); // temp debug
        // return false;
      }
      logger.debug("GribCollectionBuilderFromIndex proto len = {}", size);

      byte[] m = new byte[size];
      raf.readFully(m);

      GribCollectionProto.GribCollection proto = GribCollectionProto.GribCollection.parseFrom(m);

      // need to read this first to get this.tables initialized
      gc.center = proto.getCenter();
      gc.subcenter = proto.getSubcenter();
      gc.master = proto.getMaster();
      gc.local = proto.getLocal();
      gc.genProcessType = proto.getGenProcessType();
      gc.genProcessId = proto.getGenProcessId();
      gc.backProcessId = proto.getBackProcessId();
      this.tables = makeCustomizer();
      gc.cust = this.tables;

      if (!gc.name.equals(proto.getName())) {
        logger.info("GribCollectionBuilderFromIndex raf {}: has different name= '{}' than stored in ncx= '{}' ",
            raf.getLocation(), gc.getName(), proto.getName());
      }

      // directory always taken from proto, since ncx2 file may be moved, or in cache, etc
      gc.setOrgDirectory(proto.getTopDir());
      gc.indexVersion = proto.getVersion();

      gc.setCalendarDateRange(proto.getStartTime(), proto.getEndTime());

      int fsize = 0;
      int n = proto.getMfilesCount();
      Map<Integer, MFile> fileMap = new HashMap<>(2 * n);
      for (int i = 0; i < n; i++) {
        GribCollectionProto.MFile mf = proto.getMfiles(i);
        fileMap.put(mf.getIndex(),
            new GcMFile(gc.directory, mf.getFilename(), mf.getLastModified(), mf.getLength(), mf.getIndex()));
        fsize += mf.getFilename().length();
      }
      gc.setFileMap(fileMap);
      logger.debug("GribCollectionBuilderFromIndex files len = {}", fsize);

      gc.masterRuntime = (CoordinateRuntime) importCoord(proto.getMasterRuntime());

      gc.datasets = new ArrayList<>(proto.getDatasetCount());
      for (int i = 0; i < proto.getDatasetCount(); i++) {
        importDataset(proto.getDataset(i));
      }
      gc.partitions = this.importPartitions(gc, proto);
      return true;

    } catch (Throwable t) {
      logger.warn("Error reading index " + raf.getLocation(), t);
      if (stackTrace)
        t.printStackTrace();
      return false;
    }
  }

  private GribCollection.Dataset importDataset(GribCollectionProto.Dataset p) {
    CollectionType type = CollectionType.valueOf(p.getType().toString());
    GribCollection.Dataset ds = gc.makeDataset(type);

    List<GribCollection.GroupGC> groups = new ArrayList<>(p.getGroupsCount());
    for (int i = 0; i < p.getGroupsCount(); i++)
      groups.add(importGroup(ds, p.getGroups(i)));
    ds.groups = groups;

    return ds;
  }

  protected GribCollection.GroupGC importGroup(GribCollection.Dataset ds, GribCollectionProto.Group p) {
    GribCollection.GroupGC group = gc.makeGroup(ds);

    group.horizCoordSys = importGribHorizCoordSystem(p.getGds());

    // read coords before variables
    group.coords = new ArrayList<>();
    for (int i = 0; i < p.getCoordsCount(); i++) {
      group.coords.add(importCoord(p.getCoords(i)));
    }

    group.filenose = new int[p.getFilenoCount()];
    for (int i = 0; i < p.getFilenoCount(); i++) {
      group.filenose[i] = p.getFileno(i);
    }

    for (int i = 0; i < p.getVariablesCount(); i++) {
      group.addVariable(importVariable(gc, group, p.getVariables(i)));
    }

    // assign names, units to coordinates
    // CalendarDate firstRef = null;
    int reftimeCoord = 0;
    int timeCoord = 0;
    int ensCoord = 0;
    List<CoordinateVert> vertCoords = new ArrayList<>();
    List<CoordinateTime2D> time2DCoords = new ArrayList<>();
    Map<CoordinateRuntime, CoordinateRuntime> runtimes = new HashMap<>();
    for (Coordinate coord : group.coords) {
      Coordinate.Type type = coord.getType();
      switch (type) {
        case runtime -> {
          CoordinateRuntime reftime = (CoordinateRuntime) coord;
          if (reftimeCoord > 0) {
            reftime.setName("reftime" + reftimeCoord);
          }
          reftimeCoord++;
          runtimes.put(reftime, reftime);
        }
        case time -> {
          CoordinateTime tc = (CoordinateTime) coord;
          if (timeCoord > 0) {
            tc.setName("time" + timeCoord);
          }
          timeCoord++;
        }
        case timeIntv -> {
          CoordinateTimeIntv tci = (CoordinateTimeIntv) coord;
          if (timeCoord > 0) {
            tci.setName("time" + timeCoord);
          }
          timeCoord++;
        }
        case time2D -> {
          CoordinateTime2D t2d = (CoordinateTime2D) coord;
          if (timeCoord > 0) {
            // make sure 2d time coordinate (non-unique) does not use the same name as the dimension
            // note: dimension name gets set in GribIosp, GribIospBuilder
            // See https://github.com/Unidata/netcdf-java/issues/152
            if (!t2d.hasUniqueTimes()) {
              t2d.setName("validtime" + timeCoord);
            } else {
              t2d.setName("time" + timeCoord);
            }
          }
          timeCoord++;
          time2DCoords.add(t2d);
        }
        case vert -> vertCoords.add((CoordinateVert) coord);
        case ens -> {
          CoordinateEns ce = (CoordinateEns) coord;
          if (ensCoord > 0) {
            ce.setName("ens" + ensCoord);
          }
          ensCoord++;
        }
      }
    }
    assignVertNames(vertCoords);
    assignRuntimeNames(runtimes, time2DCoords, group.getId() + "-" + (group.isTwoD ? "TwoD" : "Best"));

    return group;
  }

  public void assignVertNames(List<CoordinateVert> vertCoords) {
    Map<String, Integer> map = new HashMap<>(2 * vertCoords.size());

    // assign name
    for (CoordinateVert vc : vertCoords) {
      String shortName = getLevelNameShort(vc.getCode()).toLowerCase();
      if (vc.isLayer())
        shortName = shortName + "_layer";

      Integer countName = map.get(shortName);
      if (countName == null) {
        map.put(shortName, 0);
      } else {
        countName++;
        map.put(shortName, countName);
        shortName = shortName + countName;
      }

      vc.setName(shortName);
    }
  }

  public void assignRuntimeNames(Map<CoordinateRuntime, CoordinateRuntime> runtimes,
      List<CoordinateTime2D> time2DCoords, String groupId) {

    // assign same name to internal time2D runtime as matched the external runtime
    for (CoordinateTime2D t2d : time2DCoords) {
      CoordinateRuntime runtime2D = t2d.getRuntimeCoordinate();
      CoordinateRuntime runtime = runtimes.get(runtime2D);
      if (runtime == null)
        logger.warn("assignRuntimeNames failed on {} group {}", t2d.getName(), groupId);
      else
        runtime2D.setName(runtime.getName());
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////
  // these objects are created from the ncx index. lame - should only be in the builder i think
  private final Set<String> hcsNames = new HashSet<>(5);

  protected String makeHorizCoordSysName(GdsHorizCoordSys hcs) {
    // default id
    String base = hcs.makeId();
    // ensure uniqueness
    String tryit = base;
    int count = 1;
    while (hcsNames.contains(tryit)) {
      count++;
      tryit = base + "-" + count;
    }
    hcsNames.add(tryit);
    return tryit;
  }


  /*
   * message Coord {
   * required int32 type = 1; // Coordinate.Type.oridinal
   * required int32 code = 2; // time unit; level type
   * required string unit = 3;
   * repeated float values = 4;
   * repeated float bound = 5; // only used if interval, then = (value, bound)
   * repeated int64 msecs = 6; // calendar date
   * }
   */
  private Coordinate importCoord(GribCollectionProto.Coord pc) {
    Coordinate.Type type = importAxisType(pc.getAxisType());
    int code = pc.getCode();
    @Nullable
    String unit = pc.getUnit();
    if (unit.isEmpty()) {
      unit = null;
    }

    switch (type) {
      case runtime:
        if (unit == null) {
          throw new IllegalStateException("Null units");
        }
        CalendarDateUnit cdUnit = CalendarDateUnit.fromUdunitString(null, unit).orElseThrow(IllegalStateException::new);
        return new CoordinateRuntime(pc.getMsecsList(), cdUnit.getCalendarPeriod());

      case time:
        List<Long> offs = new ArrayList<>(pc.getValuesCount());
        for (float val : pc.getValuesList()) {
          offs.add((long) val);
        }
        CalendarDate refDate = CalendarDate.of(pc.getMsecs(0));
        if (unit == null) {
          throw new IllegalStateException("Null units");
        }
        CalendarPeriod timeUnit = CalendarPeriod.of(unit);
        return new CoordinateTime(code, timeUnit, refDate, offs, readTime2Runtime(pc));

      case timeIntv:
        List<TimeCoordIntvValue> tinvs = new ArrayList<>(pc.getValuesCount());
        for (int i = 0; i < pc.getValuesCount(); i++) {
          int val1 = (int) pc.getValues(i);
          int val2 = (int) pc.getBound(i);
          tinvs.add(new TimeCoordIntvValue(val1, val2));
        }
        refDate = CalendarDate.of(pc.getMsecs(0));
        if (unit == null)
          throw new IllegalStateException("Null units");
        CalendarPeriod timeUnit2 = CalendarPeriod.of(unit);
        return new CoordinateTimeIntv(code, timeUnit2, refDate, tinvs, readTime2Runtime(pc));

      case time2D:
        if (unit == null)
          throw new IllegalStateException("Null units");
        CalendarPeriod timeUnit3 = CalendarPeriod.of(unit);
        CoordinateRuntime runtime = new CoordinateRuntime(pc.getMsecsList(), timeUnit3);

        List<Coordinate> times = new ArrayList<>(pc.getTimesCount());
        for (GribCollectionProto.Coord coordp : pc.getTimesList())
          times.add(importCoord(coordp));
        boolean isOrthogonal = pc.getIsOrthogonal();
        boolean isRegular = pc.getIsRegular();
        if (isOrthogonal)
          return new CoordinateTime2D(code, timeUnit3, null, runtime, (CoordinateTimeAbstract) times.get(0), null,
              readTime2Runtime(pc));
        else if (isRegular)
          return new CoordinateTime2D(code, timeUnit3, null, runtime, times, null, readTime2Runtime(pc));
        else
          return new CoordinateTime2D(code, timeUnit3, null, runtime, times, readTime2Runtime(pc));

      case vert:
        boolean isLayer = pc.getValuesCount() == pc.getBoundCount();
        List<VertCoordValue> levels = new ArrayList<>(pc.getValuesCount());
        for (int i = 0; i < pc.getValuesCount(); i++) {
          if (isLayer)
            levels.add(new VertCoordValue(pc.getValues(i), pc.getBound(i)));
          else
            levels.add(new VertCoordValue(pc.getValues(i)));
        }
        return new CoordinateVert(code, tables.getVertUnit(code), levels);

      case ens:
        List<EnsCoordValue> ecoords = new ArrayList<>(pc.getValuesCount());
        for (int i = 0; i < pc.getValuesCount(); i++) {
          double val1 = pc.getValues(i);
          double val2 = pc.getBound(i);
          ecoords.add(new EnsCoordValue((int) val1, (int) val2));
        }
        return new CoordinateEns(code, ecoords);
    }
    throw new IllegalStateException("Unknown Coordinate type = " + type);
  }

  @Nullable
  private int[] readTime2Runtime(GribCollectionProto.Coord pc) {
    if (pc.getTime2RuntimeCount() > 0) {
      int[] time2runtime = new int[pc.getTime2RuntimeCount()];
      for (int i = 0; i < pc.getTime2RuntimeCount(); i++)
        time2runtime[i] = pc.getTime2Runtime(i);
      return time2runtime;
    }
    return null;
  }

  private VariableIndex importVariable(GribCollection gc,
                                       GribCollection.GroupGC group,
                                       GribCollectionProto.Variable pv) {
    int discipline = pv.getDiscipline();

    byte[] rawPds = pv.getPds().toByteArray();

    // extra id info
    int nids = pv.getIdsCount();
    int center = (nids > 0) ? pv.getIds(0) : 0;
    int subcenter = (nids > 1) ? pv.getIds(1) : 0;

    long recordsPos = pv.getRecordsPos();
    int recordsLen = pv.getRecordsLen();
    List<Integer> index = pv.getCoordIdxList();

    boolean isGrib1 = this instanceof Grib1CollectionIndexReader;
    VariableIndex result =
       new VariableIndex(isGrib1, gc, group, tables, discipline, center, subcenter, rawPds, index, recordsPos, recordsLen);

    result.ndups = pv.getNdups();
    result.nrecords = pv.getNrecords();
    result.nmissing = pv.getMissing();

    result.vpartition = importVariablePartitions(pv.getPartVariableList());

    return result;
  }

  private static Coordinate.Type importAxisType(GribCollectionProto.GribAxisType type) {
    switch (type) {
      case runtime:
        return Coordinate.Type.runtime;
      case time:
        return Coordinate.Type.time;
      case time2D:
        return Coordinate.Type.time2D;
      case timeIntv:
        return Coordinate.Type.timeIntv;
      case ens:
        return Coordinate.Type.ens;
      case vert:
        return Coordinate.Type.vert;
    }
    throw new IllegalStateException("illegal axis type " + type);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////

  @Nullable
  private Partitions importPartitions(GribCollection gc, GribCollectionProto.GribCollection proto) {
    if (proto.getPartitionsCount() == 0) {
      return null;
    }
    boolean isPartitionOfPartitions = proto.getIsPartitionOfPartitions();
    List<Partitions.Partition> partitions = proto.getPartitionsList().stream().map(it -> importPartition(it, gc.getDirectory())).toList();
    List<Integer> list = proto.getRun2PartList();

    int[] run2part = new int[list.size()];
    int count = 0;
    for (int partno : list) {
      run2part[count++] = partno;
    }
    return new Partitions(gc, isPartitionOfPartitions, partitions, run2part);
  }

  /*
message Partition {
  string name = 1;       // name is used in TDS - eg the subdirectory when generated by TimePartitionCollections
  string filename = 2;   // the gribCollection.ncx file, reletive to gc.
  uint64 lastModified = 4;
  int64 length = 5;
  int64 partitionDate = 6;  // partition date added 11/25/14
}
   */
  private Partitions.Partition importPartition(GribCollectionProto.Partition proto, File parentDirectory) {
    long partitionDateMillisecs = proto.getPartitionDate();
    CalendarDate partitionDate = partitionDateMillisecs > 0 ? CalendarDate.of(partitionDateMillisecs) : null;
    return new Partitions.Partition(proto.getName(), proto.getFilename(), proto.getLastModified(), proto.getLength(),
            partitionDate, parentDirectory);
  }

  /*
  message PartitionVariable {
  uint32 groupno = 1;
  uint32 varno = 2;
  uint32 partno = 4;

  // optionally keep stats
  uint32 ndups = 8;
  uint32 nrecords = 9;
  uint32 missing = 10;
}
   */
  @Nullable
  public Partitions.VariablePartition importVariablePartitions(List<GribCollectionProto.PartitionVariable> pvList) {
    int nparts = pvList.size();
    if (nparts == 0) {
      return null;
    }

    int[] partno = new int[nparts];
    int[] groupno = new int[nparts];
    int[] varno = new int[nparts];

    int count = 0;
    for (GribCollectionProto.PartitionVariable part : pvList) {
      partno[count] = part.getPartno();
      groupno[count] = part.getGroupno();
      varno[count] = part.getVarno();
      count++;
    }

    return new Partitions.VariablePartition(nparts,
        new SmartArrayInt(partno),
        new SmartArrayInt(groupno),
        new SmartArrayInt(varno));
  }


}
