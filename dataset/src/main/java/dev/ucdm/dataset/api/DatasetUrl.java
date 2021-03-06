/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.dataset.api;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import dev.ucdm.core.http.HttpService;
import dev.ucdm.core.util.EscapeStrings;
import dev.ucdm.core.util.StringUtil2;

import org.jetbrains.annotations.Nullable;
import dev.ucdm.array.Immutable;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Detection of the protocol from a location string. Contacts the server if necessary to disambiguate, eg opendap
 * from plain http.
 * TODO rewrite
 */
@Immutable
public class DatasetUrl {
  private static final String alpha = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
  private static final String slashalpha = "\\/" + alpha;

  // TODO clean up this mess
  private static final String[] FRAGPROTOCOLS =
      {"dap4", "dap2", "dods", "cdmremote", "thredds", "ncml", "cdmr", "gcdm"};

  public enum ServiceType { GCDM, HTTPServer, NCML }

  /**
   * This creates a DatasetUrl, figures out the ServiceType if possible, and canonicalizes the URL string.
   * 
   * @param orgLocation TODO define syntax.
   */
  public static DatasetUrl findDatasetUrl(String orgLocation) throws IOException {
    ServiceType serviceType = null;

    // Canonicalize the location
    String location = StringUtil2.replace(orgLocation.trim(), '\\', "/");
    List<String> allProtocols = getProtocols(location);

    String trueUrl = location;
    String leadProtocol;
    if (allProtocols.isEmpty()) {
      leadProtocol = "file"; // The location has no leading protocols, assume file:
    } else {
      leadProtocol = allProtocols.get(0);
    }

    // Priority in deciding
    // the service type is as follows.
    // 1. "protocol" tag in fragment
    // 2. specific protocol in fragment
    // 3. leading protocol
    // 4. path extension
    // 5. contact the server (if defined)

    // temporarily remove any trailing query or fragment
    String fragment = null;
    int pos = trueUrl.lastIndexOf('#');
    if (pos >= 0) {
      fragment = trueUrl.substring(pos + 1);
      trueUrl = trueUrl.substring(0, pos);
    }
    pos = location.lastIndexOf('?');
    String query = null;
    if (pos >= 0) {
      query = trueUrl.substring(pos + 1);
      trueUrl = trueUrl.substring(0, pos);
    }
    if (fragment != null) {
      serviceType = searchFragment(fragment);
    }

    if (serviceType == null) {// See if leading protocol tells us how to interpret
      serviceType = decodeLeadProtocol(leadProtocol);
    }

    if (serviceType == null) {// See if path tells us how to interpret
      serviceType = searchPath(trueUrl);
    }

    if (serviceType == null) {
      // There are several possibilities at this point; all of which
      // require further info to disambiguate
      // - we have file://<path> or file:<path>; we need to see if
      // the extension can help, otherwise, start defaulting.
      // - we have a simple url: e.g. http://... ; contact the server
      if (leadProtocol.equals("file")) {
        serviceType = decodePathExtension(trueUrl); // look at the path extension
        if (serviceType == null && checkIfNcml(new File(location))) {
          serviceType = ServiceType.NCML;
        }
      } else {
        serviceType = disambiguateHttp(trueUrl);
        // special cases
        if ((serviceType == null || serviceType == ServiceType.HTTPServer)) {
          // ncml file being served over http?
          if (checkIfRemoteNcml(trueUrl)) {
            serviceType = ServiceType.NCML;
          }
        }
      }
    }

    if (serviceType == ServiceType.NCML) { // ??
      // If lead protocol was null, then pretend it was a file
      // Note that technically, this should be 'file://'
      trueUrl = (allProtocols.isEmpty() ? "file:" + trueUrl : trueUrl);
    }

    // Add back the query and fragment (if any)
    if (query != null || fragment != null) {
      StringBuilder buf = new StringBuilder(trueUrl);
      if (query != null) {
        buf.append('?');
        buf.append(query);
      }
      if (fragment != null) {
        buf.append('#');
        buf.append(fragment);
      }
      trueUrl = buf.toString();
    }
    return create(serviceType, trueUrl);
  }

