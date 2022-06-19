/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.cdm.dataset.conv;

import dev.cdm.array.Array;
import dev.cdm.array.ArrayType;
import dev.cdm.array.Arrays;
import dev.cdm.core.api.Attribute;
import dev.cdm.core.api.Dimension;
import dev.cdm.core.api.CdmFile;
import dev.cdm.core.api.Variable;
import dev.cdm.core.constants.CDM;
import dev.cdm.dataset.api.CdmDataset;
import dev.cdm.dataset.api.VariableDS;
import dev.cdm.dataset.ncml.NcmlReader;
import dev.cdm.dataset.spi.CoordSystemBuilderProvider;
import dev.cdm.dataset.internal.CoordSystemBuilder;
import dev.cdm.dataset.internal.CoordSystemFactory;
import dev.cdm.core.util.CancelTask;

import java.io.IOException;

/** Zebra ATD files. */
public class ZebraConvention extends CoordSystemBuilder {
  private static final String CONVENTION_NAME = "Zebra";

  ZebraConvention(CdmDataset.Builder<?> datasetBuilder) {
    super(datasetBuilder);
    this.conventionName = CONVENTION_NAME;
  }

  @Override
  protected void augmentDataset(CancelTask cancelTask) throws IOException {
    NcmlReader.wrapNcmlResource(datasetBuilder, CoordSystemFactory.resourcesDir + "Zebra.ncml", cancelTask);

    // special time handling
    // the time coord var is created in the NcML
    // set its values = base_time + time_offset(time)
    Dimension timeDim = rootGroup.findDimension("time").orElse(null);
    VariableDS.Builder<?> base_time = (VariableDS.Builder<?>) rootGroup.findVariableLocal("base_time").orElse(null);
    VariableDS.Builder<?> time_offset = (VariableDS.Builder<?>) rootGroup.findVariableLocal("time_offset").orElse(null);
    Variable.Builder<?> time = rootGroup.findVariableLocal("time").orElse(null);
    if ((timeDim == null) || (base_time == null) || (time_offset == null) || (time == null))
      return;

    String units =
        base_time.getAttributeContainer().findAttributeString(CDM.UNITS, "seconds since 1970-01-01 00:00 UTC");
    time.addAttribute(new Attribute(CDM.UNITS, units));

    Array<Number> orgData;
    Array<Number> modData;
    try {
      double baseValue = ((Number) base_time.orgVar.readArray().getScalar()).doubleValue();
      orgData = (Array<Number>) time_offset.orgVar.readArray();

      int n = (int) orgData.length();
      double[] storage = new double[n];
      int count = 0;
      for (Number val : orgData) {
        storage[count++] = val.doubleValue() + baseValue;
      }
      modData = Arrays.factory(ArrayType.DOUBLE, orgData.getShape(), storage);

    } catch (IOException ioe) {
      parseInfo.format("ZebraConvention failed to create time Coord Axis for file %s err= %s%n",
          datasetBuilder.location, ioe);
      return;
    }

    time.setSourceData(modData);
  }

  public static class Factory implements CoordSystemBuilderProvider {
    @Override
    public String getConventionName() {
      return CONVENTION_NAME;
    }

    @Override
    public boolean isMine(CdmFile ncfile) {
      String s = ncfile.getRootGroup().findAttributeString("Convention", "none");
      return s.startsWith("Zebra");
    }

    @Override
    public CoordSystemBuilder open(CdmDataset.Builder<?> datasetBuilder) {
      return new ZebraConvention(datasetBuilder);
    }
  }

}
