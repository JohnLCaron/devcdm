/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.cdm.dataset.ncml;

import com.google.common.collect.Sets;
import dev.cdm.array.*;
import dev.cdm.core.api.*;
import dev.cdm.core.util.URLnaming;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.Format;
import org.jdom2.output.LineSeparator;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Formatter;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;

/** Helper class to write NcML. */
public class NcmlWriter {
  private static final Logger log = LoggerFactory.getLogger(NcmlWriter.class);

  /**
   * A default namespace constructed from the NcML URI: {@code https://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2}.
   */
  // A default namespace means that we can use it without having to prepend the "ncml:" prefix to every element name.
  // thredds.client.catalog.Catalog.ncmlNS is *not* default and therefore *does* require the prefix.
  private static final Namespace ncmlDefaultNamespace = Namespace.getNamespace(NcmlReader.NJ22_NAMESPACE);


  //////////////////////////////////////// Variable-writing predicates ////////////////////////////////////////

  /** Predicate that always returns {@code false}. */
  public static final Predicate<? super Variable> writeNoVariablesPredicate = attributes -> false;

  /**
   * Predicate that returns {@code true} for variables that are {@link Variable#isMetadata() metadata variables}.
   * For such variables, the data is not actually present in the file, so we must include it in the NcML.
   * It could be synthesized (i.e. generated by an IOSP) or specified in a <values> element of the input NcML.
   */
  public static final Predicate<? super Variable> writeMetadataVariablesPredicate = Variable::isMetadata;

  /**
   * Predicate that returns {@code true} for variables that are
   * {@link Variable#isCoordinateVariable() coordinate variables}.
   **/
  public static final Predicate<? super Variable> writeCoordinateVariablesPredicate = Variable::isCoordinateVariable;

  /** Predicate that always returns {@code true}. */
  public static final Predicate<? super Variable> writeAllVariablesPredicate = (v) -> true;

  /** Predicate that returns {@code true} for variables whose names are specified to the constructor. */
  public static class WriteVariablesWithNamesPredicate implements Predicate<Variable> {
    private final Set<String> variableNames;

    public WriteVariablesWithNamesPredicate(Iterable<String> variableNames) {
      this.variableNames = Sets.newHashSet(variableNames);
    }

    @Override
    public boolean test(Variable var) {
      return variableNames.contains(var.getFullName());
    }
  }

  ////////////////////////////////////////////////////////////////////////////////

  private final Namespace namespace;
  private final Format xmlFormat;
  private final Predicate<? super Variable> writeValuesPredicate;
  private final XMLOutputter xmlOutputter = new XMLOutputter();

  /**
   * Constructor allows you to set parameters. Any parameters may be null for default.
   *
   * @param namespace if null use ncmlDefaultNamespace.
   * @param xmlFormat if null, use Format.getPrettyFormat().setLineSeparator(LineSeparator.UNIX).
   * @param writeValuesPredicate if null, write a Variable's values if Variable.isMetadata(). This determines whether
   *        values should be written or not. The values will be contained within a {@code <values>} element.
   *        By default, the predicate will be {@link #writeMetadataVariablesPredicate}. There could be data loss if the
   *        values
   *        of metadata variables aren't included in the NcML, so we recommend that you always use it, possibly as part
   *        of a
   *        compound predicate. For example, suppose you wanted to print the values of metadata <b>and</b> coordinate
   *        variables:
   * 
   *        <pre>
   *        Predicate<Variable> compoundPred =
   *        Predicates.or(writeMetadataVariablesPredicate, writeCoordinateVariablesPredicate);
   *        ncmlWriter.setWriteVariablesPredicate(compoundPred);
   *        </pre>
   */
  public NcmlWriter(@Nullable Namespace namespace, @Nullable Format xmlFormat,
                    @Nullable Predicate<? super Variable> writeValuesPredicate) {
    this.namespace = namespace == null ? ncmlDefaultNamespace : namespace;
    this.xmlFormat = xmlFormat == null ? Format.getPrettyFormat().setLineSeparator(LineSeparator.UNIX) : xmlFormat;
    this.writeValuesPredicate = writeValuesPredicate == null ? writeMetadataVariablesPredicate : writeValuesPredicate;
  }