  /**
   * Return the set of leading protocols for a url; may be more than one.
   * Watch out for Windows paths starting with a drive letter => protocol
   * names must all have a length > 1.
   * Watch out for '::'
   * Each captured protocol is saved without trailing ':'
   * Assume: the protocols MUST be terminated by the occurrence of '/'.
   *
   * @param url the url whose protocols to return
   * @return list of leading protocols without the trailing :
   */
  @VisibleForTesting
  static List<String> getProtocols(String url) {
    List<String> allprotocols = new ArrayList<>(); // all leading protocols upto path or host

    // Note, we cannot use split because of the context sensitivity
    // This code is quite ugly because of all the confounding cases
    // (e.g. windows path, embedded colons, etc.).
    // Specifically, the 'file:' protocol is a problem because
    // it has so many non-standard forms such as file:x/y file://x/y file:///x/y.
    StringBuilder buf = new StringBuilder(url);
    // If there are any leading protocols, then they must stop at the first '/'.
    int slashpos = buf.indexOf("/");
    // Check special case of file:<path> with no slashes after file:
    if (url.startsWith("file:") && "/\\".indexOf(url.charAt(5)) < 0) {
      allprotocols.add("file");
    } else if (slashpos >= 0) {
      // Remove everything after the first slash
      buf.delete(slashpos + 1, buf.length());
      int index = buf.indexOf(":");
      while (index > 0) {
        // Validate protocol
        if (!validateProtocol(buf, 0, index))
          break;
        String protocol = buf.substring(0, index); // not including trailing ':'
        allprotocols.add(protocol);
        buf.delete(0, index + 1); // remove the leading protocol
        index = buf.indexOf(":");
      }
    }
    return allprotocols;
  }

  // Eliminate windows drive letters.
  // "protocol:" must be followed by alpha or "/"
  private static boolean validateProtocol(StringBuilder buf, int startpos, int endpos) {
    int len = endpos - startpos;
    if (len == 0) {
      return false;
    }
    char cs = buf.charAt(startpos);
    char ce1 = buf.charAt(endpos + 1);
    if (len == 1 && alpha.indexOf(cs) >= 0 && (ce1 == '/' || ce1 == '\\')) {
      return false; // looks like windows drive letter
    }
    // If trailing colon is not followed by alpha or /, then assume not url
    return slashalpha.indexOf(ce1) >= 0;
  }

  /**
   * Given a location, find markers indicated which protocol to use
   * TODO: what use case is this handling ?
   *
   * @param fragment the fragment is to be examined
   * @return The discovered ServiceType, or null
   */
  @Nullable
  private static ServiceType searchFragment(String fragment) {
    if (fragment.isEmpty())
      return null;
    Map<String, String> map = parseFragment(fragment);
    if (map == null)
      return null;
    String protocol = map.get("protocol");

    if (protocol == null) {
      for (String p : FRAGPROTOCOLS) {
        if (map.get(p) != null) {
          protocol = p;
          break;
        }
      }
    }
    if (protocol != null) {
      if (protocol.equalsIgnoreCase("ncml"))
        return ServiceType.NCML;
    }
    return null;
  }

  /**
   * Given the fragment part of a url, see if it
   * parses as name=value pairs separated by '&'
   * (same as query part).
   *
   * @param fragment the fragment part of a url
   * @return a map of the name value pairs (possibly empty),
   *         or null if the fragment does not parse.
   */
  private static Map<String, String> parseFragment(String fragment) {
    Map<String, String> map = new HashMap<>();
    if (fragment != null && fragment.length() > 0) {
      if (fragment.charAt(0) == '#') {
        fragment = fragment.substring(1);
      }
      String[] pairs = fragment.split("[ \t]*[&][ \t]*");
      for (String pair : pairs) {
        String[] pieces = pair.split("[ \t]*[=][ \t]*");
        switch (pieces.length) {
          case 1:
            map.put(EscapeStrings.urlDecode(pieces[0]).toLowerCase(), "true");
            break;
          case 2:
            map.put(EscapeStrings.urlDecode(pieces[0]).toLowerCase(), EscapeStrings.urlDecode(pieces[1]).toLowerCase());
            break;
          default:
            return null; // does not parse
        }
      }
    }
    return map;
  }

  /**
   * Check path extension; assumes no query or fragment
   *
   * @param path the path to examine for extension
   * @return ServiceType inferred from the extension or null
   */
  private static ServiceType decodePathExtension(String path) {
    if (path.endsWith(".xml") || path.endsWith(".ncml"))
      return ServiceType.NCML;
    return null;
  }

