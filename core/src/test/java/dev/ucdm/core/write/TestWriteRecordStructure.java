/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.core.write;

import dev.ucdm.core.constants.CDM;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import dev.ucdm.array.ArrayType;
import dev.ucdm.array.Arrays;
import dev.ucdm.array.Index;
import dev.ucdm.core.api.Attribute;
import dev.ucdm.core.api.Dimension;

import java.io.File;

/** Test {@link Netcdf3FormatWriter} create a new netCDF file with a "record structure" */
public class TestWriteRecordStructure {
  @TempDir
  public static File tempFolder;

  @Test
  public void testWriteRecordStructure() throws Exception {
    String filename = File.createTempFile("testWriteRecordStructure", ".tmp", tempFolder).getAbsolutePath();

    Netcdf3FormatWriter.Builder<?> writerb = Netcdf3FormatWriter.createNewNetcdf3(filename).setFill(false);
    writerb.addUnlimitedDimension("time");
    Dimension latDim = writerb.addDimension("lat", 3);
    Dimension lonDim = writerb.addDimension("lon", 4);

    // define Variables
    writerb.addVariable("lat", ArrayType.FLOAT, "lat").addAttribute(new Attribute("units", "degrees_north"));

    writerb.addVariable("lon", ArrayType.FLOAT, "lon").addAttribute(new Attribute("units", "degrees_east"));

    writerb.addVariable("rh", ArrayType.INT, "time lat lon")
        .addAttribute(new Attribute(CDM.LONG_NAME, "relative humidity"))
        .addAttribute(new Attribute("units", "percent"));

    writerb.addVariable("T", ArrayType.DOUBLE, "time lat lon")
        .addAttribute(new Attribute(CDM.LONG_NAME, "surface temperature")).addAttribute(new Attribute("units", "degC"));

    writerb.addVariable("time", ArrayType.INT, "time").addAttribute(new Attribute("units", "hours since 1990-01-01"));

    try (Netcdf3FormatWriter writer = writerb.build()) {
      // write out the non-record variables
      writer.write(writer.findVariable("lat"), Index.ofRank(1),
          Arrays.factory(ArrayType.FLOAT, new int[] {3}, new float[] {41, 40, 39}));
      writer.write(writer.findVariable("lon"), Index.ofRank(1),
          Arrays.factory(ArrayType.FLOAT, new int[] {4}, new float[] {-109, -107, -105, -103}));

      //// heres where we write the record variables

      // different ways to create the data arrays. Note the outer dimension has shape 1.
      int[] timeData = new int[1];
      int[] rhData = new int[latDim.getLength() * lonDim.getLength()];
      double[] tempData = new double[latDim.getLength() * lonDim.getLength()];

      int[] dataShape = new int[] {1, latDim.getLength(), lonDim.getLength()};
      int[] timeShape = new int[] {1};
      Index dataIndex = Index.ofRank(3);
      Index timeIndex = Index.ofRank(1);

      // loop over each record
      for (int time = 0; time < 10; time++) {
        // make up some data for this record, using different ways to fill the data arrays.
        timeData[0] = time * 12;

        int count = 0;
        for (int lat = 0; lat < latDim.getLength(); lat++) {
          for (int lon = 0; lon < lonDim.getLength(); lon++) {
            rhData[count] = time * lat * lon;
            tempData[count] = time * lat * lon / 3.14159;
            count++;
          }
        }

        // write the data out for this record
        writer.write(writer.findVariable("rh"), dataIndex.set0(time), Arrays.factory(ArrayType.INT, dataShape, rhData));
        writer.write(writer.findVariable("T"), dataIndex.set0(time),
            Arrays.factory(ArrayType.DOUBLE, dataShape, tempData));
        writer.write(writer.findVariable("time"), timeIndex.set0(time),
            Arrays.factory(ArrayType.INT, timeShape, timeData));
      }
    }
  }
}
