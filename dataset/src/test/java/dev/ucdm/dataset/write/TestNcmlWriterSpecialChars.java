/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.dataset.write;

import dev.ucdm.core.api.Attribute;
import dev.ucdm.core.api.CdmFile;
import dev.ucdm.core.api.Dimension;
import dev.ucdm.core.api.Variable;
import dev.ucdm.core.write.Netcdf3FormatWriter;
import dev.ucdm.dataset.api.CdmDatasets;
import dev.ucdm.dataset.ncml.NcmlWriter;
import org.jdom2.Element;
import org.junit.jupiter.api.Test;
import dev.ucdm.array.ArrayType;
import dev.ucdm.array.Index;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import static com.google.common.truth.Truth.assertThat;

/** Test writing and reading NcmlWriter with special characters. */
public class TestNcmlWriterSpecialChars {

  String trouble = "here is a &, <, >, \', \", \n, \r, \t, to handle";

  @TempDir
  public static File tempFolder;

  @Test
  public void testNcmlWriter() throws Exception {
    String filename = File.createTempFile("testNcmlWriter", ".nc", tempFolder).getCanonicalPath();

    Netcdf3FormatWriter.Builder<?> writerb = Netcdf3FormatWriter.createNewNetcdf3(filename);
    writerb.addAttribute(new Attribute("omy", trouble));
    writerb.addDimension("t", 1);
    writerb
        .addDimension(Dimension.builder().setName("t_strlen").setLength(trouble.length()).setIsShared(false).build());

    // define Variables
    writerb.addVariable("t", ArrayType.CHAR, "t_strlen").addAttribute(new Attribute("yow", trouble));

    try (Netcdf3FormatWriter writer = writerb.build()) {
      Variable v = writer.findVariable("t");
      assertThat(v).isNotNull();
      writer.writeStringData(v, Index.ofRank(1), trouble);
    }

    String ncmlFilePath = File.createTempFile("testNcmlWriter", ".ncml", tempFolder).getAbsolutePath();
    try (CdmFile ncfile = CdmDatasets.openDataset(filename)) {
      String val = ncfile.getRootGroup().findAttributeString("omy", null);
      assertThat(val).isNotNull();
      assertThat(val).isEqualTo(trouble);

      Variable v = ncfile.findVariable("t");
      assertThat(v).isNotNull();

      val = v.findAttributeString("yow", null);
      assertThat(val).isNotNull();
      assertThat(val).isEqualTo(trouble);

      try (OutputStream out = new FileOutputStream(ncmlFilePath)) {
        NcmlWriter ncmlWriter = new NcmlWriter();
        Element netcdfElem = ncmlWriter.makeNetcdfElement(ncfile, null);
        ncmlWriter.writeToStream(netcdfElem, out);
      }

      try (CdmFile ncfile2 = CdmDatasets.openDataset(filename)) {
        String val2 = ncfile2.getRootGroup().findAttributeString("omy", null);
        assertThat(val2).isNotNull();
        assertThat(val2).isEqualTo(trouble);

        Variable v2 = ncfile2.findVariable("t");
        assertThat(v2).isNotNull();

        val2 = v2.findAttributeString("yow", null);
        assertThat(val2).isNotNull();
        assertThat(val2).isEqualTo(trouble);
      }
    }

    try (CdmFile ncfile3 = CdmDatasets.openDataset(filename)) {
      System.out.println("ncml= " + ncfile3.getLocation());

      String val = ncfile3.getRootGroup().findAttributeString("omy", null);
      assertThat(val).isNotNull();
      assertThat(val).isEqualTo(trouble);

      Variable v = ncfile3.findVariable("t");
      assertThat(v).isNotNull();

      val = v.findAttributeString("yow", null);
      assertThat(val).isNotNull();
      assertThat(val).isEqualTo(trouble);
    }
  }
}
