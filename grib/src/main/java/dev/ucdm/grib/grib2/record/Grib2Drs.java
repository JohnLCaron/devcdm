/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.grib2.record;

import com.google.common.base.MoreObjects;
import dev.ucdm.core.io.RandomAccessFile;
import dev.ucdm.grib.common.util.BitReader;
import dev.ucdm.grib.common.util.GribDataUtils;
import dev.ucdm.grib.common.util.GribNumbers;

import java.io.IOException;

/**
 * Template-specific fields for Grib2SectionDataRepresentation
 *
 * Section 5 – Data representation section
 * Octet No. Contents
 *  1–4 Length of section in octets (nn)
 *  5 Number of section (5)
 *  6–9 Number of data points where one or more values are specified in Section 7 when a bit map
 *      is present, total number of data points when a bit map is absent.
 *  10–11 Data representation template number (see Code table 5.0)
 *  12–nn Data representation template (see Template 5.X, where X is the data representation
 *      template number given in octets 10–11)
 */
public abstract class Grib2Drs {

  public static Grib2Drs factory(int template, RandomAccessFile raf) throws IOException {
    return switch (template) {
      case 0, 41 -> new SimplePacking(raf);
      case 2 -> new ComplexPacking(raf);
      case 3 -> new SpatialDifferencing(raf);
      case 40 -> new Jpeg2000(raf);
      // ECMWF's second order packing
      case 50002 -> new Ecmwf50002(raf);
      default -> throw new UnsupportedOperationException("Unsupported DRS type = " + template);
    };
  }

  // for debugging
  abstract GribDataUtils.Info getBinaryDataInfo(RandomAccessFile raf);

  /*
   * Data representation template 5.0 – Grid point data – simple packing
   * Note: For most templates, details of the packing process are described in Regulation 92.9.4.
   * Octet No. Contents
   * 12–15 Reference value (R) (IEEE 32-bit floating-point value)
   * 16–17 Binary scale factor (E)
   * 18–19 Decimal scale factor (D)
   * 20 Number of bits used for each packed value for simple packing, or for each group reference
   * value for complex packing or spatial differencing
   * 21 Type of original field values (see Code table 5.1)
   * Note: Negative values of E or D shall be represented according to Regulation 92.1.5.
   */

  public static class SimplePacking extends Grib2Drs {
    public float referenceValue;
    int binaryScaleFactor, decimalScaleFactor, numberOfBits, originalType;

    SimplePacking(RandomAccessFile raf) throws IOException {
      this.referenceValue = raf.readFloat();
      this.binaryScaleFactor = GribNumbers.int2(raf);
      this.decimalScaleFactor = GribNumbers.int2(raf);
      this.numberOfBits = raf.read();
      this.originalType = raf.read();
    }

    public GribDataUtils.Info getBinaryDataInfo(RandomAccessFile raf) {
      GribDataUtils.Info info = new GribDataUtils.Info();
      info.referenceValue = this.referenceValue;
      info.binaryScaleFactor = this.binaryScaleFactor;
      info.decimalScaleFactor = this.decimalScaleFactor;
      info.numberOfBits = this.numberOfBits;
      info.originalType = this.originalType;

      return info;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("referenceValue", referenceValue)
          .add("binaryScaleFactor", binaryScaleFactor).add("decimalScaleFactor", decimalScaleFactor)
          .add("numberOfBits", numberOfBits).add("originalType", originalType).toString();
    }
  }

