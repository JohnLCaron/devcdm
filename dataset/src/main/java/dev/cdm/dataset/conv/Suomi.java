/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.cdm.dataset.conv;

import dev.cdm.core.api.Attribute;
import dev.cdm.core.api.CdmFile;
import dev.cdm.core.api.Variable;
import dev.cdm.core.calendar.CalendarDate;
import dev.cdm.core.calendar.CalendarDateFormatter;
import dev.cdm.core.constants.AxisType;
import dev.cdm.core.constants.CDM;
import dev.cdm.dataset.api.CdmDataset;
import dev.cdm.dataset.api.VariableDS;
import dev.cdm.dataset.spi.CoordSystemBuilderProvider;
import dev.cdm.dataset.internal.CoordSystemBuilder;
import dev.cdm.core.util.CancelTask;

/** Suomi coord sys builder. */
public class Suomi extends CoordSystemBuilder {
  private static final String CONVENTION_NAME = "Suomi";

  private Suomi(CdmDataset.Builder<?> datasetBuilder) {
    super(datasetBuilder);
    this.conventionName = CONVENTION_NAME;
  }

  public static class Factory implements CoordSystemBuilderProvider {
    @Override
    public String getConventionName() {
      return CONVENTION_NAME;
    }

    @Override
    public boolean isMine(CdmFile ncfile) {
      Variable v = ncfile.findVariable("time_offset");
      if (v == null || !v.isCoordinateVariable())
        return false;
      String desc = v.getDescription();
      if (desc == null || (!desc.equals("Time delta from start_time")
          && !desc.equals("PWV window midpoint time delta from start_time")))
        return false;

      if (null == ncfile.findAttribute("start_date"))
        return false;
      return null != ncfile.findAttribute("start_time");
    }

    @Override
    public CoordSystemBuilder open(CdmDataset.Builder<?> datasetBuilder) {
      return new Suomi(datasetBuilder);
    }
  }

  @Override
  public void augmentDataset(CancelTask cancelTask) {
    String start_date = rootGroup.getAttributeContainer().findAttributeString("start_date", null);
    if (start_date == null)
      return;

    CalendarDateFormatter formatter = new CalendarDateFormatter("yyyy.DDD.HH.mm.ss"); // "2006.105.00.00.00"
    CalendarDate start = formatter.parse(start_date);

    rootGroup.findVariableLocal("time_offset")
        .ifPresent(v -> v.addAttribute(new Attribute(CDM.UNITS, "seconds since " + start)));

    rootGroup.addAttribute(new Attribute(CDM.CONVENTIONS, "Suomi-Station-CDM"));
  }

  @Override
  protected AxisType getAxisType(VariableDS.Builder<?> v) {
    String name = v.shortName;
    if (name.equals("time_offset"))
      return AxisType.Time;
    if (name.equals("lat"))
      return AxisType.Lat;
    if (name.equals("lon"))
      return AxisType.Lon;
    if (name.equals("height"))
      return AxisType.Height;
    return null;
  }
}
