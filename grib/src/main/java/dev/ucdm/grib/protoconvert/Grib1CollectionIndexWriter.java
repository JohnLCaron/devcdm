/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.protoconvert;

import java.nio.charset.StandardCharsets;

import com.google.protobuf.ByteString;
import dev.ucdm.grib.collection.CollectionType;
import dev.ucdm.grib.inventory.GcMFile;
import dev.ucdm.grib.collection.Grib1CollectionBuilder;
import dev.ucdm.grib.collection.GribCollectionBuilder;
import dev.ucdm.core.calendar.CalendarDate;
import dev.ucdm.core.calendar.CalendarDateRange;
import dev.ucdm.core.io.RandomAccessFile;
import dev.ucdm.grib.inventory.MCollection;
import dev.ucdm.grib.inventory.MFile;
import dev.ucdm.grib.coord.*;
import dev.ucdm.grib.grib1.record.*;
import dev.ucdm.grib.protogen.GribCollectionProto;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Convert Grib1 collection indexes (ncx4) to/from proto.
 * The generated proto code is in dev.ucdm.grib.protogen.GribCollectionProto.
 */
public class Grib1CollectionIndexWriter extends GribCollectionIndexWriter {

  public static final String MAGIC_START = "Grib1Collectio2Index"; // was Grib1CollectionIndex
  static final int minVersion = 1; // if less than this, force rewrite or at least do not read
  protected static final int version = 3; // increment this as needed, must be backwards compatible through minVersion

  public Grib1CollectionIndexWriter(MCollection dcm, org.slf4j.Logger logger) {
    super(dcm, logger);
  }

  public static class Group implements GribCollectionBuilder.Group {

    final Grib1SectionGridDefinition gdss;
    public final int hashCode;
    public final CalendarDate runtime;

    public List<Grib1CollectionBuilder.VariableBag> gribVars;
    public List<Coordinate> coords;

    public final Set<Long> runtimes = new HashSet<>();
    public final List<Grib1Record> records = new ArrayList<>();
    Set<Integer> fileSet; // this is so we can show just the component files that are in this group

    public Group(Grib1SectionGridDefinition gdss, int hashCode, CalendarDate runtime) {
      this.gdss = gdss;
      this.hashCode = hashCode;
      this.runtime = runtime;
    }

    @Override
    public CalendarDate getRuntime() {
      return runtime;
    }

    @Override
    public List<Coordinate> getCoordinates() {
      return coords;
    }

    @Override
    public Set<Long> getCoordinateRuntimes() {
      return runtimes;
    }
  }

  /*
   * MAGIC_START
   * version
   * sizeRecords
   * VariableRecords (sizeRecords bytes)
   * sizeIndex
   * GribCollectionIndex (sizeIndex bytes)
   */

