package dev.ucdm.grib.grib2.table;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import dev.ucdm.grib.common.GribTables;
import dev.ucdm.grib.grib2.iosp.Grib2Parameter;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * The results of EcmwfParamTableCompare indicate there are no significant differences of the parameter tables with WMO.
 * So this class is not used currently in production.
 */
public class EccodesParamTable implements Grib2ParamTableInterface {
  private static final boolean debugOpen = false;
  private static final boolean debug = false;
  private static final String PATH = "resources/grib2/ecmwf/tables";

  private String title;
  private String source;
  private String tableName;

  private final int version, discipline, category;
  private final ImmutableMap<Integer, Grib2Parameter> paramMap;

  public static EccodesParamTable factory(int version, int discipline, int category) {
    try {
      return new EccodesParamTable(version, discipline, category);
    } catch (Exception e) {
      return null;
    }
  }

  private EccodesParamTable(int version, int discipline, int category) throws IOException {
    this.version = version;
    this.discipline = discipline;
    this.category = category;

    this.paramMap = this.readTable(getTablePath());
  }

  @Override
  public String getName() {
    return String.format("Ecmwf version %d discipline %d category %d (%s)", version, discipline, category,
        getTablePath());
  }

  @Override
  public String getShortName() {
    return String.format("Ecmwf version %d (%d-%d)", version, discipline, category);
  }

  @Override
  public List<GribTables.Parameter> getParameters() {
    return paramMap.values().stream().sorted().map(p -> (GribTables.Parameter) p)
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  @Nullable
  public Grib2Parameter getParameter(int code) {
    return paramMap.get(code);
  }

  @Nullable
  public Grib2Parameter getParameter(int discipline, int category, int number) {
    return paramMap.get(number);
  }

  private String getTablePath() {
    return String.format("%s/%d/4.2.%d.%d.table", PATH, version, discipline, category);
  }

  /*
   * # Code table 4.2 - Parameter number by product discipline and parameter category
   * 0 0 Estimated precipitation (kg m-2)
   * 1 1 Instantaneous rain rate (kg m-2 s-1)
   * 2 2 Cloud top height (m)
   * 3 3 Cloud top height quality indicator (Code table 4.219)
   * 4 4 Estimated u-component of wind (m/s)
   * 5 5 Estimated v-component of wind (m/s)
   * 6 6 Number of pixel used (Numeric)
   * 7 7 Solar zenith angle (deg)
   * 8 8 Relative azimuth angle (deg)
   * 9 9 Reflectance in 0.6 micron channel (%)
   * 10 10 Reflectance in 0.8 micron channel (%)
   * 11 11 Reflectance in 1.6 micron channel (%)
   * 12 12 Reflectance in 3.9 micron channel (%)
   * 13 13 Atmospheric divergence (/s)
   * 14 14 Cloudy brightness temperature (K)
   * 15 15 Clear-sky brightness temperature (K)
   * 16 16 Cloudy radiance (with respect to wave number) (W m-1 sr-1)
   * 17 17 Clear-sky radiance (with respect to wave number) (W m-1 sr-1)
   * 18 18 Reserved
   * 19 19 Wind speed (m/s)
   * 20 20 Aerosol optical thickness at 0.635 um
   * 21 21 Aerosol optical thickness at 0.810 um
   * 22 22 Aerosol optical thickness at 1.640 um
   * 23 23 Angstrom coefficient
   * # 24-26 Reserved
   * 27 27 Bidirectional reflectance factor (numeric)
   * 28 28 Brightness temperature (K)
   * 29 29 Scaled radiance (numeric)
   * # 30-191 Reserved
   * # 192-254 Reserved for local use
   * 255 255 Missing
   */
  private ImmutableMap<Integer, Grib2Parameter> readTable(String path) throws IOException {
    ImmutableMap.Builder<Integer, Grib2Parameter> builder = ImmutableMap.builder();

    if (debugOpen) {
      System.out.printf("readEcmwfTable path= %s%n", path);
    }

    ClassLoader cl = Grib2TableConfig.class.getClassLoader();
    try (InputStream is = cl.getResourceAsStream(path)) {
      if (is == null) {
        throw new IllegalStateException("Cant find " + path);
      }
      try (BufferedReader dataIS = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
        int count = 0;
        while (true) {
          String line = dataIS.readLine();
          if (line == null) {
            break;
          }
          if (line.startsWith("#") || line.trim().isEmpty()) {
            continue;
          }
          count++;

          int posBlank1 = line.indexOf(' ');
          int posBlank2 = line.indexOf(' ', posBlank1 + 1);
          int lastParen = line.lastIndexOf('(');

          String num1 = line.substring(0, posBlank1).trim();
          String num2 = line.substring(posBlank1 + 1, posBlank2);
          String desc =
              (lastParen > 0) ? line.substring(posBlank2 + 1, lastParen).trim() : line.substring(posBlank2 + 1).trim();
          String units = (lastParen > 0) ? line.substring(lastParen).trim() : "";
          if (units.startsWith("(") & units.endsWith(")"))
            units = units.substring(1, units.length() - 1);

          if (!num1.equals(num2)) {
            if (debug) {
              System.out.printf("*****num1 != num2 for %s%n", line);
            }
            continue;
          }
          int number = Integer.parseInt(num1);

          // public Grib2Parameter(int discipline, int category, int number, String name, String unit, String abbrev) {
          Grib2Parameter parameter = new Grib2Parameter(discipline, category, number, desc, units, null, desc);
          builder.put(parameter.getNumber(), parameter);
          if (debug) {
            System.out.printf(" %s%n", parameter);
          }
        }
      }
    }
    return builder.build();
  }
}