  /** Constructor with default values */
  public NcmlWriter() {
    this.namespace = ncmlDefaultNamespace;
    this.xmlFormat = Format.getPrettyFormat().setLineSeparator(LineSeparator.UNIX);
    this.writeValuesPredicate = writeMetadataVariablesPredicate;
  }

  /**
   * Gets the XML namespace for the elements in the NcML. By default, it is {@link #ncmlDefaultNamespace}.
   *
   * @return the XML namespace.
   */
  public Namespace getNamespace() {
    return namespace;
  }

  /**
   * Gets the object that encapsulates XML formatting options. By default, the format is
   * {@link Format#getPrettyFormat() pretty} with {@link LineSeparator#UNIX UNIX line separators}.
   *
   * @return the XML formatting options.
   */
  public Format getXmlFormat() {
    return xmlFormat;
  }

  /**
   * Gets the predicate that will be applied to variables to determine wither their values should be written in
   * addition to their metadata. The values will be contained within a {@code <values>} element.
   *
   * @return the predicate.
   */
  public Predicate<? super Variable> getWriteValuesPredicate() {
    return writeValuesPredicate;
  }

  /**
   * Writes an NcML element to a string.
   *
   * @param elem an NcML element.
   * @return the string that represents the NcML document.
   */
  public String writeToString(Element elem) {
    try (StringWriter writer = new StringWriter()) {
      writeToWriter(elem, writer);
      return writer.toString();
    } catch (IOException e) {
      throw new AssertionError("CAN'T HAPPEN: StringWriter.close() is a no-op.", e);
    }
  }

  /**
   * Writes an NcML element to an output file.
   *
   * @param elem an NcML element.
   * @param outFile the file to write the NcML document to.
   * @throws IOException if there's any problem writing.
   */
  public void writeToFile(Element elem, File outFile) throws IOException {
    try (OutputStream outStream = new BufferedOutputStream(new FileOutputStream(outFile, false))) {
      writeToStream(elem, outStream);
    }
  }

  /**
   * Writes an NcML element to an output stream.
   *
   * @param elem an NcML element.
   * @param outStream the stream to write the NcML document to. Will be closed at end of the method.
   * @throws IOException if there's any problem writing.
   */
  public void writeToStream(Element elem, OutputStream outStream) throws IOException {
    try (Writer writer =
        new BufferedWriter(new OutputStreamWriter(new BufferedOutputStream(outStream), xmlFormat.getEncoding()))) {
      writeToWriter(elem, writer);
    }
  }

  /**
   * Writes an NcML element to a Writer.
   *
   * @param elem an NcML element.
   * @param writer the Writer to write the NcML document to. Will be closed at end of the method.
   * @throws IOException if there's any problem writing.
   */
  public void writeToWriter(Element elem, Writer writer) throws IOException {
    xmlOutputter.setFormat(xmlFormat);
    elem.detach(); // In case this element had previously been added to a Document.
    xmlOutputter.output(new Document(elem), writer);
  }

  //////////////////////////////////////// Element creation ////////////////////////////////////////

  public Element makeExplicitNetcdfElement(CdmFile ncFile, @Nullable String location) {
    Element netcdfElem = makeNetcdfElement(ncFile, location);
    netcdfElem.addContent(0, new Element("explicit", namespace));
    return netcdfElem;
  }

  // Set location to empty to suppress location attribute

  /**
   * Make the netcdf element for this file.
   * 
   * @param location Set to non-empty String to override location attribute.
   */
  public Element makeNetcdfElement(CdmFile ncFile, @Nullable String location) {
    Element rootElem = makeGroupElement(ncFile.getRootGroup());

    // rootElem isn't just like any other group element; we must undo some of the changes made to it in writeGroup().
    rootElem.setName("netcdf"); // Was "group".
    rootElem.removeAttribute("name"); // This attribute is not defined on the root "netcdf" element.

    rootElem.addNamespaceDeclaration(namespace);

    if (null == location) {
      location = ncFile.getLocation();
    }

    if (null != location && !location.isEmpty()) {
      rootElem.setAttribute("location", URLnaming.canonicalizeWrite(location));
    }

    if (null != ncFile.getId())
      rootElem.setAttribute("id", ncFile.getId());

    if (null != ncFile.getTitle())
      rootElem.setAttribute("title", ncFile.getTitle());

    return rootElem;
  }

