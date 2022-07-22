/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.protoconvert;

import com.google.protobuf.ByteString;

import dev.ucdm.core.io.RandomAccessFile;
import dev.ucdm.grib.grib2.record.*;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Convert Grib2 indexes (gbx9) to/from proto.
 * The generated proto code is in dev.ucdm.grib.protogen.Grib2IndexProto.
 */
public class Grib2IndexProto {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib2IndexProto.class);

  public static final String MAGIC_START = "Grib2Index";
  private static final int version = 6; // index must be this version, or else force rewrite.

  /**
   * Read Grib2 gbx9.
   * @param idxFile gbx9 index filename.
   * Return null if version doesnt match or file is malformed.
   */
  @Nullable
  public static Grib2Index readGrib2Index(String idxFile) {
    List<Grib2SectionGridDefinition> gdsList = new ArrayList<>();
    List<Grib2Record> records = new ArrayList<>();

    // open the index file, which is a size-delimited stream of Grib2Record's.
    try (FileInputStream fin = new FileInputStream(idxFile)) {
      //// check header is ok
      if (!Streams.readAndTest(fin, MAGIC_START.getBytes(StandardCharsets.UTF_8))) {
        logger.info("Bad magic number of grib index on file= {}", idxFile);
        return null;
      }

      int protoVersion = Streams.readVInt(fin);
      if (protoVersion != version) {
        if (logger.isDebugEnabled()) {
          logger.debug("Grib2Index found version " + protoVersion + ", want version " + version + " on " + idxFile);
        }
        return null;
      }

      int size = Streams.readVInt(fin);
      if (size <= 0 || size > 100 * 1000 * 1000) { // try to catch garbage
        logger.warn("Grib2Index bad size = " + size + " for " + idxFile);
        return null;
      }

      byte[] m = new byte[size];
      Streams.readFully(fin, m);

      var proto = dev.ucdm.grib.protogen.Grib2IndexProto.Grib2Index.parseFrom(m);
      logger.debug("{} for {}", proto.getFilename(), idxFile);

      for (dev.ucdm.grib.protogen.Grib2IndexProto.GribGdsSection pgds : proto.getGdsListList()) {
        Grib2SectionGridDefinition gds = importGdsSection(pgds);
        gdsList.add(gds);
      }
      logger.debug(" read {} gds", gdsList.size());

      for (dev.ucdm.grib.protogen.Grib2IndexProto.Grib2Record precord : proto.getRecordsList()) {
        records.add(importGrib2Record(precord, gdsList));
      }
      logger.debug(" read {} records", records.size());

    } catch (NegativeArraySizeException | IOException e) {
      logger.error("Grib2Index error on " + idxFile, e);
      return null;
    }

    return new Grib2Index(gdsList, records);
  }

  private static Grib2Record importGrib2Record(dev.ucdm.grib.protogen.Grib2IndexProto.Grib2Record proto,
                                               List<Grib2SectionGridDefinition> gdsList) throws IOException {
    var is = new Grib2SectionIndicator(proto.getGribMessageStart(), proto.getGribMessageLength(), proto.getDiscipline());
    var ids = importIdSection(proto.getIds());

    Grib2SectionLocalUse lus = null;
    if (!proto.getLus().isEmpty()) {
      lus = new Grib2SectionLocalUse(proto.getLus().toByteArray());
    }

    int gdsIndex = proto.getGdsIdx();
    var gds = gdsList.get(gdsIndex);
    var pds = new Grib2SectionProductDefinition(proto.getPds().toByteArray());
    var drs = new Grib2SectionDataRepresentation(proto.getDrsPos(), proto.getDrsNpoints(), proto.getDrsTemplate());
    var bms = new Grib2SectionBitMap(proto.getBmsPos(), proto.getBmsIndicator());
    var data = new Grib2SectionData(proto.getDataPos(), proto.getDataLen());
    boolean bmsReplaced = proto.getBmsReplaced();

    int scanMode = proto.getScanMode(); // TODO why is this separate?
    return new Grib2Record(proto.getHeader().toByteArray(), is, ids, lus, gds, pds, drs, bms, data, bmsReplaced, scanMode);
  }

  private static Grib2SectionGridDefinition importGdsSection(dev.ucdm.grib.protogen.Grib2IndexProto.GribGdsSection proto) {
    ByteString bytes = proto.getGds();
    return new Grib2SectionGridDefinition(bytes.toByteArray());
  }

  private static Grib2SectionIdentification importIdSection(dev.ucdm.grib.protogen.Grib2IndexProto.GribIdSection proto) {
    // Grib2SectionIdentification(int center_id, int subcenter_id, int master_table_version,
    // int local_table_version, int significanceOfRT, int year, int month, int day, int hour, int minute, int second,
    // int productionStatus, int processedDataType) {
    return new Grib2SectionIdentification(proto.getCenterId(), proto.getSubcenterId(), proto.getMasterTableVersion(),
            proto.getLocalTableVersion(), proto.getSignificanceOfRT(), proto.getRefDate(0), proto.getRefDate(1), proto.getRefDate(2),
            proto.getRefDate(3), proto.getRefDate(4), proto.getRefDate(5), proto.getProductionStatus(), proto.getProcessedDataType());
  }

  ////////////////////////////////////////////////////////////////////////////////

