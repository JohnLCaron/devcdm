/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.collection;

import dev.ucdm.core.calendar.CalendarDate;
import dev.ucdm.core.calendar.CalendarDateRange;
import dev.ucdm.core.util.StringUtil2;
import dev.ucdm.grib.common.GribCollectionIndex;
import dev.ucdm.grib.common.GribConfig;
import dev.ucdm.grib.common.util.GribIndexCache;
import dev.ucdm.grib.common.util.SmartArrayInt;
import dev.ucdm.grib.coord.Coordinate;
import dev.ucdm.grib.coord.CoordinateRuntime;
import dev.ucdm.grib.coord.CoordinateTimeAbstract;
import dev.ucdm.grib.inventory.MCollection;
import dev.ucdm.grib.inventory.MFile;
import dev.ucdm.grib.inventory.MPartition;
import dev.ucdm.grib.protoconvert.GribHorizCoordSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GribPartitionBuilder creates GribPartition which is written by GribPartitionIndexWriter to an index.
 * This is the writing part; reading existing indexes create GribCollections and Partitions.
 */
public class GribPartition {
  private static final Logger logger = LoggerFactory.getLogger(GribPartition.class);

  public class DatasetP {
    public CollectionType gctype;
    public List<GroupP> groups; // must be kept in order, because PartitionForVariable2D has index into it

    public DatasetP(CollectionType type) {
      this.gctype = type;
      groups = new ArrayList<>();
    }

    public List<GroupP> getGroups() {
      return groups;
    }

    public CollectionType getType() {
      return gctype;
    }

    GroupP addGroupCopy(GribCollection.GroupGC from) {
      GroupP g = new GroupP(from);
      groups.add(g);
      return g;
    }
  }

  public class GroupP {
    public GribHorizCoordSystem horizCoordSys;
    public final List<VariableIndexPartitioned> variList;
    public List<Coordinate> coords; // shared coordinates
    public int[] filenose; // key for GC.fileMap
    HashMap<Object, VariableIndexPartitioned> varHashCodes = new HashMap<>();

    // copy constructor for PartitionBuilder
    GroupP(GribCollection.GroupGC from) {
      this.horizCoordSys = from.horizCoordSys; // reference
      this.variList = new ArrayList<>(from.variList.size()); // empty list
      this.coords = new ArrayList<>(from.coords.size()); // empty list
    }

    public VariableIndexPartitioned addVariable(VariableIndexPartitioned vip) {
      VariableIndexPartitioned old = varHashCodes.put(vip.vi.gribVariable, vip);
      if (old != null) {
        logger.error("GribPartition has duplicate VariableIndexPartitioned {} == {}", vip, old);
      } else {
        variList.add(vip);
      }
      return vip;
    }

    // unique name for Group
    public String getId() {
      return horizCoordSys.getId();
    }

    // human readable
    public String getDescription() {
      return horizCoordSys.getDescription();
    }

    // get the variable in this group that has same object equality as gribVariable
    public VariableIndexPartitioned findVariableByHash(Object gribVariable) {
      return varHashCodes.get(gribVariable);
    }

    private CalendarDateRange dateRange;

    public CalendarDateRange getCalendarDateRange() {
      if (dateRange == null) {
        CalendarDateRange result = null;
        for (Coordinate coord : coords) {
          switch (coord.getType()) {
            case time, timeIntv, time2D -> {
              CoordinateTimeAbstract time = (CoordinateTimeAbstract) coord;
              CalendarDateRange range = time.makeCalendarDateRange();
              if (result == null)
                result = range;
              else
                result = result.extend(range);
            }
          }
        }
        dateRange = result;
      }
      return dateRange;
    }
  }

  record PartitionForVariable2D(int partno, int groupno, int varno) {}

  public class VariableIndexPartitioned {
    public final GroupP group;
    public final VariableIndex vi;
    public final int nparts;

    public SmartArrayInt partnoSA;
    public SmartArrayInt groupnoSA;
    public SmartArrayInt varnoSA;
    public  List<PartitionForVariable2D> partList;
    public  List<Coordinate> coords; // used ?
    public List<Integer> coordIndex; // indexes into group.coords

    // stats
    public int ndups, nrecords, nmissing;

    VariableIndexPartitioned(GroupP group, VariableIndex vi, int nparts) {
      this.group = group;
      this.vi = vi;
      this.nparts = nparts;
    }

    public String id() {
      return vi.id();
    }

