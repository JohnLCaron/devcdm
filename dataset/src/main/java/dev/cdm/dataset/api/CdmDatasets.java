/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.cdm.dataset.api;

import dev.cdm.core.api.CdmFile;
import dev.cdm.core.api.CdmFiles;
import dev.cdm.core.util.CancelTask;
import dev.cdm.dataset.internal.DatasetEnhancer;
import dev.cdm.dataset.spi.CdmFileProvider;
import dev.cdm.dataset.api.CdmDataset.Enhance;
import dev.cdm.dataset.internal.DatasetCSEnhancer;
import dev.cdm.dataset.ncml.NcmlReader;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Static helper methods for CdmDataset.
 */
public class CdmDatasets {

  private CdmDatasets() {
  }

  ////////////////////////////////////////////////////////////////////////////////////
  // enhanced datasets

  /**
   * Factory method for opening a dataset with all enhancements and coordinate systems.
   *
   * @param location location of file
   */
  public static CdmDatasetCS openDatasetCS(String location) throws IOException {
    DatasetUrl durl = DatasetUrl.findDatasetUrl(location);
    CdmFile ncfile = openProtocolOrFile(durl, -1, null, null);
    return openDatasetCS(ncfile);
  }

  public static CdmDatasetCS openDatasetCS(CdmFile ncfile) throws IOException {
    CdmDatasetCS.Builder<?> builder = CdmDatasetCS.builder().copyFrom(ncfile).setOrgFile(ncfile);
    DatasetCSEnhancer enhancer = new DatasetCSEnhancer(builder, CdmDataset.getEnhanceAll());
    return enhancer.enhance().build();
  }

  /**
   * Factory method for opening a dataset through the netCDF API, and identifying its coordinate variables.
   *
   * @param location location of file
   */
  public static CdmDataset openDataset(String location) throws IOException {
    return openDataset(location, true, null);
  }

  /**
   * Factory method for opening a dataset through the netCDF API, and identifying its coordinate variables.
   *
   * @param location   location of file
   * @param enhance    if true, use defaultEnhanceMode, else no enhancements
   * @param cancelTask allow task to be cancelled; may be null.
   */
  public static CdmDataset openDataset(String location, boolean enhance, @Nullable CancelTask cancelTask)
          throws IOException {
    return openDataset(location, enhance ? CdmDataset.getDefaultEnhanceMode() : null, cancelTask);
  }

  /**
   * Factory method for opening a dataset through the netCDF API, and identifying its coordinate variables.
   *
   * @param location    location of file
   * @param enhanceMode set of enhancements. If null, then none
   * @param cancelTask  allow task to be cancelled; may be null.
   */
  public static CdmDataset openDataset(String location, @Nullable Set<Enhance> enhanceMode,
                                       @Nullable CancelTask cancelTask) throws IOException {
    DatasetUrl durl = DatasetUrl.findDatasetUrl(location);
    return openDataset(durl, enhanceMode, -1, cancelTask, null);
  }

  /**
   * Factory method for opening a dataset through the netCDF API, and identifying its coordinate variables.
   *
   * @param location    location of file
   * @param enhance     if true, use defaultEnhanceMode, else no enhancements
   * @param cancelTask  allow task to be cancelled; may be null.
   * @param iospMessage send to iosp.sendIospMessage() if not null
   */
  public static CdmDataset openDataset(String location, boolean enhance, @Nullable CancelTask cancelTask,
                                       @Nullable Object iospMessage) throws IOException {
    DatasetUrl durl = DatasetUrl.findDatasetUrl(location);
    return openDataset(durl, enhance ? CdmDataset.getDefaultEnhanceMode() : null, -1, cancelTask, iospMessage);
  }

  /**
   * Factory method for opening a dataset through the netCDF API, and identifying its coordinate variables.
   *
   * @param location    location of file
   * @param enhanceMode set of enhancements. If null, then none
   * @param buffer_size RandomAccessFile buffer size, if &le; 0, use default size
   * @param cancelTask  allow task to be cancelled; may be null.
   * @param iospMessage send to iosp.sendIospMessage() if not null
   */
  public static CdmDataset openDataset(DatasetUrl location, @Nullable Set<Enhance> enhanceMode, int buffer_size,
                                       @Nullable CancelTask cancelTask, @Nullable Object iospMessage) throws IOException {
    CdmFile ncfile = openProtocolOrFile(location, buffer_size, cancelTask, iospMessage);
    return enhance(ncfile, enhanceMode, cancelTask);
  }

  /**
   * Read NcML doc from a Reader, and construct a CdmDataset.
   *
   * @param reader       the Reader containing the NcML document
   * @param ncmlLocation the URL location string of the NcML document, used to resolve reletive path of the referenced
   *                     dataset, or may be just a unique name for caching purposes.
   * @param cancelTask   allow user to cancel the task; may be null
   */
  public static CdmDataset openNcmlDataset(Reader reader, String ncmlLocation, @Nullable CancelTask cancelTask)
          throws IOException {
    CdmDataset.Builder<?> builder = NcmlReader.readNcml(reader, ncmlLocation, cancelTask);
    if (!builder.getEnhanceMode().isEmpty()) {
      DatasetEnhancer enhancer = new DatasetEnhancer(builder, builder.getEnhanceMode());
      return enhancer.enhance().build();
    } else {
      return builder.build();
    }
  }

