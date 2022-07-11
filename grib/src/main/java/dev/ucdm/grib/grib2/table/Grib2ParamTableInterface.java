package dev.ucdm.grib.grib2.table;

import dev.ucdm.grib.common.GribTables;

import org.jetbrains.annotations.Nullable;
import java.util.List;

public interface Grib2ParamTableInterface {
  String getName();

  String getShortName();

  List<GribTables.Parameter> getParameters();

  /**
   * Find the Parameter in this table with the given number.
   * 
   * @param number unsigned byte.
   */
  @Nullable
  GribTables.Parameter getParameter(int number);
}
