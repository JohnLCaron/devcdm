/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.core.util;

import com.google.common.io.CharStreams;
import dev.ucdm.core.io.RandomAccessFile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/** Input/Output static utilities. */
public class IO {
  public static final int default_file_buffersize = 9200;
  private static final boolean showCopy = false;

  /**
   * Open a resource as a Stream. First try ClassLoader.getResourceAsStream().
   * If that fails, try a plain old FileInputStream().
   * 
   * @param resourcePath name of file path (use forward slashes!)
   * @return InputStream or FileNotFoundException on failure
   */
  public static InputStream getFileResource(String resourcePath) throws FileNotFoundException {
    Class<IO> cl = IO.class;
    InputStream is = cl.getResourceAsStream(resourcePath);
    if (is != null) {
      return is;
    }
    return new FileInputStream(resourcePath);
  }

  /**
   * Copy all bytes from in to out.
   *
   * @param in InputStream
   * @param out OutputStream
   * @return number of bytes copied
   */
  public static long copy(InputStream in, OutputStream out) throws IOException {
    return copyBuffered(in, out, default_file_buffersize);
  }

  /**
   * Copy all bytes from in to out, setting buffer size.
   * 
   * @param in InputStream
   * @param out OutputStream
   * @param buffer_size size of buffer to read through.
   * @return number of bytes copied
   */
  public static long copyBuffered(InputStream in, OutputStream out, int buffer_size) throws IOException {
    long totalBytesRead = 0;
    byte[] buffer = new byte[buffer_size];
    while (true) {
      int bytesRead = in.read(buffer);
      if (bytesRead == -1)
        break;
      out.write(buffer, 0, bytesRead);
      totalBytesRead += bytesRead;
    }
    out.flush();
    return totalBytesRead;
  }


  /**
   * copy all bytes from in and throw them away.
   *
   * @param in InputStream
   * @param buffersize size of buffer to use, if -1 uses default value (9200)
   * @return number of bytes copied
   */
  public static long copy2null(InputStream in, int buffersize) throws IOException {
    long totalBytesRead = 0;
    if (buffersize <= 0)
      buffersize = default_file_buffersize;
    byte[] buffer = new byte[buffersize];
    while (true) {
      int n = in.read(buffer);
      if (n == -1)
        break;
      totalBytesRead += n;
    }
    // if (fout != null) fout.format("done=%d %n",totalBytesRead);
    return totalBytesRead;
  }

  /**
   * copy all bytes from in and throw them away.
   *
   * @param in FileChannel
   * @param buffersize size of buffer to use, if -1 uses default value (9200)
   * @return number of bytes copied
   */
  public static long copy2null(FileChannel in, int buffersize) throws IOException {
    long totalBytesRead = 0;
    if (buffersize <= 0)
      buffersize = default_file_buffersize;
    ByteBuffer buffer = ByteBuffer.allocate(buffersize);
    while (true) {
      int n = in.read(buffer);
      if (n == -1)
        break;
      totalBytesRead += n;
      ((Buffer) buffer).flip();
    }
    return totalBytesRead;
  }

  /**
   * copy all bytes from in to out, specify buffer size
   *
   * @param in InputStream
   * @param out OutputStream
   * @param bufferSize : internal buffer size.
   * @return number of bytes copied
   * @throws IOException on io error
   */
  public static long copyB(InputStream in, OutputStream out, int bufferSize) throws IOException {
    long totalBytesRead = 0;
    int done = 0, next = 1;

    byte[] buffer = new byte[bufferSize];
    while (true) {
      int n = in.read(buffer);
      if (n == -1)
        break;
      out.write(buffer, 0, n);
      totalBytesRead += n;

      if (showCopy) {
        done += n;
        if (done > 1000 * 1000 * next) {
          System.out.println(next + " Mb");
          next++;
        }
      }
    }
    out.flush();
    return totalBytesRead;
  }

  /**
   * Copy up to maxBytes bytes from in to out.
   *
   * @param in InputStream
   * @param out OutputStream
   * @param maxBytes number of bytes to copy
   * @throws IOException on io error
   */
  public static void copyMaxBytes(InputStream in, OutputStream out, int maxBytes) throws IOException {
    byte[] buffer = new byte[default_file_buffersize];
    int count = 0;
    while (true) {
      int bytesRead = in.read(buffer);
      if (bytesRead == -1)
        break;
      int transfer = Math.min(maxBytes - count, bytesRead);
      out.write(buffer, 0, transfer);
      count += transfer;
      if (count >= maxBytes)
        return;
    }
    out.flush();
  }

