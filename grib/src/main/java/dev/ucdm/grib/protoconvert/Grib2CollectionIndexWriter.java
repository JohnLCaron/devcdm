/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.protoconvert;

import com.google.protobuf.ByteString;
import dev.ucdm.core.calendar.CalendarDate;
import dev.ucdm.core.calendar.CalendarDateRange;
import dev.ucdm.core.io.RandomAccessFile;
import dev.ucdm.grib.collection.CollectionType;
import dev.ucdm.grib.inventory.GcMFile;
import dev.ucdm.grib.collection.Grib2CollectionBuilder;
import dev.ucdm.grib.collection.GribCollectionBuilder;
import dev.ucdm.grib.inventory.MCollection;
import dev.ucdm.grib.inventory.MFile;
import dev.ucdm.grib.coord.*;
import dev.ucdm.grib.grib2.record.*;
import dev.ucdm.grib.protogen.GribCollectionProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Convert Grib2 collection indexes (ncx4) to/from proto.
 * The generated proto code is in dev.ucdm.grib.protogen.GribCollectionProto.
 */
public class Grib2CollectionIndexWriter extends GribCollectionIndexWriter {
  private static final Logger logger = LoggerFactory.getLogger(Grib2CollectionIndexWriter.class);

  public static final String MAGIC_START = "Grib2Collectio2Index"; // was Grib2CollectionIndex
  static final int minVersion = 1; // increment this when you want to force index rebuild
  protected static final int version = 3; // increment this as needed, must be backwards compatible through minVersion

  private final MCollection dcm;
  public Grib2CollectionIndexWriter(MCollection dcm) {
    this.dcm = dcm;
  }

  public static class Group implements GribCollectionBuilder.Group {
    public final Grib2SectionGridDefinition gdss;
    public final int hashCode; // may have been modified
    public CalendarDate runtime;

    public List<Grib2CollectionBuilder.VariableBag> gribVars = new ArrayList<>();
    public List<Coordinate> coords;
    public final List<Grib2Record> records = new ArrayList<>();
    public final Set<Long> runtimes = new HashSet<>();
    Set<Integer> fileSet; // this is so we can show just the component files that are in this group

    public Group(Grib2SectionGridDefinition gdss, int hashCode) {
      this.gdss = gdss;
      this.hashCode = hashCode;
    }

    public Group(Grib2SectionGridDefinition gdss, int hashCode, CalendarDate runtime) {
      this.gdss = gdss;
      this.hashCode = hashCode;
      this.runtime = runtime;
    }

    @Override
    public CalendarDate getRuntime() {
      return runtime;
    }

    @Override
    public Set<Long> getCoordinateRuntimes() {
      return runtimes;
    }

    @Override
    public List<Coordinate> getCoordinates() {
      return coords;
    }
  }

