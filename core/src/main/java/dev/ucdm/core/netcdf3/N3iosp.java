/* Copyright Unidata */
package dev.ucdm.core.netcdf3;

import com.google.common.base.Preconditions;
import dev.ucdm.array.*;
import dev.ucdm.core.api.Group;
import dev.ucdm.core.api.CdmFile;
import dev.ucdm.core.api.Structure;
import dev.ucdm.core.api.Variable;
import dev.ucdm.core.constants.DataFormatType;
import dev.ucdm.core.io.RandomAccessFile;
import dev.ucdm.core.iosp.AbstractIOServiceProvider;
import dev.ucdm.core.iosp.IOServiceProvider;
import dev.ucdm.core.iosp.IospArrayHelper;
import dev.ucdm.core.iosp.Layout;
import dev.ucdm.core.iosp.LayoutRegular;
import dev.ucdm.core.iosp.LayoutRegularSegmented;
import dev.ucdm.core.util.CancelTask;

import dev.ucdm.core.write.NetcdfFileFormat;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Formatter;
import java.util.Optional;

import static dev.ucdm.core.api.CdmFile.IOSP_MESSAGE_GET_NETCDF_FILE_FORMAT;

/** Netcdf 3 version iosp, using Builders for immutability. */
public class N3iosp extends AbstractIOServiceProvider implements IOServiceProvider {
  protected static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(N3iosp.class);

  /*
   * CLASSIC
   * The maximum size of a record in the classic format in versions 3.5.1 and earlier is 2^32 - 4 bytes.
   * In versions 3.6.0 and later, there is no such restriction on total record size for the classic format
   * or 64-bit offset format.
   *
   * If you don't use the unlimited dimension, only one variable can exceed 2 GiB in size, but it can be as
   * large as the underlying file system permits. It must be the last variable in the dataset, and the offset
   * to the beginning of this variable must be less than about 2 GiB.
   *
   * The limit is really 2^31 - 4. If you were to specify a variable size of 2^31 -3, for example, it would be
   * rounded up to the nearest multiple of 4 bytes, which would be 2^31, which is larger than the largest
   * signed integer, 2^31 - 1.
   *
   * If you use the unlimited dimension, record variables may exceed 2 GiB in size, as long as the offset of the
   * start of each record variable within a record is less than 2 GiB - 4.
   */

  /*
   * LARGE FILE
   * Assuming an operating system with Large File Support, the following restrictions apply to the netCDF 64-bit offset
   * format.
   *
   * No fixed-size variable can require more than 2^32 - 4 bytes of storage for its data, unless it is the last
   * fixed-size variable and there are no record variables. When there are no record variables, the last
   * fixed-size variable can be any size supported by the file system, e.g. terabytes.
   *
   * A 64-bit offset format netCDF file can have up to 2^32 - 1 fixed sized variables, each under 4GiB in size.
   * If there are no record variables in the file the last fixed variable can be any size.
   *
   * No record variable can require more than 2^32 - 4 bytes of storage for each record's worth of data,
   * unless it is the last record variable. A 64-bit offset format netCDF file can have up to 2^32 - 1 records,
   * of up to 2^32 - 1 variables, as long as the size of one record's data for each record variable except the
   * last is less than 4 GiB - 4.
   *
   * Note also that all netCDF variables and records are padded to 4 byte boundaries.
   */

  protected N3header header;
  protected long lastModified; // used by sync
  private Charset valueCharset;

  @Override
  public boolean isValidFile(RandomAccessFile raf) throws IOException {
    return N3header.isValidFile(raf);
  }

  @Override
  public String getDetailInfo() {
    Formatter f = new Formatter();
    f.format("%s", super.getDetailInfo());

    try {
      header.showDetail(f);
    } catch (IOException e) {
      return e.getMessage();
    }

    return f.toString();

  }

  // properties
  boolean useRecordStructure;

