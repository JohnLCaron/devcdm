/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.grib1.table;

/**
 * NCAR (center 60) overrides
 *
 * @author caron
 * @since 8/29/13
 */
public class NcarTables extends Grib1Customizer {

  NcarTables(Grib1ParamTables tables) {
    super(60, tables);
  }

  // from http://rda.ucar.edu/docs/formats/grib/gribdoc/
  @Override
  public String getSubCenterName(int subcenter) {
    return switch (subcenter) {
      case 1 -> "CISL/SCD/Data Support Section";
      case 2 -> "NCAR Command Language";
      case 3 -> "ESSL/MMM/WRF Model";
      default -> "unknown";
    };
  }
}