  public boolean writeIndex(String name, File idxFile, CoordinateRuntime masterRuntime, List<Group> groups, List<MFile> files,
                            CollectionType type, CalendarDateRange dateRange) throws IOException {
    Grib2Record first = null; // take global metadata from here
    boolean deleteOnClose = false;

    if (idxFile.exists()) {
      // TODO RandomAccessFile.eject(idxFile.getPath());
      if (!idxFile.delete()) {
        logger.error("gc2 cant delete index file {}", idxFile.getPath());
      }
    }
    logger.debug(" createIndex for {}", idxFile.getPath());

    try (RandomAccessFile raf = new RandomAccessFile(idxFile.getPath(), "rw")) {
      //// header message
      raf.order(RandomAccessFile.BIG_ENDIAN);
      raf.write(MAGIC_START.getBytes(StandardCharsets.UTF_8));
      raf.writeInt(version);
      long lenPos = raf.getFilePointer();
      raf.writeLong(0); // save space to write the length of the record section
      long countBytes = 0;
      int countRecords = 0;

      Set<Integer> allFileSet = new HashSet<>();
      for (Group g : groups) {
        g.fileSet = new HashSet<>();
        for (Grib2CollectionBuilder.VariableBag vb : g.gribVars) {
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

      if (logger.isDebugEnabled()) {
        long bytesPerRecord = countBytes / ((countRecords == 0) ? 1 : countRecords);
        logger.debug("  write RecordMaps: bytes = {} record = {} bytesPerRecord={}", countBytes, countRecords,
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
      for (Object go : groups) {
        Group g = (Group) go;
        indexBuilder.addGds(publishGdsProto(g.gdss.getRawBytes(), -1));
      }

      // the GC dataset
      indexBuilder.addDataset(publishDatasetProto(type, groups));

      // what about just storing first ??
      Grib2SectionIdentification ids = first.getId();
      indexBuilder.setCenter(ids.getCenter_id());
      indexBuilder.setSubcenter(ids.getSubcenter_id());
      indexBuilder.setMaster(ids.getMaster_table_version());
      indexBuilder.setLocal(ids.getLocal_table_version());

      Grib2Pds pds = first.getPDS();
      indexBuilder.setGenProcessType(pds.getGenProcessType());
      indexBuilder.setGenProcessId(pds.getGenProcessId());
      indexBuilder.setBackProcessId(pds.getBackProcessId());

      indexBuilder.setStartTime(dateRange.getStart().getMillisFromEpoch());
      indexBuilder.setEndTime(dateRange.getEnd().getMillisFromEpoch());

      GribCollectionProto.GribCollection index = indexBuilder.build();
      byte[] b = index.toByteArray();
      Streams.writeVInt(raf, b.length); // message size
      raf.write(b); // message - all in one gulp
      logger.debug("  write GribCollectionIndex= {} bytes", b.length);

    } finally {
      // remove it on failure
      if (deleteOnClose && !idxFile.delete())
        logger.error(" gc2 cant deleteOnClose index file {}", idxFile.getPath());
    }

    return true;
  }

  private GribCollectionProto.SparseArray publishSparseArray(Grib2CollectionBuilder.VariableBag vb,
                                                             Set<Integer> fileSet) {
    GribCollectionProto.SparseArray.Builder b = GribCollectionProto.SparseArray.newBuilder();
    SparseArray<Grib2Record> sa = vb.coordND.getSparseArray();
    for (int size : sa.getShape())
      b.addSize(size);
    for (int track : sa.getTrack())
      b.addTrack(track);

    for (Grib2Record gr : sa.getContent()) {
      GribCollectionProto.Record.Builder br = GribCollectionProto.Record.newBuilder();

      br.setFileno(gr.getFile());
      fileSet.add(gr.getFile());
      long startPos = gr.getIs().getStartPos();
      br.setStartPos(startPos);

      if (gr.isBmsReplaced()) {
        Grib2SectionBitMap bms = gr.getBitmapSection();
        br.setBmsOffset((int) (bms.getStartingPosition() - startPos));
      }

      Grib2SectionDataRepresentation drs = gr.getDataRepresentationSection();
      br.setDrsOffset((int) (drs.getStartingPosition() - startPos));
      b.addRecords(br);
    }

    b.setNdups(sa.getNdups());
    return b.build();
  }

  private GribCollectionProto.Dataset publishDatasetProto(CollectionType type, List<Group> groups) {
    GribCollectionProto.Dataset.Builder b = GribCollectionProto.Dataset.newBuilder();

    GribCollectionProto.Dataset.Type ptype = GribCollectionProto.Dataset.Type.valueOf(type.toString());
    b.setType(ptype);

    for (Group group : groups)
      b.addGroups(publishGroupProto(group));

    return b.build();
  }

  private GribCollectionProto.Group publishGroupProto(Group g) {
    GribCollectionProto.Group.Builder b = GribCollectionProto.Group.newBuilder();

    b.setGds(publishGdsProto(g.gdss.getRawBytes(), -1));

    for (Grib2CollectionBuilder.VariableBag vbag : g.gribVars) {
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

    for (Integer aFileSet : g.fileSet)
      b.addFileno(aFileSet);

    return b.build();
  }

  private GribCollectionProto.Variable publishVariableProto(Grib2CollectionBuilder.VariableBag vb) {
    GribCollectionProto.Variable.Builder b = GribCollectionProto.Variable.newBuilder();

    b.setDiscipline(vb.first.getDiscipline());
    b.setPds(ByteString.copyFrom(vb.first.getPDSsection().getRawBytes()));

    // extra id info
    b.addIds(vb.first.getId().getCenter_id());
    b.addIds(vb.first.getId().getSubcenter_id());

    b.setRecordsPos(vb.pos);
    b.setRecordsLen(vb.length);

    for (int idx : vb.coordIndex)
      b.addCoordIdx(idx);

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
