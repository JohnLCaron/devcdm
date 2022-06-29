/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.cdm.dataset.conv;

import dev.cdm.core.api.Attribute;
import dev.cdm.core.api.Variable;
import dev.cdm.core.constants.CDM;
import dev.cdm.core.constants._Coordinate;
import dev.cdm.dataset.api.CdmDataset;
import dev.cdm.dataset.api.CdmDatasetCS;
import dev.cdm.dataset.ncml.NcmlReader;
import dev.cdm.dataset.spi.CoordSystemBuilderProvider;
import dev.cdm.dataset.internal.CoordSystemBuilder;
import dev.cdm.dataset.internal.CoordSystemFactory;
import dev.cdm.core.util.CancelTask;

import java.io.IOException;

/**
 * GEIF Convention.
 * https://www.metnet.navy.mil/~hofschnr/GIEF-F/1.2/
 */
public class GIEFConvention extends CoordSystemBuilder {
  private static final String CONVENTION_NAME = "GIEF";

  GIEFConvention(CdmDatasetCS.Builder<?> datasetBuilder) {
    super(datasetBuilder);
    this.conventionName = CONVENTION_NAME;
  }

  @Override
  protected void augmentDataset(CancelTask cancelTask) throws IOException {
    NcmlReader.wrapNcmlResource(datasetBuilder, CoordSystemFactory.resourcesDir + "GIEF.ncml", cancelTask);

    Variable.Builder<?> timeVar =
        rootGroup.findVariableLocal("time").orElseThrow(() -> new IllegalStateException("must have time variable"));
    String time_units = rootGroup.getAttributeContainer().findAttributeString("time_units", null);
    timeVar.addAttribute(new Attribute(CDM.UNITS, time_units));

    Variable.Builder<?> levelVar =
        rootGroup.findVariableLocal("level").orElseThrow(() -> new IllegalStateException("must have level variable"));
    String level_units = rootGroup.getAttributeContainer().findAttributeString("level_units", null);
    String level_name = rootGroup.getAttributeContainer().findAttributeString("level_name", null);
    levelVar.addAttribute(new Attribute(CDM.UNITS, level_units));
    levelVar.addAttribute(new Attribute(CDM.LONG_NAME, level_name));

    // may be 1 or 2 data variables
    String unit_name = rootGroup.getAttributeContainer().findAttributeString("unit_name", null);
    String parameter_name = rootGroup.getAttributeContainer().findAttributeString("parameter_name", null);
    for (Variable.Builder<?> v : rootGroup.vbuilders) {
      if (v.getRank() > 1) {
        v.addAttribute(new Attribute(CDM.UNITS, unit_name));
        v.addAttribute(new Attribute(CDM.LONG_NAME, v.shortName + " " + parameter_name));
        v.addAttribute(new Attribute(_Coordinate.Axes, "time level latitude longitude"));
      }
    }

    Attribute translation = rootGroup.getAttributeContainer().findAttributeIgnoreCase("translation");
    Attribute affine = rootGroup.getAttributeContainer().findAttributeIgnoreCase("affine_transformation");

    // add lat
    double startLat = translation.getNumericValue(1).doubleValue();
    double incrLat = affine.getNumericValue(6).doubleValue();
    Variable.Builder<?> latVar = rootGroup.findVariableLocal("latitude")
        .orElseThrow(() -> new IllegalStateException("must have latitude variable"));
    latVar.setAutoGen(startLat, incrLat);

    // add lon
    double startLon = translation.getNumericValue(0).doubleValue();
    double incrLon = affine.getNumericValue(3).doubleValue();
    Variable.Builder<?> lonVar = rootGroup.findVariableLocal("longitude")
        .orElseThrow(() -> new IllegalStateException("must have longitude variable"));
    lonVar.setAutoGen(startLon, incrLon);
  }

  public static class Factory implements CoordSystemBuilderProvider {
    @Override
    public String getConventionName() {
      return CONVENTION_NAME;
    }

    @Override
    public CoordSystemBuilder open(CdmDatasetCS.Builder<?> datasetBuilder) {
      return new GIEFConvention(datasetBuilder);
    }
  }

}
