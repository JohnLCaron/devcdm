/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.core.api;

import com.google.common.base.Preconditions;
import dev.ucdm.core.io.InMemoryRandomAccessFile;
import dev.ucdm.core.io.RandomAccessFile;
import dev.ucdm.core.io.Uncompress;
import dev.ucdm.core.iosp.IOServiceProvider;
import dev.ucdm.core.util.CancelTask;
import dev.ucdm.core.util.DiskCache;
import dev.ucdm.core.util.EscapeStrings;
import dev.ucdm.core.io.RandomAccessFileProvider;
import dev.ucdm.core.util.IO;
import dev.ucdm.core.util.StringLocker;
import dev.ucdm.core.util.StringUtil2;
import dev.ucdm.core.util.URLnaming;

import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Static helper methods for CdmFile objects.
 * These use builders and new versions of Iosp's when available.
 *
 * @author caron
 * @since 10/3/2019.
 */
public class CdmFiles {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CdmFile.class);
  private static final List<IOServiceProvider> registeredProviders = new ArrayList<>();
  private static final int default_buffersize = 8092;
  private static final StringLocker stringLocker = new StringLocker();
  private static final List<String> possibleCompressedSuffixes = Arrays.asList("Z", "zip", "gzip", "gz", "bz2");
  private static final boolean loadWarnings = false;
  private static final boolean userLoadsFirst;

  // load core service providers
  static {
    // Most IOSPs are loaded using the ServiceLoader mechanism. One problem with this is that we no longer
    // control the order which IOSPs try to open. So its harder to avoid mis-behaving and slow IOSPs from
    // making open() slow. So we load the core ones here to make sure they are tried first.
    try {
      registerIOProvider(dev.ucdm.core.netcdf3.N3iosp.class);
    } catch (Throwable e) {
      if (loadWarnings)
        log.info("Cant load class H4iosp", e);
    }
    try {
      registerIOProvider(dev.ucdm.core.hdf5.H5iosp.class);
    } catch (Throwable e) {
      if (loadWarnings)
        log.info("Cant load class H5iosp", e);
    }

    // if a user explicitly registers an IOSP or RandomAccessFile implementation via
    // registerIOProvider or registerRandomAccessFileProvider, this ensures they are tried first,
    // even before the core implementations.
    // if false, user registered implementations will be tried after the core implementations.
    // Implementations loaded by the ServiceLoader mechanism are only tried after core and explicitly
    // loaded implementations are tried.
    userLoadsFirst = true;
  }

  private CdmFiles() {
  }

  //////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Register an IOServiceProvider. A new instance will be created when one of its files is opened.
   *
   * @param iospClass Class that implements IOServiceProvider.
   */
  public static void registerIOProvider(Class<?> iospClass)
          throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
    registerIOProvider(iospClass, false);
  }

  /**
   * Register an IOServiceProvider. A new instance will be created when one of its files is opened.
   *
   * @param iospClass Class that implements IOServiceProvider.
   * @param last      true=>insert at the end of the list; otherwise front
   */
  private static void registerIOProvider(Class<?> iospClass, boolean last)
          throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
    IOServiceProvider spi;
    spi = (IOServiceProvider) iospClass.getDeclaredConstructor().newInstance(); // fail fast
    if (userLoadsFirst && !last) {
      registeredProviders.add(0, spi); // put user stuff first
    } else {
      registeredProviders.add(spi);
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Open an existing netcdf file (read only).
   *
   * @param location location of file.
   * @return the CdmFile.
   * @throws IOException if error
   */
  public static CdmFile open(String location) throws IOException {
    return open(location, -1, null);
  }

  /**
   * Open an existing file (read only), with option of cancelling.
   *
   * @param location   location of the file.
   * @param cancelTask allow task to be cancelled; may be null.
   * @return CdmFile object, or null if cant find IOServiceProver
   * @throws IOException if error
   */
  public static CdmFile open(String location, CancelTask cancelTask) throws IOException {
    return open(location, -1, cancelTask);
  }

  /**
   * Open an existing file (read only), with option of cancelling, setting the RandomAccessFile buffer size for
   * efficiency.
   *
   * @param location    location of file.
   * @param buffer_size RandomAccessFile buffer size, if &le; 0, use default size
   * @param cancelTask  allow task to be cancelled; may be null.
   * @return CdmFile object, or null if cant find IOServiceProver
   * @throws IOException if error
   */
  public static CdmFile open(String location, int buffer_size, CancelTask cancelTask)
          throws IOException {
    return open(location, buffer_size, cancelTask, null);
  }

  /**
   * Open an existing file (read only), with option of cancelling, setting the RandomAccessFile buffer size for
   * efficiency, with an optional special object for the iosp.
   *
   * @param location    location of file. This may be a
   * <ol>
   * <li>local netcdf-3 filename (with a file: prefix or no prefix)
   * <li>remote netcdf-3 filename (with an http: prefix)
   * <li>local netcdf-4 filename (with a file: prefix or no prefix)
   * <li>local hdf-5 filename (with a file: prefix or no prefix)
   * <li>local iosp filename (with a file: prefix or no prefix)
   * </ol>
   * If file ends with ".Z", ".zip", ".gzip", ".gz", or ".bz2", it will uncompress/unzip and write to new file without the suffix,
   * then use the uncompressed file. It will look for the uncompressed file before it does any of that. Generally it prefers to
   * place the uncompressed file in the same directory as the original file. If it does not have write permission
   * on that directory, it will use the directory defined by the DiskCache class.
   *
   * @param buffer_size RandomAccessFile buffer size, if &le; 0, use default size
   * @param cancelTask  allow task to be cancelled; may be null.
   * @param iospMessage special iosp tweaking (sent before open is called), may be null
   * @return CdmFile object, or null if cant find IOServiceProver
   * @throws IOException if error
   */
  public static CdmFile open(String location, int buffer_size, CancelTask cancelTask,
                             Object iospMessage) throws IOException {

    RandomAccessFile raf = getRaf(location, buffer_size);
    try {
      return open(raf, location, cancelTask, iospMessage);
    } catch (Throwable t) {
      raf.close();
      t.printStackTrace();
      throw new IOException(t);
    }
  }

  /**
   * Open an existing file (read only), specifying which IOSP is to be used.
   *
   * @param location      location of file
   * @param iospClassName fully qualified class name of the IOSP class to handle this file
   * @param bufferSize    RandomAccessFile buffer size, if &le; 0, use default size
   * @param cancelTask    allow task to be cancelled; may be null.
   * @param iospMessage   special iosp tweaking (sent before open is called), may be null
   * @return CdmFile object, or null if cant find IOServiceProver
   */
  public static CdmFile open(String location, String iospClassName, int bufferSize, CancelTask cancelTask,
                             Object iospMessage) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
          InstantiationException, IllegalAccessException, IOException {

    Class<?> iospClass = CdmFile.class.getClassLoader().loadClass(iospClassName);
    IOServiceProvider spi = (IOServiceProvider) iospClass.getDeclaredConstructor().newInstance(); // fail fast

    // send iospMessage before iosp is opened
    if (iospMessage != null) {
      spi.sendIospMessage(iospMessage);
    }

    if (bufferSize <= 0) {
      bufferSize = default_buffersize;
    }

    String urlCanonical = URLnaming.canonicalizeUriString(location);
    RandomAccessFile raf = new RandomAccessFile(urlCanonical, "r", bufferSize);

    CdmFile result = build(spi, raf, location, cancelTask);
    spi.buildFinish(result);

    // send after iosp is opened
    if (iospMessage != null) {
      spi.sendIospMessage(iospMessage);
    }

    return result;
  }

  /**
   * Find out if the location can be opened, but dont actually open it.
   * <p>
   * In order for a location to be openable by netCDF-java, we must have 1) a proper
   * {@link RandomAccessFile} implementation, and 2) a proper {@link IOServiceProvider}
   * implementation.
   *
   * @param location location of file
   * @return true if can be opened
   */
  public static boolean canOpen(String location) {
    boolean canOpen = false;
    // do we have a RandomAccessFile class that will work?
    try (RandomAccessFile raf = getRaf(location, -1)) {
      log.info(String.format("%s can be accessed with %s", raf.getLocation(), raf.getClass()));
      // do we have an appropriate IOSP?
      IOServiceProvider iosp = getIosp(raf);
      if (iosp != null) {
        canOpen = true;
        log.info(String.format("%s can be opened by %s", raf.getLocation(), iosp.getClass()));
      }
    } catch (IOException e) {
      return false;
    }
    return canOpen;
  }

  private static RandomAccessFile getRaf(String location, int bufferSize) throws IOException {
    String uriString = location.trim();
    if (bufferSize <= 0) {
      bufferSize = default_buffersize;
    }

    RandomAccessFile raf = null;

    // look for dynamically loaded RandomAccessFile Providers
    for (RandomAccessFileProvider provider : ServiceLoader.load(RandomAccessFileProvider.class)) {
      if (provider.isOwnerOf(location)) {
        raf = provider.open(location, bufferSize);
        Preconditions.checkNotNull(raf);
        if (looksCompressed(uriString)) {
          raf = downloadAndDecompress(raf, uriString, bufferSize);
        }
        break;
      }
    }

    if (raf == null) {
      // ok, treat as a local file
      // get rid of crappy microsnot \ replace with happy /
      uriString = StringUtil2.replace(uriString, '\\', "/");

      if (uriString.startsWith("file:")) {
        // uriString = uriString.substring(5);
        uriString = StringUtil2.unescape(uriString.substring(5)); // 11/10/2010 from erussell@ngs.org
      }

      String uncompressedFileName = null;
      if (looksCompressed(uriString)) {
        try {
          // Avoid race condition where the decompressed file is trying to be read by one
          // thread while another is decompressing it
          stringLocker.control(uriString);
          uncompressedFileName = Uncompress.makeUncompressedFile(uriString);
        } catch (Exception e) {
          log.warn("Failed to uncompress {}, err= {}; try as a regular file.", uriString, e.getMessage());
          // allow to fall through to open the "compressed" file directly - may be a misnamed suffix
        } finally {
          stringLocker.release(uriString);
        }
      }

      if (uncompressedFileName != null) {
        // open uncompressed file as a RandomAccessFile.
        raf = new RandomAccessFile(uncompressedFileName, "r", bufferSize);
      } else {
        // normal case - not compressed
        raf = new RandomAccessFile(uriString, "r", bufferSize);
      }
    }
    return raf;
  }

  private static boolean looksCompressed(String filename) {
    int pos = filename.lastIndexOf('.');
    boolean looksCompressed = false;
    if (pos > 0) {
      String suffix = filename.substring(pos + 1).toLowerCase();
      looksCompressed = possibleCompressedSuffixes.stream().anyMatch(suffix::equalsIgnoreCase);
    }
    return looksCompressed;
  }

  private static RandomAccessFile downloadAndDecompress(RandomAccessFile raf,
                                                        String uriString, int bufferSize) throws IOException {
    int pos = uriString.lastIndexOf('/');

    if (pos < 0)
      pos = uriString.lastIndexOf(':');

    String tmp = System.getProperty("java.io.tmpdir");
    String filename = uriString.substring(pos + 1);
    String sep = File.separator;

    uriString = DiskCache.getFileStandardPolicy(tmp + sep + filename).getPath();
    copy(raf, new FileOutputStream(uriString), 1 << 20);
    try {
      String uncompressedFileName = Uncompress.makeUncompressedFile(uriString);
      return new RandomAccessFile(uncompressedFileName, "r", bufferSize);
    } catch (Exception e) {
      throw new IOException();
    }
  }

  private static void copy(RandomAccessFile in, OutputStream out, int bufferSize) throws IOException {
    long length = in.length();
    byte[] buffer = new byte[bufferSize];
    int bytesRead = 0;
    while (length > 0) {
      if (length > bufferSize) {
        in.readFully(buffer, 0, bufferSize);
        bytesRead = bufferSize;
      } else if (length <= bufferSize) {
        in.readFully(buffer, 0, (int) length);
        bytesRead = (int) length;
      }
      length -= bufferSize;
      out.write(buffer, 0, bytesRead);
    }
  }


  //////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Read a local CDM file into memory. All reads are then done from memory.
   *
   * @param filename location of CDM file, must be a local file.
   * @return a CdmFile, which is completely in memory
   * @throws IOException if error reading file
   */
  public static CdmFile openInMemory(String filename) throws IOException {
    File file = new File(filename);
    ByteArrayOutputStream bos = new ByteArrayOutputStream((int) file.length());
    try (InputStream in = new BufferedInputStream(new FileInputStream(filename))) {
      IO.copy(in, bos);
    }
    return openInMemory(filename, bos.toByteArray());
  }

  /**
   * Read a remote CDM file into memory. All reads are then done from memory.
   *
   * @param uri location of CDM file, must be accessible through url.toURL().openStream().
   * @return a CdmFile, which is completely in memory
   * @throws IOException if error reading file
   */
  public static CdmFile openInMemory(URI uri) throws IOException {
    URL url = uri.toURL();
    byte[] contents;
    try (InputStream in = url.openStream()) {
      contents = IO.readContentsToByteArray(in);
    }
    return openInMemory(uri.toString(), contents);
  }

  /**
   * Open an in-memory netcdf file.
   *
   * @param name name of the dataset. Typically use the filename or URI.
   * @param data in-memory netcdf file
   * @return memory-resident CdmFile
   */
  public static CdmFile openInMemory(String name, byte[] data) throws IOException {
    InMemoryRandomAccessFile raf = new InMemoryRandomAccessFile(name, data);
    return open(raf, name, null, null);
  }

  /**
   * Open an in-memory netcdf file, with a specific iosp.
   *
   * @param name          name of the dataset. Typically use the filename or URI.
   * @param data          in-memory netcdf file
   * @param iospClassName fully qualified class name of the IOSP class to handle this file
   * @return CdmFile object, or null if cant find IOServiceProver
   */
  public static CdmFile openInMemory(String name, byte[] data, String iospClassName)
          throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException,
          InvocationTargetException {

    InMemoryRandomAccessFile raf = new InMemoryRandomAccessFile(name, data);
    Class<?> iospClass = CdmFile.class.getClassLoader().loadClass(iospClassName);
    IOServiceProvider spi = (IOServiceProvider) iospClass.getDeclaredConstructor().newInstance();

    return build(spi, raf, name, null);
  }

  /**
   * Open a RandomAccessFile as a CdmFile, if possible.
   *
   * @param raf         The open raf; is closed by this method onlu on IOException.
   * @param location    human readable location of this dataset.
   * @param cancelTask  used to monitor user cancellation; may be null.
   * @param iospMessage send this message to iosp; may be null.
   * @return CdmFile or throw an Exception.
   */
  public static CdmFile open(RandomAccessFile raf, String location,
                             @Nullable CancelTask cancelTask, @Nullable Object iospMessage) throws IOException {

    IOServiceProvider spi = getIosp(raf);
    if (spi == null) {
      raf.close();
      throw new IOException("Cant read " + location + ": not a valid CDM file.");
    }

    // send iospMessage before the iosp is opened
    if (iospMessage != null) {
      spi.sendIospMessage(iospMessage);
    }

    CdmFile ncfile = build(spi, raf, location, cancelTask);
    spi.buildFinish(ncfile);

    // send iospMessage after iosp is opened
    if (iospMessage != null) {
      spi.sendIospMessage(iospMessage);
    }

    return ncfile;
  }

  @Nullable
  private static IOServiceProvider getIosp(RandomAccessFile raf) throws IOException {
    if (CdmFile.debugSPI)
      log.info("CdmFile try to open = {}", raf.getLocation());

    // Registered providers override defaults.
    for (IOServiceProvider registeredSpi : registeredProviders) {
      if (CdmFile.debugSPI) {
        log.info(" try iosp = {}", registeredSpi.getClass().getName());
      }

      if (registeredSpi.isValidFile(raf)) {
        // need a new instance for thread safety
        Class<?> c = registeredSpi.getClass();
        try {
          return (IOServiceProvider) c.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
          throw new IOException("IOServiceProvider failed for " + c.getName(), e);
        }
      }
    }

    // look for dynamically loaded IOSPs
    for (IOServiceProvider loadedSpi : ServiceLoader.load(IOServiceProvider.class)) {
      if (loadedSpi.isValidFile(raf)) {
        Class<?> c = loadedSpi.getClass();
        try {
          return (IOServiceProvider) c.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
          throw new IOException("IOServiceProvider ServiceLoader failed for " + c.getName(), e);
        }
      }
    }
    return null;
  }

  public static CdmFile build(IOServiceProvider spi, RandomAccessFile raf, String location,
                              CancelTask cancelTask) throws IOException {

    CdmFile.Builder<?> builder = CdmFile.builder().setIosp(spi).setLocation(location);

    try {
      Group.Builder root = Group.builder().setName("");
      spi.build(raf, root, cancelTask);
      builder.setRootGroup(root);

      String id = root.getAttributeContainer().findAttributeString("_Id", null);
      if (id != null) {
        builder.setId(id);
      }
      String title = root.getAttributeContainer().findAttributeString("_Title", null);
      if (title != null) {
        builder.setTitle(title);
      }

    } catch (IOException | RuntimeException e) {
      try {
        raf.close();
      } catch (Throwable ignored) {
      }
      try {
        spi.close();
      } catch (Throwable ignored) {
      }
      throw e;

    } catch (Throwable t) {
      try {
        spi.close();
      } catch (Throwable ignored) {
      }
      try {
        raf.close();
      } catch (Throwable ignored) {
      }
      throw new RuntimeException(t);
    }

    return builder.build();
  }

  ///////////////////////////////////////////////////////////////////////
  // All CDM naming convention enforcement are here.

  // reservedFullName defines the characters that must be escaped
  // when a short name is inserted into a full name
  public static final String reservedFullName = ".\\";

  // reservedSectionSpec defines the characters that must be escaped
  // when a short name is inserted into a section specification.
  private static final String reservedSectionSpec = "();,.\\";

  // reservedSectionCdl defines the characters that must be escaped
  // when what?
  private static final String reservedCdl = "[ !\"#$%&'()*,:;<=>?[]^`{|}~\\";

  /**
   * Create a valid CDM object name.
   * Control chars (&lt; 0x20) are not allowed.
   * Trailing and leading blanks are not allowed and are stripped off.
   * A space is converted into an underscore "_".
   * A forward slash "/" is converted into an underscore "_".
   *
   * @param shortName from this name
   * @return valid CDM object name
   */
  public static String makeValidCdmObjectName(String shortName) {
    if (shortName == null) {
      return null;
    }
    return StringUtil2.makeValidCdmObjectName(shortName);
  }

  /**
   * Escape special characters in a netcdf short name when
   * it is intended for use in CDL.
   *
   * @param vname the name
   * @return escaped version of it
   */
  public static String makeValidCDLName(String vname) {
    return EscapeStrings.backslashEscape(vname, reservedCdl);
  }

  /**
   * Escape special characters in a netcdf short name when
   * it is intended for use in a fullname
   *
   * @param vname the name
   * @return escaped version of it
   */
  public static String makeValidPathName(String vname) {
    return EscapeStrings.backslashEscape(vname, reservedFullName);
  }

  /**
   * Escape special characters in a netcdf short name when
   * it is intended for use in a sectionSpec
   *
   * @param vname the name
   * @return escaped version of it
   */
  static String makeValidSectionSpecName(String vname) {
    return EscapeStrings.backslashEscape(vname, reservedSectionSpec);
  }

  /**
   * Unescape any escaped characters in a name.
   *
   * @param vname the escaped name
   * @return unescaped version of it
   */
  public static String makeNameUnescaped(String vname) {
    return EscapeStrings.backslashUnescape(vname);
  }

  /**
   * Create a Groups's full name with appropriate backslash escaping.
   */
  public static String makeFullName(Group g) {
    Group parent = g.getParentGroup();
    String groupName = EscapeStrings.backslashEscape(g.getShortName(), reservedFullName);
    if ((parent == null) || parent.isRoot()) // common case?
      return groupName;
    StringBuilder sbuff = new StringBuilder();
    appendGroupName(sbuff, parent, reservedFullName);
    sbuff.append(groupName);
    return sbuff.toString();
  }

  /**
   * Create a Variable's full name with appropriate backslash escaping.
   * Warning: do not use for a section spec.
   *
   * @param v the Variable
   * @return full name
   */
  public static String makeFullName(Variable v) {
    return makeFullName(v, reservedFullName);
  }

  /**
   * Create a Variable's full name with
   * appropriate backslash escaping for use in a section spec.
   *
   * @param v the cdm node
   * @return full name
   */
  public static String makeFullNameSectionSpec(Variable v) {
    return makeFullName(v, reservedSectionSpec);
  }

  /**
   * Given a Variable, create its full name with
   * appropriate backslash escaping of the specified characters.
   *
   * @param node          the cdm node
   * @param reservedChars the set of characters to escape
   * @return full name
   */
  private static String makeFullName(Variable node, String reservedChars) {
    Group parent = node.getParentGroup();
    if (((parent == null) || parent.isRoot()) && !node.isMemberOfStructure()) { // common case?
      return EscapeStrings.backslashEscape(node.getShortName(), reservedChars);
    }
    StringBuilder sbuff = new StringBuilder();
    appendGroupName(sbuff, parent, reservedChars);
    appendStructureName(sbuff, node, reservedChars);
    return sbuff.toString();
  }

  private static void appendGroupName(StringBuilder sbuff, Group g, String reserved) {
    if (g == null) {
      return;
    }
    if (g.getParentGroup() == null) {
      return;
    }
    appendGroupName(sbuff, g.getParentGroup(), reserved);
    sbuff.append(EscapeStrings.backslashEscape(g.getShortName(), reserved));
    sbuff.append("/");
  }

  private static void appendStructureName(StringBuilder sbuff, Variable n, String reserved) {
    if (n == null) {
      return;
    }
    if (n.isMemberOfStructure()) {
      appendStructureName(sbuff, n.getParentStructure(), reserved);
      sbuff.append(".");
    }
    sbuff.append(EscapeStrings.backslashEscape(n.getShortName(), reserved));
  }

  /**
   * Create a synthetic full name from a group plus a string
   *
   * @param parent parent group
   * @param name   synthetic name string
   * @return synthetic name
   */
  protected static String makeFullNameWithString(Group parent, String name) {
    name = makeValidPathName(name); // escape for use in full name
    StringBuilder sbuff = new StringBuilder();
    appendGroupName(sbuff, parent, null);
    sbuff.append(name);
    return sbuff.toString();
  }

}