  // indexFile is in the cache
  public boolean writeIndex(String name, File idxFile, CoordinateRuntime masterRuntime, List<Group> groups, List<MFile> files,
                            CollectionType type, CalendarDateRange dateRange) throws IOException {
    Grib1Record first = null; // take global metadata from here
    boolean deleteOnClose = false;

    if (idxFile.exists()) {
      // TODO RandomAccessFile.eject(idxFile.getPath());
      if (!idxFile.delete()) {
        logger.warn(" gc1 cant delete index file {}", idxFile.getPath());
      }
    }
    logger.debug(" createIndex for {}", idxFile.getPath());

    try (RandomAccessFile raf = new RandomAccessFile(idxFile.getPath(), "rw")) {
      raf.order(RandomAccessFile.BIG_ENDIAN);

      //// header message
      raf.write(MAGIC_START.getBytes(StandardCharsets.UTF_8));
      raf.writeInt(version);
      long lenPos = raf.getFilePointer();
      raf.writeLong(0); // save space to write the length of the record section
      long countBytes = 0;
      int countRecords = 0;

      Set<Integer> allFileSet = new HashSet<>();
      for (Group g : groups) {
        g.fileSet = new HashSet<>();
        for (Grib1CollectionBuilder.VariableBag vb : g.gribVars) {
          if (first == null) {
            first = vb.first;
          }
          GribCollectionProto.SparseArray vr = publishSparseArray(vb, g.fileSet);
          byte[] b = vr.toByteArray();
          vb.pos = raf.getFilePointer();
          vb.length = b.length;
          raf.write(b);
          countBytes += b.length;
          countRecords += vb.coordND.getSparseArray().countNotMissing();
        }
        allFileSet.addAll(g.fileSet);
      }
      long bytesPerRecord = countBytes / ((countRecords == 0) ? 1 : countRecords);
      if (logger.isDebugEnabled()) {
        logger.debug("  write RecordMaps: bytes = {} records = {} bytesPerRecord={}", countBytes, countRecords,
            bytesPerRecord);
      }

      if (first == null) {
        deleteOnClose = true;
        throw new IOException("GribCollection " + name + " has no records");
      }

      long pos = raf.getFilePointer();
      raf.seek(lenPos);
      raf.writeLong(countBytes);
      raf.seek(pos); // back to the output.


      /*
       * message GribCollection {
       * string name = 1; // must be unique - index filename is name.ncx
       * string topDir = 2; // MFile, Partition filenames are reletive to this
       * repeated MFile mfiles = 3; // list of grib MFiles
       * repeated Dataset dataset = 4;
       * repeated Gds gds = 5; // unique Gds, shared amongst datasets
       * Coord masterRuntime = 6; // list of runtimes in this GC
       * 
       * int32 center = 7; // these 4 fields are to get a GribCustomizer
       * int32 subcenter = 8;
       * int32 master = 9;
       * int32 local = 10; // grib1 table Version
       * 
       * int32 genProcessType = 11;
       * int32 genProcessId = 12;
       * int32 backProcessId = 13;
       * int32 version = 14; // >= 3 for proto3 (5.0+)
       * 
       * // repeated Parameter params = 20; // not used
       * FcConfig config = 21;
       * uint64 startTime = 22; // calendar date, first valid time
       * uint64 endTime = 23; // calendar date, last valid time
       * 
       * // extensions
       * repeated Partition partitions = 100;
       * bool isPartitionOfPartitions = 101;
       * repeated uint32 run2part = 102 [packed=true]; // masterRuntime index to partition index
       * }
       */

      GribCollectionProto.GribCollection.Builder indexBuilder = GribCollectionProto.GribCollection.newBuilder();
      indexBuilder.setName(name);
      indexBuilder.setTopDir(dcm.getRoot());
      indexBuilder.setVersion(currentVersion);

      // directory and mfile list
      File directory = new File(dcm.getRoot());
      List<GcMFile> gcmfiles = GcMFile.makeFiles(directory, files, allFileSet);
      for (GcMFile gcmfile : gcmfiles) {
        GribCollectionProto.MFile.Builder b = GribCollectionProto.MFile.newBuilder();
        b.setFilename(gcmfile.getShortName());
        b.setLastModified(gcmfile.getLastModified());
        b.setLength(gcmfile.getLength());
        b.setIndex(gcmfile.index);
        indexBuilder.addMfiles(b.build());
      }

      indexBuilder.setMasterRuntime(publishCoordinateRuntime(masterRuntime));

      // gds
      for (Group g : groups) {
        indexBuilder.addGds(writeGdsProto(g.gdss.getRawBytes(), g.gdss.getPredefinedGridDefinition()));
      }

      // the GC dataset
      indexBuilder.addDataset(publishDatasetProto(type, groups));

      // what about just storing first ??
      Grib1SectionProductDefinition pds = first.getPDSsection();
      indexBuilder.setCenter(pds.getCenter());
      indexBuilder.setSubcenter(pds.getSubCenter());
      indexBuilder.setLocal(pds.getTableVersion());
      indexBuilder.setMaster(0);
      indexBuilder.setGenProcessId(pds.getGenProcess());

      indexBuilder.setStartTime(dateRange.getStart().getMillisFromEpoch());
      indexBuilder.setEndTime(dateRange.getEnd().getMillisFromEpoch());

      GribCollectionProto.GribCollection index = indexBuilder.build();
      byte[] b = index.toByteArray();
      Streams.writeVInt(raf, b.length); // message size
      raf.write(b); // message - all in one gulp

      logger.debug("  write GribCollectionIndex= {} bytes", b.length);
      logger.debug("  file size =  {} bytes", raf.length());
      return true;

    } finally {

      // remove it on failure
      if (deleteOnClose && !idxFile.delete()) {
        logger.error(" gc1 cant deleteOnClose index file {}", idxFile.getPath());
      }
    }
  }