    public int getVarid() {
      return vi.getVarid();
    }

    public void finish() {
      if (partList == null)
        return; // nothing to do
      if (partList.size() > nparts) // might be smaller due to failed partition
        logger.warn("PartitionCollectionMutable partList.size() > nparts, vi = " + vi.id());

      int[] partno = new int[nparts];
      int[] groupno = new int[nparts];
      int[] varno = new int[nparts];
      int count = 0;
      for (PartitionForVariable2D part : partList) {
        partno[count] = part.partno;
        groupno[count] = part.groupno;
        varno[count] = part.varno;
        count++;
      }
      this.partnoSA = new SmartArrayInt(partno);
      this.groupnoSA = new SmartArrayInt(groupno);
      this.varnoSA = new SmartArrayInt(varno);

      partList = null; // GC
    }

    void addPartition(int partno, int groupno, int varno, int ndups, int nrecords, int nmissing) {
      if (partList == null)
        partList = new ArrayList<>(nparts);
      partList.add(new PartitionForVariable2D(partno, groupno, varno));
      this.ndups += ndups;
      this.nrecords += nrecords;
      this.nmissing += nmissing;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      VariableIndexPartitioned that = (VariableIndexPartitioned) o;
      return vi.equals(that.vi);
    }

    @Override
    public int hashCode() {
      return vi.hashCode();
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////

  // wrapper around the children GribCollections, allows us to manipulate them without always opening the grib collection.
  public static class ChildCollection implements Comparable<ChildCollection> {
    public final String name;
    public final String directory;
    // public long lastModified;
    public long fileSize;
    public CalendarDate partitionDate;
    public String indexFilename;

    // constructor from a MCollection object
    public ChildCollection(MCollection dcm) {
      this.indexFilename = dcm.getIndexFilename();
      this.name = dcm.getCollectionName();
      // this.lastModified = dcm.getLastModified().getMillisFromEpoch();
      this.directory = StringUtil2.replace(dcm.getRoot(), '\\', "/");
      // LOOK this.partitionDate = dcm.getPartitionDate();

      /*
      String indexFilename = StringUtil2.replace(dcm.getIndexFilename(), '\\', "/");
      //if (partitionDate == null) {
      //  partitionDate = getDateExtractor().getCalendarDateFromPath(indexFilename); // TODO dicey
      //}

      // now remove the directory
      if (indexFilename.startsWith(directory)) {
        indexFilename = indexFilename.substring(directory.length());
        if (indexFilename.startsWith("/"))
          indexFilename = indexFilename.substring(1);
      }
      filename = indexFilename; */
    }

    // constructor from a MPartition object
    public ChildCollection(MPartition tpm) {
      this.indexFilename = tpm.getIndexFilename();
      this.name = tpm.getCollectionName();
      // this.lastModified = tpm.getLastModified().getMillisFromEpoch();
      this.directory = StringUtil2.replace(tpm.getRoot(), '\\', "/");
      // LOOK this.partitionDate = dcm.getPartitionDate();

      /*
      String indexFilename = StringUtil2.replace(tpm.getIndexFilename(), '\\', "/");
      //if (partitionDate == null) {
      //  partitionDate = getDateExtractor().getCalendarDateFromPath(indexFilename); // TODO dicey
      //}

      // now remove the directory
      if (indexFilename.startsWith(directory)) {
        indexFilename = indexFilename.substring(directory.length());
        if (indexFilename.startsWith("/"))
          indexFilename = indexFilename.substring(1);
      }
      filename = indexFilename; */
    }

    @Nullable
    String getIndexFilenameInCache() {
      File file = new File(directory, indexFilename);
      File existingFile = GribIndexCache.getExistingFileOrCache(file.getPath());
      /* LOOK if (existingFile == null) {
        // try reletive to index file
        File parent = getIndexParentFile();
        if (parent == null)
          return null;
        existingFile = new File(parent, filename);
        if (!existingFile.exists())
          return null;
      } */
      return existingFile.getPath();
    }

    // the children must already exist
    @Nullable
    public GribCollection makeGribCollection() throws IOException {
      Formatter errlog = new Formatter();

      GribCollection result = GribCollectionIndex.readCollectionFromIndex(indexFilename, true, errlog);
      if (result == null) {
        logger.error("Failed on readCollectionFromIndex {} '{}'", indexFilename, errlog);
        return null;
      }

      // lastModified = result.lastModified;
      fileSize = result.fileSize;
      if (result.masterRuntime != null) {
        partitionDate = result.masterRuntime.getFirstDate();
      }
      return result;
    }

    @Override
    public int compareTo(@Nonnull ChildCollection o) {
      if (partitionDate != null && o.partitionDate != null) {
        return partitionDate.compareTo(o.partitionDate);
      }
      return name.compareTo(o.name);
    }

    @Override
    public String toString() {
      return String.format("Partition{name='%s', directory='%s', indexFilename='%s' partitionDate='%s'",
              name, directory, indexFilename, partitionDate);
    }

  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public final String name;
  public final File directory;
  public final GribConfig config;
  public final boolean isGrib1;
  public final List<DatasetP> datasets = new ArrayList<>();

  public GribCollection canonicalGC;
  public List<ChildCollection> childCollections;
  public boolean isPartitionOfPartitions;
  public CoordinateRuntime masterRuntime;
  public Map<Integer, MFile> fileMap;
  public int[] run2part; // masterRuntime.length; which partition to use for masterRuntime i
  public CalendarDateRange dateRange;

  protected GribPartition(String name, File directory, GribConfig config, boolean isGrib1) {
    this.name = name;
    this.directory = directory;
    this.config = config;
    this.isGrib1 = isGrib1;
    this.childCollections = new ArrayList<>();
  }

  public int getCenter() {
    return canonicalGC.center;
  }

  public int getSubcenter() {
    return canonicalGC.subcenter;
  }

  public int getMaster() {
    return canonicalGC.master;
  }

  public int getLocal() {
    return canonicalGC.local;
  }

  public int getGenProcessType() {
    return canonicalGC.genProcessType;
  }

  public int getGenProcessId() {
    return canonicalGC.genProcessId;
  }

  public int getBackProcessId() {
    return canonicalGC.backProcessId;
  }

  public void addChildCollection(MCollection dcm) {
    ChildCollection partition = new ChildCollection(dcm);
    try (GribCollection gc = partition.makeGribCollection()) { // make sure we can open the collection
      if (gc == null) {
        logger.warn("failed to open collection {} =skipping", dcm.getIndexFilename());
      } else {
        childCollections.add(partition);
      }
    } catch (Exception e) {
      logger.warn("failed to open collection {} -skipping", dcm.getIndexFilename(), e);
    }
  }

  public void addChildCollection(MPartition tcp) {
    ChildCollection partition = new ChildCollection(tcp);
    try (GribCollection gc = partition.makeGribCollection()) { // make sure we can open the collection
      if (gc == null) {
        logger.warn("failed to open partition {} =skipping", tcp.getIndexFilename());
      } else {
        childCollections.add(partition);
      }
    } catch (Exception e) {
      logger.warn("failed to open partition {} -skipping", tcp.getIndexFilename(), e);
    }
  }

  void sortChildCollections() {
    Collections.sort(childCollections);
    childCollections = Collections.unmodifiableList(childCollections);
  }

  public String showLocation() {
    return "name=" + name + " directory=" + directory;
  }

  void setCanonicalCollection(GribCollection gc) {
    this.canonicalGC = gc;
  }

  DatasetP makeDataset(CollectionType type) {
    DatasetP result = new DatasetP(type);
    datasets.add(result);
    return result;
  }

  /**
   * Create a VariableIndexPartitioned, add it to the given group
   *
   * @param group the new VariableIndexPartitioned is in this group
   * @param from copy info from here
   * @param nparts size of partition list
   * @return a new VariableIndexPartitioned
   */
  VariableIndexPartitioned makeVariableIndexPartitioned(GroupP group, VariableIndex from, int nparts) {

    VariableIndexPartitioned vip = new VariableIndexPartitioned(group, from, nparts);
    group.addVariable(vip);

    /* LOOK wtf? isPartitionOfPartitions?
    if (from instanceof VariableIndexPartitioned && !isPartitionOfPartitions) {
      VariableIndexPartitioned vipFrom = (VariableIndexPartitioned) from;
      Preconditions.checkArgument(vipFrom.partList == null); // // check if vipFrom has been finished
      for (int i = 0; i < vipFrom.nparts; i++)
        vip.addPartition(vipFrom.partnoSA.get(i), vipFrom.groupnoSA.get(i), vipFrom.varnoSA.get(i), 0, 0, 0, vipFrom);
    }

     */

    return vip;
  }

  ChildCollection getPartition(int idx) {
    return childCollections.get(idx);
  }

}