  private static ServiceType searchPath(String url) {
    return null;
  }

  /*
   * Attempt to map a leading url protocol url to a service type (see thredds.catalog.ServiceType).
   * Possible service types should include at least the following.
   * <ol>
   * <li> OPENDAP (DAP2 protocol)
   * <li> DAP4 (DAP4 protocol)
   * <li> CdmRemote (remote ncstream)
   * </ol>
   *
   * @param protocol The leading protocol
   *
   * @return ServiceType indicating how to handle the url, or null.
   */
  private static ServiceType decodeLeadProtocol(String protocol) {
    switch (protocol) {
      case "httpserver":
      case "nodods":
        return ServiceType.HTTPServer;
    }

    return null;
  }

  //////////////////////////////////////////////////////////////////

  /**
   * If the URL alone is not sufficient to disambiguate the location,
   * then this method will attempt to do a specific kind of request on
   * the server, typically a HEAD call using the URL.
   * It finds the header "Content-Description"
   * and uses it value (e.g. "ncstream" or "dods", etc)
   * in order to disambiguate.
   *
   * @param location the url to disambiguate
   * @return ServiceType indicating how to handle the url
   */
  private static ServiceType disambiguateHttp(String location) throws IOException {
    return null;
  }

  // The first 128 bytes should contain enough info to tell if this looks like an actual ncml file or not.
  // For example, here is an example 128 byte response:
  // <?xml version="1.0" encoding="UTF-8"?>\n<netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2"
  // location="dods://ma
  private static final int NUM_BYTES_TO_DETERMINE_NCML = 128;

  private static boolean checkIfRemoteNcml(String location) throws IOException {
    if (!location.startsWith("http")) {
      return false;
    }

    if (decodePathExtension(location) == ServiceType.NCML) {
      // just because location ends with ncml does not mean it's ncml
      // if the ncml file is being served up via http by a remote server,
      // we should be able to read the first bit of it and see if it even
      // looks like an ncml file.
      HttpRequest request = HttpService.standardGetRequestBuilder(location).header("accept-encoding", "identity")
          .header("Range", String.format("bytes=%d-%d", 0, NUM_BYTES_TO_DETERMINE_NCML)).build();
      HttpResponse<String> response = HttpService.standardRequestForString(request);
      return checkIfNcml(response.toString());
    }

    return false;
  }

  private static boolean checkIfNcml(File file) throws IOException {
    if (!file.exists()) {
      return false;
    }

    try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file), NUM_BYTES_TO_DETERMINE_NCML)) {
      byte[] bytes = new byte[NUM_BYTES_TO_DETERMINE_NCML];
      int bytesRead = in.read(bytes);

      if (bytesRead <= 0) {
        return false;
      } else {
        return checkIfNcml(new String(bytes, 0, bytesRead));
      }
    }
  }

  private static boolean checkIfNcml(String string) {
    // Look for the ncml element as well as a reference to the ncml namespace URI.
    return string.contains("<netcdf ") && string.contains("unidata.ucar.edu/namespaces/netcdf/ncml");
  }

  /////////////////////////////////////////////////////////////////////
  private final @Nullable ServiceType serviceType;
  private final String trueurl;

  /**
   * Create a DatasetUrl, which annotates a url with its service type.
   * 
   * @param serviceType The serviceType, may be null if not known.
   * @param trueurl The actual URL string.
   */
  public static DatasetUrl create(@Nullable ServiceType serviceType, String trueurl) {
    return new DatasetUrl(serviceType, trueurl);
  }

  private DatasetUrl(ServiceType serviceType, String trueurl) {
    this.serviceType = serviceType;
    this.trueurl = Preconditions.checkNotNull(trueurl);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DatasetUrl that = (DatasetUrl) o;
    return serviceType == that.serviceType && Objects.equals(trueurl, that.trueurl);
  }

  @Override
  public int hashCode() {
    return Objects.hash(serviceType, trueurl);
  }

  /** The ServiceType, or null if not known. */
  @Nullable
  public ServiceType getServiceType() {
    return serviceType;
  }

  /** The actual URL string which you give to the service specified by getServiceType(). */
  public String getTrueurl() {
    return trueurl;
  }
}
