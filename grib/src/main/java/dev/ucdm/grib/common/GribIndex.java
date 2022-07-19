/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.common;

import org.jetbrains.annotations.Nullable;

import dev.ucdm.grib.collection.CollectionUpdateType;
import dev.ucdm.grib.inventory.MFile;
import dev.ucdm.grib.common.util.GribIndexCache;
import dev.ucdm.grib.protoconvert.Grib1Index;
import dev.ucdm.grib.protoconvert.Grib1IndexProto;
import dev.ucdm.grib.protoconvert.Grib2Index;
import dev.ucdm.grib.protoconvert.Grib2IndexProto;

import java.io.File;
import java.io.IOException;
import java.util.Formatter;

/**
 * Logic for creating / managing gbx9 indices.
 */
public abstract class GribIndex {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GribIndex.class);

  public static final String GBX9_IDX = ".gbx9";
  public static final boolean debug = false;

  /**
   * Read a Grib2 gbx9 index, create it if it doest exist.
   * Use the existing index if it already exists and is not older than the data file.
   *
   * @param mfile the grib data or gbx9 file
   * @param force force writing index
   * @return the resulting GribIndex
   * @throws IOException on io error
   */
  @Nullable
  public static Grib2Index readOrCreateIndex2(MFile mfile, CollectionUpdateType force, Formatter errlog) throws IOException {

    String idxPath = mfile.getPath();
    if (!idxPath.endsWith(GBX9_IDX)) {
      idxPath += GBX9_IDX;
    }
    // look to see if the file is in some special cache (eg when cant write to data directory)
    File idxFile = GribIndexCache.getExistingFileOrCache(idxPath);
    boolean idxFileExists = idxFile != null;

    Grib2Index index = null;
    if (idxFileExists && force != CollectionUpdateType.always) { // always create a new index
      // look to see if the index file is older than the data file
      boolean isOlder = idxFile.lastModified() < mfile.getLastModified();

      if (force == CollectionUpdateType.nocheck || isOlder) {
        // try to read it
        index = Grib2IndexProto.readGrib2Index(idxFile.getAbsolutePath());
      }
    }

    // create the index
    if (index == null) {
      // may not exist, overwrite if it does.
      File idxFile2 = GribIndexCache.getFileOrCache(idxPath);
      if (idxFile2 == null) {
        errlog.format("Failed to find a place to write the index file for '%s'", idxPath);
        return null;
      }

      if (!Grib2IndexProto.writeGrib2Index(mfile.getPath(), idxFile2, errlog)) {
        logger.warn("  Index writing failed on {} errlog = '{}'", mfile.getPath(), errlog);
      } else {
        // read it back in
        index = Grib2IndexProto.readGrib2Index(idxFile2.getAbsolutePath());
        logger.debug("  Index written: {} == {} records", idxPath, index.getNRecords());
      }
    } else {
      logger.debug("  Index read: {} == {} records", idxPath, index.getNRecords());
    }

    return index;
  }

  //////////////////////////////////////////

  /**
   * Read a Grib2 gbx9 index, create it if it doest exist.
   * Use the existing index if it already exists and is not older than the data file.
   *
   * @param mfile the grib data or gbx9 file
   * @param force force writing index
   * @return the resulting GribIndex
   * @throws IOException on io error
   */
  @Nullable
  public static Grib1Index readOrCreateIndex1(MFile mfile, CollectionUpdateType force, Formatter errlog) throws IOException {

    String idxPath = mfile.getPath();
    if (!idxPath.endsWith(GBX9_IDX)) {
      idxPath += GBX9_IDX;
    }
    // look to see if the file is in some special cache (eg when cant write to data directory)
    File idxFile = GribIndexCache.getExistingFileOrCache(idxPath);
    boolean idxFileExists = idxFile != null;

    Grib1Index index = null;
    if (idxFileExists && force != CollectionUpdateType.always) { // always create a new index
      // look to see if the index file is older than the data file
      boolean isOlder = idxFile.lastModified() < mfile.getLastModified();

      if (force == CollectionUpdateType.nocheck || isOlder) {
        // try to read it
        index = Grib1IndexProto.readGrib1Index(idxFile.getAbsolutePath());
      }
    }

    // create the index
    if (index == null) {
      // may not exist, overwrite if it does.
      File idxFile2 = GribIndexCache.getFileOrCache(idxPath);
      if (idxFile2 == null) {
        errlog.format("Failed to find a place to write the index file for '%s'", idxPath);
        return null;
      }

      if (!Grib1IndexProto.writeGrib1Index(mfile.getPath(), idxFile2, errlog)) {
        logger.warn("  Index writing failed on {} errlog = '{}'", mfile.getPath(), errlog);
      } else {
        // read it back in
        index = Grib1IndexProto.readGrib1Index(idxFile2.getAbsolutePath());
        logger.debug("  Index written: {} == {} records", idxPath, index.getNRecords());
      }
    } else {
      logger.debug("  Index read: {} == {} records", idxPath, index.getNRecords());
    }

    return index;
  }
}
