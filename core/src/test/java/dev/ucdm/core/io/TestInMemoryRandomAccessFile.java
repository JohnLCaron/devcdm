package dev.ucdm.core.io;

import dev.ucdm.core.util.IO;
import dev.ucdm.core.util.KMPMatch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;

import static com.google.common.truth.Truth.assertThat;
import static dev.ucdm.core.io.TestRandomAccessFile.DATA_AS_BE_DOUBLES;
import static dev.ucdm.core.io.TestRandomAccessFile.DATA_AS_BE_FLOATS;
import static dev.ucdm.core.io.TestRandomAccessFile.DATA_AS_BE_INTS;
import static dev.ucdm.core.io.TestRandomAccessFile.DATA_AS_BE_LONGS;
import static dev.ucdm.core.io.TestRandomAccessFile.DATA_AS_BE_SHORTS;
import static dev.ucdm.core.io.TestRandomAccessFile.DATA_AS_LE_DOUBLES;
import static dev.ucdm.core.io.TestRandomAccessFile.DATA_AS_LE_FLOATS;
import static dev.ucdm.core.io.TestRandomAccessFile.DATA_AS_LE_INTS;
import static dev.ucdm.core.io.TestRandomAccessFile.DATA_AS_LE_LONGS;
import static dev.ucdm.core.io.TestRandomAccessFile.DATA_AS_LE_SHORTS;
import static org.junit.Assert.fail;

/** Test {@link InMemoryRandomAccessFile} */
public class TestInMemoryRandomAccessFile {
  // Test public methods of RandomAccessFile
  // NOTE: Does not test cache methods

  // use small buffer size for test cases
  private static final int TEST_BUFFER_SIZE = 10;

  // test file
  private static InMemoryRandomAccessFile testFile;
  private static RandomAccessFile rafFile;
  private static final String TEST_FILE_PATH = "src/test/data/preserveLineEndings/testUTF8.txt";
  private static final String TEST_FILE_NAME = "InMemory";

  // contents of test file
  private static final String TEST_FILE_STRING = "Hello world, this is a test.\r\nThis is a second line of text.";
  private static final byte[] UTF8_BYTES = TEST_FILE_STRING.getBytes(StandardCharsets.UTF_8);
  private static final int TEST_FILE_LENGTH = UTF8_BYTES.length;

  @BeforeEach
  public void setUpTests() throws IOException {
    rafFile = new RandomAccessFile(TEST_FILE_PATH, "r", TEST_BUFFER_SIZE);

    byte[] contents = IO.readFileToByteArray(TEST_FILE_PATH);
    testFile = new InMemoryRandomAccessFile(TEST_FILE_NAME, contents);
  }

  @AfterEach
  public void cleanUpTest() throws IOException {
    rafFile.close();
    testFile.close();
  }

  ////////////////////
  // test reads for persistent test files
  // no changes made to file

  @Test
  public void testSetBufferSize() {
    int expected = 100;
    int actual = testFile.getBufferSize();
    testFile.setBufferSize(expected); // no effect.
    assertThat(testFile.getBufferSize()).isEqualTo(actual);
  }

  @Test
  public void testIsAtEndOfFile() throws IOException {
    testFile.seek(TEST_FILE_LENGTH);
    assertThat(testFile.isAtEndOfFile()).isTrue();
    testFile.seek(0);
    assertThat(testFile.isAtEndOfFile()).isFalse();
  }

  @Test
  public void testSeek() throws IOException {
    long pos = 5;
    testFile.seek(pos);
    assertThat(testFile.getFilePointer()).isEqualTo(pos);
    pos = 15;
    testFile.seek(pos);
    assertThat(testFile.getFilePointer()).isEqualTo(pos);
  }

  @Test
  public void testOrder() throws IOException {
    testFile.order(ByteOrder.LITTLE_ENDIAN);
    testFile.seek(0);
    assertThat(testFile.readInt()).isNotEqualTo(DATA_AS_BE_INTS[0]);
    testFile.order(ByteOrder.BIG_ENDIAN);
    assertThat(testFile.readInt()).isEqualTo(DATA_AS_BE_INTS[1]);
  }

  @Test
  public void testGetLocation() {
    assertThat(testFile.getLocation()).isEqualTo(TEST_FILE_NAME);
  }

  @Test
  public void testLength() {
    assertThat(testFile.length()).isEqualTo(TEST_FILE_LENGTH);
  }