  public Element makeGroupElement(Group group) {
    Element elem = new Element("group", namespace);
    elem.setAttribute("name", group.getShortName());

    // enumTypeDef
    for (EnumTypedef etd : group.getEnumTypedefs()) {
      elem.addContent(makeEnumTypedefElement(etd));
    }

    // dimensions
    for (Dimension dim : group.getDimensions()) {
      elem.addContent(makeDimensionElement(dim));
    }

    // regular variables
    for (Variable var : group.getVariables()) {
      boolean showValues = writeValuesPredicate.test(var);
      elem.addContent(makeVariableElement(var, showValues));
    }

    // nested groups
    for (Group g : group.getGroups()) {
      Element groupElem = new Element("group", namespace);
      groupElem.setAttribute("name", g.getShortName());
      elem.addContent(makeGroupElement(g));
    }

    // attributes
    for (Attribute att : group.attributes()) {
      elem.addContent(makeAttributeElement(att));
    }

    return elem;
  }

  // enum Typedef
  public Element makeEnumTypedefElement(EnumTypedef etd) {
    Element typeElem = new Element("enumTypedef", namespace);
    typeElem.setAttribute("name", etd.getShortName());
    typeElem.setAttribute("type", etd.getBaseArrayType().toNcml());

    // Use a TreeMap so that the key-value pairs are emitted in a consistent order.
    TreeMap<Integer, String> map = new TreeMap<>(etd.getMap());

    for (Entry<Integer, String> entry : map.entrySet()) {
      typeElem.addContent(new Element("enum", namespace).setAttribute("key", Integer.toString(entry.getKey()))
          .addContent(entry.getValue()));
    }

    return typeElem;
  }

  // Only for shared dimensions.
  public Element makeDimensionElement(Dimension dim) throws IllegalArgumentException {
    if (!dim.isShared()) {
      throw new IllegalArgumentException(
          "Cannot create private dimension: " + "in NcML, <dimension> elements are always shared.");
    }

    Element dimElem = new Element("dimension", namespace);
    dimElem.setAttribute("name", dim.getShortName());
    dimElem.setAttribute("length", Integer.toString(dim.getLength()));

    if (dim.isUnlimited())
      dimElem.setAttribute("isUnlimited", "true");

    return dimElem;
  }

  public Element makeVariableElement(Variable var, boolean showValues) {
    boolean isStructure = var instanceof Structure;

    Element varElem = new Element("variable", namespace);
    varElem.setAttribute("name", var.getShortName());

    StringBuilder buff = new StringBuilder();
    List<Dimension> dims = var.getDimensions();
    for (int i = 0; i < dims.size(); i++) {
      Dimension dim = dims.get(i);
      if (i > 0)
        buff.append(" ");
      if (dim.isShared())
        buff.append(dim.getShortName());
      else if (dim.isVariableLength())
        buff.append("*");
      else
        buff.append(dim.getLength());
    }
    // if (buff.length() > 0)
    varElem.setAttribute("shape", buff.toString());

    ArrayType dt = var.getArrayType();
    if (dt != null) {
      varElem.setAttribute("type", dt.toNcml());
      if (dt.isEnum())
        varElem.setAttribute("typedef", var.getEnumTypedef().getShortName());
    }


    // attributes
    for (Attribute att : var.attributes()) {
      varElem.addContent(makeAttributeElement(att));
    }

    if (isStructure) {
      Structure s = (Structure) var;
      for (Variable variable : s.getVariables()) {
        varElem.addContent(makeVariableElement(variable, showValues));
      }
    } else if (showValues) {
      try {
        varElem.addContent(makeValuesElement(var, true));
      } catch (IOException e) {
        String message = String.format("Couldn't read values for %s. Omitting <values> element.%n\t%s",
            var.getFullName(), e.getMessage());
        log.warn(message);
      }
    }

    return varElem;
  }