  //////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public void close() throws IOException {
    if (raf != null) {
      if (header != null) {
        long size = header.calcFileSize();
        raf.setMinLength(size);
      }
      raf.close();
    }
    raf = null;
  }

  @Override
  public Object sendIospMessage(Object message) {
    if (message instanceof Charset) {
      setValueCharset((Charset) message);
      return Boolean.TRUE;
    }
    if (message == CdmFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE) {
      // TODO does this work? must be sent before construction????
      this.useRecordStructure = true;
      return Boolean.TRUE;
    }
    if (message.toString().equals(CdmFile.IOSP_MESSAGE_GET_HEADER)) {
      return header;
    }
    if (message.equals(IOSP_MESSAGE_GET_NETCDF_FILE_FORMAT)) {
      return header.useLongOffset ? NetcdfFileFormat.NETCDF3_64BIT_OFFSET : NetcdfFileFormat.NETCDF3;
    }
    return super.sendIospMessage(message);
  }

  /**
   * Return {@link Charset value charset} if it was defined. Definition of charset
   * occurs by sending a charset as a message using the {@link #sendIospMessage}
   * method.
   *
   * @return {@link Charset value charset} if it was defined.
   */
  Optional<Charset> getValueCharset() {
    return Optional.ofNullable(valueCharset);
  }

  /**
   * Define {@link Charset value charset}.
   *
   * @param charset may be null.
   */
  private void setValueCharset(@Nullable Charset charset) {
    this.valueCharset = charset;
  }

  @Override
  public String getCdmFileTypeId() {
    return DataFormatType.NETCDF.getDescription();
  }

  @Override
  public String getCdmFileTypeDescription() {
    return "NetCDF-3/CDM";
  }

  @Override
  public String getCdmFileTypeVersion() {
    return header.useLongOffset ? "2" : "1"; // classic vs 64-bit Offset Format Variant
  }

  //////////////////////////////////////////////////////////////////////////////////////
  // open existing file

  @Override
  public void build(RandomAccessFile raf, Group.Builder rootGroup, CancelTask cancelTask) throws IOException {
    setRaf(raf);

    String location = raf.getLocation();
    if (!location.startsWith("http:")) {
      File file = new File(location);
      if (file.exists())
        lastModified = file.lastModified();
    }

    raf.order(RandomAccessFile.BIG_ENDIAN);
    header = createHeader();
    header.read(raf, rootGroup, null);
  }

  /** Create header for reading netcdf file. */
  private N3header createHeader() {
    return new N3header(this);
  }

  /////////////////////////////////////////////////////////////////////////////
  // data reading

  @Override
  public Array<?> readArrayData(Variable v2, Section section)
      throws IOException, InvalidRangeException {
    if (v2 instanceof Structure) {
      return readStructureDataArray((Structure) v2, section);
    }

    Object data = readDataObject(v2, section);
    return Arrays.factory(v2.getArrayType(), section.getShape(), data);
  }

  /** Read data subset from file for a variable, create primitive array. */
  private Object readDataObject(Variable v2, Section section) throws IOException, InvalidRangeException {
    N3header.Vinfo vinfo = (N3header.Vinfo) v2.getSPobject();
    ArrayType dataType = v2.getArrayType();

    Layout layout = (!v2.isUnlimited()) ? new LayoutRegular(vinfo.begin, v2.getElementSize(), v2.getShape(), section)
        : new LayoutRegularSegmented(vinfo.begin, v2.getElementSize(), header.recsize, v2.getShape(), section);
    return IospArrayHelper.readDataFill(raf, layout, dataType, null, null);
  }

  /**
   * Read data from record structure. For N3, this is the only possible structure, and there can be no nesting.
   * Read all variables for each record, put in ByteBuffer.
   *
   * @param s the record structure
   * @param section the record range to read
   * @return an ArrayStructure, with all the data read in.
   * @throws IOException on error
   */
  private Array<StructureData> readStructureDataArray(Structure s, Section section)
      throws IOException {
    // has to be 1D
    Preconditions.checkArgument(section.getRank() == 1);
    Range recordRange = section.getRange(0);

    // create the StructureMembers
    StructureMembers.Builder membersb = s.makeStructureMembersBuilder();
    for (StructureMembers.MemberBuilder m : membersb.getStructureMembers()) {
      Variable v2 = s.findVariable(m.getName());
      N3header.Vinfo vinfo = (N3header.Vinfo) v2.getSPobject();
      m.setOffset((int) (vinfo.begin - header.recStart));
    }

    // protect against too large of reads
    if (header.recsize > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Cant read records when recsize > " + Integer.MAX_VALUE);
    }
    long nrecs = section.computeSize();
    if (nrecs * header.recsize > Integer.MAX_VALUE) {
      throw new IllegalArgumentException(
          "Too large read: nrecs * recsize= " + (nrecs * header.recsize) + " bytes exceeds " + Integer.MAX_VALUE);
    }
    membersb.setStructureSize((int) header.recsize);

    byte[] result = new byte[(int) (nrecs * header.recsize)];
    int rcount = 0;
    // loop over records
    for (int recnum : recordRange) {
      raf.seek(header.recStart + recnum * header.recsize); // where the record starts

      if (recnum != header.numrecs - 1) {
        raf.readFully(result, (int) (rcount * header.recsize), (int) header.recsize);
      } else {
        // "wart" allows file to be one byte short. since its always padding, we allow
        raf.read(result, (int) (rcount * header.recsize), (int) header.recsize);
      }
      rcount++;
    }

    StructureMembers members = membersb.build();
    Storage<StructureData> storage =
        new StructureDataStorageBB(members, ByteBuffer.wrap(result), (int) section.computeSize());
    return new StructureDataArray(members, section.getShape(), storage);
  }
}
