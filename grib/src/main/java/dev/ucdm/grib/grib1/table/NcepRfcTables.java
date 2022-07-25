/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.grib1.table;

import java.nio.charset.StandardCharsets;
import org.jetbrains.annotations.Nullable;
import dev.ucdm.core.util.StringUtil2;
import dev.ucdm.grib.common.util.GribResourceReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * NCEP River Forecast Centers
 *
 * @author caron
 * @since 1/13/12
 */
public class NcepRfcTables extends NcepTables {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(NcepRfcTables.class);
  private static Map<Integer, String> nwsoSubCenter;

  NcepRfcTables(Grib1ParamTables tables) {
    super(8, tables);
  }

  @Override
  @Nullable
  public String getGeneratingProcessName(int genProcess) {
    return switch (genProcess) {
      case 150 -> "NWS River Forecast System (NWSRFS)";
      case 151 -> "NWS Flash Flood Guidance System (NWSFFGS)";
      case 152 -> "Quantitative Precipitation Estimation (QPE) - 1 hr dur";
      case 154 -> "Quantitative Precipitation Estimation (QPE) - 6 hr dur";
      case 155 -> "Quantitative Precipitation Estimation (QPE) - 24hr dur";
      case 156 -> "Process 1 (P1) Precipitation Estimation - automatic";
      case 157 -> "Process 1 (P1) Precipitation Estimation - manual";
      case 158 -> "Process 2 (P2) Precipitation Estimation - automatic";
      case 159 -> "Process 2 (P2) Precipitation Estimation - manual";
      case 160 -> "Multisensor Precipitation Estimation (MPE) - automatic";
      case 161 -> "Multisensor Precipitation Estimation (MPE) - manual";
      case 165 -> "Enhanced MPE - automatic";
      case 166 -> "Bias Enhanced MPE - automatic";
      case 170 -> "Post Analysis of Precipitation Estimation (aggregate)";
      case 171 -> "XNAV Aggregate Precipitation Estimation";
      case 172 -> "Mountain Mapper Precipitation Estimation";
      case 180 -> "Quantitative Precipitation Forecast (QPF)";
      case 185 -> "NOHRSC_OPPS";
      case 190 -> "Satellite Autoestimator Precipitation";
      case 191 -> "Satellite Interactive Flash Flood Analyzer (IFFA)";
      default -> null;
    };
  }

  ///////////////////////////////////////////////////////////////////////
  /*
   * TABLE C - SUB-CENTERS FOR CENTER 9 US NWS FIELD STATIONS
   * from bdgparm.f John Halquist <John.Halquist@noaa.gov> 9/12/2011
   * These are not in the WMO common tables like NCEP's are
   */
  @Override
  @Nullable
  public String getSubCenterName(int subcenter) {
    if (nwsoSubCenter == null)
      nwsoSubCenter = readNwsoSubCenter("resources/grib1/noaa_rfc/tableC.txt");
    if (nwsoSubCenter == null)
      return null;

    return nwsoSubCenter.get(subcenter);
  }

  // order: num, name, desc, unit
  @Nullable
  private static Map<Integer, String> readNwsoSubCenter(String path) {
    Map<Integer, String> result = new HashMap<>();

    try (InputStream is = GribResourceReader.getInputStream(path);
         BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
      while (true) {
        String line = br.readLine();
        if (line == null) {
          break;
        }
        if ((line.isEmpty()) || line.startsWith("#")) {
          continue;
        }

        StringBuilder lineb = new StringBuilder(line);
        StringUtil2.removeAll(lineb, "'+,/");
        String[] flds = lineb.toString().split("[:]");

        int val = Integer.parseInt(flds[0].trim()); // must have a number
        String name = flds[1].trim() + ": " + flds[2].trim();

        result.put(val, name);
      }
      return Collections.unmodifiableMap(result); // all at once - thread safe

    } catch (IOException ioError) {
      logger.warn("An error occurred in Grib1Tables while trying to open the table " + path + " : " + ioError);
      return null;
    }
  }
}
