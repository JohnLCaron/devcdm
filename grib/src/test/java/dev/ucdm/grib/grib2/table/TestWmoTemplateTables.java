package dev.ucdm.grib.grib2.table;

import dev.ucdm.grib.grib2.table.WmoTemplateTables.TemplateTable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public class TestWmoTemplateTables {

  @Test
  public void testWmoTableConsistency() throws IOException {
    WmoTemplateTables tables = WmoTemplateTables.getInstance();
    for (TemplateTable table : tables.getTemplateTables()) {
      for (WmoTemplateTables.Field fld : table.getFlds()) {
        assertThat(fld.getContent() != null);
      }
    }
  }
}
