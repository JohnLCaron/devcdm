package dev.cdm.dataset.internal;

import com.google.common.collect.ImmutableList;
import dev.cdm.core.api.Attribute;
import dev.cdm.core.api.Group;
import dev.cdm.core.api.CdmFile;
import dev.cdm.core.constants.CDM;
import dev.cdm.core.constants._Coordinate;
import dev.cdm.dataset.api.CdmDataset;
import dev.cdm.dataset.api.CdmDatasetCS;
import dev.cdm.dataset.spi.CoordSystemBuilderProvider;
import dev.cdm.dataset.conv.CF1Convention;
import dev.cdm.dataset.conv.DefaultConventions;
import dev.cdm.dataset.ncml.NcmlReader;
import dev.cdm.core.util.CancelTask;
import dev.cdm.core.util.StringUtil2;

import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.StringTokenizer;

/** Static methods for managing CoordSystemBuilderProvider classes */
public class CoordSystemFactory {
  protected static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CoordSystemFactory.class);

  // Place where resources can be placed, eg NcML files used to wrap datasets.
  public static final String resourcesDir = "resources/nj22/coords/";

  private static final List<Convention> conventionList = new ArrayList<>();
  private static final Map<String, String> ncmlHash = new HashMap<>();
  private static boolean useMaximalCoordSys = true;

  /**
   * Allow plug-ins to determine if it owns a file based on the file's Convention attribute.
   */
  public interface ConventionNameOk {

    /**
     * Do you own this file?
     *
     * @param convName name in the file
     * @param wantName name passed into registry
     * @return true if you own this file
     */
    boolean isMatch(String convName, String wantName);
  }

  // These get precedence
  static {
    registerConvention(_Coordinate.Convention, new CoordSystemBuilder.Factory());
    registerConvention("CF-1.", new CF1Convention.Factory(), String::startsWith);
    registerConvention("CDM-Extended-CF", new CF1Convention.Factory());
    // this is to test DefaultConventions, not needed when we remove old convention builders.
    registerConvention("MARS", new DefaultConventions.Factory());
  }

  /**
   * Register an NcML file that implements a Convention by wrapping the dataset in the NcML.
   * It is then processed by CoordSystemBuilder, using the _Coordinate attributes.
   *
   * @param conventionName name of Convention, must be in the "Conventions" global attribute.
   * @param ncmlLocation location of NcML file, may be local file or URL.
   */
  public static void registerNcml(String conventionName, String ncmlLocation) {
    ncmlHash.put(conventionName, ncmlLocation);
  }

  /**
   * Register a class that implements a CoordSystemBuilderProvider.
   *
   * @param conventionName name of Convention.
   *        This name will be used to look in the "Conventions" global attribute.
   * @param className name of class that implements CoordSystemBuilderProvider.
   * @throws ClassNotFoundException if class could not be found
   */
  public static void registerConvention(String conventionName, String className) throws ClassNotFoundException {
    Class<?> c = Class.forName(className);

    if (!(CoordSystemBuilderProvider.class.isAssignableFrom(c)))
      throw new IllegalArgumentException(c.getName() + " must implement CoordSystemBuilderProvider");

    // fail fast - check newInstance works
    CoordSystemBuilderProvider fac;
    try {
      fac = (CoordSystemBuilderProvider) c.getDeclaredConstructor().newInstance();
    } catch (Exception e) {
      throw new IllegalArgumentException("CoordSysBuilderIF failed for " + c.getName(), e);
    }

    registerConvention(conventionName, fac);
  }

  /**
   * Register a class that implements a Convention. Will match (ignoring case) the COnvention name.
   *
   * @param conventionName name of Convention.
   *        This name will be used to look in the "Conventions" global attribute.
   * @param c implementation of CoordSystemBuilderProvider that parses those kinds of netcdf files.
   */
  public static void registerConvention(String conventionName, CoordSystemBuilderProvider c) {
    registerConvention(conventionName, c, null);
  }

  /**
   * Register a class that implements a Convention.
   *
   * @param conventionName name of Convention.
   *        This name will be used to look in the "Conventions" global attribute.
   *        Otherwise, you must implement the isMine() static method.
   * @param match pass in your own matcher. if null, equalsIgnoreCase() will be used.
   * @param factory implementation of CoordSystemBuilderProvider that parses those kinds of netcdf files.
   */
  public static void registerConvention(String conventionName, CoordSystemBuilderProvider factory,
      ConventionNameOk match) {
    conventionList.add(new Convention(conventionName, factory, match));
  }

  @Nullable
  private static CoordSystemBuilderProvider matchConvention(String convName) {
    for (Convention c : conventionList) {
      if ((c.match == null) && c.convName.equalsIgnoreCase(convName))
        return c.factory;
      if ((c.match != null) && c.match.isMatch(convName, c.convName))
        return c.factory;
    }
    return null;
  }

  /**
   * If true, assign implicit CoordinateSystem objects to variables that dont have one yet.
   * Default value is false.
   *
   * @param b true if if you want to guess at Coordinate Systems
   */
  public static void setUseMaximalCoordSys(boolean b) {
    useMaximalCoordSys = b;
  }

  /**
   * Get whether to make records into Structures.
   *
   * @return whether to make records into Structures.
   */
  public static boolean getUseMaximalCoordSys() {
    return useMaximalCoordSys;
  }

  /**
   * Breakup list of Convention names in the Convention attribute in CF compliant way.
   *
   * @param convAttValue original value of Convention attribute
   * @return list of Convention names
   */
  public static List<String> breakupConventionNames(String convAttValue) {
    ArrayList<String> names = new ArrayList<>();

    if ((convAttValue.indexOf(',') > 0) || (convAttValue.indexOf(';') > 0)) {
      StringTokenizer stoke = new StringTokenizer(convAttValue, ",;");
      while (stoke.hasMoreTokens()) {
        String name = stoke.nextToken();
        names.add(name.trim());
      }
    } else if ((convAttValue.indexOf('/') > 0)) {
      StringTokenizer stoke = new StringTokenizer(convAttValue, "/");
      while (stoke.hasMoreTokens()) {
        String name = stoke.nextToken();
        names.add(name.trim());
      }
    } else {
      return ImmutableList.of(convAttValue);
    }
    return names;
  }

  /**
   * Build a list of Conventions
   *
   * @param mainConv this is the main convention
   * @param convAtts list of others, only use "extra" Conventions
   * @return comma separated list of Conventions
   */
  public static String buildConventionAttribute(String mainConv, String... convAtts) {
    List<String> result = new ArrayList<>();
    result.add(mainConv);
    for (String convs : convAtts) {
      if (convs == null)
        continue;
      List<String> ss = breakupConventionNames(convs); // may be a list
      for (String s : ss) {
        if (matchConvention(s) == null) // only add extra ones, not ones that compete with mainConv
          result.add(s);
      }
    }

    // now form comma separated result
    boolean start = true;
    Formatter f = new Formatter();
    for (String s : result) {
      if (start)
        f.format("%s", s);
      else
        f.format(", %s", s);
      start = false;
    }
    return f.toString();
  }

  /**
   * Get a CoordSystemBuilder whose job it is to add Coordinate information to a NetcdfDataset.Builder.
   * @param ds the CdmDatasetCS.Builder to modify
   */
  public static CoordSystemBuilder factory(CdmDatasetCS.Builder<?> ds, CancelTask cancelTask)
      throws IOException {

    // look for the Conventions attribute
    Group.Builder root = ds.rootGroup;
    String convName = root.getAttributeContainer().findAttributeString(CDM.CONVENTIONS, null);
    if (convName == null) {
      // common mistake Convention instead of Conventions
      convName = root.getAttributeContainer().findAttributeString("Convention", null);
    }
    if (convName != null) {
      convName = convName.trim();
    }

    // look for ncml first
    if (convName != null) {
      String convNcml = ncmlHash.get(convName);
      if (convNcml != null) {
        CoordSystemBuilder csb = new CoordSystemBuilder(ds);
        NcmlReader.wrapNcml(ds, convNcml, cancelTask);
        return csb;
      }
    }
    CoordSystemBuilderProvider coordSysFactory = null;

    // Try to match on convention name. Must be first in case NcML has set Convention name.
    if (convName != null) {
      coordSysFactory = findConventionByName(convName);
    }

    // Try to match on isMine() TODO: why use orgFile instead of ds?
    if (coordSysFactory == null && ds.orgFile != null) {
      coordSysFactory = findConventionByIsMine(ds.orgFile);
    }

    boolean isDefault = false;
    // if no convention class found, use the default
    if (coordSysFactory == null) {
      coordSysFactory = new DefaultConventions.Factory();
      isDefault = true;
    }

    // Now process it.
    CoordSystemBuilder coordSystemBuilder = coordSysFactory.open(ds);
    if (convName == null)
      coordSystemBuilder.addUserAdvice("No 'Conventions' global attribute.");
    else if (isDefault)
      coordSystemBuilder.addUserAdvice("No CoordSystemBuilder is defined for Conventions= '" + convName + "'\n");
    else {
      coordSystemBuilder.setConventionUsed(coordSysFactory.getConventionName());
    }

    ds.rootGroup.addAttribute(new Attribute(_Coordinate._CoordSysBuilder, coordSystemBuilder.getClass().getName()));
    return coordSystemBuilder;
  }

  private static CoordSystemBuilderProvider findConventionByIsMine(CdmFile orgFile) {
    // Look for Convention using isMine()
    for (Convention conv : conventionList) {
      CoordSystemBuilderProvider candidate = conv.factory;
      if (candidate.isMine(orgFile)) {
        return candidate;
      }
    }

    // Use service loader mechanism isMine()
    for (CoordSystemBuilderProvider csb : ServiceLoader.load(CoordSystemBuilderProvider.class)) {
      if (csb.isMine(orgFile)) {
        return csb;
      }
    }

    return null;
  }

  private static CoordSystemBuilderProvider findConventionByName(String convName) {
    // Try to match on convention name as is
    CoordSystemBuilderProvider coordSysFactory = findRegisteredConventionByName(convName);
    if (coordSysFactory != null)
      return coordSysFactory;

    coordSysFactory = findLoadedConventionByName(convName);
    if (coordSysFactory != null)
      return coordSysFactory;

    // Try splitting up the Convention string.
    List<String> names = breakupConventionNames(convName);
    for (String name : names) {
      coordSysFactory = findRegisteredConventionByName(name);
      if (coordSysFactory != null)
        return coordSysFactory;
    }
    for (String name : names) {
      coordSysFactory = findLoadedConventionByName(name);
      if (coordSysFactory != null)
        return coordSysFactory;
    }

    // last ditch desperate - split on white space
    Iterable<String> tokens = StringUtil2.split(convName);
    for (String name : tokens) {
      coordSysFactory = findRegisteredConventionByName(name);
      if (coordSysFactory != null)
        return coordSysFactory;
    }
    for (String name : tokens) {
      coordSysFactory = findLoadedConventionByName(name);
      if (coordSysFactory != null)
        return coordSysFactory;
    }

    return null;
  }

  private static CoordSystemBuilderProvider findRegisteredConventionByName(String convName) {
    // look for registered conventions using convention name
    return matchConvention(convName);
  }

  private static CoordSystemBuilderProvider findLoadedConventionByName(String convName) {
    // use service loader mechanism
    for (CoordSystemBuilderProvider csb : ServiceLoader.load(CoordSystemBuilderProvider.class)) {
      if (convName.equals(csb.getConventionName())) {
        return csb;
      }
    }
    return null;
  }

  private static class Convention {
    final String convName;
    final CoordSystemBuilderProvider factory;
    final ConventionNameOk match;

    Convention(String convName, CoordSystemBuilderProvider factory, ConventionNameOk match) {
      this.convName = convName;
      this.factory = factory;
      this.match = match;
    }
  }

}
