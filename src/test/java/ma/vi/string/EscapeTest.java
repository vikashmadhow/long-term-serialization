/*
 * Copyright (c) 2018 Vikash Madhow
 */

package ma.vi.string;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test for escaping strings.
 *
 * @author Vikash Madhow (vikash.madhow@gmail.com)
 */
public class EscapeTest {
  private String toEscape = "[,]\\";
  private Escape esc;

  @Before
  public void init() {
    esc = new Escape(toEscape);
  }

  @Test
  public void escape() throws Exception {
    assertEquals(esc.escape(null), null);
    assertEquals(esc.escape(""), "");
    assertEquals(esc.escape(" [ ] , \\ "), " \\[ \\] \\, \\\\ ");
  }

  @Test
  public void remap() throws Exception {
    assertEquals(esc.map(null), null);
    assertEquals(esc.map(""), "");
    assertEquals(esc.map(" \\[ \\] \\, \\\\ "), " \ue000 \ue002 \ue001 \ue003 ");
    assertEquals(esc.map(" \\[ \\] \\, \\\\ \\"), " \ue000 \ue002 \ue001 \ue003 \\");
    assertEquals(esc.map(" \\[ \\] \\, \\\\ \\c"), " \ue000 \ue002 \ue001 \ue003 \\c");
  }

  @Test
  public void unescape() throws Exception {
    assertEquals(esc.demap(null), null);
    assertEquals(esc.demap(""), "");
    assertEquals(esc.demap(" \ue000 \ue002 \ue001 \ue003 "), " [ ] , \\ ");
  }
}