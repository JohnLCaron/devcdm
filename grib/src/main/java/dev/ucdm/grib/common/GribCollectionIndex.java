/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.common;

import dev.ucdm.core.calendar.CalendarDate;
import dev.ucdm.grib.inventory.SingleFileMCollection;
import org.jetbrains.annotations.Nullable;

import com.google.common.base.Preconditions;

import dev.ucdm.grib.inventory.CollectionUpdate;
import dev.ucdm.grib.collection.Grib1Collection;
import dev.ucdm.grib.collection.Grib1CollectionBuilder;
import dev.ucdm.grib.collection.Grib2Collection;
import dev.ucdm.grib.collection.Grib2CollectionBuilder;
import dev.ucdm.grib.protoconvert.Grib1CollectionIndexReader;
import dev.ucdm.grib.protoconvert.Grib1CollectionIndexWriter;
import dev.ucdm.grib.protoconvert.Grib2CollectionIndexReader;
import dev.ucdm.grib.collection.GribCollection;
import dev.ucdm.grib.inventory.MCollection;
import dev.ucdm.grib.inventory.MFile;
import dev.ucdm.grib.inventory.MFileOS;
import dev.ucdm.grib.common.util.GribIndexCache;
import dev.ucdm.grib.grib1.record.Grib1RecordScanner;
import dev.ucdm.grib.grib2.record.Grib2RecordScanner;
import dev.ucdm.grib.protoconvert.Grib2CollectionIndexWriter;
import dev.ucdm.grib.protoconvert.GribCollectionIndexWriter;

import dev.ucdm.core.io.RandomAccessFile;
import dev.ucdm.core.util.StringUtil2;
import java.io.File;
import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.util.Formatter;


