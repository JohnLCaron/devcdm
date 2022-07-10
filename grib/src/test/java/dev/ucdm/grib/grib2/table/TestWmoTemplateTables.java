package dev.ucdm.grib.grib2.table;

import dev.ucdm.grib.grib2.table.WmoTemplateTables.TemplateTable;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

public class TestWmoTemplateTables {

  @Test
  public void testWmoTableConsistency()  {
    WmoTemplateTables tables = WmoTemplateTables.getInstance();
    for (TemplateTable table : tables.getTemplateTables()) {
      for (WmoTemplateTables.Field fld : table.getFlds()) {
        assertThat(fld.getContent() != null);
      }
    }
  }
}