  /**
   * Read the contents from the inputStream and place into a String,
   * with any error messages put in the return String.
   * Assume UTF-8 encoding.
   *
   * @param is the inputStream to read from.
   * @return String holding the contents, or an error message.
   * @throws IOException on io error
   */
  public static String readContents(InputStream is) throws IOException {
    return readContents(is, StandardCharsets.UTF_8);
  }

  /**
   * Read the contents from the inputStream and place into a String,
   * with any error messages put in the return String.
   *
   * @param is the inputStream to read from.
   * @return String holding the contents, or an error message.
   * @throws IOException on io error
   */
  public static String readContents(InputStream is, Charset charset) throws IOException {
    ByteArrayOutputStream bout = new ByteArrayOutputStream(10 * default_file_buffersize);
    IO.copy(is, bout);
    return bout.toString(charset);
  }

  /**
   * Read the contents from the inputStream and place into a byte array.
   *
   * @param is the inputStream to read from.
   * @return byte[] holding the contents, or an error message.
   * @throws IOException on io error
   */
  public static byte[] readContentsToByteArray(InputStream is) throws IOException {
    ByteArrayOutputStream bout = new ByteArrayOutputStream(10 * default_file_buffersize);
    IO.copy(is, bout);
    return bout.toByteArray();
  }

