package dev.ucdm.grib.grib2.iosp;

import dev.cdm.core.calendar.CalendarPeriod;
import dev.cdm.core.util.StringUtil2;
import org.jdom2.Element;
import org.jdom2.Namespace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class GribConfig {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GribConfig.class);

  // keys for storing AuxInfo objects
  public static final String AUX_CONFIG = "gribConfig";

  public enum GribDatasetType {
    TwoD, Best, Analysis, Files, Latest, LatestFile
  }

  private static final Set<GribDatasetType> defaultGribDatasetTypes = Collections.unmodifiableSet(
          EnumSet.of(GribDatasetType.TwoD, GribDatasetType.Best, GribDatasetType.Files, GribDatasetType.Latest));

  public static boolean useGenTypeDef, useTableVersionDef, intvMergeDef = true, useCenterDef;

  public Map<Integer, Integer> gdsHash; // map one gds hash to another
  public Map<Integer, String> gdsNamer; // hash, group name
  public boolean useGenType = useGenTypeDef;
  public boolean useTableVersion = useTableVersionDef;
  public boolean intvMerge = intvMergeDef;
  public boolean useCenter = useCenterDef;
  public boolean unionRuntimeCoord;

  public GribIntvFilter intvFilter;
  public TimeUnitConverterHash tuc;
  public CalendarPeriod userTimeUnit;

  // late binding
  public String latestNamer, bestNamer;
  private Optional<Boolean> filesSortIncreasing = Optional.empty();
  public Set<GribDatasetType> datasets = defaultGribDatasetTypes;

  public String lookupTablePath, paramTablePath; // user defined tables
  public Element paramTable; // ??

  private boolean explicitDatasets;

  public TimeUnitConverter getTimeUnitConverter() {
    return tuc;
  }

  public void configFromXml(Element configElem, Namespace ns) {
    String datasetTypes = configElem.getAttributeValue("datasetTypes");
    if (null != datasetTypes)
      addDatasetType(datasetTypes);

    List<Element> gdsElems = configElem.getChildren("gdsHash", ns);
    for (Element gds : gdsElems)
      addGdsHash(gds.getAttributeValue("from"), gds.getAttributeValue("to"));

    List<Element> tuElems = configElem.getChildren("timeUnitConvert", ns);
    for (Element tu : tuElems)
      addTimeUnitConvert(tu.getAttributeValue("from"), tu.getAttributeValue("to"));

    gdsElems = configElem.getChildren("gdsName", ns);
    for (Element gds : gdsElems)
      addGdsName(gds.getAttributeValue("hash"), gds.getAttributeValue("groupName"));

    if (configElem.getChild("parameterMap", ns) != null)
      paramTable = configElem.getChild("parameterMap", ns);
    if (configElem.getChild("gribParameterTable", ns) != null)
      paramTablePath = configElem.getChildText("gribParameterTable", ns);
    if (configElem.getChild("gribParameterTableLookup", ns) != null)
      lookupTablePath = configElem.getChildText("gribParameterTableLookup", ns);
    if (configElem.getChild("latestNamer", ns) != null)
      latestNamer = configElem.getChild("latestNamer", ns).getAttributeValue("name");
    if (configElem.getChild("bestNamer", ns) != null)
      bestNamer = configElem.getChild("bestNamer", ns).getAttributeValue("name");

    // old way - filesSort element inside the gribConfig element
    Element filesSortElem = configElem.getChild("filesSort", ns);
    if (filesSortElem != null) {
      // String orderByS = filesSortElem.getAttributeValue("orderBy"); // filename vs date ??
      String increasingS = filesSortElem.getAttributeValue("increasing");
      if (increasingS != null) {
        if (increasingS.equalsIgnoreCase("true"))
          filesSortIncreasing = Optional.of(true);
        else if (increasingS.equalsIgnoreCase("false"))
          filesSortIncreasing = Optional.of(false);

      } else { // older way
        Element lexigraphicByName = filesSortElem.getChild("lexigraphicByName", ns);
        if (lexigraphicByName != null) {
          increasingS = lexigraphicByName.getAttributeValue("increasing");
          if (increasingS != null) {
            if (increasingS.equalsIgnoreCase("true"))
              filesSortIncreasing = Optional.of(true);
            else if (increasingS.equalsIgnoreCase("false"))
              filesSortIncreasing = Optional.of(false);
          }
        }
      }
    }

    List<Element> intvElems = configElem.getChildren("intvFilter", ns);
    for (Element intvElem : intvElems) {
      if (intvFilter == null)
        intvFilter = new GribIntvFilter();
      String excludeZero = intvElem.getAttributeValue("excludeZero");
      if (excludeZero != null)
        setExcludeZero(!excludeZero.equals("false"));
      String intervalS = intvElem.getAttributeValue("interval");
      if (intervalS != null)
        intvFilter.addInterval(intervalS);

      String intvLengthS = intvElem.getAttributeValue("intvLength");
      if (intvLengthS != null) {
        int intvLength = Integer.parseInt(intvLengthS);
        List<Element> varElems = intvElem.getChildren("variable", ns);
        for (Element varElem : varElems)
          intvFilter.addVariable(intvLength, varElem.getAttributeValue("id"), varElem.getAttributeValue("prob"));
      }
    }

    List<Element> paramElems = configElem.getChildren("option", ns);
    if (paramElems.isEmpty())
      paramElems = configElem.getChildren("parameter", ns); // backwards compatible
    for (Element param : paramElems) {
      String name = param.getAttributeValue("name");
      String value = param.getAttributeValue("value");
      if (name != null && value != null) {
        setOption(name, value);
      }
    }

    Element pdsHashElement = configElem.getChild("pdsHash", ns);
    useGenType = readValue(pdsHashElement, "useGenType", ns, useGenTypeDef);
    useTableVersion = readValue(pdsHashElement, "useTableVersion", ns, useTableVersionDef);
    intvMerge = readValue(pdsHashElement, "intvMerge", ns, intvMergeDef);
    useCenter = readValue(pdsHashElement, "useCenter", ns, useCenterDef);
  }

  public boolean setOption(String name, String value) {
    if (name == null || value == null)
      return false;

    if (name.equalsIgnoreCase("timeUnit")) {
      setUserTimeUnit(value); // eg "10 min" or "minute"
      return true;
    }
    if (name.equalsIgnoreCase("runtimeCoordinate") && value.equalsIgnoreCase("union")) {
      unionRuntimeCoord = true;
      return true;
    }
    return false;
  }

  public void setUserTimeUnit(String value) {
    if (value != null)
      userTimeUnit = CalendarPeriod.of(value); // eg "10 min" or "minute
  }

  public void setExcludeZero(boolean val) {
    if (intvFilter == null)
      intvFilter = new GribIntvFilter();
    intvFilter.isZeroExcluded = val;
  }

  public void setUseTableVersion(boolean val) {
    useTableVersion = val;
  }

  public void setIntervalLength(int intvLength, String varId) {
    if (intvFilter == null)
      intvFilter = new GribIntvFilter();
    intvFilter.addVariable(intvLength, varId, null);
  }

  private boolean readValue(Element pdsHashElement, String key, Namespace ns, boolean value) {
    if (pdsHashElement != null) {
      Element e = pdsHashElement.getChild(key, ns);
      if (e != null) {
        value = true; // no value means true
        String t = e.getTextNormalize();
        if ("true".equalsIgnoreCase(t))
          value = true;
        if ("false".equalsIgnoreCase(t))
          value = false;
      }
    }
    return value;
  }

  public void addDatasetType(String datasetTypes) {
    // if they list datasetType explicitly, remove defaults
    if (!explicitDatasets)
      datasets = EnumSet.noneOf(GribDatasetType.class);
    explicitDatasets = true;

    for (String type : StringUtil2.split(datasetTypes)) {
      try {
        GribDatasetType fdt = GribDatasetType.valueOf(type);
        if (fdt == GribDatasetType.LatestFile)
          fdt = GribDatasetType.Latest;
        datasets.add(fdt);
      } catch (Exception e) {
        logger.warn("Dont recognize GribDatasetType {}", type);
      }
    }
  }

  public boolean hasDatasetType(GribDatasetType type) {
    return datasets.contains(type);
  }

  public void addGdsHash(String fromS, String toS) {
    if (fromS == null || toS == null)
      return;
    if (gdsHash == null)
      gdsHash = new HashMap<>(10);

    try {
      int from = Integer.parseInt(fromS);
      int to = Integer.parseInt(toS);
      gdsHash.put(from, to);
    } catch (Exception e) {
      logger.warn("Failed  to parse as Integer = {} {}", fromS, toS);
    }
  }

  public void addTimeUnitConvert(String fromS, String toS) {
    if (fromS == null || toS == null)
      return;
    if (tuc == null)
      tuc = new TimeUnitConverterHash();

    try {
      int from = Integer.parseInt(fromS);
      int to = Integer.parseInt(toS);
      tuc.map.put(from, to);
    } catch (Exception e) {
      logger.warn("Failed  to parse as Integer = {} {}", fromS, toS);
    }
  }

  public void addGdsName(String hashS, String name) {
    if (hashS == null || name == null)
      return;
    if (gdsNamer == null)
      gdsNamer = new HashMap<>(5);

    try {
      int hash = Integer.parseInt(hashS);
      gdsNamer.put(hash, name);
    } catch (Exception e) {
      logger.warn("Failed  to parse as Integer = {} {}", hashS, name);
    }
  }

  public void show(Formatter f) {
    f.format("GribConfig= ");
    if (useGenType != useGenTypeDef)
      f.format(" useGenType=%s", useGenType);
    if (useTableVersion != useTableVersionDef)
      f.format(" useTableVersion=%s", useTableVersion);
    if (intvMerge != intvMergeDef)
      f.format(" intvMerge=%s", intvMerge);
    if (useCenter != useCenterDef)
      f.format(" useCenter=%s", useCenter);
    if (userTimeUnit != null)
      f.format(" userTimeUnit= %s", userTimeUnit);
    f.format("%n");
    if (gdsHash != null)
      f.format("  gdsHash=%s%n", gdsHash);
    if (gdsNamer != null)
      f.format("  gdsNamer=%s%n", gdsNamer);
    if (intvFilter != null)
      f.format("  intvFilter=%s%n", intvFilter);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("GribConfig{");
    sb.append("datasets=").append(datasets);
    if (gdsHash != null)
      sb.append(", gdsHash=").append(gdsHash);
    if (gdsNamer != null)
      sb.append(", gdsNamer=").append(gdsNamer);
    sb.append(", useGenType=").append(useGenType);
    sb.append(", useTableVersion=").append(useTableVersion);
    sb.append(", intvMerge=").append(intvMerge);
    sb.append(", useCenter=").append(useCenter);
    if (lookupTablePath != null)
      sb.append(", lookupTablePath='").append(lookupTablePath).append('\'');
    if (paramTablePath != null)
      sb.append(", paramTablePath='").append(paramTablePath).append('\'');
    if (latestNamer != null)
      sb.append(", latestNamer='").append(latestNamer).append('\'');
    if (bestNamer != null)
      sb.append(", bestNamer='").append(bestNamer).append('\'');
    if (paramTable != null)
      sb.append(", paramTable=").append(paramTable);
    if (filesSortIncreasing.isPresent())
      sb.append(", filesSortIncreasing=").append(filesSortIncreasing);
    if (intvFilter != null)
      sb.append(", intvFilter=").append(intvFilter);
    if (userTimeUnit != null)
      sb.append(", userTimeUnit='").append(userTimeUnit).append('\'');
    sb.append('}');
    return sb.toString();
  }

  public Object getIospMessage() {
    if (lookupTablePath != null)
      return "gribParameterTableLookup=" + lookupTablePath;
    if (paramTablePath != null)
      return "gribParameterTable=" + paramTablePath;
    return null;
  }

  public int convertGdsHash(int hashcode) {
    if (gdsHash == null)
      return hashcode;
    Integer convertedValue = gdsHash.get(hashcode);
    if (convertedValue == null)
      return hashcode;
    return convertedValue;
  }

  //////////////////////////////////////////////////////////////////////////

  public interface TimeUnitConverter {
    int convertTimeUnit(int timUnit);
  }

  interface IntvFilter {
    // true means discard
    boolean filter(int id, int intvStart, int intvEnd, int prob);
  }

  static class IntvLengthFilter implements IntvFilter {
    public final int id;
    public final int intvLength;
    public final int prob; // none = Integer.MIN_VALUE;

    public IntvLengthFilter(int id, int intvLength, int prob) {
      this.id = id;
      this.intvLength = intvLength;
      this.prob = prob;
    }

    public boolean filter(int id, int intvStart, int intvEnd, int prob) {
      int intvLength = intvEnd - intvStart;
      boolean needProb = (this.prob != Integer.MIN_VALUE); // filter uses prob
      boolean hasProb = (prob != Integer.MIN_VALUE); // record has prob
      boolean isMine = !needProb || hasProb && (this.prob == prob);
      if (this.id == id && isMine) { // first match in the filter list is used
        return this.intvLength != intvLength; // remove the ones whose intervals dont match
      }
      return false;
    }
  }

  static class IntervalFilter implements IntvFilter {
    public final int start, end;

    public IntervalFilter(int start, int end) {
      this.start = start;
      this.end = end;
    }

    public boolean filter(int id, int intvStart, int intvEnd, int prob) {
      boolean match = (this.start == intvStart) && (this.end == intvEnd); // remove ones that match
      if (match) {
        logger.info("interval filter applied id=" + id);
      }
      return match;
    }
  }

  public static class GribIntvFilter {
    public List<IntvFilter> filterList = new ArrayList<>();
    public boolean isZeroExcluded; // default is false 1/31/2019

    public boolean isZeroExcluded() {
      return isZeroExcluded;
    }

    public boolean hasFilter() {
      return (!filterList.isEmpty());
    }

    // true means discard
    public boolean filter(int id, int intvStart, int intvEnd, int prob) {
      int intvLength = intvEnd - intvStart;
      if (intvLength == 0 && isZeroExcluded())
        return true;

      for (IntvFilter filter : filterList) {
        if (filter.filter(id, intvStart, intvEnd, prob))
          return true;
      }
      return false;
    }

    // <intvFilter interval="225,228">
    void addInterval(String intervalS) {
      if (intervalS == null) {
        logger.warn("Error on interval attribute: must not be empty");
        return;
      }

      String[] s = intervalS.split(",");
      if (s.length != 2) {
        logger.warn("Error on interval attribute: must be of form 'start,end'");
        return;
      }

      try {
        int start = Integer.parseInt(s[0]);
        int end = Integer.parseInt(s[1]);

        filterList.add(new IntervalFilter(start, end));

      } catch (NumberFormatException e) {
        logger.info("Error on intvFilter element - attribute must be an integer");
      }
    }

    /*
     * <intvFilter intvLength="12">
     * <variable id="0-1-8" prob="50800"/>
     * </intvFilter>
     *
     * <intvFilter intvLength="3">
     * <variable id="0-1-8"/>
     * </intvFilter>
     */
    void addVariable(int intvLength, String idS, String probS) {
      if (idS == null) {
        logger.warn("Error on intvFilter: must have an id attribute");
        return;
      }

      String[] s = idS.split("-");
      if (s.length != 3 && s.length != 4) {
        logger.warn(
                "Error on intvFilter: id attribute must be of form 'discipline-category-number' (GRIB2) or 'center-subcenter-version-param' (GRIB1)");
        return;
      }

      try {
        int id;
        if (s.length == 3) { // GRIB2
          int discipline = Integer.parseInt(s[0]);
          int category = Integer.parseInt(s[1]);
          int number = Integer.parseInt(s[2]);
          id = (discipline << 16) + (category << 8) + number;
        } else { // GRIB1
          int center = Integer.parseInt(s[0]);
          int subcenter = Integer.parseInt(s[1]);
          int version = Integer.parseInt(s[2]);
          int param = Integer.parseInt(s[3]);
          id = (center << 8) + (subcenter << 16) + (version << 24) + param;
        }
        int prob = (probS == null) ? Integer.MIN_VALUE : Integer.parseInt(probS);

        filterList.add(new IntvLengthFilter(id, intvLength, prob));

      } catch (NumberFormatException e) {
        logger.info("Error on intvFilter element - attribute must be an integer");
      }
    }

  }

  public static class TimeUnitConverterHash implements TimeUnitConverter {
    public Map<Integer, Integer> map = new HashMap<>(5);

    public int convertTimeUnit(int timeUnit) {
      if (map == null)
        return timeUnit;
      Integer convert = map.get(timeUnit);
      return (convert == null) ? timeUnit : convert;
    }
  }

} // GribConfig