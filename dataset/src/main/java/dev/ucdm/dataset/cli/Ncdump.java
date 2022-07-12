/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.dataset.cli;

import com.google.common.base.Preconditions;
import dev.ucdm.core.util.CancelTask;
import dev.ucdm.core.write.CDLWriter;
import dev.ucdm.dataset.ncml.NcmlWriter;
import org.jdom2.Element;
import dev.ucdm.array.*;
import dev.ucdm.core.api.*;
import dev.ucdm.dataset.api.CdmDatasets;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Formatter;
import java.util.StringTokenizer;
import java.util.function.Predicate;

import static dev.ucdm.array.PrintArray.printArray;

/**
 * Utility to implement ncdump.
 * A difference with ncdump is that the nesting of multidimensional array data is represented by nested brackets,
 * so the output is not legal CDL that can be used as input for ncgen. Also, the default is header only (-h).
 */
@Immutable
public class Ncdump {

  /** Tell Ncdump if you want values printed. */
  public enum WantValues {
    none, coordsOnly, all
  }

  /**
   * ncdump that parses a command string.
   *
   * @param command command string
   * @param out send output here
   * @param cancel allow task to be cancelled; may be null.
   * @throws IOException on write error
   */
  public static void ncdump(String command, Writer out, CancelTask cancel) throws IOException {
    // pull out the filename from the command
    String filename;
    StringTokenizer stoke = new StringTokenizer(command);
    if (stoke.hasMoreTokens())
      filename = stoke.nextToken();
    else {
      out.write(usage);
      return;
    }

    try (CdmFile nc = CdmDatasets.openFile(filename, cancel)) {
      // the rest of the command
      int pos = command.indexOf(filename);
      command = command.substring(pos + filename.length());
      ncdump(nc, command, out, cancel);

    } catch (FileNotFoundException e) {
      out.write("file not found= ");
      out.write(filename);

    } finally {
      out.close();
    }
  }

  /**
   * ncdump, parsing command string, file already open.
   *
   * @param nc apply command to this file
   * @param command : command string
   * @param out send output here
   * @param cancel allow task to be cancelled; may be null.
   */
  public static void ncdump(CdmFile nc, String command, Writer out, CancelTask cancel) throws IOException {
    WantValues showValues = WantValues.none;

    Builder builder = builder(nc).setCancelTask(cancel);

    if (command != null) {
      StringTokenizer stoke = new StringTokenizer(command);

      while (stoke.hasMoreTokens()) {
        String toke = stoke.nextToken();
        if (toke.equalsIgnoreCase("-help")) {
          out.write(usage);
          out.write('\n');
          return;
        }
        if (toke.equalsIgnoreCase("-vall")) {
          showValues = WantValues.all;
        }
        if (toke.equalsIgnoreCase("-c") && (showValues == WantValues.none)) {
          showValues = WantValues.coordsOnly;
        }
        if (toke.equalsIgnoreCase("-ncml")) {
          builder.setNcml(true);
        }
        if (toke.equalsIgnoreCase("-cdl") || toke.equalsIgnoreCase("-strict")) {
          builder.setStrict(true);
        }
        if (toke.equalsIgnoreCase("-v") && stoke.hasMoreTokens()) {
          builder.setVarNames(stoke.nextToken());
        }
        if (toke.equalsIgnoreCase("-datasetname") && stoke.hasMoreTokens()) {
          builder.setLocationName(stoke.nextToken());
        }
      }
    }
    builder.setWantValues(showValues);

    out.write(builder.build().print());
    out.flush();
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////

  public static Builder builder(CdmFile ncfile) {
    return new Builder(ncfile);
  }

  public static class Builder {
    private final CdmFile ncfile;
    private WantValues wantValues = WantValues.none;
    private boolean ncml;
    private boolean strict;
    private String varNames;
    @Nullable
    private String locationName; // NcML location attribute
    private CancelTask cancelTask;

    private Builder(CdmFile ncfile) {
      this.ncfile = ncfile;
    }

    /** show all Variable's values */
    public Builder setShowAllValues() {
      this.wantValues = WantValues.all;
      return this;
    }

    /** show Coordinate Variable's values only */
    public Builder setShowCoordValues() {
      this.wantValues = WantValues.coordsOnly;
      return this;
    }

    public Builder setLocationName(String locationName) {
      if (locationName != null && !locationName.isEmpty()) {
        this.locationName = locationName;
      }
      return this;
    }

    /** set what Variables' values you want output. */
    public Builder setWantValues(WantValues wantValues) {
      this.wantValues = wantValues;
      return this;
    }

    /** set true if outout should be ncml, otherwise CDL. */
    public Builder setNcml(boolean ncml) {
      this.ncml = ncml;
      return this;
    }

    /** strict CDL representation, default false */
    public Builder setStrict(boolean strict) {
      this.strict = strict;
      return this;
    }

    /**
     * @param varNames semicolon delimited list of variables whose data should be printed. May have
     *        Fortran90 like selector: eg varName(1:2,*,2)
     */
    public Builder setVarNames(String varNames) {
      this.varNames = varNames;
      return this;
    }

    /** allow task to be cancelled */
    public Builder setCancelTask(CancelTask cancelTask) {
      this.cancelTask = cancelTask;
      return this;
    }

    public Ncdump build() {
      return new Ncdump(this);
    }
  }

  private final CdmFile ncfile;
  private final WantValues wantValues;
  private final boolean ncml;
  private final boolean strict;
  private final String varNames;
  private final String locationName;
  private final CancelTask cancelTask;

  private Ncdump(Builder builder) {
    this.ncfile = builder.ncfile;
    this.wantValues = builder.wantValues;
    this.ncml = builder.ncml;
    this.strict = builder.strict;
    this.varNames = builder.varNames;
    this.locationName = builder.locationName;
    this.cancelTask = builder.cancelTask;
  }

  public String print() {
    boolean headerOnly = (wantValues == WantValues.none) && (varNames == null);
    Formatter out = new Formatter();

    try {
      if (ncml) {
        return writeNcml(ncfile, wantValues, locationName); // output schema in NcML
      } else if (headerOnly) {
        CDLWriter.writeCDL(ncfile, out, strict, locationName);
      } else {
        Indent indent = new Indent(2);
        CDLWriter cdlWriter = new CDLWriter(ncfile, out, strict);
        cdlWriter.toStringStart(indent, strict, locationName);

        indent.incr();
        out.format("%n%sdata:%n", indent);
        indent.incr();

        if (wantValues == WantValues.all) { // dump all data
          for (Variable v : ncfile.getVariables()) {
            out.format("%s%s = ", indent, v.getFullName());
            printArray(out, v.readArray(), indent);
            if (cancelTask != null && cancelTask.isCancel())
              return out.toString();
          }
        } else if (wantValues == WantValues.coordsOnly) { // dump coordVars
          for (Variable v : ncfile.getVariables()) {
            if (v.isCoordinateVariable()) {
              out.format("%s%s = ", indent, v.getFullName());
              printArray(out, v.readArray(), indent);
            }
            if (cancelTask != null && cancelTask.isCancel())
              return out.toString();
          }
        }

        if ((wantValues != WantValues.all) && (varNames != null)) { // dump the list of variables
          StringTokenizer stoke = new StringTokenizer(varNames, ";");
          while (stoke.hasMoreTokens()) {
            String varSubset = stoke.nextToken(); // variable name and optionally a subset

            if (varSubset.indexOf('(') >= 0) { // has a selector
              Array<?> data = ncfile.readSectionArray(varSubset);
              out.format("%s%s = ", indent, varSubset);
              printArray(out, data, indent);

            } else { // do entire variable
              Variable v = ncfile.findVariable(varSubset);
              if (v == null) {
                out.format(" cant find variable: %s%n   %s", varSubset, usage);
                continue;
              }
              // dont print coord vars if they are already printed
              if ((wantValues != WantValues.coordsOnly) || v.isCoordinateVariable())
                out.format("%s%s = ", indent, v.getFullName());
              printArray(out, v.readArray(), indent);
            }
            if (cancelTask != null && cancelTask.isCancel())
              return out.toString();
          }
        }

        indent.decr();
        indent.decr();
        cdlWriter.toStringEnd();
      }

    } catch (Exception e) {
      out.format("%n%s%n", e.getMessage());
    }

    return out.toString();
  }


  /**
   * Print all the data of the given Variable.
   *
   * @param v variable to print
   * @return String result
   * @throws IOException on write error
   */
  public static String printVariableData(Variable v) throws IOException {
    Array<?> data = v.readArray();
    Formatter out = new Formatter();
    out.format("%s = ", v.getFullName());
    printArray(out, data, new Indent(2));
    return out.toString();
  }

  /**
   * Print array as undifferentiated sequence of values.
   * 
   * @param array any Array except StructureDataArray
   */
  public static String printArrayPlain(Array<?> array) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    for (Object val : array) {
      pw.print(val);
      pw.print(' ');
    }
    return sw.toString();
  }