/** Logic for creating / managing ncx4 indices. */
public class GribCollectionIndex {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GribCollectionIndex.class);

  public enum Type {
    GRIB1, GRIB2, Partition1, Partition2, none
  }

  public static final String NCX_SUFFIX = ".ncx4";

  // raf is a single data file or an ncx4 file
  @Nullable
  public static GribCollection openGribCollectionFromRaf(
          RandomAccessFile raf, CollectionUpdate update, GribConfig config, Formatter errlog) throws IOException {

    // check if its a plain ole GRIB1/2 data file
    boolean isGrib1 = false;
    boolean isGrib2 = Grib2RecordScanner.isValidFile(raf);
    if (!isGrib2) {
      isGrib1 = Grib1RecordScanner.isValidFile(raf);
    }
    if (isGrib1 || isGrib2) {
      // TODO close the data file, the ncx raf file is managed by gribCollection ??
      // raf.close();
      return openGribCollectionFromDataFile(isGrib1, raf, update, config, errlog);
    }

    // check if its a collection dataset
    if (getType(raf) == Type.none) {
      return null;
    }

    // its a collection dataset ncx4 file
    GribCollection result = readCollectionFromIndex(raf.getLocation(), false, errlog);
    // TODO close the data file, the ncx raf file is managed by gribCollection ??
    // raf.close();
    return result;
  }

  /** raf is a grib data file. If the corresponding ncx4 file exists, use it, else create it. */
  public static GribCollection openGribCollectionFromDataFile(boolean isGrib1, RandomAccessFile dataRaf,
                                                              CollectionUpdate update, GribConfig config, Formatter errlog)
      throws IOException {

    File dataFile = new File(dataRaf.getLocation());
    MFile mfile = new MFileOS(dataFile);
    MCollection dcm = new SingleFileMCollection(mfile);
    return updateCollectionIndex(isGrib1, dcm, update, config, errlog);
  }

  /**
   * The general case of creating a collection from Grib data or gbx files.
   * If the corresponding ncx4 file exists, use it, update it, or create it.
   */
  @Nullable
  public static GribCollection updateCollectionIndex(
          boolean isGrib1, MCollection dcm, CollectionUpdate update, GribConfig config, Formatter errlog) throws IOException {

    String idxPath = dcm.getIndexFilename();
    // look to see if the file is in some special cache (eg when cant write to data directory)
    File idxFile = GribIndexCache.getExistingFileOrCache(idxPath);
    boolean idxFileExists = idxFile != null;

    GribCollection gribCollection = null;
    if (idxFileExists && update != CollectionUpdate.always) { // always create a new index
      // look to see if the index file is older than the collection
      boolean indexIsOlder = CalendarDate.of(idxFile.lastModified()).isBefore(dcm.getLastModified());
      // dont read it if index is older
      if (update == CollectionUpdate.nocheck || update == CollectionUpdate.never || !indexIsOlder) {
        gribCollection = readCollectionFromIndex(dcm.getIndexFilename(), false, errlog);
      }
    }

    if (gribCollection == null) {
      if (update == CollectionUpdate.never) {
        errlog.format("Failed to open existing index for '%s'", idxPath);
        return null;
      }
      // may not exist, overwrite if it does.
      File idxFile2 = GribIndexCache.getFileOrCache(idxPath);
      if (idxFile2 == null) {
        errlog.format("Failed to find a place to write the index file for '%s'", idxPath);
        return null;
      }

      if (!createCollectionIndex(isGrib1, dcm, config, errlog)) {
        logger.warn("  Index writing failed on {} errlog = '{}'", idxFile2, errlog);

      } else {
        // read it back in
        gribCollection = readCollectionFromIndex(dcm.getIndexFilename(), false, errlog);
        logger.debug("  Index written: {}", idxPath);
      }
    } else {
      logger.debug("  Index read: {}", idxPath);
    }

    return gribCollection;
  }

  /** create ncx4 file. The GribConfig is attached to the MCollection */
  public static boolean createCollectionIndex(boolean isGrib1, MCollection dcm, GribConfig config, Formatter errlog) throws IOException {
    if (isGrib1) {
      Grib1CollectionBuilder builder = new Grib1CollectionBuilder(dcm.getCollectionName(), dcm, config, logger);
      if (!builder.createIndex(errlog)) {
        return false;
      }
    } else {
      Grib2CollectionBuilder builder = new Grib2CollectionBuilder(dcm.getCollectionName(), dcm, config, logger);
      if (!builder.createIndex(errlog)) {
        return false;
      }
    }
    return true;
  }

  /** read existing ncx4 file */
  // LOOK what is useCache??
  @Nullable
  public static GribCollection readCollectionFromIndex(String indexFilename, boolean useCache, Formatter errlog) throws IOException {
    File indexFileInCache = useCache ? GribIndexCache.getExistingFileOrCache(indexFilename) : new File(indexFilename);
    if (indexFileInCache == null) {
      errlog.format("indexFilename %s not found in cache%n", indexFilename);
      return null;
    }

    // LOOK shouldnt we be using this?
    String indexFilenameInCache = indexFileInCache.getPath();
    String dataName = makeNameFromIndexFilename(indexFilename);

    GribCollection result;
    try (RandomAccessFile raf = new RandomAccessFile(indexFilename, "r")) {
      GribCollectionIndex.Type collectionType = getType(raf);
      if (collectionType == Type.none) {
        errlog.format("indexFilename %s not a GribCollectionIndex%n", indexFilename);
        return null;
      }

      // TODO read GribConfig from index
      GribConfig config = new GribConfig();

      boolean isGrib1 = (collectionType == Type.GRIB1) || (collectionType == Type.Partition1);
      if (isGrib1) {
        result = new Grib1Collection(dataName, null, config);
        Grib1CollectionIndexReader reader = new Grib1CollectionIndexReader(result, config);
        if (!reader.readIndex(raf)) {
          errlog.format("indexFilename %s Grib1CollectionIndexReader failed%n", indexFilename);
          return null;
        }

      } else {
        result = new Grib2Collection(dataName, null, config);
        Grib2CollectionIndexReader reader = new Grib2CollectionIndexReader(result, config);
        if (!reader.readIndex(raf)) {
          errlog.format("indexFilename %s Grib2CollectionIndexReader failed%n", indexFilename);
          return null;
        }
      }
    } catch (IOException ioe) {
      logger.warn("Failed to open index file {} msg = '{}'", indexFilename, ioe.getMessage());
      errlog.format("indexFilename %s ioe failed '%s'%n", indexFilename, ioe.getMessage());
      ioe.printStackTrace();
      return null;
    }

    return result;
  }

  private static String makeNameFromIndexFilename(String idxPathname) {
    idxPathname = StringUtil2.replace(idxPathname, '\\', "/");
    int pos = idxPathname.lastIndexOf('/');
    String idxFilename = (pos < 0) ? idxPathname : idxPathname.substring(pos + 1);
    Preconditions.checkArgument(idxFilename.endsWith(NCX_SUFFIX), idxFilename);
    return idxFilename.substring(0, idxFilename.length() - NCX_SUFFIX.length());
  }

  /** Find out what kind of index this is */
  public static GribCollectionIndex.Type getType(RandomAccessFile raf) throws IOException {
    // they all have the same number of bytes
    raf.seek(0);
    String magic = raf.readString(Grib2CollectionIndexWriter.MAGIC_START.getBytes(StandardCharsets.UTF_8).length);
    //System.out.printf("%s GribCollectionIndex has magic '%s'%n", raf.getLocation(), magic);

    return switch (magic) {
      case Grib2CollectionIndexWriter.MAGIC_START -> Type.GRIB2;
      case Grib1CollectionIndexWriter.MAGIC_START -> Type.GRIB1;
      case GribCollectionIndexWriter.PARTITION2_START -> Type.Partition2;
      case GribCollectionIndexWriter.PARTITION1_START -> Type.Partition1;
      default -> Type.none;
    };
  }

}
