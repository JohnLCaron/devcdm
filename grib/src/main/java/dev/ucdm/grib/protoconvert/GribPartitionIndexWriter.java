/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.protoconvert;

import com.google.protobuf.ByteString;
import dev.ucdm.core.io.RandomAccessFile;
import dev.ucdm.core.util.StringUtil2;
import dev.ucdm.grib.collection.GribPartition;
import dev.ucdm.grib.common.GribCollectionIndex;
import dev.ucdm.grib.common.util.GribIndexCache;
import dev.ucdm.grib.coord.*;
import dev.ucdm.grib.inventory.MPartition;
import dev.ucdm.grib.protogen.GribCollectionProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Formatter;

/** Writing GribPartition index (ncx) files */
public class GribPartitionIndexWriter extends GribCollectionIndexWriter {
  static final Logger logger = LoggerFactory.getLogger(GribPartitionIndexWriter.class);

  private final MPartition partitionManager; // defines the partition
  protected final String name; // collection name

  public GribPartitionIndexWriter(String name, MPartition tpc) {
    this.name = name;
    this.partitionManager = tpc;
  }

  /*
   * MAGIC_START
   * version
   * sizeRecords
   * VariableRecords (sizeRecords bytes)
   * sizeIndex
   * GribCollectionProto.GribCollection (sizeIndex bytes)
   */
  public boolean writeIndex(GribPartition pc, boolean isGrib1, Formatter msg) throws IOException {
    File idxFile = GribIndexCache.getFileOrCache(partitionManager.getIndexFilename(GribCollectionIndex.NCX_SUFFIX));
    if (idxFile.exists()) {
      // RandomAccessFile.eject(idxFile.getPath());
      if (!idxFile.delete()) {
        logger.error("gc2tp cant delete " + idxFile.getPath());
      }
    }

    try (RandomAccessFile raf = new RandomAccessFile(idxFile.getPath(), "rw")) {
      raf.order(RandomAccessFile.BIG_ENDIAN);

      String magicStart = isGrib1 ? GribCollectionIndexWriter.PARTITION1_START : GribCollectionIndexWriter.PARTITION2_START;
      int version = isGrib1 ? Grib1CollectionIndexWriter.version : Grib2CollectionIndexWriter.version;;

      //// header message
      raf.write(magicStart.getBytes(StandardCharsets.UTF_8));
      raf.writeInt(version);
      raf.writeLong(0); // no record section

      GribCollectionProto.GribCollection.Builder indexBuilder = GribCollectionProto.GribCollection.newBuilder();
      indexBuilder.setName(pc.name);
      Path topDir = pc.directory.toPath();
      String pathS = StringUtil2.replace(topDir.toString(), '\\', "/");
      indexBuilder.setTopDir(pathS);

      // mfiles are the partition indexes
      int count = 0;
      for (GribPartition.ChildCollection part : pc.getChildCollections()) {
        GribCollectionProto.MFile.Builder b = GribCollectionProto.MFile.newBuilder();
        String pathRS = makeReletiveFilename(pc, part); // reletive to pc.directory
        b.setFilename(pathRS);
        b.setLastModified(part.lastModified);
        b.setLength(part.fileSize);
        b.setIndex(count++);
        indexBuilder.addMfiles(b.build());
      }

      indexBuilder.setCenter(pc.getCenter());
      indexBuilder.setSubcenter(pc.getSubcenter());
      indexBuilder.setMaster(pc.getMaster());
      indexBuilder.setLocal(pc.getLocal());

      indexBuilder.setGenProcessId(pc.getGenProcessId());
      indexBuilder.setGenProcessType(pc.getGenProcessType());
      indexBuilder.setBackProcessId(pc.getBackProcessId());

      indexBuilder.setStartTime(pc.dateRange.getStart().getMillisFromEpoch());
      indexBuilder.setEndTime(pc.dateRange.getEnd().getMillisFromEpoch());

      indexBuilder.setMasterRuntime(publishCoordinateRuntime(pc.masterRuntime));

      // dataset
      for (GribPartition.DatasetP ds : pc.datasets) {
        indexBuilder.addDataset(publishDataset(pc, ds));
      }

      // extensions
      if (pc.run2part != null) {
        for (int part : pc.run2part) {
          indexBuilder.addRun2Part(part);
        }
      }
      for (GribPartition.ChildCollection part : pc.partitions) {
        indexBuilder.addPartitions(publishPartition(pc, part));
      }
      indexBuilder.setIsPartitionOfPartitions(pc.isPartitionOfPartitions);

      // write it out
      GribCollectionProto.GribCollection index = indexBuilder.build();
      byte[] b = index.toByteArray();
      Streams.writeVInt(raf, b.length); // message size
      raf.write(b); // message - all in one gulp
      msg.format("Grib2PartitionIndex= %d bytes file size =  %d bytes%n%n", b.length, raf.length());
    }

    return true;
  }

