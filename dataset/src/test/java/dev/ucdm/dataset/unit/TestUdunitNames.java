package dev.ucdm.dataset.unit;

import dev.ucdm.dataset.api.TestCdmDatasets;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.Test;
import tech.units.indriya.format.SimpleUnitFormat;

import javax.measure.spi.ServiceProvider;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TestUdunitNames {

  private static SimpleUnitFormat unitFormat = (SimpleUnitFormat) ServiceProvider.current().getFormatService().getUnitFormat("SIMPLE_ASCII");


  //private static Map<String, Unit<?>> units = Units.getInstance().getUnits().stream()
  //        .collect(Collectors.toMap(u -> u.toString(), u -> u));

  void testAgainstUnit(List<Udunit> udunits) {
    int matches = 0;
    for (Udunit udunit : udunits) {
      if (testAgainstUnit(udunit)) {
        matches++;
      }
    }
    System.out.printf("total = %d, match = %d%n", udunits.size(), matches);
  }

  boolean testAgainstUnit(Udunit udunit) {
    boolean match = false;
    System.out.printf("name %s (%s), ", udunit.name, unitFormat.unitFor(udunit.name) );
    if (udunit.symbol != null) {
      boolean ok = unitFormat.unitFor(udunit.symbol) != null;
      System.out.printf("symbol %s (%s), ", udunit.symbol, ok);
      if (ok) match = true;
    }
    for (String alias : udunit.aliases) {
      boolean ok = unitFormat.unitFor(alias)  != null;
      System.out.printf("alias %s (%s), ", alias, ok);
      if (ok) match = true;
    }
    System.out.printf("== %s%n", match);
    return match;
  }

  @Test
  public void testUdunitsBase() throws IOException {
    testAgainstUnit(readUdunitXmlFile("udunits/udunits2-base.xml"));
  }

  @Test
  public void testUdunitsDerived() throws IOException {
    // change sievert, not an alias
    // change becquerel, not an alias
    testAgainstUnit(readUdunitXmlFile("udunits/udunits2-derived.xml"));
  }

  @Test
  public void testUdunitsAccepted() throws IOException {
    // capitalize Litre
    testAgainstUnit(readUdunitXmlFile("udunits/udunits2-accepted.xml"));
  }

  @Test
  public void testUdunitsCommon() throws IOException {
    testAgainstUnit(readUdunitXmlFile("udunits/udunits2-common.xml"));
  }

  @Test
  public void showUdunits() throws IOException {
    readUdunitXmlFile("udunits/udunits2-base.xml")
            .stream()
            .sorted(Comparator.comparing(u -> u.getId().toLowerCase()))
            .forEach(u -> System.out.printf("%s%n", u));

    readUdunitXmlFile("udunits/udunits2-derived.xml")
            .stream()
            .sorted(Comparator.comparing(u -> u.getId().toLowerCase()))
            .forEach(u -> System.out.printf("%s%n", u));


    readUdunitXmlFile("udunits/udunits2-accepted.xml")
            .stream()
            .sorted(Comparator.comparing(u -> u.getId().toLowerCase()))
            .forEach(u -> System.out.printf("%s%n", u));

    readUdunitXmlFile("udunits/udunits2-common.xml")
            .stream()
            .sorted(Comparator.comparing(u -> u.getId().toLowerCase()))
            .forEach(u -> System.out.printf("%s%n", u));
  }

  private final static int shortDefLen = 60;

  private class Udunit {
    String base;
    String name;
    String def;
    String symbol;
    List<String> aliases = new ArrayList<>();
    String definition;
    boolean dimensionless;

    String getId() {
      return name.isEmpty() ? def : name;
    }

    String getShortDefinition() {
      return definition.length() < shortDefLen ? definition : definition.substring(0, shortDefLen);
    }

    @Override
    public String toString() {
      return String.format("%20s %3s %s %s", getId(), symbol, aliases, getShortDefinition());
    }
  }

  private List<Udunit> readUdunitXmlFile(String xmlFilename) throws IOException {
    System.out.printf("%n**** readUdunitXmlFile %s%n", TestCdmDatasets.datasetLocalDir + xmlFilename);
    List<Udunit> result = new ArrayList<>();
    try (InputStream ios = new FileInputStream(TestCdmDatasets.datasetLocalDir + xmlFilename)) {
      org.jdom2.Document doc;
      try {
        SAXBuilder builder = new SAXBuilder();
        builder.setExpandEntities(false);
        doc = builder.build(ios);
      } catch (JDOMException e) {
        throw new IOException(e.getMessage());
      }
      Element root = doc.getRootElement();
      Namespace ns = root.getNamespace();
      List<Element> units = root.getChildren("unit", ns);
      for (Element elem : units) {
        Udunit udunit = new Udunit();
        udunit.base = elem.getChildTextNormalize("base");
        udunit.name = readName(elem, ns, udunit.aliases);
        udunit.def = elem.getChildTextNormalize("def");
        udunit.symbol = elem.getChildTextNormalize("symbol");
        udunit.definition = elem.getChildTextNormalize("definition");
        readAliases(elem.getChild("aliases", ns), ns, udunit.aliases);
        udunit.dimensionless = elem.getChild("dimensionless", ns) != null;
        result.add(udunit);
      }
    }
    return result;
  }

  String readName(Element elem, Namespace ns, List<String> aliases) {
    Element nameElement = elem.getChild("name", ns);
    if (nameElement != null) {
      if (nameElement.getChildTextNormalize("plural") != null) {
        aliases.add(nameElement.getChildTextNormalize("plural"));
      }
      return nameElement.getChildTextNormalize("singular");
    }
    return "";
  }

  void readAliases(Element aliasesElem, Namespace ns, List<String> aliases) {
    if (aliasesElem == null) {
      return;
    }
    for (Element nameElement : aliasesElem.getChildren("name", ns)) {
      if (nameElement.getChildTextNormalize("singular") != null) {
        aliases.add(nameElement.getChildTextNormalize("singular"));
      }
      if (nameElement.getChildTextNormalize("plural") != null) {
        aliases.add(nameElement.getChildTextNormalize("plural"));
      }
    }
  }
}
