/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.core.util;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** Static String utilities. Replace with standard java library when possible. */
public class StringUtil2 {

  public static String classShortName(Class<?> classz) {
    return Iterables.getLast(Splitter.on('.').trimResults().omitEmptyStrings().split(classz.getName()));
  }

  /**
   * Delete any non-printable characters
   *
   * @param h byte array
   * @return cleaned up string
   */
  public static String cleanup(byte[] h) {
    byte[] bb = new byte[h.length];
    int count = 0;
    for (byte b : h) {
      if (b >= 32 && b < 127)
        bb[count++] = b;
    }
    return new String(bb, 0, count, StandardCharsets.UTF_8);
  }

  // remove leading and trailing blanks
  // remove control characters (< 0x20)
  // transform "/" to "_"
  // transform embedded space to "_"
  public static String makeValidCdmObjectName(String name) {
    name = name.trim();
    // common case no change
    boolean ok = true;
    for (int i = 0; i < name.length(); i++) {
      int c = name.charAt(i);
      if (c < 0x20)
        ok = false;
      if (c == '/')
        ok = false;
      if (c == ' ')
        ok = false;
      if (!ok)
        break;
    }
    if (ok)
      return name;

    StringBuilder sbuff = new StringBuilder(name.length());
    for (int i = 0, len = name.length(); i < len; i++) {
      int c = name.charAt(i);
      if ((c == '/') || (c == ' '))
        sbuff.append('_');
      else if (c >= 0x20)
        sbuff.append((char) c);
    }
    return sbuff.toString();
  }

  /**
   * Remove all occurrences of the substring sub in the string s.
   *
   * @param s operate on this string
   * @param sub remove all occurrences of this substring.
   * @return result with substrings removed
   */
  public static String remove(String s, String sub) {
    int len = sub.length();
    int pos;
    while (0 <= (pos = s.indexOf(sub))) {
      s = s.substring(0, pos) + s.substring(pos + len);
    }
    return s;
  }

  /**
   * Remove all occurrences of the character c in the string s.
   *
   * @param s operate on this string
   * @param c remove all occurrences of this character.
   * @return result with any character c removed
   */
  public static String remove(String s, int c) {
    if (0 > s.indexOf(c)) { // none
      return s;
    }

    StringBuilder buff = new StringBuilder(s);
    int i = 0;
    while (i < buff.length()) {
      if (buff.charAt(i) == c) {
        buff.deleteCharAt(i);
      } else {
        i++;
      }
    }
    return buff.toString();
  }

  /**
   * Remove all occurrences of the character c at the end of s.
   *
   * @param s operate on this string
   * @param c remove all occurrences of this character that are at the end of the string.
   * @return result with any character c removed
   */
  public static String removeFromEnd(String s, int c) {
    if (0 > s.indexOf(c)) // none
      return s;

    int len = s.length();
    while ((s.charAt(len - 1) == c) && (len > 0))
      len--;

    if (len == s.length())
      return s;
    return s.substring(0, len);
  }

  /**
   * Collapse continuous whitespace into one single " ".
   *
   * @param s operate on this string
   * @return result with collapsed whitespace
   */
  public static String collapseWhitespace(String s) {
    int len = s.length();
    StringBuilder b = new StringBuilder(len);
    for (int i = 0; i < len; i++) {
      char c = s.charAt(i);
      if (!Character.isWhitespace(c)) {
        b.append(c);
      } else {
        b.append(' ');
        while ((i + 1 < len) && Character.isWhitespace(s.charAt(i + 1))) {
          i++; /// skip further whitespace
        }
      }
    }
    return b.toString();
  }

  /**
   * Replace any char "out" in s with "in".
   *
   * @param s string to replace
   * @param out replace this character
   * @param in with this string
   * @return modified string if needed
   */
  public static String replace(String s, char out, String in) {
    if (s.indexOf(out) < 0) {
      return s;
    }

    // gotta do it
    StringBuilder sb = new StringBuilder(s);
    replace(sb, out, in);
    return sb.toString();
  }


  /**
   * Replace all occurrences of any char in replaceChar with corresponding String in replaceWith
   *
   * @param x operate on this string
   * @param replaceChar get rid of these
   * @param replaceWith replace with these
   * @return resulting string
   */
  public static String replace(String x, char[] replaceChar, String[] replaceWith) {
    // common case no replacement
    boolean ok = true;
    for (char aReplaceChar : replaceChar) {
      int pos = x.indexOf(aReplaceChar);
      ok = (pos < 0);
      if (!ok)
        break;
    }
    if (ok)
      return x;

    // gotta do it
    StringBuilder sb = new StringBuilder(x);
    for (int i = 0; i < replaceChar.length; i++) {
      int pos = x.indexOf(replaceChar[i]);
      if (pos >= 0) {
        replace(sb, replaceChar[i], replaceWith[i]);
      }
    }

    return sb.toString();
  }

