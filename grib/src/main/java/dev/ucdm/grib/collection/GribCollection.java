/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.collection;

import com.google.common.base.MoreObjects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import dev.cdm.core.api.Attribute;
import dev.cdm.core.api.AttributeContainer;
import dev.cdm.core.api.AttributeContainerMutable;
import dev.cdm.core.calendar.CalendarDate;
import dev.cdm.core.calendar.CalendarDateFormatter;
import dev.cdm.core.calendar.CalendarDateRange;
import dev.cdm.core.constants.CDM;
import dev.cdm.core.constants.CF;
import dev.cdm.core.constants.FeatureType;
import dev.cdm.core.io.RandomAccessFile;
import dev.cdm.core.util.StringUtil2;
import dev.ucdm.grib.common.GdsHorizCoordSys;
import dev.ucdm.grib.common.GribTables;
import dev.ucdm.grib.common.util.GribUtils;
import dev.ucdm.grib.common.wmo.CommonCodeTable;
import dev.ucdm.grib.coord.*;
import dev.ucdm.grib.grib2.iosp.GribCollectionIndex;
import dev.ucdm.grib.grib2.iosp.GribConfig;

import javax.annotation.concurrent.Immutable;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * A mutable class for writing to or reading from ncx indices.
 *
 * @author John
 * @since 12/1/13
 */
