/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.core.testutil;

import dev.ucdm.array.*;
import dev.ucdm.core.api.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Formatter;
import java.util.Iterator;

/**
 * Compare reading data for two Variables.
 */
public class CompareCdmFiles {

    public static boolean compareVariable(CdmFile arrayFile1, CdmFile arrayFile2, String varName, boolean justOne) {
    boolean ok = true;

    Variable vorg = arrayFile1.findVariable(varName);
    if (vorg == null) {
      System.out.printf("  Cant find variable %s in original %s%n", varName, arrayFile1.getLocation());
      return false;
    }
    Variable vnew = arrayFile2.findVariable(varName);
    if (vnew == null) {
      System.out.printf("  Cant find variable %s in copy %s%n", varName, arrayFile2.getLocation());
      return false;
    }
    return compareVariableData(new Formatter(), vorg, vnew, justOne);
  }

  public static boolean compareVariableData(Formatter f, Variable vorg, Variable vnew, boolean justOne) {
    boolean ok = true;

    if (vorg.getArrayType() == ArrayType.SEQUENCE) {
      System.out.printf("  read sequence %s %s%n", vorg.getArrayType(), vorg.getShortName());
      Sequence s = (Sequence) vorg;
      Iterator<StructureData> orgSeq = s.iterator();
      Sequence copyv = (Sequence) vnew;
      Iterator<StructureData> array = copyv.iterator();
      boolean ok1 = CompareArrayToArray.compareSequence(f, vorg.getShortName(), orgSeq, array);
      if (!ok1) {
        System.out.printf("%s%n", f);
      }
      ok &= ok1;

    } else {
      long size = vorg.getSize();
      if (size < Integer.MAX_VALUE) {
        try {
          Array<?> org = vorg.readArray();
          Array<?> array = vnew.readArray();
          boolean ok1 = CompareArrayToArray.compareData(f, vorg.getShortName(), org, array, justOne);
          if (!ok1) {
            System.out.printf("%s%n", f);
          }
          ok &= ok1;
        } catch (IOException ioe) {
          throw new RuntimeException(ioe);
        }
      }
    }

    return ok;
  }

  public static boolean compareSequence(String filename) throws IOException {
    try (CdmFile org = CdmFiles.open(filename, -1, null);
        CdmFile copy = CdmFiles.open(filename, -1, null)) {
      System.out.println("Test CdmFile: " + org.getLocation());

      boolean ok = true;
      for (Variable v : org.getVariables()) {
        if (v.getArrayType() == ArrayType.SEQUENCE) {
          System.out.printf("  read sequence %s %s%n", v.getArrayType(), v.getShortName());
          Sequence s = (Sequence) v;
          Iterator<StructureData> orgSeq = s.iterator();
          Sequence copyv = (Sequence) copy.findVariable(v.getFullName());
          Iterator<StructureData> array = copyv.iterator();
          Formatter f = new Formatter();
          boolean ok1 = compareSequence(f, v.getShortName(), orgSeq, array);
          if (!ok1) {
            System.out.printf("%s%n", f);
          }
          ok &= ok1;
        }
      }
      return ok;
    } catch (FileNotFoundException e) {
      File file = new File(filename);
      System.out.printf("File.getAbsolutePath = %s%n", file.getAbsolutePath());
      throw e;
    }
  }

  public static boolean compareSequence(Formatter f, String name, Iterator<StructureData> org,
      Iterator<StructureData> array) {
    boolean ok = true;
    int obsrow = 0;
    System.out.printf(" compareSequence %s%n", name);
    while (org.hasNext() && array.hasNext()) {
      ok &= CompareArrayToArray.compareStructureData(f, org.next(), array.next(), false);
      obsrow++;
    }
    return ok;
  }

}

