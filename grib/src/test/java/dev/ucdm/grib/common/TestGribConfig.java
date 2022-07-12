package dev.ucdm.grib.common;

import dev.ucdm.test.util.TestFilesKt;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

// TODO how much of this actually is used ??
public class TestGribConfig {

  @Test
  public void testXmlConfig() throws IOException, JDOMException {
    File configFile = new File(TestFilesKt.gribLocalDir + "config/gribConfig.xml");
    org.jdom2.Document doc;
    SAXBuilder builder = new SAXBuilder();
    builder.setExpandEntities(false);
    doc = builder.build(configFile);

    XMLOutputter xmlOut = new XMLOutputter();
    System.out.println(xmlOut.outputString(doc));

    GribConfig config = new GribConfig();
    config.configFromXml(doc.getRootElement(), Namespace.NO_NAMESPACE);
    System.out.printf("config = %s%n", config);

    assertThat(config.datasets).hasSize(3);
    assertThat(config.hasDatasetType(GribConfig.GribDatasetType.Best)).isTrue();

    assertThat(config.latestNamer).isEqualTo("Latest FNMOC COAMPS Equatorial America 0.15 degree");
    assertThat(config.bestNamer).isEqualTo("Best FNMOC COAMPS Equatorial America 0.15 degree Time Series");
    assertThat(config.gdsHash).hasSize(1);
    assertThat(config.useTableVersion).isTrue();

    assertThat(config.getTimeUnitConverter()).isNotNull();
    assertThat(config.getTimeUnitConverter().convertTimeUnit(8)).isEqualTo(11);
    assertThat(config.getTimeUnitConverter().convertTimeUnit(7)).isEqualTo(7);

    assertThat(config.convertGdsHash(-2121584860)).isEqualTo(28944332);
    assertThat(config.getIospMessage()).isEqualTo("gribParameterTableLookup=lookupTablePath");

    System.out.printf("show = %s%n", config.show(new Formatter()).toString());
  }

}
