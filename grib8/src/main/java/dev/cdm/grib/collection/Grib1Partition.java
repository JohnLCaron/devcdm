/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.cdm.grib.collection;

import dev.cdm.core.api.*;
import dev.cdm.core.constants.DataFormatType;
import dev.cdm.core.constants.CDM;
import dev.cdm.dataset.api.CdmDataset;
import dev.cdm.dataset.api.CdmDatasets;
import dev.cdm.grib.util.GribUtils;
import java.io.IOException;
import java.util.Formatter;

import dev.cdm.core.io.RandomAccessFile;

/**
 * PartitionCollection for Grib1.
 *
 * @author caron
 * @since 2/21/14
 */
public class Grib1Partition extends PartitionCollectionImmutable {

  Grib1Partition(PartitionCollectionMutable pc) {
    super(pc);
  }

  @Override
  public dev.cdm.dataset.api.CdmDataset getNetcdfDataset(Dataset ds, GroupGC group, String filename,
      GribConfig config, Formatter errlog, org.slf4j.Logger logger) throws IOException {

    Grib1Iosp iosp = new Grib1Iosp(group, ds.getType());
    RandomAccessFile raf = (RandomAccessFile) iosp.sendIospMessage(CdmFile.IOSP_MESSAGE_RANDOM_ACCESS_FILE);
    CdmFile ncfile = CdmFiles.build(iosp, raf, getLocation(), null);
    return CdmDatasets.enhance(ncfile, CdmDataset.getDefaultEnhanceMode(), null);
  }

  @Override
  public GribIosp getIosp() throws IOException {
    GribIosp result = new Grib1Iosp(this);
    result.createCustomizer();
    return result;
  }

  @Override
  public void addGlobalAttributes(AttributeContainerMutable result) {
    String val = cust.getGeneratingProcessName(getGenProcessId());
    if (val != null)
      result.addAttribute(new Attribute(GribUtils.GEN_PROCESS, val));
    result.addAttribute(new Attribute(CDM.FILE_FORMAT, DataFormatType.GRIB1.getDescription()));
  }

  @Override
  public void addVariableAttributes(AttributeContainerMutable v, GribCollectionImmutable.VariableIndex vindex) {
    Grib1Collection.addVariableAttributes(v, vindex, this);
  }

  @Override
  public String makeVariableId(GribCollectionImmutable.VariableIndex v) {
    return Grib1Collection.makeVariableId(getCenter(), getSubcenter(), v.getTableVersion(), v.getParameter(),
        v.getLevelType(), v.isLayer(), v.getIntvType(), v.getIntvName());
  }

}
