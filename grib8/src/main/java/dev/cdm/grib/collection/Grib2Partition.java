/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.cdm.grib.collection;

import java.io.Closeable;
import java.io.IOException;
import java.util.Formatter;

import dev.cdm.core.api.CdmFile;
import dev.cdm.core.api.CdmFiles;
import dev.cdm.core.api.Attribute;
import dev.cdm.core.api.AttributeContainerMutable;
import dev.cdm.core.constants.CDM;
import dev.cdm.core.constants.DataFormatType;
import dev.cdm.dataset.api.CdmDataset;
import dev.cdm.dataset.api.CdmDatasets;
import dev.cdm.core.io.RandomAccessFile;

/**
 * PartitionCollection for Grib2.
 *
 * @author John
 * @since 12/7/13
 */
public class Grib2Partition extends PartitionCollectionImmutable implements Closeable {

  Grib2Partition(PartitionCollectionMutable pc) {
    super(pc);
  }

  @Override
  public dev.cdm.dataset.api.CdmDataset getNetcdfDataset(Dataset ds, GroupGC group, String filename,
      GribConfig config, Formatter errlog, org.slf4j.Logger logger) throws IOException {

    Grib2Iosp iosp = new Grib2Iosp(group, ds.getType());
    RandomAccessFile raf = (RandomAccessFile) iosp.sendIospMessage(CdmFile.IOSP_MESSAGE_RANDOM_ACCESS_FILE);
    CdmFile ncfile = CdmFiles.build(iosp, raf, getLocation(), null);
    return CdmDatasets.enhance(ncfile, CdmDataset.getDefaultEnhanceMode(), null);
  }

  @Override
  public GribIosp getIosp() throws IOException {
    GribIosp result = new Grib2Iosp(this);
    result.createCustomizer();
    return result;
  }

  @Override
  public void addGlobalAttributes(AttributeContainerMutable result) {
    String val = cust.getGeneratingProcessTypeName(getGenProcessType());
    if (val != null)
      result.addAttribute(new Attribute("Type_of_generating_process", val));
    val = cust.getGeneratingProcessName(getGenProcessId());
    if (val != null)
      result.addAttribute(
          new Attribute("Analysis_or_forecast_generating_process_identifier_defined_by_originating_centre", val));
    val = cust.getGeneratingProcessName(getBackProcessId());
    if (val != null)
      result.addAttribute(new Attribute("Background_generating_process_identifier_defined_by_originating_centre", val));
    result.addAttribute(new Attribute(CDM.FILE_FORMAT, DataFormatType.GRIB2.getDescription()));
  }

  @Override
  public void addVariableAttributes(AttributeContainerMutable v, GribCollectionImmutable.VariableIndex vindex) {
    Grib2Collection.addVariableAttributes(v, vindex, this);
  }

  @Override
  protected String makeVariableId(VariableIndex v) {
    return Grib1Collection.makeVariableId(getCenter(), getSubcenter(), v.getTableVersion(), v.getParameter(),
        v.getLevelType(), v.isLayer(), v.getIntvType(), v.getIntvName());
  }

}
