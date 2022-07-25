/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.grib1.table;

import org.jetbrains.annotations.Nullable;

import dev.ucdm.grib.common.GribStatType;
import dev.ucdm.grib.common.GribTables;
import dev.ucdm.grib.common.wmo.CommonCodeTable;
import dev.ucdm.grib.coord.VertCoordUnit;
import dev.ucdm.grib.grib1.record.Grib1ParamLevel;
import dev.ucdm.grib.grib1.record.Grib1ParamTime;
import dev.ucdm.grib.grib1.iosp.Grib1Parameter;
import dev.ucdm.grib.grib1.record.Grib1Record;
import dev.ucdm.grib.grib1.record.Grib1SectionProductDefinition;
import dev.ucdm.grib.common.GribConfig;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import dev.ucdm.grib.common.util.GribResourceReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interprets grib1 info in a way that may be customized.
 * This class handles the default case, using only standard WMO tables.
 * Subclasses override as needed.
 *
 * Bit of a contradiction, since getParamter() allows different center, subcenter, version (the version is for sure
 * needed). But other tables are fixed by center.
 */
public class Grib1Customizer implements GribTables {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib1Customizer.class);

  public static Grib1Customizer factory(Grib1Record proto, Grib1ParamLookup tables) {
    int center = proto.getPDSsection().getCenter();
    int subcenter = proto.getPDSsection().getSubCenter();
    int version = proto.getPDSsection().getTableVersion();
    return factory(center, subcenter, version, tables);
  }

  public static Grib1Customizer factory(int center, int subcenter, int version, Grib1ParamLookup tables) {
    return switch (center) {
      case 7 -> new NcepTables(tables);
      case 9 -> new NcepRfcTables(tables);
      case 34 -> new JmaTables(tables);
      case 57 -> new AfwaTables(tables);
      case 58 -> new FnmocTables(tables);
      case 60 -> new NcarTables(tables);
      default -> new Grib1Customizer(center, tables);
    };
  }

  public static String getSubCenterNameStatic(int center, int subcenter) {
    Grib1Customizer cust = Grib1Customizer.factory(center, subcenter, 0, null);
    return cust.getSubCenterName(subcenter);
  }

  ///////////////////////////////////////
  private final int center;
  private final Grib1ParamLookup tables;

  protected Grib1Customizer(int center, Grib1ParamLookup tables) {
    this.center = center;
    this.tables = (tables == null) ? new Grib1ParamLookup() : tables;

    synchronized (Grib1Customizer.class) {
      if (wmoTable3 == null)
        wmoTable3 = readTable3("resources/grib1/wmoTable3.xml");
    }
  }

  public int getCenter() {
    return center;
  }

  public Grib1Parameter getParameter(int center, int subcenter, int tableVersion, int param_number) {
    return tables.getParameter(center, subcenter, tableVersion, param_number);
  }

  @Override
  @Nullable
  public String getGeneratingProcessName(int genProcess) {
    return null;
  }

  @Override
  public String getGeneratingProcessTypeName(int genProcess) {
    return null;
  }

  @Nullable
  public String getSubCenterName(int subcenter) {
    return CommonCodeTable.getSubCenterName(center, subcenter);
  }

  @Override
  @Nullable
  public String getSubCenterName(int center, int subcenter) {
    return CommonCodeTable.getSubCenterName(center, subcenter);
  }

  ///////////////////////////////////////////////////
  // time

  public Grib1ParamTime getParamTime(Grib1SectionProductDefinition pds) {
    return Grib1ParamTime.factory(this, pds);
  }

  // code table 5
  public String getTimeTypeName(int timeRangeIndicator) {
    return Grib1ParamTime.getTimeTypeName(timeRangeIndicator);
  }

  @Override
  @Nullable
  public GribStatType getStatType(int timeRangeIndicator) {
    return Grib1WmoTimeType.getStatType(timeRangeIndicator);
  }

  /////////////////////////////////////////
  // level

  public Grib1ParamLevel getParamLevel(Grib1SectionProductDefinition pds) {
    return Grib1ParamLevel.factory(this, pds);
  }

  @Override
  public VertCoordUnit getVertUnit(int code) {
    return makeVertUnit(code);
  }

  public boolean isVerticalCoordinate(int levelType) {
    return getLevelUnits(levelType) != null;
  }

  // below are the methods a subclass may need to override for levels

  protected VertCoordUnit makeVertUnit(int code) {
    return getLevelType(code);
  }

  @Override
  public String getLevelNameShort(int levelType) {
    VertCoordUnit lt = getLevelType(levelType);
    String result = lt.abbrev();
    return (result == null) ?"unknownLevel" + levelType : result;
  }

  @Nullable
  public String getLevelDescription(int levelType) {
    VertCoordUnit lt = getLevelType(levelType);
    return lt.description();
  }

  public boolean isLayer(int levelType) {
    VertCoordUnit lt = getLevelType(levelType);
    return lt.isLayer();
  }

  // only for 3D
  public boolean isPositiveUp(int levelType) {
    VertCoordUnit lt = getLevelType(levelType);
    return lt.isPositiveUp();
  }

  // only for 3D
  public String getLevelUnits(int levelType) {
    VertCoordUnit lt = getLevelType(levelType);
    return lt.units();
  }

  // only for 3D
  public String getLevelDatum(int levelType) {
    VertCoordUnit lt = getLevelType(levelType);
    return lt.datum();
  }

  /////////////////////////////////////////////
  private GribConfig.TimeUnitConverter timeUnitConverter;

  public void setTimeUnitConverter(GribConfig.TimeUnitConverter timeUnitConverter) {
    this.timeUnitConverter = timeUnitConverter;
  }

  public int convertTimeUnit(int timeUnit) {
    if (timeUnitConverter == null)
      return timeUnit;
    return timeUnitConverter.convertTimeUnit(timeUnit);
  }

  ////////////////////////////////////////////////////////////////////////

  private static Map<Integer, VertCoordUnit> wmoTable3; // shared by all instances

  protected VertCoordUnit getLevelType(int code) {
    VertCoordUnit result = wmoTable3.get(code);
    if (result == null)
      result = new VertCoordUnit(code, "unknownLayer" + code, null, "unknownLayer" + code, null, false, false);
    return result;
  }

  @Nullable
  protected synchronized Map<Integer, VertCoordUnit> readTable3(String path) {
    try (InputStream is = GribResourceReader.getInputStream(path)) {
      SAXBuilder builder = new SAXBuilder();
      builder.setExpandEntities(false);
      org.jdom2.Document doc = builder.build(is);
      Element root = doc.getRootElement();

      Map<Integer, VertCoordUnit> result = new HashMap<>(200);
      List<Element> params = root.getChildren("parameter");
      for (Element elem1 : params) {
        int code = Integer.parseInt(elem1.getAttributeValue("code"));
        String desc = elem1.getChildText("description");
        String abbrev = elem1.getChildText("abbrev");
        String units = elem1.getChildText("units");
        String datum = elem1.getChildText("datum");
        boolean isLayer = elem1.getChild("isLayer") != null;
        boolean isPositiveUp = elem1.getChild("isPositiveUp") != null;
        VertCoordUnit lt = new VertCoordUnit(code, desc, abbrev, units, datum, isPositiveUp, isLayer);
        result.put(code, lt);
      }

      return Collections.unmodifiableMap(result); // all at once - thread safe
    } catch (IOException | JDOMException e) {
      logger.error("Cant parse NcepLevelTypes = " + path, e);
      return null;
    }
  }

  @Override
  public String toString() {
    return String.format("Grib1Customizer{ class=%s, center=%d, Grib1ParamTables=%s}", getClass().getName(), center,
        tables);
  }
}
