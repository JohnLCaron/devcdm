/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.grib2.table;

import com.google.common.collect.ImmutableList;
import dev.ucdm.core.util.StringUtil2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * This manages configuring Grib2Tables and their local overrides.
 * Configured by resources/grib2/standardTableMap.txt.
 *
 * @author caron
 * @since 8/1/2014
 */
record Grib2TableConfig(String name, int center, int subCenter, int masterVersion, int localVersion,
                        int genProcessId, String path, Grib2TablesId.Type type) {

  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib2TableConfig.class);
  private static final String tableMapPath = "resources/grib2/standardTableMap.txt";
  private static ImmutableList<Grib2TableConfig> tables;
  private static Grib2TableConfig standardTable;

  private static ImmutableList<Grib2TableConfig> init() {
    List<Grib2TableConfig> result = new ArrayList<>();
    ClassLoader cl = Grib2TableConfig.class.getClassLoader();
    try (InputStream is = cl.getResourceAsStream(tableMapPath)) {
      if (is == null)
        throw new IllegalStateException("Cant find " + tableMapPath);
      try (BufferedReader dataIS = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
        int count = 0;
        while (true) {
          String line = dataIS.readLine();
          if (line == null) {
            break;
          }
          if (line.startsWith("#")) {
            continue;
          }
          count++;

          String[] flds = line.split(";");
          if (flds.length < 7) {
            logger.warn("{} BAD format == {}", count, line);
            continue;
          }

          int fldidx = 0;
          try {
            int center = Integer.parseInt(flds[fldidx++].trim());
            int subcenter = Integer.parseInt(flds[fldidx++].trim());
            int master = Integer.parseInt(flds[fldidx++].trim());
            int local = Integer.parseInt(flds[fldidx++].trim());
            int genProcess = Integer.parseInt(flds[fldidx++].trim());
            String typeName = StringUtil2.remove(flds[fldidx++].trim(), '"');
            String name = StringUtil2.remove(flds[fldidx++].trim(), '"');
            String resource = (flds.length > 7) ? StringUtil2.remove(flds[fldidx++].trim(), '"') : null;
            Grib2TablesId.Type type = Grib2TablesId.Type.valueOf(typeName);
            Grib2TableConfig table =
                new Grib2TableConfig(name, center, subcenter, master, local, genProcess, resource, type);
            result.add(table);

          } catch (Exception e) {
            logger.warn("{} {} BAD line == {} : {}", count, fldidx, line, e.getMessage());
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    standardTable =
        new Grib2TableConfig("WMO", 0, -1, -1, -1, -1, WmoCodeFlagTables.standard.getResourceName(), Grib2TablesId.Type.wmo);
    result.add(standardTable);
    return ImmutableList.copyOf(result);
  }

  static Grib2TableConfig matchTable(Grib2TablesId id) {
    if (tables == null) {
      tables = init();
    }

    // first match wins
    for (Grib2TableConfig table : tables) {
      if (table.id().match(id))
        return table;
    }

    // no match
    return standardTable;
  }

  static ImmutableList<Grib2TableConfig> getTables() {
    if (tables == null)
      tables = init();

    return tables;
  }

  String getPath() {
    return path;
  }

  String getName() {
    return name;
  }

  Grib2TablesId.Type getType() {
    return type;
  }

  // The id in the configuration, will match but maybe not equal the Grib2Record's id.
  Grib2TablesId getConfigId() {
    return id();
  }

  public Grib2TablesId id() {
    return new Grib2TablesId(center, subCenter, masterVersion, localVersion, genProcessId);
  }
}