/**
 * Write Grib2 gbx9.
 * @param dataLocation grib2 data filename to read data from.
 * @param idxFile index File to write the index to.
 * @param errlog error messages.
 * Return null if version doesnt match or file is malformed.
*/
public static boolean writeGrib2Index(String dataLocation, File idxFile, Formatter errlog) throws IOException {
    Map<Long, Integer> gdsMap = new HashMap<>();
    ArrayList<Grib2SectionGridDefinition> gdsList = new ArrayList<>();
    ArrayList<Grib2Record> records = new ArrayList<>();

    var idxBuilder = dev.ucdm.grib.protogen.Grib2IndexProto.Grib2Index.newBuilder();
    idxBuilder.setFilename(dataLocation);

    // read from the data file
    try (RandomAccessFile gribDataFile = new RandomAccessFile(dataLocation, "r")) {
      Grib2RecordScanner scan = new Grib2RecordScanner(gribDataFile);
      while (scan.hasNext()) {
        Grib2Record r = scan.next();
        if (r == null)
          break; // done
        records.add(r);

        Grib2SectionGridDefinition gdss = r.getGDSsection();
        Integer index = gdsMap.get(gdss.calcCRC());
        if (index == null) {
          gdsList.add(gdss);
          index = gdsList.size() - 1;
          gdsMap.put(gdss.calcCRC(), index);
          idxBuilder.addGdsList(publishGdsSection(gdss));
        }
        idxBuilder.addRecords(publishGrib2Record(r, index, r.getGDS().getScanMode()));
      }
    }
    if (records.isEmpty()) {
      errlog.format("No GRIB2 records found in " + dataLocation);
      return false;
    }

  // write to the index file
    try (FileOutputStream fout = new FileOutputStream(idxFile)) {
      //// header message
      fout.write(MAGIC_START.getBytes(StandardCharsets.UTF_8));
      Streams.writeVInt(fout, version);

      dev.ucdm.grib.protogen.Grib2IndexProto.Grib2Index index = idxBuilder.build();
      byte[] b = index.toByteArray();
      Streams.writeVInt(fout, b.length); // message size
      fout.write(b); // message - all in one gulp
      logger.debug("  made gbx9 index for {} size={}", dataLocation, b.length);
    }
    return true;
  }

  private static dev.ucdm.grib.protogen.Grib2IndexProto.Grib2Record publishGrib2Record(Grib2Record r, int gdsIndex, int scanMode) {
    dev.ucdm.grib.protogen.Grib2IndexProto.Grib2Record.Builder b = dev.ucdm.grib.protogen.Grib2IndexProto.Grib2Record.newBuilder();

    b.setHeader(ByteString.copyFrom(r.getHeader()));

    // is
    b.setGribMessageStart(r.getIs().getStartPos());
    b.setGribMessageLength(r.getIs().getMessageLength());
    b.setDiscipline(r.getDiscipline());

    // is
    b.setIds(publishIdSection(r.getId()));

    // lus
    byte[] lus = r.getLocalUseSection().getRawBytes();
    if (lus != null && lus.length > 0)
      b.setLus(ByteString.copyFrom(lus));

    b.setGdsIdx(gdsIndex);
    b.setPds(ByteString.copyFrom(r.getPDSsection().getRawBytes()));

    Grib2SectionDataRepresentation drs = r.getDataRepresentationSection();
    b.setDrsPos(drs.getStartingPosition());
    b.setDrsNpoints(drs.getDataPoints());
    b.setDrsTemplate(drs.getDataTemplate());

    Grib2SectionBitMap bms = r.getBitmapSection();
    b.setBmsPos(bms.getStartingPosition());
    b.setBmsIndicator(bms.getBitMapIndicator());
    b.setBmsReplaced(r.isBmsReplaced());

    Grib2SectionData ds = r.getDataSection();
    b.setDataPos(ds.getStartingPosition());
    b.setDataLen(ds.getMsgLength());

    b.setScanMode(scanMode);

    return b.build();
  }

  private static dev.ucdm.grib.protogen.Grib2IndexProto.GribGdsSection publishGdsSection(Grib2SectionGridDefinition gds) {
    dev.ucdm.grib.protogen.Grib2IndexProto.GribGdsSection.Builder b = dev.ucdm.grib.protogen.Grib2IndexProto.GribGdsSection.newBuilder();
    b.setGds(ByteString.copyFrom(gds.getRawBytes()));
    return b.build();
  }

  private static dev.ucdm.grib.protogen.Grib2IndexProto.GribIdSection publishIdSection(Grib2SectionIdentification id) {
    dev.ucdm.grib.protogen.Grib2IndexProto.GribIdSection.Builder b = dev.ucdm.grib.protogen.Grib2IndexProto.GribIdSection.newBuilder();

    b.setCenterId(id.getCenter_id());
    b.setSubcenterId(id.getSubcenter_id());
    b.setMasterTableVersion(id.getMaster_table_version());
    b.setLocalTableVersion(id.getLocal_table_version());
    b.setSignificanceOfRT(id.getSignificanceOfRT());
    b.addRefDate(id.getYear());
    b.addRefDate(id.getMonth());
    b.addRefDate(id.getDay());
    b.addRefDate(id.getHour());
    b.addRefDate(id.getMinute());
    b.addRefDate(id.getSecond());
    b.setProductionStatus(id.getProductionStatus());
    b.setProcessedDataType(id.getTypeOfProcessedData());

    return b.build();
  }

}