  public Element makeAttributeElement(Attribute attribute) {
    Element attElem = new Element("attribute", namespace);
    attElem.setAttribute("name", attribute.getShortName());

    ArrayType dt = attribute.getArrayType();
    if ((dt != null) && (dt != ArrayType.STRING))
      attElem.setAttribute("type", dt.toNcml());

    if (attribute.getLength() == 0) {
      // if (attribute.isUnsigned())
      // attElem.setAttribute("isUnsigned", "true");
      return attElem;
    }

    if (attribute.isString()) {
      StringBuilder buff = new StringBuilder();
      for (int i = 0; i < attribute.getLength(); i++) {
        String sval = attribute.getStringValue(i);
        if (i > 0)
          buff.append("|");
        buff.append(sval);
      }

      attElem.setAttribute("value", cleanCharacterData(buff.toString()));
      if (attribute.getLength() > 1)
        attElem.setAttribute("separator", "|");

    } else {
      StringBuilder buff = new StringBuilder();
      for (int i = 0; i < attribute.getLength(); i++) {
        Number val = attribute.getNumericValue(i);
        if (i > 0)
          buff.append(" ");
        buff.append(val);
      }
      attElem.setAttribute("value", buff.toString());

      // if (attribute.isUnsigned())
      // attElem.setAttribute("isUnsigned", "true");
    }
    return attElem;
  }

  /**
   * Creates a {@code <values>} element from the variable's data.
   *
   * @param variable the variable to read values from
   * @param allowRegular {@code true} if regular values should be represented with {@code start}, {@code increment},
   *        and {@code npts} attributes instead of space-separated Element text. Has no effect if the data isn't
   *        regular.
   * @return the {@code <values>} element.
   * @throws IOException if there was an I/O error when reading the variable's data.
   */
  public Element makeValuesElement(Variable variable, boolean allowRegular) throws IOException {
    Element elem = new Element("values", namespace);

    Formatter buff = new Formatter();
    Array<?> a = variable.readArray();

    if (variable.getArrayType() == ArrayType.CHAR) {
      Array<Byte> bdata = (Array<Byte>) a;
      elem.setText(Arrays.makeStringFromChar(bdata));

    } else if (variable.getArrayType() == ArrayType.STRING) {
      elem.setAttribute("separator", "|");
      int count = 0;
      Array<String> sdata = (Array<String>) a;
      for (String s : sdata) {
        if (count++ > 0) {
          buff.format("|");
        }
        buff.format("%s", s);
      }
      elem.setText(buff.toString());

    } else {
      Array<Number> ndata = (Array<Number>) a;
      // check to see if regular
      if (allowRegular && (a.getRank() == 1) && (a.getSize() > 2)) {
        
        Index ima = ndata.getIndex();
        double start = ndata.get(ima.set(0)).doubleValue();
        double incr = ndata.get(ima.set(1)).doubleValue() - start;
        boolean isRegular = true;
        for (int i = 2; i < ndata.getSize(); i++) {
          double v1 = ndata.get(ima.set(i)).doubleValue();
          double v0 = ndata.get(ima.set(i - 1)).doubleValue();
          if (!NumericCompare.nearlyEquals(v1 - v0, incr, 2.e-7)) {
            isRegular = false;
          }
        }

        if (isRegular) {
          elem.setAttribute("start", String.format("%f", start));
          elem.setAttribute("increment", String.format("%f", incr));
          elem.setAttribute("npts", String.format("%d", variable.getSize()));
          return elem;
        }
      }

      // not regular
      boolean first = true;
      for (Number val : ndata) {
        if (!first) {
          buff.format(" ");
        }
        switch (variable.getArrayType()) {
          case FLOAT:
            buff.format("%f", val.floatValue());
            break;
          case DOUBLE:
            buff.format("%f", val.doubleValue());
            break;
          default:
            buff.format("%d", val.intValue());
            break;
        }
        first = false;
      }
      elem.setText(buff.toString());

    } // not string

    return elem;
  }

  /**
   * Make sure that text is XML safe
   *
   * @param text check this
   * @return original text if ok, else with bad characters removed
   */
  private static String cleanCharacterData(String text) {
    if (text == null)
      return null;

    boolean bad = false;
    for (int i = 0, len = text.length(); i < len; i++) {
      int ch = text.charAt(i);
      if (!org.jdom2.Verifier.isXMLCharacter(ch)) {
        bad = true;
        break;
      }
    }

    if (!bad)
      return text;

    StringBuilder sbuff = new StringBuilder(text.length());
    for (int i = 0, len = text.length(); i < len; i++) {
      int ch = text.charAt(i);
      if (org.jdom2.Verifier.isXMLCharacter(ch))
        sbuff.append((char) ch);
    }
    return sbuff.toString();
  }
}