public abstract class GribCollection implements Closeable {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GribCollection.class);
  public static final long MISSING_RECORD = -1;

  private static final CalendarDateFormatter cf = new CalendarDateFormatter("yyyyMMdd-HHmmss");

  static String makeName(String collectionName, CalendarDate runtime) {
    String nameNoBlanks = StringUtil2.replace(collectionName, ' ', "_");
    return nameNoBlanks + "-" + cf.toString(runtime);
  }

  static MFile makeIndexMFile(String collectionName, File directory) {
    String nameNoBlanks = StringUtil2.replace(collectionName, ' ', "_");
    // TODO dont know lastMod, size. can it be added later?
    return new GcMFile(directory, nameNoBlanks + GribCollectionIndex.NCX_SUFFIX, -1, -1, -1);
  }


  public abstract void addGlobalAttributes(AttributeContainerMutable result);

  public abstract void addVariableAttributes(AttributeContainerMutable v, VariableIndex vindex);

  public abstract String makeVariableId(VariableIndex v);

  ////////////////////////////////////////////////////////////////
  public final String name; // collection name; index filename must be directory/name.ncx2
  public final GribConfig config;
  public final boolean isGrib1;
  public File directory;
  public String orgDirectory; // wtf ??

  // set by the builder
  public int version; // the ncx version
  public int center, subcenter, master, local; // GRIB 1 uses "local" for table version
  public int genProcessType, genProcessId, backProcessId;
  public Map<Integer, MFile> fileMap; // all the files used in the GC; key is the index in original collection, GC
                                         // has subset of them
                                         public List<Dataset> datasets;
  public CoordinateRuntime masterRuntime;
  public GribTables cust;
  public int indexVersion;

  public void setCalendarDateRange(long startMsecs, long endMsecs) {
    this.dateRange = CalendarDateRange.of(CalendarDate.of(startMsecs), CalendarDate.of(endMsecs));
  }

  public CalendarDateRange dateRange;

  // not stored in index
  protected RandomAccessFile indexRaf; // this is the raf of the index (ncx) file
  protected String indexFilename;
  protected long lastModified;
  protected long fileSize;

  private static int countGC;

  protected GribCollection(String name, File directory, GribConfig config, boolean isGrib1) {
    countGC++;
    this.name = name;
    this.directory = directory;
    this.config = config;
    this.isGrib1 = isGrib1;
    if (config == null)
      logger.error("GribCollection {} has empty config%n", name);
    if (name == null)
      logger.error("GribCollection has null name dir={}%n", directory);
  }

  // for making partition collection
  void copyInfo(GribCollection from) {
    this.center = from.center;
    this.subcenter = from.subcenter;
    this.master = from.master;
    this.local = from.local;
    this.genProcessType = from.genProcessType;
    this.genProcessId = from.genProcessId;
    this.backProcessId = from.backProcessId;
  }

  public String getName() {
    return name;
  }

  public File getDirectory() {
    return directory;
  }

  public String getLocation() {
    if (indexRaf != null)
      return indexRaf.getLocation();
    return null; // getIndexFilepathInCache();
  }

  public Collection<MFile> getFiles() {
    return fileMap.values();
  }

  public GribConfig getConfig() {
    return config;
  }

  /**
   * The files that comprise the collection.
   * Actual paths, including the grib cache if used.
   *
   * @return list of filename.
   */
  public List<String> getFilenames() {
    List<String> result = new ArrayList<>();
    for (MFile file : fileMap.values())
      result.add(file.getPath());
    Collections.sort(result);
    return result;
  }

  @Nullable
  File getIndexParentFile() {
    if (indexRaf == null)
      return null;
    Path index = Paths.get(indexRaf.getLocation());
    Path parent = index.getParent();
    return parent.toFile();
  }

  public String getFilename(int fileno) {
    return fileMap.get(fileno).getPath();
  }

  public List<Dataset> getDatasets() {
    return datasets;
  }

  public Dataset makeDataset(CollectionType type) {
    Dataset result = new Dataset(type);
    datasets.add(result);
    return result;
  }

  Dataset getDatasetCanonical() {
    for (Dataset ds : datasets) {
      if (ds.gctype != CollectionType.Best)
        return ds;
    }
    throw new IllegalStateException("GC.getDatasetCanonical failed on=" + name);
  }

  public int getVersion() {
    return version;
  }

  public int getCenter() {
    return center;
  }

  public int getSubcenter() {
    return subcenter;
  }

  public int getMaster() {
    return master;
  }

  public CoordinateRuntime getMasterRuntime() {
    return masterRuntime;
  }

  public int getLocal() {
    return local;
  }

  public int getGenProcessType() {
    return genProcessType;
  }

  public int getGenProcessId() {
    return genProcessId;
  }

  public int getBackProcessId() {
    return backProcessId;
  }

  public void setFileMap(Map<Integer, MFile> fileMap) {
    this.fileMap = fileMap;
  }

  /**
   * public by accident, do not use
   *
   * @param indexRaf the open raf of the index file
   */
  public void setIndexRaf(RandomAccessFile indexRaf) {
    this.indexRaf = indexRaf;
    if (indexRaf != null) {
      this.indexFilename = indexRaf.getLocation();
    }
  }

  /**
   * get index filename
   *
   * @return index filename; may not exist; may be in disk cache
   *
  private String getIndexFilepathInCache() {
    File indexFile = GribCdmIndex.makeIndexFile(name, directory);
    return GribIndexCache.getFileOrCache(indexFile.getPath()).getPath();
  } */

  // set from GribCollectionBuilderFromIndex.readFromIndex()
  public File setOrgDirectory(String orgDirectory) {
    this.orgDirectory = orgDirectory;
    directory = new File(orgDirectory);
    if (!directory.exists()) {
      File indexFile = new File(indexFilename);
      File parent = indexFile.getParentFile();
      if (parent.exists())
        directory = parent;
    }
    return directory;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // stuff for FileCacheable

  public void close() throws java.io.IOException {

    if (indexRaf != null) {
      indexRaf.close();
      indexRaf = null;
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////

  public class Dataset {
    public CollectionType gctype;
    public List<GroupGC> groups; // must be kept in order, because PartitionForVariable2D has index into it

    public Dataset(CollectionType type) {
      this.gctype = type;
      groups = new ArrayList<>();
    }

    Dataset(Dataset from) {
      this.gctype = from.gctype;
      groups = new ArrayList<>(from.groups.size());
    }

    GroupGC addGroupCopy(GroupGC from) {
      GroupGC g = new GroupGC(from);
      groups.add(g);
      return g;
    }

    public List<GroupGC> getGroups() {
      return groups;
    }

    public CollectionType getType() {
      return gctype;
    }
  }

  public class GroupGC implements Comparable<GroupGC> {
    public GribHorizCoordSystem horizCoordSys;
    public final List<VariableIndex> variList;
    public List<Coordinate> coords; // shared coordinates
    public int[] filenose; // key for GC.fileMap
    HashMap<VariableIndex, VariableIndex> varMap;
    public boolean isTwoD = true; // true except for Best (?)

    GroupGC() {
      this.variList = new ArrayList<>();
      this.coords = new ArrayList<>();
    }

    // copy constructor for PartitionBuilder
    GroupGC(GroupGC from) {
      this.horizCoordSys = from.horizCoordSys; // reference
      this.variList = new ArrayList<>(from.variList.size()); // empty list
      this.coords = new ArrayList<>(from.coords.size()); // empty list
      this.isTwoD = from.isTwoD;
    }

    public VariableIndex addVariable(VariableIndex vi) {
      variList.add(vi);
      return vi;
    }

    public GribCollection getGribCollection() {
      return GribCollection.this;
    }

    public Iterable<VariableIndex> getVariables() {
      return variList;
    }

    public Iterable<Coordinate> getCoordinates() {
      return coords;
    }

    // unique name for Group
    public String getId() {
      return horizCoordSys.getId();
    }

    // human readable
    public String getDescription() {
      return horizCoordSys.getDescription();
    }

    public byte[] getGdsBytes() {
      return horizCoordSys.getRawGds();
    }

    public Object getGdsHash() {
      return horizCoordSys.getGdsHash();
    }

    public GdsHorizCoordSys getGdsHorizCoordSys() {
      return horizCoordSys.getHcs();
    }


    @Override
    public int compareTo(@Nonnull GroupGC o) {
      return getDescription().compareTo(o.getDescription());
    }

    public List<MFile> getFiles() {
      List<MFile> result = new ArrayList<>();
      if (filenose == null)
        return result;
      for (int fileno : filenose)
        result.add(fileMap.get(fileno));
      Collections.sort(result);
      return result;
    }

    public List<String> getFilenames() {
      List<String> result = new ArrayList<>();
      if (filenose == null)
        return result;
      for (int fileno : filenose)
        result.add(fileMap.get(fileno).getPath());
      Collections.sort(result);
      return result;
    }

    // get the variable in this group that has same object equality as want
    public VariableIndex findVariableByHash(VariableIndex want) {
      if (varMap == null) {
        varMap = new HashMap<>(variList.size() * 2);
        for (VariableIndex vi : variList) {
          VariableIndex old = varMap.put(vi, vi);
          if (old != null) {
            logger.error("GribCollectionMutable has duplicate variable hash {} == {}", vi, old);
          }
        }
      }
      return varMap.get(want);
    }

    private CalendarDateRange dateRange;

    public CalendarDateRange getCalendarDateRange() {
      if (dateRange == null) {
        CalendarDateRange result = null;
        for (Coordinate coord : coords) {
          switch (coord.getType()) {
            case time:
            case timeIntv:
            case time2D:
              CoordinateTimeAbstract time = (CoordinateTimeAbstract) coord;
              CalendarDateRange range = time.makeCalendarDateRange();
              if (result == null)
                result = range;
              else
                result = result.extend(range);
          }
        }
        dateRange = result;
      }
      return dateRange;
    }

    public int getNFiles() {
      if (filenose == null)
        return 0;
      return filenose.length;
    }

    public void show(Formatter f) {
      f.format("Group %s (%d) isTwoD=%s%n", horizCoordSys.getId(), horizCoordSys.getGdsHash().hashCode(), isTwoD);
      f.format(" nfiles %d%n", filenose == null ? 0 : filenose.length);
      f.format(" hcs = %s%n", horizCoordSys.getHcs());
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("horizCoordSys", horizCoordSys).add("variList", variList)
          .add("coords", coords).add("filenose", filenose).add("varMap", varMap).add("isTwoD", isTwoD)
          .add("dateRange", dateRange).toString();
    }
  }

  public VariableIndex makeVariableIndex(GroupGC g, GribTables customizer, int discipline, int center,
                                         int subcenter, byte[] rawPds, List<Integer> index, long recordsPos,
                                         int recordsLen) {
    return new VariableIndex(config, g, customizer, discipline, center, subcenter, rawPds, index, recordsPos, recordsLen);
  }

  VariableIndex makeVariableIndex(GroupGC group, VariableIndex from) {
    VariableIndex vip = new VariableIndex(group, from);
    group.addVariable(vip);
    return vip;
  }

  public AttributeContainer makeGlobalAttributes() {
    AttributeContainerMutable result = new AttributeContainerMutable(name);
    String centerName = CommonCodeTable.getCenterName(getCenter(), 2);
    result.addAttribute(new Attribute(GribUtils.CENTER, centerName));
    String val = cust.getSubCenterName(getCenter(), getSubcenter());
    result.addAttribute(new Attribute(GribUtils.SUBCENTER, val == null ? Integer.toString(getSubcenter()) : val));
    result.addAttribute(new Attribute(GribUtils.TABLE_VERSION, getMaster() + "," + getLocal()));

    addGlobalAttributes(result); // add subclass atts

    result.addAttribute(new Attribute(CDM.CONVENTIONS, "CF-1.6"));
    result.addAttribute(new Attribute(CDM.HISTORY, "Read using CDM IOSP GribCollection v3"));
    result.addAttribute(new Attribute(CF.FEATURE_TYPE, FeatureType.GRID.name()));

    return result.toImmutable();
  }

  @Immutable
  public record ReadRecord(int fileno, long pos, int bmsOffset, int drsOffset) {
  }

/*  @Immutable
  public static class Record {
    public final int fileno; // which file
    public final long pos; // offset on file where data starts
    public final long bmsPos; // if non-zero, offset where bms starts
    public final int scanMode; // from gds

    public Record(int fileno, long pos, long bmsPos, int scanMode) {
      this.fileno = fileno;
      this.pos = pos;
      this.bmsPos = bmsPos;
      this.scanMode = scanMode;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("fileno", fileno).add("pos", pos).add("bmsPos", bmsPos)
          .add("scanMode", scanMode).toString();
    }
  } */

  public void showIndex(Formatter f) {
    f.format("Class (%s)%n", getClass().getName());
    f.format("%s%n%n", this);

    for (Dataset ds : datasets) {
      f.format("Dataset %s%n", ds.gctype);
      for (GroupGC g : ds.groups) {
        f.format(" Group %s%n", g.horizCoordSys.getId());
        for (VariableIndex v : g.variList) {
          f.format("  %s%n", v.toStringShort());
        }
      }
    }
    if (fileMap == null) {
      f.format("Files empty%n");
    } else {
      f.format("Files (%d)%n", fileMap.size());
      for (int index : fileMap.keySet()) {
        f.format("  %d: %s%n", index, fileMap.get(index));
      }
      f.format("%n");
    }
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("name", name).add("config", config).add("isGrib1", isGrib1)
        .add("directory", directory).add("orgDirectory", orgDirectory).add("version", version).add("center", center)
        .add("subcenter", subcenter).add("master", master).add("local", local).add("genProcessType", genProcessType)
        .add("genProcessId", genProcessId).add("backProcessId", backProcessId).add("fileMap", fileMap)
        .add("datasets", datasets).add("masterRuntime", masterRuntime).add("cust", cust)
        .add("indexVersion", indexVersion).add("dateRange", dateRange).add("indexRaf", indexRaf)
        .add("indexFilename", indexFilename).add("lastModified", lastModified).add("fileSize", fileSize).toString();
  }

  public String showLocation() {
    return "name=" + name + " directory=" + directory;
  }

  public GroupGC makeGroup() {
    return new GroupGC();
  }

  public RandomAccessFile getDataRaf(int fileno) throws IOException {
    // absolute location
    MFile mfile = fileMap.get(fileno);
    String filename = mfile.getPath();
    File dataFile = new File(filename);

    // if data file does not exist, check reletive location - eg may be /upc/share instead of Q:
    if (!dataFile.exists()) {
      if (fileMap.size() == 1) {
        dataFile = new File(directory, name); // single file case
      } else {
        dataFile = new File(directory, dataFile.getName()); // must be in same directory as the ncx file
      }
    }

    // data file not here
    if (!dataFile.exists()) {
      throw new FileNotFoundException("data file not found = " + dataFile.getPath());
    }

    // TODO what about the cache ??
    // RandomAccessFile want = RandomAccessFile.acquire(dataFile.getPath());
    RandomAccessFile want = new RandomAccessFile(dataFile.getPath(), "r");
    want.order(RandomAccessFile.BIG_ENDIAN);
    return want;
  }

}