  /*
   * Data representation template 5.2 – Grid point data – complex packing
   * Note: For most templates, details of the packing process are described in Regulation 92.9.4.
   * 
   * 22 Group splitting method used (see Code table 5.4)
   * 23 Missing value management used (see Code table 5.5)
   * 24–27 Primary missing value substitute
   * 28–31 Secondary missing value substitute
   * 32–35 NG – number of groups of data values into which field is split
   * 36 Reference for group widths (see Note 12)
   * 37 Number of bits used for the group widths (after the reference value in octet 36 has been removed)
   * 38–41 Reference for group lengths (see Note 13)
   * 42 Length increment for the group lengths (see Note 14)
   * 43–46 True length of last group
   * 47 Number of bits used for the scaled group lengths (after subtraction of the reference value
   * given in octets 38–41 and division by the length increment given in octet 42)
   * 
   * Notes:
   * (1) Group lengths have no meaning for row by row packing, where groups are coordinate lines (so the grid description
   * section and possibly the bit-map section are enough); for consistency, associated field width and reference should
   * then be encoded as 0.
   * (2) For row by row packing with a bit-map, there should always be as many groups as rows. In case of rows with only
   * missing values, all associated descriptors should be coded as zero.
   * (3) Management of widths into a reference and increments, together with management of lengths as scaled incremental
   * values, are intended to save descriptor size (which is an issue as far as compression gains are concerned).
   * (4) Management of explicitly missing values is an alternative to bit-map use within Section 6; it is intended to
   * reduce the whole GRIB message size.
   * (5) There may be two types of missing value(s), such as to make a distinction between static misses (for instance,
   * due to a land/sea mask) and occasional misses.
   * (6) As an extra option, substitute value(s) for missing data may be specified. If not wished (or not applicable),
   * all bits should be set to 1 for relevant substitute value(s).
   * (7) If substitute value(s) are specified, type of content should be consistent with original field values
   * (floating-point - and then IEEE 32-bit encoded-, or integer).
   * (8) If primary missing values are used, such values are encoded within appropriate group with all bits set to 1 at
   * packed data level.
   * (9) If secondary missing values are used, such values are encoded within appropriate group with all bits set to 1,
   * except the last one set to 0, at packed data level.
   * (10) A group containing only missing values (of either type) will be encoded as a constant group (null width, no
   * associated data) and the group reference will have all bits set to 1 for primary type, and all bits set to 1,
   * except the last bit set to 0, for secondary type.
   * (11) If necessary, group widths and/or field width of group references may be enlarged to avoid ambiguities between
   * missing value indicator(s) and true data.
   * (12) The group width is the number of bits used for every value in a group.
   * (13) The group length (L) is the number of values in a group.
   * (14) The essence of the complex packing method is to subdivide a field of values into NG groups, where the values
   * in each group have similar sizes. In this procedure, it is necessary to retain enough information to recover the
   * group lengths upon decoding. The NG group lengths for any given field can be described by
   *    Ln = ref + Kn x len_inc, n = 1, NG
   * where ref is given by octets 38–41 and len_inc by octet 42.
   * The NG values of K (the scaled group lengths) are stored in the data section, each with the number of bits
   * specified by octet 47.
   * Since the last group is a special case which may not be able to be specified by this relationship, the length of
   * the last group is stored in octets 43–46.
   * (15) See data template 7.2 and associated Notes for complementary information.
   * 
   * Code Table Code table 5.5 - Missing value management for complex packing (5.5)
   *   0: No explicit missing values included within data values
   *   1: Primary missing values included within data values
   *   2: Primary and secondary missing values included within data values
   */

  public static class ComplexPacking extends SimplePacking {
    float secondaryMissingValue, primaryMissingValue;
    int missingValueManagement, splittingMethod, numberOfGroups, referenceGroupWidths, bitsGroupWidths;
    int referenceGroupLength, lengthIncrement, lengthLastGroup, bitsScaledGroupLength;

    ComplexPacking(RandomAccessFile raf) throws IOException {
      super(raf);
      this.splittingMethod = raf.read();
      this.missingValueManagement = raf.read();
      this.primaryMissingValue = raf.readFloat();
      this.secondaryMissingValue = raf.readFloat();
      this.numberOfGroups = GribNumbers.int4(raf);
      this.referenceGroupWidths = raf.read();
      this.bitsGroupWidths = raf.read();
      this.referenceGroupLength = GribNumbers.int4(raf);
      this.lengthIncrement = raf.read();
      this.lengthLastGroup = GribNumbers.int4(raf);
      this.bitsScaledGroupLength = raf.read();
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("secondaryMissingValue", secondaryMissingValue)
          .add("primaryMissingValue", primaryMissingValue).add("missingValueManagement", missingValueManagement)
          .add("splittingMethod", splittingMethod).add("numberOfGroups", numberOfGroups)
          .add("referenceGroupWidths", referenceGroupWidths).add("bitsGroupWidths", bitsGroupWidths)
          .add("referenceGroupLength", referenceGroupLength).add("lengthIncrement", lengthIncrement)
          .add("lengthLastGroup", lengthLastGroup).add("bitsScaledGroupLength", bitsScaledGroupLength).toString();
    }
  }

  /*
   * Data representation template 5.3 – Grid point data – complex packing and spatial differencing
   * Note: For most templates, details of the packing process are described in Regulation 92.9.4.
   * Octet No. Contents
   *  12–47 Same as data representation template 5.2
   *  48 Order of spatial differencing (see Code table 5.6)
   *  49 Number of octets required in the data section to specify extra descriptors needed for spatial differencing
   *     (octets 6–ww in data template 7.3)
   *
   * Notes:
   * (1) Spatial differencing is a pre-processing before group splitting at encoding time. It is intended to reduce the size of
   * sufficiently smooth fields, when combined with a splitting scheme as described in data representation template 5.2. At
   * order 1, an initial field of values f is replaced by a new field of values g, where
   *      g1 = f1, g2 = f2 – f1, …, gn = fn – fn–1.
   * At order 2, the field of values g is itself replaced by a new field of values h, where
   *      h1 = f1, h2 = f2, h3 = g3 – g2, …, hn = gn – gn–1.
   * To keep values positive, the overall minimum of the resulting field (either gmin or hmin) is removed. At decoding
   * time, after bit string unpacking, the original scaled values are recovered by adding the overall minimum and summing up recursively.
   * (2) For differencing of order n, the first n values in the array that are not missing are set to zero in the packed
   * array. These dummy values are not used in unpacking.
   * (3) See data template 7.3 and associated Notes for complementary information.
   */
  public static class SpatialDifferencing extends ComplexPacking {