  //////////////////////////////////////////////////////////////////////////////////////
  // standard NCML writing.

  /**
   * Write the NcML representation for a file.
   * Note that NcMLWriter has a JDOM implementation, for complete NcML.
   * This method implements only the "core" NcML for plain ole netcdf files.
   *
   * @param ncfile write NcML for this file
   * @param showValues do you want the variable values printed?
   * @param url use this for the url attribute; if null use getLocation(). // ??
   */
  private static String writeNcml(CdmFile ncfile, WantValues showValues, @Nullable String url) {
    Preconditions.checkNotNull(ncfile);
    Preconditions.checkNotNull(showValues);

    Predicate<? super Variable> writeVarsPred;
    switch (showValues) {
      case none -> writeVarsPred = NcmlWriter.writeNoVariablesPredicate;
      case coordsOnly -> writeVarsPred = NcmlWriter.writeCoordinateVariablesPredicate;
      case all -> writeVarsPred = NcmlWriter.writeAllVariablesPredicate;
      default -> {
        String message =
                String.format("CAN'T HAPPEN: showValues (%s) != null and checked all possible enum values.", showValues);
        throw new AssertionError(message);
      }
    }

    NcmlWriter ncmlWriter = new NcmlWriter(null, null, writeVarsPred);
    Element netcdfElement = ncmlWriter.makeNetcdfElement(ncfile, url);
    return ncmlWriter.writeToString(netcdfElement);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////

  // TODO use jcommander or another cli
  private static final String usage =
      "usage: Ncdump <filename> [-cdl | -ncml] [-c | -vall] [-v varName1;varName2;..] [-v varName(0:1,:,12)]\n";

  public static void main(String[] args) {
    if (args.length == 0) {
      System.out.println(usage);
      return;
    }

    // pull out the filename from the command
    String filename = args[0];
    try (Writer writer = new BufferedWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8));
        CdmFile nc = CdmDatasets.openFile(filename, null)) {
      // the rest of the command
      StringBuilder command = new StringBuilder();
      for (int i = 1; i < args.length; i++) {
        command.append(args[i]);
        command.append(" ");
      }
      ncdump(nc, command.toString(), writer, null);
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }
}