  @Test
  public void testRead() throws IOException {
    int pos = 0;
    testFile.seek(pos);
    assertThat(testFile.read()).isEqualTo((int) UTF8_BYTES[pos]);
    pos = 15;
    testFile.seek(pos);
    assertThat(testFile.read()).isEqualTo((int) UTF8_BYTES[pos]);
    testFile.seek(TEST_FILE_LENGTH);
    assertThat(testFile.read()).isEqualTo(-1);
  }

  @Test
  public void testReadBytes() throws IOException {
    int offset = 0;
    byte[] buff;
    int len;
    int n;

    // read byte
    testFile.seek(0);
    assertThat(testFile.readByte()).isEqualTo(UTF8_BYTES[0]);
    // read unsigned byte
    assertThat(testFile.readUnsignedByte()).isEqualTo(UTF8_BYTES[1]);

    // read across buffer
    testFile.seek(0);
    len = TEST_BUFFER_SIZE + 1;
    buff = new byte[len];
    n = testFile.readBytes(buff, offset, len);
    assertThat(n).isEqualTo(len);
    assertThat(arraysMatch(buff, UTF8_BYTES, 0, 0, len)).isTrue();

    // read with offset
    testFile.seek(0);
    offset = 2;
    len = len - offset;
    n = testFile.readBytes(buff, offset, len);
    assertThat(n).isEqualTo(len);
    assertThat(buff[0]).isEqualTo(UTF8_BYTES[0]);
    assertThat(arraysMatch(buff, UTF8_BYTES, offset, 0, len)).isTrue();

    // read directly from file (more than an extra buffer length)
    testFile.seek(0);
    offset = 0;
    len = (TEST_BUFFER_SIZE * 2) + 1;
    buff = new byte[len];
    n = testFile.readBytes(buff, offset, len);
    assertThat(n).isEqualTo(len);
    assertThat(arraysMatch(buff, UTF8_BYTES, 0, 0, len)).isTrue();

    // read over end of file
    len = 2;
    testFile.seek(TEST_FILE_LENGTH - 1);
    buff = new byte[len];
    n = testFile.readBytes(buff, offset, len);
    assertThat(n).isLessThan(len);

    // read at end of file
    testFile.seek(TEST_FILE_LENGTH);
    n = testFile.readBytes(buff, offset, len);
    assertThat(n).isEqualTo(-1);
  }

  @Test
  public void testReadFully() throws IOException {
    // read fully, buff < file length
    testFile.seek(0);
    int len = 11;
    byte[] buff = new byte[len];
    testFile.readFully(buff);
    assertThat(arraysMatch(buff, UTF8_BYTES, 0, 0, len)).isTrue();

    // read fully, buff > file length
    testFile.seek(0);
    len = TEST_FILE_LENGTH + 1;
    byte[] finalBuff = new byte[len];
    try {
      testFile.readFully(finalBuff);
      fail();
    } catch (Exception ioe) {
      // expected.
    }

    // read fully with offset
    testFile.seek(0);
    int offset = 5;
    len = 11 - offset;
    testFile.readFully(buff, offset, len);
    assertThat(arraysMatch(buff, UTF8_BYTES, 0, 0, offset)).isTrue();
    assertThat(arraysMatch(buff, UTF8_BYTES, offset, 0, len)).isTrue();
  }

  @Test
  public void testSkipBytes() throws IOException {
    testFile.seek(0);
    int skip = 5;
    testFile.skipBytes(skip);
    assertThat(testFile.getFilePointer()).isEqualTo(skip);
    int val = testFile.read();
    assertThat(val).isEqualTo((int) UTF8_BYTES[skip]);
  }

  @Test
  public void testUnread() throws IOException {
    testFile.seek(0);
    int a = testFile.read();
    assertThat(testFile.getFilePointer()).isEqualTo(1);
    testFile.unread();
    assertThat(testFile.getFilePointer()).isEqualTo(0);
    int b = testFile.read();
    assertThat(b).isEqualTo(a);
  }