    int orderSpatial, descriptorSpatial;

    SpatialDifferencing(RandomAccessFile raf) throws IOException {
      super(raf);
      this.orderSpatial = raf.read();
      this.descriptorSpatial = raf.read();
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("orderSpatial", orderSpatial)
          .add("descriptorSpatial", descriptorSpatial).toString();
    }
  }

  /**
   * Data template 7.40 – Grid point data – JPEG 2000 code stream format
   * Note: For most templates, details of the packing process are described in Regulation 92.9.4.
   * Octet No.
   * 6–nn
   * Contents
   * JPEG 2000 code stream as described in Part 1 of the JPEG 2000 standard
   * (ISO/IEC 15444-1:2000)
   * Note: For simplicity, image data should be
   */
  public static class Jpeg2000 extends SimplePacking {
    int compressionMethod, compressionRatio;
    boolean hasSignedProblem;

    Jpeg2000(RandomAccessFile raf) throws IOException {
      super(raf);
      this.compressionMethod = raf.read();
      this.compressionRatio = raf.read();
    }

    public boolean hasSignedProblem() {
      return hasSignedProblem;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("referenceValue", referenceValue)
          .add("binaryScaleFactor", binaryScaleFactor).add("decimalScaleFactor", decimalScaleFactor)
          .add("numberOfBits", numberOfBits).add("originalType", originalType)
          .add("compressionMethod", compressionMethod).add("compressionRatio", compressionRatio)
          .add("hasSignedProblem", hasSignedProblem).toString();
    }

  }

  // Special ECMWF packing format
  // pull request #52 "lost-carrier" jkaehler@meteomatics.com
  // see https://github.com/erdc-cm/grib_api/blob/master/definitions/grib2/template.5.50002.def
  public static class Ecmwf50002 extends Grib2Drs {
    public float referenceValue;
    public int binaryScaleFactor, decimalScaleFactor, numberOfBits;
    public int p1, p2;
    int widthOfFirstOrderValues, widthOfWidth, widthOfLength;
    int boustrophonic, orderOfSPD, widthOfSPD;
    int[] spd;
    int lengthOfSection6, section6;
    int bitMapIndicator;
    int lengthOfSection7, section7;

    Ecmwf50002(RandomAccessFile raf) throws IOException {
      this.referenceValue = raf.readFloat();
      this.binaryScaleFactor = GribNumbers.int2(raf);
      this.decimalScaleFactor = GribNumbers.int2(raf);
      this.numberOfBits = raf.read();
      this.widthOfFirstOrderValues = raf.read();
      this.p1 = GribNumbers.int4(raf);
      this.p2 = GribNumbers.int4(raf);
      this.widthOfWidth = raf.read();
      this.widthOfLength = raf.read();
      this.boustrophonic = raf.read();
      this.orderOfSPD = raf.read();
      this.widthOfSPD = raf.read();
      this.spd = new int[orderOfSPD + 1];

      BitReader bitReader = new BitReader(raf, raf.getFilePointer());
      for (int i = 0; i < orderOfSPD; i++) {
        this.spd[i] = (int) bitReader.bits2UInt(widthOfSPD);
      }
      this.spd[orderOfSPD] = (int) bitReader.bits2SInt(widthOfSPD);
      this.lengthOfSection6 = GribNumbers.int4(raf);
      this.section6 = raf.read();
      this.bitMapIndicator = raf.read();
      this.lengthOfSection7 = GribNumbers.int4(raf);
      this.section7 = raf.read();
    }

    @Override
    public GribDataUtils.Info getBinaryDataInfo(RandomAccessFile raf) {
      GribDataUtils.Info info = new GribDataUtils.Info();
      info.referenceValue = this.referenceValue;
      info.binaryScaleFactor = this.binaryScaleFactor;
      info.decimalScaleFactor = this.decimalScaleFactor;
      info.numberOfBits = this.numberOfBits;
      // info.originalType = this.originalType; dunno

      return info;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("referenceValue", referenceValue)
          .add("binaryScaleFactor", binaryScaleFactor).add("decimalScaleFactor", decimalScaleFactor)
          .add("numberOfBits", numberOfBits).add("p1", p1).add("p2", p2)
          .add("widthOfFirstOrderValues", widthOfFirstOrderValues).add("widthOfWidth", widthOfWidth)
          .add("widthOfLength", widthOfLength).add("boustrophonic", boustrophonic).add("orderOfSPD", orderOfSPD)
          .add("widthOfSPD", widthOfSPD).add("spd", spd).add("lengthOfSection6", lengthOfSection6)
          .add("section6", section6).add("bitMapIndicator", bitMapIndicator).add("lengthOfSection7", lengthOfSection7)
          .add("section7", section7).toString();
    }
  }

}