  /**
   * Wite the contents from the String to a Stream,
   *
   * @param contents String holding the contents.
   * @param os write to this OutputStream
   * @throws IOException on io error
   */
  public static void writeContents(String contents, OutputStream os) throws IOException {
    ByteArrayInputStream bin = new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8));
    IO.copy(bin, os);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Files

  /**
   * copy one file to another.
   *
   * @param fileInName copy from this file, which must exist.
   * @param fileOutName copy to this file, which is overrwritten if already exists.
   * @throws IOException on io error
   */
  public static void copyFile(String fileInName, String fileOutName) throws IOException {
    try (FileInputStream fin = new FileInputStream(fileInName);
        FileOutputStream fout = new FileOutputStream(fileOutName)) {

      InputStream in = new BufferedInputStream(fin);
      OutputStream out = new BufferedOutputStream(fout);
      IO.copy(in, out);
    }
  }

  /**
   * copy one file to another.
   *
   * @param fileIn copy from this file, which must exist.
   * @param fileOut copy to this file, which is overrwritten if already exists.
   * @throws IOException on io error
   */
  public static void copyFile(File fileIn, File fileOut) throws IOException {
    try (FileInputStream fin = new FileInputStream(fileIn); FileOutputStream fout = new FileOutputStream(fileOut)) {
      InputStream in = new BufferedInputStream(fin);
      OutputStream out = new BufferedOutputStream(fout);
      IO.copy(in, out);
    }
  }

  /**
   * copy bytes to File
   *
   * @param src source
   * @param fileOut copy to this file
   * @throws IOException on io error
   */
  public static void copy2File(byte[] src, String fileOut) throws IOException {
    try (FileOutputStream fout = new FileOutputStream(fileOut)) {
      InputStream in = new BufferedInputStream(new ByteArrayInputStream(src));
      OutputStream out = new BufferedOutputStream(fout);
      IO.copy(in, out);
    }
  }

  /**
   * copy file to output stream
   *
   * @param fileInName open this file
   * @param out copy here
   * @throws IOException on io error
   */
  public static void copyFile(String fileInName, OutputStream out) throws IOException {
    copyFileB(new File(fileInName), out, default_file_buffersize);
  }

  /**
   * copy file to output stream, specify internal buffer size
   *
   * @param fileIn copy this file
   * @param out copy to this stream
   * @param bufferSize internal buffer size.
   * @throws IOException on io error
   */
  public static void copyFileB(File fileIn, OutputStream out, int bufferSize) throws IOException {
    try (FileInputStream fin = new FileInputStream(fileIn)) {
      InputStream in = new BufferedInputStream(fin);
      IO.copyB(in, out, bufferSize);
    }
  }

  /**
   * Copy part of a RandomAccessFile to output stream, specify internal buffer size
   *
   * @param raf copy this file
   * @param offset start here (byte offset)
   * @param length number of bytes to copy
   * @param out copy to this stream
   * @param buffer use this buffer.
   * @return number of bytes copied
   * @throws IOException on io error
   */
  public static long copyRafB(RandomAccessFile raf, long offset, long length, OutputStream out,
                              byte[] buffer) throws IOException {
    int bufferSize = buffer.length;
    long want = length;
    raf.seek(offset);
    while (want > 0) {
      int len = (int) Math.min(want, bufferSize);
      int bytesRead = raf.read(buffer, 0, len);
      if (bytesRead <= 0)
        break;
      out.write(buffer, 0, bytesRead);
      want -= bytesRead;
    }
    out.flush();
    return length - want;
  }

  /**
   * Copy an entire directory tree.
   *
   * @param fromDirName from this directory (do nothing if not exist)
   * @param toDirName to this directory (will create if not exist)
   * @throws IOException on io error
   */
  public static void copyDirTree(String fromDirName, String toDirName) throws IOException {
    File fromDir = new File(fromDirName);
    File toDir = new File(toDirName);

    if (!fromDir.exists())
      return;

    if (!toDir.exists()) {
      if (!toDir.mkdirs()) {
        throw new IOException("Could not create directory: " + toDir);
      }
    }

    File[] files = fromDir.listFiles();
    if (files != null)
      for (File f : files) {
        if (f.isDirectory())
          copyDirTree(f.getAbsolutePath(), toDir.getAbsolutePath() + "/" + f.getName());
        else
          copyFile(f.getAbsolutePath(), toDir.getAbsolutePath() + "/" + f.getName());
      }
  }

  /**
   * Read the file and place contents into a byte array,
   * with any error messages put in the return String.
   *
   * @param filename the file to read from.
   * @return byte[] holding the contents, or an error message.
   * @throws IOException on io error
   */
  public static byte[] readFileToByteArray(String filename) throws IOException {
    try (FileInputStream fin = new FileInputStream(filename)) {
      InputStream in = new BufferedInputStream(fin);
      return readContentsToByteArray(in);
    }
  }

  /**
   * Read the contents from the named file and place into a String, assuming UTF-8 encoding.
   *
   * @param filename the URL to read from.
   * @return String holding the file contents
   * @throws IOException on io error
   */
  public static String readFile(String filename) throws IOException {
    try (FileInputStream fin = new FileInputStream(filename);
        InputStreamReader stream = new InputStreamReader(fin, StandardCharsets.UTF_8)) {
      return CharStreams.toString(stream);
    }
  }

  /**
   * Write String contents to a file, using UTF-8 encoding.
   *
   * @param contents String holding the contents
   * @param file write to this file (overwrite if exists)
   * @throws IOException on io error
   */
  public static void writeToFile(String contents, File file) throws IOException {
    try (FileOutputStream fout = new FileOutputStream(file);
        OutputStreamWriter fw = new OutputStreamWriter(fout, StandardCharsets.UTF_8)) {
      fw.write(contents);
    }
  }

  /**
   * Write byte[] contents to a file.
   *
   * @param contents String holding the contents
   * @param file write to this file (overwrite if exists)
   * @throws IOException on io error
   */
  public static void writeToFile(byte[] contents, File file) throws IOException {
    try (FileOutputStream fw = new FileOutputStream(file)) {
      fw.write(contents);
      fw.flush();
    }
  }

  /**
   * Write contents to a file, using UTF-8 encoding.
   *
   * @param contents String holding the contents
   * @param fileOutName write to this file (overwrite if exists)
   * @throws IOException on io error
   */
  public static void writeToFile(String contents, String fileOutName) throws IOException {
    writeToFile(contents, new File(fileOutName));
  }

  /**
   * copy input stream to file. close input stream when done.
   *
   * @param in copy from here
   * @param fileOutName open this file (overwrite) and copy to it.
   * @return number of bytes copied
   * @throws IOException on io error
   */
  public static long writeToFile(InputStream in, String fileOutName) throws IOException {
    try (FileOutputStream fout = new FileOutputStream(fileOutName)) {
      OutputStream out = new BufferedOutputStream(fout);
      return IO.copy(in, out);
    } finally {
      if (null != in)
        in.close();
    }
  }

  public static long appendToFile(InputStream in, String fileOutName) throws IOException {
    try (FileOutputStream fout = new FileOutputStream(fileOutName, true)) {
      OutputStream out = new BufferedOutputStream(fout);
      return IO.copy(in, out);
    }
  }

}