  @Test
  public void testReadLittleEndian() throws IOException {
    // set byte order
    testFile.order(ByteOrder.LITTLE_ENDIAN);

    // read boolean
    testFile.seek(0);
    assertThat(testFile.readBoolean()).isTrue();

    // read short
    testFile.seek(0);
    assertThat(testFile.readShort()).isEqualTo(DATA_AS_LE_SHORTS[0]);
    // read short array
    short[] outShort = new short[2];
    testFile.readShort(outShort, 0, 2);
    assertThat(outShort[0]).isEqualTo(DATA_AS_LE_SHORTS[1]);
    assertThat(outShort[1]).isEqualTo(DATA_AS_LE_SHORTS[2]);
    // read unsigned short
    testFile.seek(0);
    assertThat(testFile.readUnsignedShort()).isEqualTo(DATA_AS_LE_SHORTS[0]);

    // read char
    assertThat(testFile.readChar()).isEqualTo((char) DATA_AS_LE_SHORTS[1]);

    // read int
    testFile.seek(0);
    assertThat(testFile.readInt()).isEqualTo(DATA_AS_LE_INTS[0]);
    // read int array
    int[] outInt = new int[2];
    testFile.readInt(outInt, 0, 2);
    assertThat(outInt[0]).isEqualTo(DATA_AS_LE_INTS[1]);
    assertThat(outInt[1]).isEqualTo(DATA_AS_LE_INTS[2]);
    // read int unbuffered
    assertThat(testFile.readIntUnbuffered(4)).isEqualTo(DATA_AS_LE_INTS[1]);

    // read long
    testFile.seek(0);
    assertThat(testFile.readLong()).isEqualTo(DATA_AS_LE_LONGS[0]);
    // read long array
    long[] outLong = new long[2];
    testFile.readLong(outLong, 0, 2);
    assertThat(outLong[0]).isEqualTo(DATA_AS_LE_LONGS[1]);
    assertThat(outLong[1]).isEqualTo(DATA_AS_LE_LONGS[2]);

    // read float
    testFile.seek(0);
    assertThat(compareFloats(testFile.readFloat(), DATA_AS_LE_FLOATS[0])).isTrue();
    // read float array
    float[] outFloat = new float[2];
    testFile.readFloat(outFloat, 0, 2);
    assertThat(compareFloats(outFloat[0], DATA_AS_LE_FLOATS[1])).isTrue();
    assertThat(compareFloats(outFloat[1], DATA_AS_LE_FLOATS[2])).isTrue();

    // read double
    testFile.seek(0);
    assertThat(compareDoubles(testFile.readDouble(), DATA_AS_LE_DOUBLES[0])).isTrue();
    // read double array
    double[] outDouble = new double[2];
    testFile.readDouble(outDouble, 0, 2);
    assertThat(compareDoubles(outDouble[0], DATA_AS_LE_DOUBLES[1])).isTrue();
    assertThat(compareDoubles(outDouble[1], DATA_AS_LE_DOUBLES[2])).isTrue();
  }

  @Test
  public void testReadBigEndian() throws IOException {
    // set byte order
    testFile.order(ByteOrder.BIG_ENDIAN);

    // read boolean
    testFile.seek(0);
    assertThat(testFile.readBoolean()).isTrue();

    // read short
    testFile.seek(0);
    assertThat(testFile.readShort()).isEqualTo(DATA_AS_BE_SHORTS[0]);
    // read short array
    short[] outShort = new short[2];
    testFile.readShort(outShort, 0, 2);
    assertThat(outShort[0]).isEqualTo(DATA_AS_BE_SHORTS[1]);
    assertThat(outShort[1]).isEqualTo(DATA_AS_BE_SHORTS[2]);
    // read unsigned short
    testFile.seek(0);
    assertThat(testFile.readUnsignedShort()).isEqualTo(DATA_AS_BE_SHORTS[0]);

    // read char
    assertThat(testFile.readChar()).isEqualTo((char) DATA_AS_BE_SHORTS[1]);

    // read int
    testFile.seek(0);
    assertThat(testFile.readInt()).isEqualTo(DATA_AS_BE_INTS[0]);
    // read int array
    int[] outInt = new int[2];
    testFile.readInt(outInt, 0, 2);
    assertThat(outInt[0]).isEqualTo(DATA_AS_BE_INTS[1]);
    assertThat(outInt[1]).isEqualTo(DATA_AS_BE_INTS[2]);
    // read int unbuffered
    assertThat(testFile.readIntUnbuffered(4)).isEqualTo(DATA_AS_BE_INTS[1]);

    // read long
    testFile.seek(0);
    assertThat(testFile.readLong()).isEqualTo(DATA_AS_BE_LONGS[0]);
    // read long array
    long[] outLong = new long[2];
    testFile.readLong(outLong, 0, 2);
    assertThat(outLong[0]).isEqualTo(DATA_AS_BE_LONGS[1]);
    assertThat(outLong[1]).isEqualTo(DATA_AS_BE_LONGS[2]);

    // read float
    testFile.seek(0);
    assertThat(compareFloats(testFile.readFloat(), DATA_AS_BE_FLOATS[0])).isTrue();
    // read float array
    float[] outFloat = new float[2];
    testFile.readFloat(outFloat, 0, 2);
    assertThat(compareFloats(outFloat[0], DATA_AS_BE_FLOATS[1])).isTrue();
    assertThat(compareFloats(outFloat[1], DATA_AS_BE_FLOATS[2])).isTrue();

    // read double
    testFile.seek(0);
    assertThat(compareDoubles(testFile.readDouble(), DATA_AS_BE_DOUBLES[0])).isTrue();
    // read double array
    double[] outDouble = new double[2];
    testFile.readDouble(outDouble, 0, 2);
    assertThat(compareDoubles(outDouble[0], DATA_AS_BE_DOUBLES[1])).isTrue();
    assertThat(compareDoubles(outDouble[1], DATA_AS_BE_DOUBLES[2])).isTrue();
  }

