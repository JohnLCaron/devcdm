/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.grib2.table;

import com.google.common.collect.ImmutableList;
import dev.ucdm.grib.grib2.iosp.Grib2Parameter;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Superclass for local table implementations.
 * A Local table overrides some of the methods in Grib2Tables.
 *
 * @author John
 * @since 6/22/11
 */
abstract class LocalTables extends Grib2Tables {
  protected Map<Integer, Grib2Parameter> localParams = new HashMap<>(); // subclass must set

  LocalTables(Grib2TableConfig config) {
    super(config);
  }

  @Override
  public String getParamTablePathUsedFor(int discipline, int category, int number) {
    return isLocal(discipline, category, number) ? super.getParamTablePathUsedFor(discipline, category, number)
        : config.getPath();
  }

  @Override
  public List<Parameter> getParameters() {
    return getLocalParameters();
  }

  protected List<Parameter> getLocalParameters() {
    return localParams.values().stream().sorted(new ParameterSort()).collect(ImmutableList.toImmutableList());
  }

  protected static class ParameterSort implements Comparator<Parameter> {
    public int compare(Parameter p1, Parameter p2) {
      int c = p1.getDiscipline() - p2.getDiscipline();
      if (c != 0)
        return c;
      c = p1.getCategory() - p2.getCategory();
      if (c != 0)
        return c;
      return p1.getNumber() - p2.getNumber();
    }
  }

  @Override
  public String getVariableName(int discipline, int category, int number) {
    if (isLocal(discipline, category, number))
      return super.getVariableName(discipline, category, number);

    Parameter te = getParameter(discipline, category, number);
    if (te == null)
      return super.getVariableName(discipline, category, number);
    else
      return te.getName();
  }

  @Override
  public Parameter getParameter(int discipline, int category, int number) {
    Grib2Parameter plocal = localParams.get(makeParamId(discipline, category, number));

    if (isLocal(discipline, category, number)) {
      Parameter pwmo = WmoParamTable.getParameter(discipline, category, number);
      if (plocal == null)
        return pwmo;
      if (pwmo == null)
        return plocal;

      // allow local table to override all but name, units
      return new Grib2Parameter(plocal, pwmo.getName(), pwmo.getUnit());
    }

    return plocal;
  }

  @Override
  public Parameter getParameterRaw(int discipline, int category, int number) {
    return localParams.get(makeParamId(discipline, category, number));
  }

}