  /*
   * message Record {
   * uint32 fileno = 1; // which GRIB file ? key into GC.fileMap
   * uint64 pos = 2; // offset in GRIB file of the start of entire message
   * uint64 bmsPos = 3; // use alternate bms if non-zero (grib2 only)
   * uint32 drsOffset = 4; // offset of drs from pos (grib2 only)
   * }
   * 
   * // SparseArray only at the GCs (MRC and SRC) not at the Partitions
   * // dont need SparseArray in memory until someone wants to read from the variable
   * message SparseArray {
   * repeated uint32 size = 2 [packed=true]; // multidim sizes = shape[]
   * repeated uint32 track = 3 [packed=true]; // 1-based index into record list, 0 == missing
   * repeated Record records = 4; // List<Record>
   * uint32 ndups = 5; // duplicates found when creating
   * }
   */
  private GribCollectionProto.SparseArray publishSparseArray(Grib1CollectionBuilder.VariableBag vb,
                                                             Set<Integer> fileSet) {
    GribCollectionProto.SparseArray.Builder b = GribCollectionProto.SparseArray.newBuilder();
    SparseArray<Grib1Record> sa = vb.coordND.getSparseArray();
    for (int size : sa.getShape()) {
      b.addSize(size);
    }
    for (int track : sa.getTrack()) {
      b.addTrack(track);
    }

    for (Grib1Record gr : sa.getContent()) {
      GribCollectionProto.Record.Builder br = GribCollectionProto.Record.newBuilder();

      br.setFileno(gr.getFile());
      fileSet.add(gr.getFile());
      Grib1SectionIndicator is = gr.getIs();
      br.setStartPos(is.getStartPos()); // start of entire message

      b.addRecords(br);
    }

    b.setNdups(sa.getNdups());
    return b.build();
  }

  /*
   * message Dataset {
   * Type type = 1;
   * repeated Group groups = 2;
   */
  private GribCollectionProto.Dataset publishDatasetProto(CollectionType type, List<Group> groups) {
    GribCollectionProto.Dataset.Builder b = GribCollectionProto.Dataset.newBuilder();

    GribCollectionProto.Dataset.Type ptype = GribCollectionProto.Dataset.Type.valueOf(type.toString());
    b.setType(ptype);

    for (Group group : groups) {
      b.addGroups(publishGroupProto(group));
    }

    return b.build();
  }

  /*
   * message Group {
   * Gds gds = 1; // use this to build the HorizCoordSys
   * repeated Variable variables = 2; // list of variables
   * repeated Coord coords = 3; // list of coordinates
   * repeated int32 fileno = 4 [packed=true]; // the component files that are in this group, key into gc.mfiles
   * }
   */
  private GribCollectionProto.Group publishGroupProto(Group g) {
    GribCollectionProto.Group.Builder b = GribCollectionProto.Group.newBuilder();

    b.setGds(writeGdsProto(g.gdss.getRawBytes(), g.gdss.getPredefinedGridDefinition()));

    for (Grib1CollectionBuilder.VariableBag vbag : g.gribVars) {
      b.addVariables(publishVariableProto(vbag));
    }

    for (Coordinate coord : g.coords) {
      switch (coord.getType()) {
        case runtime -> b.addCoords(publishCoordinateRuntime((CoordinateRuntime) coord));
        case time -> b.addCoords(publishCoordinateTime((CoordinateTime) coord));
        case timeIntv -> b.addCoords(publishCoordinateTimeIntv((CoordinateTimeIntv) coord));
        case time2D -> b.addCoords(publishCoordinateTime2D((CoordinateTime2D) coord));
        case vert -> b.addCoords(publishCoordinateVert((CoordinateVert) coord));
        case ens -> b.addCoords(publishCoordinateEns((CoordinateEns) coord));
      }
    }

    for (Integer aFileSet : g.fileSet) {
      b.addFileno(aFileSet);
    }

    return b.build();
  }

  private GribCollectionProto.Variable publishVariableProto(Grib1CollectionBuilder.VariableBag vb) {
    GribCollectionProto.Variable.Builder b = GribCollectionProto.Variable.newBuilder();

    b.setDiscipline(0);
    b.setPds(ByteString.copyFrom(vb.first.getPDSsection().getRawBytes()));

    b.setRecordsPos(vb.pos);
    b.setRecordsLen(vb.length);

    for (int idx : vb.coordIndex) {
      b.addCoordIdx(idx);
    }

    // keep stats
    SparseArray sa = vb.coordND.getSparseArray();
    if (sa != null) {
      b.setNdups(sa.getNdups());
      b.setNrecords(sa.countNotMissing());
      b.setMissing(sa.countMissing());
    }

    return b.build();
  }

}
