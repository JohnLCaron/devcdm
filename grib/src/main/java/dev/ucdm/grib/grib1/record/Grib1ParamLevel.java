/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.grib1.record;

import dev.ucdm.grib.common.util.GribNumbers;
import dev.ucdm.grib.grib1.table.Grib1Customizer;

import dev.ucdm.array.Immutable;

/**
 * Level information contained in a particular PDS.
 * WMO Table 3
 */
public record Grib1ParamLevel(Grib1Customizer cust, int levelType, float value1, float value2) {

  /**
   * Implements tables 3 and 3a.
   *
   * @param cust customized for this center/subcenter
   * @param pds the Grib1SectionProductDefinition
   */
  public static Grib1ParamLevel factory(Grib1Customizer cust, Grib1SectionProductDefinition pds) {

    // default surface values
    float value1, value2;
    int levelType = pds.getLevelType();
    int pds11 = pds.getLevelValue1();
    int pds12 = pds.getLevelValue2();
    int pds1112 = pds11 << 8 | pds12;

    switch (levelType) {
      default:
        value1 = pds11;
        value2 = pds12;
        break;

      case 20:
        value1 = (float) (pds1112 * 0.01);
        value2 = GribNumbers.MISSING;
        break;

      case 100:
      case 103:
      case 105:
      case 160:
      case 126:
      case 125:
      case 117:
      case 115:
      case 113:
      case 111:
      case 109:
        value1 = pds1112;
        value2 = GribNumbers.MISSING;
        break;

      case 101:
        value1 = pds11 * 10; // convert from kPa to hPa - who uses kPa???
        value2 = pds12 * 10;
        break;

      case 104:
      case 106:
        value1 = (pds11 * 100); // convert hm to m
        value2 = (pds12 * 100);
        break;

      case 107:
      case 119:
        value1 = (float) (pds1112 * 0.0001);
        value2 = GribNumbers.MISSING;
        break;

      case 108:
      case 120:
        value1 = (float) (pds11 * 0.01);
        value2 = (float) (pds12 * 0.01);
        break;

      case 110:
        value1 = pds11;
        value2 = pds12;
        break;

      case 112:
        value1 = pds11;
        value2 = pds12;
        break;

      case 114:
        value1 = 475 - pds11;
        value2 = 475 - pds12;
        break;

      case 116:
        value1 = pds11;
        value2 = pds12;
        break;

      case 121:
        value1 = 1100 - pds11;
        value2 = 1100 - pds12;
        break;

      case 128:
        value1 = (float) (1.1 - (pds11 * 0.001));
        value2 = (float) (1.1 - (pds12 * 0.001));
        break;

      case 141:
        // value1 = pds11*10; // convert from kPa to hPa - who uses kPa???
        value1 = pds11; // 388 nows says it is hPA
        value2 = 1100 - pds12;
        break;
    }

    return new Grib1ParamLevel(cust, levelType, value1, value2);
  }

  /**
   * Index number from table 3 (pds octet 10)
   *
   * @return index
   */
  public int getLevelType() {
    return levelType;
  }

  /**
   * gets the 1st value for the level.
   *
   * @return level value 1
   */
  public float getValue1() {
    return value1;
  }

  /**
   * gets the 2nd value for the level.
   *
   * @return level value 2
   */
  public float getValue2() {
    return value2;
  }

  public String getNameShort() {
    return cust.getLevelNameShort(levelType);
  }

  public String getDescription() {
    return cust.getLevelDescription(levelType);
  }
}