  /**
   * Replaces all occurrences of "pattern" in "string" with "value"
   *
   * @param string string to munge
   * @param pattern pattern to replace
   * @param value replacement value
   * @return munged string
   */
  public static String replace(String string, String pattern, String value) {
    if (pattern.isEmpty())
      return string;

    if (!string.contains(pattern))
      return string;

    // ok gotta do it
    StringBuilder returnValue = new StringBuilder();
    int patternLength = pattern.length();
    while (true) {
      int idx = string.indexOf(pattern);
      if (idx < 0)
        break;

      returnValue.append(string, 0, idx);
      if (value != null)
        returnValue.append(value);

      string = string.substring(idx + patternLength);
    }
    returnValue.append(string);
    return returnValue.toString();
  }

  /**
   * This finds any '%xx' and converts to the equivalent char. Inverse of
   * escape().
   *
   * @param x operate on this String
   * @return original String.
   */
  public static String unescape(String x) {
    if (x.indexOf('%') < 0) {
      return x;
    }

    // gotta do it
    char[] b = new char[2];
    StringBuilder sb = new StringBuilder(x);
    for (int pos = 0; pos < sb.length(); pos++) {
      char c = sb.charAt(pos);
      if (c != '%') {
        continue;
      }
      if (pos >= sb.length() - 2) { // malformed - should be %xx
        return x;
      }
      b[0] = sb.charAt(pos + 1);
      b[1] = sb.charAt(pos + 2);
      int value;
      try {
        value = Integer.parseInt(new String(b), 16);
      } catch (NumberFormatException e) {
        continue; // not a hex number
      }
      c = (char) value;
      sb.setCharAt(pos, c);
      sb.delete(pos + 1, pos + 3);
    }

    return sb.toString();
  }

  /**
   * Split a string on whitespace.
   *
   * @param source split this string
   * @return tokens that were seperated by whitespace, trimmed
   */
  public static List<String> splitList(String source) {
    return ImmutableList.copyOf(split(source));
  }

  /**
   * Split a string on whitespace.
   *
   * @param source split this string
   * @return tokens that were seperated by whitespace, trimmed
   */
  public static Iterable<String> split(String source) {
    return Splitter.on(CharMatcher.whitespace()).omitEmptyStrings().trimResults().split(source);
  }

  ////////////////////////////////////////////////////
  // StringBuilder

  /**
   * Remove any of the characters in out from sb
   *
   * @param sb the StringBuilder
   * @param out get rid of any of these characters
   */
  public static void removeAll(StringBuilder sb, String out) {
    int i = 0;
    while (i < sb.length()) {
      int c = sb.charAt(i);
      boolean ok = true;
      for (int j = 0; j < out.length(); j++) {
        if (out.charAt(j) == c) {
          sb.delete(i, i + 1);
          ok = false;
          break;
        }
      }
      if (ok)
        i++;
    }
  }

  /**
   * Replace any char "out" in sb with String "in".
   *
   * @param sb StringBuilder to replace
   * @param out repalce this character
   * @param in with this string
   */
  public static void replace(StringBuilder sb, char out, String in) {
    for (int i = 0; i < sb.length(); i++) {
      if (sb.charAt(i) == out) {
        sb.replace(i, i + 1, in);
        i += in.length() - 1;
      }
    }
  }

  /**
   * Replace any of the characters from out with corresponding character from in
   *
   * @param sb the StringBuilder
   * @param out get rid of any of these characters
   * @param in replacing with the character at same index
   */
  public static void replace(StringBuilder sb, String out, String in) {
    for (int i = 0; i < sb.length(); i++) {
      int c = sb.charAt(i);
      for (int j = 0; j < out.length(); j++) {
        if (out.charAt(j) == c)
          sb.setCharAt(i, in.charAt(j));
      }
    }
  }

  /**
   * Find all occurences of the "match" in original, and substitute the "subst" string,
   * directly into the original.
   *
   * @param sbuff starting string buffer
   * @param match string to match
   * @param subst string to substitute
   */
  public static void substitute(StringBuilder sbuff, String match, String subst) {
    int pos, fromIndex = 0;
    int substLen = subst.length();
    int matchLen = match.length();
    while (0 <= (pos = sbuff.indexOf(match, fromIndex))) {
      sbuff.replace(pos, pos + matchLen, subst);
      fromIndex = pos + substLen; // make sure dont get into an infinite loop
    }
  }
}