  @Test
  public void testReadStringUTF8() throws IOException {
    // read line
    testFile.seek(0);
    int linebreak = TEST_FILE_STRING.indexOf("\r\n");
    assertThat(testFile.readLine()).isEqualTo(TEST_FILE_STRING.substring(0, linebreak));
    assertThat(testFile.readLine()).isEqualTo(TEST_FILE_STRING.substring(linebreak + 2));

    // read string
    int nbytes = 11;
    testFile.seek(0);
    assertThat(testFile.readString(nbytes)).isEqualTo(TEST_FILE_STRING.substring(0, nbytes));

    // read string max
    testFile.seek(0);
    assertThat(testFile.readStringMax(TEST_FILE_LENGTH)).isEqualTo(TEST_FILE_STRING);
  }

  @Test
  public void testToString() {
    assertThat(testFile.toString()).isEqualTo(TEST_FILE_NAME);
  }

  @Test
  public void testSearchForward() throws IOException {
    testFile.seek(0);
    // test match found
    KMPMatch match = new KMPMatch("world".getBytes(StandardCharsets.UTF_8));
    assertThat(testFile.searchForward(match, -1)).isTrue();

    // test match not reached
    testFile.seek(0);
    assertThat(testFile.searchForward(match, 5)).isFalse();

    // test match not found
    KMPMatch notMatch = new KMPMatch("not match".getBytes(StandardCharsets.UTF_8));
    assertThat(testFile.searchForward(notMatch, -1)).isFalse();
  }

  private boolean arraysMatch(byte[] arr1, byte[] arr2, int start1, int start2, int n) {
    if ((start1 + n) > arr1.length || (start2 + n) > arr2.length) {
      return false;
    }

    for (int i = 0; i < n; i++) {
      if (arr1[start1 + i] != arr2[start2 + i]) {
        return false;
      }
    }
    return true;
  }

  private boolean compareFloats(float f1, float f2) {
    return Math.abs(f1 - f2) < (f1 / Math.pow(10, 7));
  }

  private boolean compareDoubles(double d1, double d2) {
    double dif = Math.abs(d1 - d2);
    double threshold = (d1 / Math.pow(10, 16));
    return Math.abs(d1 - d2) < (d1 / Math.pow(10, 15));
  }

  @Test
  public void testReadToByteChannel() throws IOException {
    TestWritableByteChannel dest;
    byte[] out;
    long n;
    int nbytes = 10;
    int offset = 0;

    // test read
    dest = new TestWritableByteChannel();
    n = testFile.readToByteChannel(dest, offset, nbytes);
    assertThat(n).isEqualTo(nbytes);
    out = dest.getBytes();
    // spot check first and last byte
    assertThat(arraysMatch(out, UTF8_BYTES, 0, 0, (int) n)).isTrue();
    dest.reset();

    // test read with offset
    offset = 10;
    n = testFile.readToByteChannel(dest, offset, nbytes);
    assertThat(n).isEqualTo(nbytes);
    out = dest.getBytes();
    assertThat(arraysMatch(out, UTF8_BYTES, 0, offset, (int) n)).isTrue();
    dest.reset();

    // test read past EOF
    offset = TEST_FILE_LENGTH - nbytes + 1;
    n = testFile.readToByteChannel(dest, offset, nbytes);
    assertThat(n).isLessThan(nbytes);
  }

  private class TestWritableByteChannel implements WritableByteChannel {

    private boolean open;
    private ByteArrayOutputStream dest;

    public TestWritableByteChannel() {
      open = true;
      dest = new ByteArrayOutputStream();
    }

    @Override
    public int write(ByteBuffer src) {
      byte[] out = src.array();
      int start = src.position();
      int length = Math.min(src.limit() - src.position(), src.capacity());
      dest.write(out, start, length);
      return length;
    }

    @Override
    public boolean isOpen() {
      return open;
    }

    @Override
    public void close() throws IOException {
      open = false;
    }

    public byte[] getBytes() {
      return dest.toByteArray();
    }

    public void reset() throws IOException {
      dest.reset();
    }
  }
}