  /*
   * message Dataset {
   * required Type type = 1;
   * repeated Group groups = 2;
   * }
   */
  private GribCollectionProto.Dataset publishDataset(GribPartition pc, GribPartition.DatasetP ds) throws IOException {
    GribCollectionProto.Dataset.Builder b = GribCollectionProto.Dataset.newBuilder();

    GribCollectionProto.Dataset.Type type = GribCollectionProto.Dataset.Type.valueOf(ds.gctype.toString());
    b.setType(type);

    for (GribPartition.GroupP group : ds.groups) {
      b.addGroups(publishGroup(pc, group));
    }

    return b.build();
  }

  /*
   * message Group {
   * Gds gds = 1; // use this to build the HorizCoordSys
   * repeated Variable variables = 2; // list of variables
   * repeated Coord coords = 3; // list of coordinates
   * repeated uint32 fileno = 4 [packed=true]; // the component files that are in this group, key into gc.mfiles
   * }
   */
  private GribCollectionProto.Group publishGroup(GribPartition pc, GribPartition.GroupP g) {
    GribCollectionProto.Group.Builder b = GribCollectionProto.Group.newBuilder();

    b.setGds(publishGdsProto(g.horizCoordSys.getRawGds(), g.horizCoordSys.getPredefinedGridDefinition()));

    for (GribPartition.VariableIndexPartitioned vb : g.variList) {
      b.addVariables(publishVariable(vb));
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

    if (g.filenose != null) {
      for (Integer fileno : g.filenose) {
        b.addFileno(fileno);
      }
    }

    return b.build();
  }

  private GribCollectionProto.Variable publishVariable(GribPartition.VariableIndexPartitioned vip) {

    GribCollectionProto.Variable.Builder b = GribCollectionProto.Variable.newBuilder();

    b.setDiscipline(vip.vi.discipline);
    b.setPds(ByteString.copyFrom(vip.vi.rawPds));

    // extra id info
    b.addIds(vip.vi.center);
    b.addIds(vip.vi.subcenter);

    // LOOK whats this ??
    b.setRecordsPos(vip.vi.recordsPos);
    b.setRecordsLen(vip.vi.recordsLen);

    for (int idx : vip.coordIndex) {
      b.addCoordIdx(idx);
    }

    b.setNdups(vip.ndups);
    b.setNrecords(vip.nrecords);
    b.setMissing(vip.nmissing);

    // extensions
    if (vip.nparts > 0 && vip.partnoSA != null) {
      for (int i = 0; i < vip.nparts; i++) {
        b.addPartVariable(publishPartitionVariable(vip.partnoSA.get(i), vip.groupnoSA.get(i), vip.varnoSA.get(i),
                vip.nrecords, vip.ndups, vip.nmissing));
      }
    }

    return b.build();
  }

  private GribCollectionProto.PartitionVariable publishPartitionVariable(
          int partno, int groupno, int varno, int nrecords, int ndups, int nmissing) {

    GribCollectionProto.PartitionVariable.Builder pb = GribCollectionProto.PartitionVariable.newBuilder();
    pb.setPartno(partno);
    pb.setGroupno(groupno);
    pb.setVarno(varno);
    pb.setNdups(ndups);
    pb.setNrecords(nrecords);
    pb.setMissing(nmissing);

    return pb.build();
  }

  private GribCollectionProto.Partition publishPartition(GribPartition pc, GribPartition.ChildCollection p) {
    GribCollectionProto.Partition.Builder b = GribCollectionProto.Partition.newBuilder();

    String pathRS = makeReletiveFilename(pc, p); // reletive to pc.directory
    b.setFilename(pathRS);
    b.setName(p.name);
    // b.setDirectory(p.directory);
    b.setLastModified(p.lastModified);
    b.setLength(p.fileSize);
    if (p.partitionDate != null) {
      b.setPartitionDate(p.partitionDate.getMillisFromEpoch()); // TODO what about calendar ??
    }

    return b.build();
  }

  private String makeReletiveFilename(GribPartition pc, GribPartition.ChildCollection part) {
    Path topDir = pc.directory.toPath();
    Path partPath = new File(part.directory, part.filename).toPath();
    Path pathRelative = topDir.relativize(partPath);
    return StringUtil2.replace(pathRelative.toString(), '\\', "/");
  }

}
