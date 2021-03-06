/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.grib.common;

/**
 * GRIB constants.
 *
 * @author caron
 * @since 2/23/2016.
 */
public class GribConstants {

  public static final String VARIABLE_ID_ATTNAME = "Grib_Variable_Id";
  public static final String GRIB_VALID_TIME = "GRIB forecast or observation time";
  public static final String GRIB_RUNTIME = "GRIB reference time";
  public static final String GRIB_STAT_TYPE = "Grib_Statistical_Interval_Type";

  public static final String XAXIS = "xaxis";
  public static final String YAXIS = "yaxis";
  public static final String LAT_AXIS = "lat";
  public static final String LON_AXIS = "lon";


  // do not use
  public static boolean debugRead;
  public static boolean debugGbxIndexOnly; // we are running with only ncx and gbx index files, no data
  static boolean debugIndexOnlyShow; // debugIndexOnly must be true; show record fetch
  static boolean debugIndexOnly; // we are running with only ncx index files, no data

  // Class, not interface, per Bloch edition 2 item 19
  private GribConstants() {} // disable instantiation
}