  public static CdmDatasetCS openNcmlDatasetCS(Reader reader, String ncmlLocation, @Nullable CancelTask cancelTask)
          throws IOException {
    CdmDatasetCS.Builder<?> builder = NcmlReader.readNcml(reader, ncmlLocation, cancelTask);
    DatasetCSEnhancer enhancer = new DatasetCSEnhancer(builder, builder.getEnhanceMode());
    return enhancer.enhance().build();
  }

  /**
   * Make CdmFile into CdmDataset and enhance if needed
   *
   * @param ncfile wrap this CdmFile or CdmDataset.
   * @param mode   using this enhance mode (may be null, meaning no enhance)
   * @return a new CdmDataset that wraps the given CdmFile or CdmDataset.
   */
  public static CdmDataset enhance(CdmFile ncfile, @Nullable Set<Enhance> mode, @Nullable CancelTask cancelTask)
          throws IOException {
    if (ncfile instanceof CdmDataset) {
      CdmDataset ncd = (CdmDataset) ncfile;
      CdmDataset.Builder<?> builder = ncd.toBuilder();
      if (DatasetEnhancer.enhanceNeeded(mode, ncd.getEnhanceMode())) {
        DatasetEnhancer enhancer = new DatasetEnhancer(builder, mode);
        return enhancer.enhance().build();
      } else {
        return ncd;
      }
    }

    // original file not a CdmDataset
    CdmDataset.Builder<?> builder = CdmDataset.builder().copyFrom(ncfile).setOrgFile(ncfile);
    if (DatasetEnhancer.enhanceNeeded(mode, null)) {
      DatasetEnhancer enhancer = new DatasetEnhancer(builder, mode);
      return enhancer.enhance().build();
    }
    return builder.build();
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public static CdmFile openFile(String location, CancelTask cancelTask) throws IOException {
    DatasetUrl durl = DatasetUrl.findDatasetUrl(location);
    return openFile(durl, -1, cancelTask, null);
  }

  /**
   * Factory method for opening a CdmFile through the netCDF API. May be any kind of file that
   * can be read through the netCDF API, including OpenDAP and NcML.
   * <ol>
   * <li>local filename (with a file: prefix or no prefix) for netCDF (version 3), hdf5 files, or any file type
   * registered with CdmFile.registerIOProvider().
   * <li>OpenDAP dataset URL (with a dods: or http: prefix).
   * <li>NcML file or URL if the location ends with ".xml" or ".ncml"
   * <li>NetCDF file through an HTTP server (http: prefix)
   * <li>thredds dataset (thredds: prefix), see DataFactory.openDataset(String location, ...));
   * </ol>
   * <p>
   * This does not necessarily return a CdmDataset, or enhance the dataset; use CdmDatasets.openDataset() method
   * for that.
   *
   * @param location    location of dataset.
   * @param buffer_size RandomAccessFile buffer size, if &le; 0, use default size
   * @param cancelTask  allow task to be cancelled; may be null.
   * @param iospMessage send to iosp.sendIospMessage() if not null
   * @return CdmFile object
   */
  public static CdmFile openFile(DatasetUrl location, int buffer_size, CancelTask cancelTask,
                                 Object iospMessage) throws IOException {
    return openProtocolOrFile(location, buffer_size, cancelTask, iospMessage);
  }

  /**
   * Open through a protocol or a file. No cache, no factories.
   *
   * @param durl        location of file, with protocol or as a file.
   * @param buffer_size RandomAccessFile buffer size, if <= 0, use default size
   * @param cancelTask  allow task to be cancelled; may be null.
   * @param iospMessage send to iosp.sendIospMessage() if not null
   * @return CdmFile or throw an Exception.
   */
  private static CdmFile openProtocolOrFile(DatasetUrl durl, int buffer_size, CancelTask cancelTask,
                                            Object iospMessage) throws IOException {

    // look for dynamically loaded CdmFileProvider
    for (CdmFileProvider provider : ServiceLoader.load(CdmFileProvider.class)) {
      if (provider.isOwnerOf(durl)) {
        return provider.open(durl.getTrueurl(), cancelTask);
      }
    }

    // look for providers who do not have an associated ServiceType.
    for (CdmFileProvider provider : ServiceLoader.load(CdmFileProvider.class)) {
      if (provider.isOwnerOf(durl.getTrueurl())) {
        return provider.open(durl.getTrueurl(), cancelTask);
      }
    }

    // Otherwise we are dealing with a file or a remote http file.
    if (durl.getServiceType() != null) {
      switch (durl.getServiceType()) {
        case HTTPServer:
          break; // fall through

        default:
          throw new IOException("Unknown service type: " + durl.getServiceType());
      }
    }

    // Open as a file or remote file
    return CdmFiles.open(durl.getTrueurl(), buffer_size, cancelTask, iospMessage);
  }
}
