/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.cdm.core.api;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;

/**
 * List available charsets and fonts
 */
public class TestCharsets {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testCharsets() {
    boolean ok = true;
    Map<String, Charset> map = Charset.availableCharsets();
    System.out.println("Charsets:");
    for (String key : map.keySet()) {
      Charset cs = map.get(key);
      System.out.println(" " + cs);
      ok &= Charset.isSupported(key);
    }
    System.out.println("default= " + Charset.defaultCharset());
    assertThat(ok).isTrue();

    System.out.println("\nFont names:");
    GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
    for (String s : env.getAvailableFontFamilyNames()) {
      System.out.println(" " + s);
    }

    int c1 = 0x1f73;
    System.out.println("\nFonts:");
    for (Font f : env.getAllFonts()) {
      f.canDisplay(c1);
      System.out.println(f.canDisplay(c1) + " " + f.getFontName());
    }

  }


}
